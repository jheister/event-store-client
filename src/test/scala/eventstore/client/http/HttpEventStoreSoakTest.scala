package eventstore.client.http

import eventstore.client.EventInStream
import eventstore.client.http.TestUtil.randomStream
import org.scalatest.{FunSpec, MustMatchers}

class HttpEventStoreSoakTest extends FunSpec with MustMatchers {
  val eventstore = new HttpEventStore("127.0.0.1", 2113, listener = SysOutLoggingListener, batchSize = 1000)

  it("can write and replay a large number of events") {
    val stream = randomStream()

    (1 to 100).foreach { x => eventstore.write(stream, (1 to 100).map(i => TestUtil.event(x * i)).toList) }

    val projection = eventstore.fromStream(stream).foldLeft(Projection())(_.apply(_))

    projection must be(Projection(10000))
  }
}

object SysOutLoggingListener extends HttpEventStoreListener {
  override def readSuccessful(stream: String, eventCount: Int, url: String): Unit = {
    println(s"Successful read request for $eventCount events from stream $stream")
  }

  override def writeSuccessful(stream: String, eventCount: Int, url: String, dataSize: Int): Unit = {
    println(s"Successful write request for $eventCount events to stream $stream")
  }
}

case class Projection(count: Int = 0) {
  def apply(evt: EventInStream) = copy(count = count + 1)
}
