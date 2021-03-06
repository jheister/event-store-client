package eventstore.client.http

import eventstore.client.http.TestUtil.{event, randomStream}
import org.scalatest.{FunSpec, MustMatchers}

class HttpEventStoreIndexedStreamsTest extends FunSpec with MustMatchers {
  val eventstore = new HttpEventStore("127.0.0.1", 2113)

  it("can read an indexed stream") {
    val streamA = randomStream()
    val streamB = randomStream()
    val indexedAandB = randomStream()

    val projections = new Projections("localhost", 2113, Credentials.default)

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

    await(4)(() => eventstore.fromStream(indexedAandB)).map(_.event).toList must be(List(event(1), event(2), event(3), event(4)))
  }

  def await[T](count: Int, times: Int = 100)(f: () => Iterator[T]): Iterator[T] = {
    val data = f().toList
    if (data.size >= count) {
      data.toIterator
    } else {
      Thread.sleep(10)
      await(count - 1, times - 1)(f)
    }
  }
}


