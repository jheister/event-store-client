package eventstore.client.http

import java.io.{PrintStream, OutputStream}
import java.net.{HttpURLConnection, URL}

import com.sun.org.apache.xml.internal.security.utils.Base64
import eventstore.client.http.ConnectionFactory.connectionFor
import eventstore.client.http.TestUtil.{event, randomStream}
import org.scalatest.{FunSpec, MustMatchers}

class HttpEventStoreIndexedStreamsTest extends FunSpec with MustMatchers {
  val eventstore = new HttpEventStore("127.0.0.1", 2113)

  it("can read an indexed stream") {
    val streamA = randomStream()
    val streamB = randomStream()
    val indexedAandB = randomStream()

    val projections = new Projections("localhost", 2113)

    projections.create("index-" + streamA + "-" + streamB,
      s"""
         |fromStreams(['$streamA','$streamB'])
         |  .when({
         |    $$any : function(s,e) {
         |      linkTo('$indexedAandB', e);
         |    }
         |  });
         |""".stripMargin, emit = true)

    eventstore.write(streamA, List(event(1), event(2)))
    eventstore.write(streamB, List(event(3), event(4)))

    eventstore.fromStream(indexedAandB).map(_.event).toList must be(List(event(1), event(2), event(3), event(4)))
  }
}

class Projections(hostname: String,
                        port: Int) {
  def create(name: String,
             content: String,
             emit: Boolean = false,
             checkpoints: Boolean = true,
             enabled: Boolean = true): Unit = {
    val connection = connectionFor(new URL(s"http://$hostname:$port/projections/continuous?name=$name&emit=${format(emit)}&checkpoints=${format(checkpoints)}&enabled=${format(enabled)}"))
    connection.setRequestMethod("POST")
    connection.setRequestProperty("Accept", "*/*")

    connection.setRequestProperty("Authorization", "Basic " + new String(Base64.encode("admin:changeit".getBytes("utf-8"))))

    val out = new PrintStream(connection.getOutputStream)

    try {
      out.println(content)

      connection.getResponseCode match {
        case 201 =>
        case code => throw new RuntimeException(s"Failed to create projection. Error $code: ${connection.getResponseMessage}");
      }
    } finally {
      out.close()
      connection.disconnect()
    }
  }

  private def format(bool: Boolean) = bool match {
    case true => "yes"
    case false => "no"
  }
}
