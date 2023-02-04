package ru.yandex.realty.feeds.mock

import ru.yandex.realty.feeds.fetchers.FeedEntriesFetcher

class MockFeedEntriesFetcher[T](entries: Seq[T]) extends FeedEntriesFetcher[T] {

  def processEntries(processor: T => Unit): Unit =
    entries.foreach(processor)
}
