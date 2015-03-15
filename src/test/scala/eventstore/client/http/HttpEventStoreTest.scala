package eventstore.client.http

import java.util.UUID

import eventstore.client.http.TestUtil.{event, randomStream}
import eventstore.client.{Event, EventInStream}
import net.liftweb.json.JsonAST.{JField, JObject, JString}
import org.mockito.Matchers.{anyInt, anyString}
import org.mockito.Mockito.{mock, times, verify}
import org.scalatest.{FunSpec, MustMatchers}

class HttpEventStoreTest extends FunSpec with MustMatchers {
  val eventstore = new HttpEventStore("127.0.0.1", 2113)

  def json(fields: (String, String)*) = JObject(fields.map(f => JField(f._1, JString(f._2))).toList)

  it("Can read & write events to a stream") {
    val fixedMessageId = UUID.randomUUID().toString
    val eventstore = new HttpEventStore("127.0.0.1", 2113, messageIdProvider = new MessageIdProvider {
      override def messageId() = fixedMessageId
    })

    val stream = randomStream()

    eventstore.write(stream, List(Event("hello", json("hello" -> "world"))))

    eventstore.fromStream(stream).toList must be(List(EventInStream(Event("hello", json("hello" -> "world")), stream, 0, fixedMessageId)))
  }

  it("executes fetches in the specified batch size") {
    val listener = mock(classOf[HttpEventStoreListener])
    val eventstore = new HttpEventStore("127.0.0.1", 2113, batchSize = 5, listener)

    val stream = randomStream()

    val events = (1 to 16).map(i  => Event("hello", json("event" -> i.toString))).toList

    eventstore.write(stream, events)

    eventstore.fromStream(stream).toList.size must be(16)
    verify(listener, times(5)).readSuccessful(anyString(), anyInt(), anyString())
  }

  it("lazily fetches events as iterator is evaluated") {
    val stream = randomStream()
    val listener = mock(classOf[HttpEventStoreListener])
    val eventstore = new HttpEventStore("127.0.0.1", 2113, batchSize = 5, listener)

    eventstore.write(stream, (1 to 20).map(i  => Event("hello", json("event" -> i.toString))).toList)

    val eventsIterator = eventstore.fromStream(stream)
    eventsIterator.take(6).toList
    verify(listener, times(2)).readSuccessful(anyString(), anyInt(), anyString())
  }

  it("can write events in batches") {
    val stream = randomStream()

    val events = List(Event("A", json("evt" -> "1")), Event("A", json("evt" -> "2")), Event("A", json("evt" -> "3")))
    eventstore.write(stream, events)

    eventstore.fromStream(stream).toList.map(_.event) must be(events)
  }

  it("succeeds in saving events if stream is at the expected version") {
    val stream = randomStream()
    eventstore.write(stream, List(event(1), event(2)))
    eventstore.write(stream, List(event(3)), expectedVersion = Some(1))

    eventstore.fromStream(stream).toList.map(_.event) must be(List(event(1), event(2), event(3)))
  }

  it("fails to save events if stream has moved beyond the expected version") {
    val stream = randomStream()
    val preExisting = List(Event("A", json("evt" -> "1")), Event("A", json("evt" -> "2")))
    eventstore.write(stream, preExisting)

    intercept[VersionMissmatch] {
      eventstore.write(stream, List(Event("A", json("evt" -> "3"))), expectedVersion = Some(0))
    }
  }

  it("fails to save events if stream is not yet at expected version") {
    val stream = randomStream()
    val preExisting = List(Event("A", json("evt" -> "1")), Event("A", json("evt" -> "2")))
    eventstore.write(stream, preExisting)

    intercept[VersionMissmatch] {
      eventstore.write(stream, List(Event("A", json("evt" -> "3"))), expectedVersion = Some(10))
    }
  }

  it("can read the stream from a specific version onwards") {
    val stream = randomStream();

    eventstore.write(stream, List(event(0), event(1), event(2), event(3)))

    eventstore.fromStream(stream, eventsAfter = Some(1)).toList.map(_.event) must be(List(event(2), event(3)))
  }
}
