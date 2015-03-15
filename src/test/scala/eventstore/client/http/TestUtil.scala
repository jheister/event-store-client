package eventstore.client.http

import java.util.UUID

import eventstore.client.Event
import net.liftweb.json.JsonAST.{JField, JInt, JObject}

object TestUtil {
  def randomStream() = "stream-" + UUID.randomUUID()

  def event(n: Int) = Event("Evt", JObject(List(JField("i", JInt(n)))))
}
