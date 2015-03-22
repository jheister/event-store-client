package eventstore.client.http

import java.io.PrintStream
import java.net.URL

import eventstore.client.http.ConnectionFactory.connectionFor

class Projections(hostname: String,
                  port: Int,
                  credentials: Option[Credentials]) {
  def create(name: String,
             content: String,
             emit: Boolean = false,
             checkpoints: Boolean = true,
             enabled: Boolean = true): Unit = {
    val connection = connectionFor(
      new URL(s"http://$hostname:$port/projections/continuous?name=$name&emit=${format(emit)}&checkpoints=${format(checkpoints)}&enabled=${format(enabled)}"),
      credentials
    )
    connection.setRequestMethod("POST")
    connection.setRequestProperty("Accept", "*/*")

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
