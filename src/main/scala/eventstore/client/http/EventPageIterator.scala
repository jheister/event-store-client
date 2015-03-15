package eventstore.client.http

import java.io.InputStreamReader
import java.net.URL

import net.liftweb.json.{DefaultFormats, JsonParser}

class EventPageIterator(stream: String, startingUrl: String, listener: HttpEventStoreListener) extends Iterator[EventPage] {
  private var nextElem: Option[String] = Some(startingUrl)

  override def hasNext: Boolean = nextElem.isDefined

  override def next(): EventPage = nextElem match {
    case Some(url) => {
      val events = readEventStreamFor(url + "?embed=body")
      nextElem = events.linkFor.get("previous")
      events
    }
    case None => Iterator.empty.next()
  }

  private def readEventStreamFor(url: String): EventPage = {
    val connection = ConnectionFactory.connectionFor(new URL(url))
    connection.setRequestMethod("GET")
    connection.setRequestProperty("Accept", "application/json")

    val responseCode = connection.getResponseCode
    val responseMessage = connection.getResponseMessage

    if (responseCode != 200) {
      throw new RuntimeException("Failed to read events: " + responseCode + " " + responseMessage)
    }

    val json = JsonParser.parse(new InputStreamReader(connection.getInputStream), true)

    connection.disconnect()

    implicit val formats = DefaultFormats
    val page = json.extract[EventPage]

    listener.readSuccessful(stream, page.events.size, url)

    page
  }
}
