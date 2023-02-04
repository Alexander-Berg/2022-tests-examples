package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.customs

import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsArray, Json}
import ru.yandex.auto.vin.decoder.raw.customs.CustomsRecordModel
import auto.carfax.common.utils.misc.StringUtils._
import ru.yandex.auto.vin.decoder.utils.text.TextUtils

import java.io.{File, FileInputStream, FileOutputStream, PrintWriter}
import scala.io.Source

object CustomsCsvToJsonConverter extends App {
  private val username = "yourname"

  convertAllInDir(
    new File(s"/Users/$username/downloads/customs/csv/"),
    new File(s"/Users/$username/downloads/customs/json/")
  )

  def convertAllInDir(inputDir: File, outputDir: File): Unit = {

    inputDir
      .listFiles()
      .foreach(file => {
        if (file.getName.startsWith("data-")) {
          println(s"Convert ${file.getName}")
          convert(file, new File(outputDir, file.getName.stripSuffix(".csv") + ".json"))
        }
      })

  }

  def convert(input: File, output: File): Unit = {

    val inputSource = Source.fromInputStream(new FileInputStream(input), "utf-8")
    val writer = new PrintWriter(new FileOutputStream(output))

    val linesIterator = inputSource.getLines()
    val header = linesIterator.next().split(";").map(prepareRawStr).toList // пропускаем заголовок

    // check two last header versions
    try {
      checkHeader(header) // checker после 17.02.2022
    } catch {
      case _: Throwable => checkHeader16_11_2021(header)
    }

    // checkHeader16_11_2021 checker после 16.11.2021
    // checkHeader20_07_2021(header) // checker после 20.07.2021
    // oldCheckHeader(header) // checker до 20.07.2021

    val records = linesIterator.map(line => {
      val splitted = line.split(";", 7)
      prepareRawStr(splitted(4))

      try {
        require(splitted.size == 7) // расчитываем, что в csv 7 полей

        fromRaw(
          rawVin = splitted(1),
          rawDate = splitted(2),
          rawCountryId = splitted(3),
          rawCountryName = countryNameBy(splitted(3).toInt, line),
          rawBodyNumber = splitted(5),
          rawChassis = splitted(6)
        )
      } catch {
        case e: Throwable =>
          println(s"Error creation record from line $line")
          throw e;
      }
    })

    val jsonArray = JsArray(records.filter(!_.countryId.contains(643)).map(r => Json.toJson(r)).toList)
    writer.print(jsonArray.toString())
    writer.close()
  }

  def countryNameBy(code: String): String = {
    code match {
      case "ru" => "РОССИЙСКАЯ ФЕДЕРАЦИЯ"
      case "by" => "РЕСПУБЛИКА БЕЛАРУСЬ"
      case "kg" => "КИРГИЗСКАЯ РЕСПУБЛИКА"
      case "kz" => "РЕСПУБЛИКА КАЗАХСТАН"
      case "am" => "РЕСПУБЛИКА АРМЕНИЯ"
      case code => throw new IllegalArgumentException(s"Unsupported country code $code")
    }
  }

  def countryNameBy(code: Int, t: String): String = {
    code match {
      case 643 => "РОССИЙСКАЯ ФЕДЕРАЦИЯ"
      case 112 => "РЕСПУБЛИКА БЕЛАРУСЬ"
      case 417 => "КИРГИЗСКАЯ РЕСПУБЛИКА"
      case 398 => "РЕСПУБЛИКА КАЗАХСТАН"
      case 51 => "РЕСПУБЛИКА АРМЕНИЯ"
      case 0 => "НЕИЗВЕСТНОЕ ГОСУДАРСТВО"
      case code => throw new IllegalArgumentException(s"Unsupported country code $code, $t")
    }
  }

  def countryIdBy(code: String): String = {
    code match {
      case "ru" => "643"
      case "by" => "112"
      case "kg" => "417"
      case "kz" => "398"
      case "am" => "051"
      case code => throw new IllegalArgumentException(s"Unsupported country code $code")
    }
  }

