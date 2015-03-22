package eventstore.client.http

import java.net.URL
import java.util.UUID

import eventstore.client.{EventInStream, Event, EventStore}
import net.liftweb.json.JsonAST.{JArray, JField, JObject, JString}
import net.liftweb.json.{JsonAST, JsonParser}

trait HttpEventStoreListener {
  def readSuccessful(stream: String, eventCount: Int, url: String): Unit

  def writeSuccessful(stream: String, eventCount: Int, url: String, dataSize: Int): Unit
}

object NoopListener extends HttpEventStoreListener {
  override def readSuccessful(stream: String, eventCount: Int, url: String): Unit = {}

  override def writeSuccessful(stream: String, eventCount: Int, url: String, dataSize: Int): Unit = {}
}

trait MessageIdProvider {
  def messageId(): String
}

object UUIDMessageIdProvider extends MessageIdProvider {
  override def messageId(): String = UUID.randomUUID().toString
}

class HttpEventStore(hostname: String,
                     port: Int,
                     batchSize: Int = 100,
                     listener: HttpEventStoreListener = NoopListener,
                     messageIdProvider: MessageIdProvider= UUIDMessageIdProvider) extends EventStore {
  override def fromStream(stream: String, eventsAfter: Option[Long] = None): Iterator[EventInStream] = {
    val startingVersion = eventsAfter.map(_ + 1).getOrElse(0)

    val startingUrl = s"http://$hostname:$port/streams/$stream/$startingVersion/forward/$batchSize"
    
    new EventIterator(new EventPageIterator(stream, startingUrl, listener))
  }

  override def write(stream: String, event: List[Event], expectedVersion: Option[Long]): Unit = {
    val json = JArray(event.map { evt =>
      JObject(List(
        JField("eventId", JString(messageIdProvider.messageId())),
        JField("eventType", JString(evt.evetyType)),
        JField("data", evt.json)
      ))
    })

    val data = JsonAST.compactRender(json).getBytes("utf-8")

    val url = "http://" + hostname + ":" + port + "/streams/" + stream

    val connection = ConnectionFactory.connectionFor(new URL(url))

    expectedVersion.foreach { v => connection.setRequestProperty("ES-ExpectedVersion", v.toString) }
    connection.setRequestProperty("Accept", "*/*")
    connection.setRequestProperty("Content-Type", "application/vnd.eventstore.events+json")
    connection.setRequestProperty("Content-Length", data.length.toString)

    val out = connection.getOutputStream

    try {
      out.write(data)

      connection.getResponseCode match {
        case 201 => {
          listener.writeSuccessful(stream, event.size, url, data.length)
          return
        };
        case 400 => throw new VersionMissmatch()
        case error => throw new RuntimeException("Failed to write event: " + error + " " + connection.getResponseMessage)
      }
    } finally {
      out.close()
      connection.disconnect()
    }
  }
}

case class EventEntry(eventId: String, eventType: String, eventNumber: Long, data: String, streamId: String) {
  def toEvent = EventInStream(Event(eventType, JsonParser.parse(data)), streamId, eventNumber, eventId)
}

case class Link(uri: String, relation: String)

case class EventPage(title: String,
                         links: List[Link],
                         entries: List[EventEntry]) {

  val linkFor = links.map(l => (l.relation, l.uri)).toMap

  val events = entries.map(_.toEvent).reverse
}

class VersionMissmatch extends RuntimeException