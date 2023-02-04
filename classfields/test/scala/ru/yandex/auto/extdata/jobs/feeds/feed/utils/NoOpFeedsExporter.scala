package ru.yandex.auto.extdata.jobs.feeds.feed.utils

import ru.yandex.auto.extdata.jobs.feeds.service.FeedsExporter

import java.io.File

object NoOpFeedsExporter extends FeedsExporter.Service {
  override def `export`(fileName: String, file: File): Unit = ()
}
