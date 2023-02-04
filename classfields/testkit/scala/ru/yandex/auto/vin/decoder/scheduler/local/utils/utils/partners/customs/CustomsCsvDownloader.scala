package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.customs

import auto.carfax.common.utils.misc.DateTimeUtils.yyyyMMddFormatter

import java.io.{File, FileInputStream, FileOutputStream, PrintWriter}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.io.{BufferedSource, Source}

object CustomsCsvDownloader extends App {
  private val passportUri = "https://customs.gov.ru/7730176610-vvozavtoru/meta.csv"
  private val fromYearMonthDay = "2021-12-01"
  private val customsDir = new File("/Users/kamilyanov-d/downloads/customs/")
  private val csvOutDir: File = new File(customsDir, "/csv/")

  private var errorsList: List[String] = List()
  private val csvFileDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
  private val fromYearMonthDayMillis: Long = LocalDate.from(yyyyMMddFormatter.parse(fromYearMonthDay)).toEpochDay

  println(s"Start download customs passport from $passportUri")

  writeFile(customsDir, Source.fromURL(passportUri, "windows-1251"), "meta.csv")
  private val metaFile: File = new File(customsDir, "/meta.csv")

  private val customsCsvFiles = getCustomsCsvUriFromFile(metaFile)

  println(s"Found ${customsCsvFiles.length} files")

  println(s"Start download files from date $fromYearMonthDay. Meta file ${metaFile.getPath}")

  customsCsvFiles.foreach { file =>
    try { downloadAndWriteCsv(file) }
    catch {
      case e: Throwable =>
        println(s"error during download and write ${e.getMessage}")
        errorsList = file.fileName +: errorsList
    }
  }

  private def getCustomsCsvUriFromFile(file: File): List[CustomsCsv] = {
    val inputSource = Source.fromInputStream(new FileInputStream(file))
    val linesIterator = inputSource.getLines()

    (for {
      line <- linesIterator
      if line.startsWith("data-")
      fileNameWithUri = line.split(",")
      if getDateMillisFromFileName(fileNameWithUri(0)) >= fromYearMonthDayMillis
      customsFile = CustomsCsv(fileNameWithUri(0), fileNameWithUri(1))
    } yield customsFile).toList
  }

  private def downloadAndWriteCsv(file: CustomsCsv): Unit = {
    val source = Source.fromURL(file.uri, "windows-1251")

    writeFile(csvOutDir, source, file.fileName)
  }

  private def writeFile(dir: File, source: BufferedSource, fileName: String): Unit = {
    val fileString = source.mkString
    val output = new File(dir, fileName)

    val writer = new PrintWriter(new FileOutputStream(output))
    writer.print(fileString)
    writer.close()
  }

  private def getDateMillisFromFileName(fileName: String): Long =
    LocalDate.from(csvFileDateFormat.parse(fileName.split("-")(1))).toEpochDay

  println("Download finished!")
  println(s"Downloaded: ${customsCsvFiles.length - errorsList.length} | Errors: ${errorsList.length}")

  if (errorsList.nonEmpty) {
    println(s"Errors uri: ${errorsList.mkString(", ")}")
  }

  private case class CustomsCsv(fileName: String, uri: String)
}
