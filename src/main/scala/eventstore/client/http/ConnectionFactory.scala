package eventstore.client.http

import java.net.{HttpURLConnection, URL}

object ConnectionFactory {
  def connectionFor(url: URL) = {
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoInput(true)
    connection.setDoOutput(true)
    connection.setInstanceFollowRedirects(false)
    connection.setUseCaches(false)
    connection.setRequestProperty("charset", "utf-8")
    connection.setConnectTimeout(10000)
    connection.setReadTimeout(10000)

    connection
  }
}
