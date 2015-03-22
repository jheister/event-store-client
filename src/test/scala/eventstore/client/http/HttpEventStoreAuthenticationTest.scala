package eventstore.client.http

import org.scalatest.{FunSpec, MustMatchers}

class HttpEventStoreAuthenticationTest extends FunSpec with MustMatchers {
  val eventstore = new HttpEventStore("127.0.0.1", 2113, credentials = Credentials.default)

  it("can read from a stream requiring authorization") {
    val all = eventstore.fromStream("$projections-$master").toList

    all.isEmpty must be(false)
  }
}
