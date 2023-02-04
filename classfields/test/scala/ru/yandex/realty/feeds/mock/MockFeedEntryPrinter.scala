package ru.yandex.realty.feeds.mock

import ru.yandex.realty.feeds.printers.FeedEntryPrinter

class MockFeedEntryPrinter[T](p: T => String) extends FeedEntryPrinter[T] {
  override def header: String = "header"
  override def print(entry: T): Option[String] = Some(p(entry))
  override def footer: String = "footer"
}
