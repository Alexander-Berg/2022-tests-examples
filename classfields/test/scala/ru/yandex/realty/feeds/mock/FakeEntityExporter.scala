package ru.yandex.realty.feeds.mock

import ru.yandex.realty.feeds.services.EntityExporter

class FakeEntityExporter(buffer: StringBuilder) extends EntityExporter[String] {
  override def `export`(t: String, name: String): Unit =
    buffer.append(t)
}
