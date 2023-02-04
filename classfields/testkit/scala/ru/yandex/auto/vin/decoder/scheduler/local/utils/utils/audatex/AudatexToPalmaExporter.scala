package ru.yandex.auto.vin.decoder.scheduler.local.utils.audatex

import auto.carfax.common.utils.concurrent.CoreFutureUtils
import auto.carfax.common.utils.tracing.TraceUtils
import cats.implicits.catsSyntaxOptionId
import com.google.protobuf.{Any, Message}
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings, UnescapedQuoteHandling}
import ru.auto.api.vin.CarfaxPalma.{AudatexDetailDescription, AudatexDetailLocation}
import ru.yandex.auto.vin.decoder.model.Audatex.{AudatexDescription, AudatexLocation, AudatexRecord}
import ru.yandex.auto.vin.decoder.scheduler.local.utils.LocalScript
import ru.yandex.auto.vin.decoder.scheduler.local.utils.audatex.PalmaProxy.AlreadyExistsException
import auto.carfax.common.utils.misc.StringUtils._
import ru.yandex.vertis.commons.http.client.RemoteHttpService
import ru.yandex.vertis.palma.services.ProtoDictionaryApiModel

import java.io.{File, FileInputStream}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Success

/**
  * 1. Забирает из csv-файлов маппинги(пример файлов в задаче)
  * 2. Переливает их в словари аудатекса в Пальме
  */
object AudatexToPalmaExporter extends LocalScript {

  private val detailsFile: Option[File] = new File("/home/maratvin/Downloads/desc.csv").some
  private val locationsFile: Option[File] = new File("/home/maratvin/Downloads/locs.csv").some

  private val palmaClient = {
    val service = new RemoteHttpService("palma", "localhost", 3000)
    new PalmaProxy(service)
  }

  private def parseFile(f: File) = {
    val settings = new CsvParserSettings()
    settings.getFormat.setDelimiter(',')
    settings.setUnescapedQuoteHandling(UnescapedQuoteHandling.STOP_AT_CLOSING_QUOTE)
    settings.trimQuotedValues(true)
    settings.setNullValue("")
    settings.setHeaderExtractionEnabled(true)

    val parser = new CsvParser(settings)
    parser.iterateRecords(new FileInputStream(f)).iterator()
  }

  private def send(dictionaryName: String, message: Message) =
    TraceUtils.traceAction("create_or_update_dictionary_entity") { implicit traced =>
      val request = ProtoDictionaryApiModel.CreateUpdateRequest
        .newBuilder()
        .setEntity(Any.pack(message))
        .setDictionaryName(dictionaryName)
        .build()
      palmaClient
        .create(request)
        .map(_.getItem)
        .recoverWith { case AlreadyExistsException =>
          palmaClient
            .update(request)
            .map(_.getItem)
        }
        .andThen { case Success(_) =>
          progressBar.inc(1)
        }
    }

  private def exportToPalma(records: List[AudatexRecord]) = {
    val batchSize = 50
    CoreFutureUtils.runSequentially(records.grouped(batchSize).toList) { grouped =>
      Future.sequence(grouped.map {
        case detail: AudatexDescription =>
          val message = AudatexDetailDescription
            .newBuilder()
            .setRawDescription(detail.rawDescription)

          detail.prettyDescription.foreach(message.setPrettyDescription)
          detail.location.foreach(message.setLocation)
          detail.operation.foreach(message.setOperation)

          send(AudatexDescriptionsPalmaDictionary, message.build())
        case loc: AudatexLocation =>
          val message = AudatexDetailLocation
            .newBuilder()
            .setRaw(loc.raw)
            .setPretty(loc.pretty)
            .build()
          send(AudatexLocationsPalmaDictionary, message)
      })
    }
  }

  override def action: Future[List[List[ProtoDictionaryApiModel.EnrichedItem]]] = {
    val details: List[AudatexRecord] = detailsFile
      .map(parseFile(_).asScala)
      .getOrElse(Iterator.empty)
      .map(r => {
        AudatexDescription(
          /**
            * Очищенное от мусора оригинальное описание аудатекса
            */
          rawDescription = r.getString("raw_description"),
          /**
            * Красивое описание, замапленное контентом
            */
          prettyDescription = r.getString("pretty_description").toOption,
          /**
            * Расположения, которые мы не нашли в очищенном описании
            */
          location = r.getString("location").toOption,
          /**
            * Операции, которые мы не нашли в очищенном описании
            */
          operation = r.getString("operation").toOption
        )
      })
      .toList

    val locations: List[AudatexRecord] = locationsFile
      .map(parseFile(_).asScala)
      .getOrElse(Iterator.empty)
      .map(r => {
        AudatexLocation(
          /**
            * оригинальное расположение аудатекса
            */
          raw = r.getString("raw"),
          /**
            * красивое расположение
            */
          pretty = r.getString("pretty")
        )
      })
      .toList
    exportToPalma(details ++ locations)
  }

}