  def convert20190401(input: File, output: File): Unit = {
    val inputSource = Source.fromInputStream(new FileInputStream(input), "utf-8")
    val writer = new PrintWriter(new FileOutputStream(output))

    val linesIterator = inputSource.getLines()
    val header = linesIterator.next().split(";").map(prepareRawStr).toList // пропускаем заголовок
    checkHeader(header)

    val records = linesIterator.map(line => {
      val splitted = line.split(";", 7)
      require(splitted.size == 7) // расчитываем, что в csv 7 полей

      // println(line)

      val record = fromRaw(
        rawVin = splitted(1),
        rawDate = splitted(2),
        rawCountryId = splitted(3),
        rawCountryName = splitted(4),
        rawBodyNumber = splitted(5),
        rawChassis = splitted(6)
      )
      // println(record)

      record
    })

    val jsonArray = JsArray(records.map(r => Json.toJson(r)).toList)
    writer.print(jsonArray.toString())
    writer.close()
  }

  def fromRaw(
      rawVin: String,
      rawDate: String,
      rawCountryId: String,
      rawCountryName: String,
      rawBodyNumber: String,
      rawChassis: String): CustomsRecordModel = {

    CustomsRecordModel(
      prepareIdentifier(rawVin),
      parseRawTamojnaDate(prepareRawStr(rawDate)),
      parseCountryId(rawCountryId),
      prepareRawStr(rawCountryName).toOption,
      prepareIdentifier(rawBodyNumber),
      prepareIdentifier(rawChassis)
    )
  }

  private def prepareRawStr(str: String): String = {
    str.trim.replace("\"", "")
  }

  private def prepareIdentifier(raw: String): String = {
    TextUtils.convertToEn(prepareRawStr(raw).toUpperCase)
  }

  lazy val rawCustomsDateFormatter = DateTimeFormat.forPattern("dd.MM.yyyy")
  lazy val secondRawCustomsDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  private def parseRawTamojnaDate(raw: String): Long = {
    // обычно в файлах таможни дата представлена так: 15.06.0018

    if (raw.contains("\\."))
      raw.split("\\.", 3) match {
        case Array(d, m, y) if y.length == 4 && y.startsWith("00") =>
          rawCustomsDateFormatter.parseMillis(s"$d.$m.20${y.takeRight(2)}")
        case _ => throw new RuntimeException(s"Unknown date format - $raw")
      }
    else secondRawCustomsDateFormatter.parseMillis(raw.take(10))

  }

  private def parseCountryId(raw: String): Option[Int] = {
    prepareRawStr(raw).toOption.map(_.toInt)
  }

  private def checkHeader(header: Seq[String]): Unit = {
    require(header.size == 7)
    require(header(0) == "ID")
    require(header(1) == "VEHICLE_VIN")
    require(header(2) == "START_DATE")
    require(header(3) == "VEHICLE_FROM_COUNTRY")
    require(header(4) == "VEHICLE_FROM_COUNTRY_NAIM")
    require(header(5) == "VEHICLE_BODY_NUMBER")
    require(header(6) == "VEHICLE_CHASSIS_NUMBER")
  }

  private def checkHeader16_11_2021(header: Seq[String]): Unit = {
    require(header.size == 7)
    require(header(0) == "ID")
    require(header(1) == "VEHICLE_VIN")
    require(header(2) == "START_DATE")
    require(header(3) == "VEHICLE_FROM_COUNTRY")
    require(header(4) == "VEHICLE_FROM_COUNTRY_NAIM")
    require(header(5) == "VEHICLE_BODY_NUMBER")
    require(header(6) == "VEHICLE_CHASSISNUMBER")
  }

  private def checkHeader20_07_2021(header: Seq[String]): Unit = {
    require(header.size == 6)
    require(header(0) == "ID")
    require(header(1) == "VEHICLE_VIN")
    require(header(2) == "VEHICLE_BODY_NUMBER")
    require(header(3) == "VEHICLE_CHASSISNUMBER")
    require(header(4) == "START_DATE")
    require(header(5) == "VEHICLE_FROM_COUNTRY_NAIM")
  }

  private def oldCheckHeader(header: Seq[String]): Unit = { // до 20.07.2021
    require(header.size == 7)
    require(header(0) == "")
    require(header(1) == "VEHICLE_VIN")
    require(header(2) == "START_DATE")
    require(header(3) == "VEHICLE_FROM_COUNTRY")
    require(header(4) == "VEHICLE_FROM_COUNTRY_NAIM")
    require(header(5) == "VEHICLE_BODY_NUMBER")
    require(header(6) == "VEHICLE_CHASSIS_NUMBER")
  }

}
