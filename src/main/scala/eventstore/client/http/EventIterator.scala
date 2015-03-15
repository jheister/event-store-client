package eventstore.client.http

import eventstore.client.EventInStream

class EventIterator(pages: Iterator[EventPage]) extends Iterator[EventInStream] {
  var current: Iterator[EventInStream] = Iterator.empty

  override def hasNext: Boolean = {
    loadNextIfRequired()
    current.hasNext
  }

  override def next(): EventInStream = {
    loadNextIfRequired()
    current.next()
  }

  private def loadNextIfRequired(): Unit = {
    if (!current.hasNext && pages.hasNext) {
      current = pages.next().events.iterator
    }
  }
}
