package ru.yandex.auto.vin.decoder.scheduler.local.utils

import auto.carfax.common.utils.app.TestJaegerTracingSupport
import com.google.protobuf.util.JsonFormat
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory

import java.io.FileReader
import java.net.URL
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, Year, ZoneId}
import scala.jdk.CollectionConverters._

class TsvParser(filename: String) {

  val headers = List(
    "vin",
    "source_id",
    "timestamp_create",
    "timestamp_update",
    "prepared_data"
  )

  val settings = new CsvParserSettings()
  settings.getFormat.setDelimiter('\t')
  //  settings.trimValues(true)
  //  settings.trimQuotedValues(true)
  settings.setHeaders(headers: _*)
  settings.setNumberOfRowsToSkip(1)

  val parser = new CsvParser(settings)

  def parseMileages(): Map[String, VinInfoHistory] = {
    parser
      .iterateRecords(new FileReader(filename))
      .asScala
      .map { item =>
        val vinInfoHistoryBuilder = VinInfoHistory.newBuilder()
        val preparedData = item.getString("prepared_data").replace("\\n", "\n")
        JsonFormat.parser().ignoringUnknownFields().merge(preparedData, vinInfoHistoryBuilder)
        item.getString("vin") -> vinInfoHistoryBuilder.build()
      }
      .toMap
  }
}

case class DiagnosticCard(date: String, number: String)

object CompareMileages extends TestJaegerTracingSupport {

  val currentYear: Int = Year.now.getValue

  private def getYear(vin: String) = {
    val url = new URL(s"http://localhost:3000/api/v1/report/raw/essentials?vin=$vin")
    val json = Json.parse(url.openStream())
    (json \ "report" \ "ptsInfo" \ "year" \ "value").asOpt[Int]
  }

  private def getAge(year: Int): Int = {
    currentYear - year
  }

  private def compareReports(
      adaperioData: Map[String, VinInfoHistory],
      autocodeData: Map[String, VinInfoHistory]): Unit = {
    println(
      List(
        "vin",
        "year",
        "age",
        "adaperio_card_count",
        "adaperio_mileage_count",
        "autocode_card_count",
        "autocode_mileage_count"
      ).mkString(", ")
    )

    adaperioData.foreach { case (vin, adaperioHistory) =>
      autocodeData.get(vin) match {
        case Some(autocodeHistory) =>
          val adaperioCardCount = adaperioHistory.getDiagnosticCardsCount
          val adaperioMileageCount = adaperioHistory.getMileageCount

          val autocodeCardCount = autocodeHistory.getDiagnosticCardsCount
          val autocodeMileageCount = autocodeHistory.getMileageCount

          val year = getYear(vin)
          val age = year.map(getAge)

          val items =
            List(
              vin,
              year.getOrElse(""),
              age.getOrElse(""),
              adaperioCardCount,
              adaperioMileageCount,
              autocodeCardCount,
              autocodeMileageCount
            ).map(_.toString)
          println(items.mkString(", "))
        case _ =>
      }
    }
  }

  val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

  private def asCard(card: VinHistory.DiagnosticCard): DiagnosticCard = {
    val from = formatter.format(LocalDate.from(Instant.ofEpochMilli(card.getFrom).atZone(ZoneId.of("UTC"))))
    val to = formatter.format(LocalDate.from(Instant.ofEpochMilli(card.getTo).atZone(ZoneId.of("UTC"))))
    DiagnosticCard(s"$from/$to", card.getNumber)
  }

  private def compareCards(
      adaperioData: Map[String, VinInfoHistory],
      autocodeData: Map[String, VinInfoHistory]): Unit = {
    println(
      List(
        "vin",
        "adaperio_card_date",
        "adaperio_card_id",
        "autocode_card_date",
        "autocode_card_id"
      ).mkString(", ")
    )

    adaperioData.foreach { case (vin, adaperioHistory) =>
      autocodeData.get(vin) match {
        case Some(autocodeHistory) =>
          val adaperioCards = adaperioHistory.getDiagnosticCardsList.asScala.toList.sortBy(_.getFrom).map(asCard)
          val autocodeCards = autocodeHistory.getDiagnosticCardsList.asScala.toList.sortBy(_.getFrom).map(asCard)

          adaperioCards.zipAll(autocodeCards, DiagnosticCard("", ""), DiagnosticCard("", "")).foreach { case (a, b) =>
            println(List(vin, a.date, a.number, b.date, b.number).mkString(", "))
          }

        case _ =>
      }
    }

  }

  def main(args: Array[String]): Unit = {
    val adaperioParser = new TsvParser("/home/nuklea/Downloads/6020509f5276f97c2937071f.tsv")
    val autocodeParser = new TsvParser("/home/nuklea/Downloads/yt___tmp_nuklea_ffbee235_c2e9f727_7afb4444_28091edc")
    val adaperioData = adaperioParser.parseMileages()
    val autocodeData = autocodeParser.parseMileages()

    //    compareReports(adaperioData, autocodeData)
    compareCards(adaperioData, autocodeData)
  }
}
