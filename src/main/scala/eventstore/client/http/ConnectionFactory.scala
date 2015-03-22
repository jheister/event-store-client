package eventstore.client.http

import java.net.{HttpURLConnection, URL}

import com.sun.org.apache.xml.internal.security.utils.Base64
import com.sun.org.apache.xml.internal.security.utils.Base64.encode

object ConnectionFactory {
  def connectionFor(url: URL, credentials: Option[Credentials]) = {
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoInput(true)
    connection.setDoOutput(true)
    connection.setInstanceFollowRedirects(false)
    connection.setUseCaches(false)
    connection.setRequestProperty("charset", "utf-8")
    connection.setConnectTimeout(10000)
    connection.setReadTimeout(10000)

    credentials.foreach {
      case Credentials(user, pwd) =>
        connection.setRequestProperty("Authorization", "Basic " + new String(encode(s"$user:$pwd".getBytes("utf-8"))))
    }

    connection
  }
}
