package ru.yandex.vertis.clustering.services.impl

import java.io.{BufferedReader, FileInputStream, InputStreamReader}
import java.time.{Instant, ZonedDateTime}
import java.util.zip.ZipInputStream

import ru.yandex.vertis.clustering.dao.FactsDao
import ru.yandex.vertis.clustering.model._
import ru.yandex.vertis.clustering.services.impl.CsvFileFactLogsDao._
import ru.yandex.vertis.clustering.utils.DateTimeUtils
import ru.yandex.vertis.clustering.utils.features.FeatureHelpers

import scala.util.Try

/**
  * Read facts from CSV file, like
  *  AutoruApiUuid,0000124929b6c8d1a0f43426ba54354f,flash_check_in,auto.ru:api:0000124929b6c8d1a0f43426ba54354f,23409992,1497709258,\N
  *
  * @author alesavin
  */
class CsvFileFactLogsDao(filePath: String) extends FactsDao {

  override def readLogs(): Try[Iterator[Fact]] = Try {
    val zis = new ZipInputStream(new FileInputStream(filePath))
    zis.getNextEntry
    val reader = new BufferedReader(new InputStreamReader(zis))
    Stream
      .continually(reader.readLine())
      .takeWhile(Option(_).nonEmpty)
      .map(asFactsLogRecord(_).get)
      .toIterator
  }

}

object CsvFileFactLogsDao {

  def asFactsLogRecord(value: String): Try[Fact] = Try {
    val parts = value.split(",")
    Fact(
      User(Domains.Autoru, parts(4)),
      FeatureHelpers
        .parse(
          FeatureTypes.withName(parts(0)),
          parts(1),
          isExcluded = None
        )
        .get,
      ZonedDateTime.ofInstant(Instant.ofEpochSecond(parts(5).toLong), DateTimeUtils.DefaultZoneId)
    )
  }
}
