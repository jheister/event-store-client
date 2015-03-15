package eventstore.client

import net.liftweb.json.JsonAST.JValue

trait EventStore {
  def fromStream(stream: String, eventsAfter: Option[Long] = None): Iterator[EventInStream]

  def write(stream: String, event: List[Event], expectedVersion: Option[Long] = None): Unit
}

case class Event(evetyType: String, json: JValue)

case class EventInStream(event: Event, stream: String, version: Long, id: String)