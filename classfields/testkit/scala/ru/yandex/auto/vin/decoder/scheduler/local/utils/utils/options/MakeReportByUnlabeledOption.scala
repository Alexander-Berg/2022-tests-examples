package ru.yandex.auto.vin.decoder.scheduler.local.utils.options

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Sink, Source}
import auto.carfax.common.storages.yt.YtConfig
import auto.carfax.common.utils.app.TestJaegerTracingSupport
import auto.carfax.common.utils.config.Environment
import com.google.protobuf.util.JsonFormat
import ru.yandex.auto.vin.decoder.extdata.ApiExtDataClient
import ru.yandex.auto.vin.decoder.model.OptionsSelector
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.providers.options.OptionsProvider
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.YtUtils
import ru.yandex.inside.yt.kosher.tables.{TableReaderOptions, YTableEntryTypes}
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.{IteratorHasAsScala, ListHasAsScala}

/**
  * ===Запрос № 1:===
  * {{{insert into `//home/verticals/.tmp/report_by_option/vin_with_option`
  * select
  * cast(vin as utf8) as vin,
  * cast(source_id as utf8) as source_id,
  * cast(prepared_data as String) as prepared_data
  * FROM  `home/verticals/carfax/raw_storage/vin_raw_storage`
  * where source_id in ("SUZUKI_VEHICLE_INFO", "BMW_VEHICLE_INFO", "TRADESOFT_INFO")
  * and length(vin) == 17 and (cast(prepared_data as String) like "%options%");}}}
  *
  * ===Запрос № 2:===
  * {{{insert into `//home/verticals/.tmp/report_by_option/vin_without_option`
  * select
  * cast(vin as utf8) as vin,
  * cast(source_id as utf8) as source_id,
  * cast(prepared_data as String) as prepared_data
  * FROM  `home/verticals/carfax/raw_storage/vin_raw_storage`
  * where source_id in ("SUZUKI_VEHICLE_INFO", "BMW_VEHICLE_INFO", "TRADESOFT_INFO")
  * and length(vin) == 17 and (cast(prepared_data as String) not like "%options%");}}}
  *
  * ==Основное:==
  * Утилита для проверки Опций которые мы получили
  * от партнёров ("SUZUKI_VEHICLE_INFO", "BMW_VEHICLE_INFO", "TRADESOFT_INFO")
  * и мы не смогли унифицировать с verba. Порядок действий:
  *
  * 1. Формируем diff'ы из vin_raw_storage запросами 1 и 2
  *  запрос №1 выгрузит нам данные имеющие в себе option, по ним мы будем запускать данные скрипт;
  *  запрос №2 выгрузит нам записи по которым вообще нет option, но партнёр нам что-то прислал;
  *
  * 2. Запускам MakeReportByUnlabeledOption.scala указывая верные пути к diff с опциями и место куда сохранить результат, пример аргументов:
  * {{{`//home/verticals/.tmp/report_by_option/vin_with_option //home/verticals/.tmp/report_by_option/vin_raw_report_unlabeled_option //home/verticals/.tmp/report_by_option/vin_all_options`}}}
  *
  * ==В итоге получаем три таблицы:==
  * vin_with_option - таблица вида vin|source_id|prepare_data в которой имеются option
  * vin_without_option - таблицы вида vin|source_id|prepare_data в которой нет option
  * vin_raw_report_unlabeled_option - таблица-отчёт(результат этого скрипта) вида code|description|mark|model|vin|source_id по тем vin-option по которым мы не смогли унифицировать с verba
  * vin_all_options - таблица-отчёт(результат этого скрипта) вида code|mark все найденые связки mark+code в diff
  */

object MakeReportByUnlabeledOption extends TestJaegerTracingSupport {

  implicit val ops = TestOperationalSupport
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  ApiExtDataClient.start()

  private val ytConfig: YtConfig = YtConfig.fromConfig(Environment.config.getConfig("auto-vin-decoder.yt.hahn"))

  private val yt: Yt = YtUtils.http(
    ytConfig.url,
    ytConfig.token
  )

  private lazy val optionsProvider: OptionsProvider = ApiExtDataClient.Providers.optionsProvider

  private def optionsSelector(): OptionsSelector = optionsProvider.get()

  private lazy val makeReportByUnlabeledOptionQueueDao: MakeReportByUnlabeledOptionQueueDao = {
    new MakeReportByUnlabeledOptionQueueDao(yt)
  }

  private def parseVinInfoHistory(str: String): VinInfoHistory = {
    val result = VinInfoHistory.getDefaultInstance.newBuilderForType()
    JsonFormat
      .parser()
      .ignoringUnknownFields()
      .merge(str, result)

    result.build()
  }

  private val MaxRecordsPerSecond = 500

  private val WriteBatchSize = 200

  private type Vin = String
  private type SourceId = String

  private def listVinInfo(ytPath: String): Source[(Vin, SourceId, VinInfoHistory), NotUsed] = {
    val resultsIterator = yt
      .tables()
      .read(
        Optional.empty[GUID](),
        false,
        YPath.simple(ytPath),
        YTableEntryTypes.YSON,
        new TableReaderOptions
      )
      .map(ytNode => {

        val vin = ytNode.getString("vin")
        val source = ytNode.getString("source_id")
        val prepared_data = parseVinInfoHistory(ytNode.getString("prepared_data"))

        (vin, source, prepared_data)
      })

    Source
      .fromIterator(() => resultsIterator.asScala)
      .throttle(
        MaxRecordsPerSecond,
        1.second,
        2 * MaxRecordsPerSecond,
        ThrottleMode.Shaping
      )
  }

  private def listUnlabeledOption(
      ytPath: String,
      unlabeledOptionSavePath: String,
      allOptionsSavePath: String,
      allOptionsCounter: AtomicInteger): Source[Unit, NotUsed] = {
    val selector = optionsSelector()

    listVinInfo(ytPath)
      .map(raws => {
        val (vin, sourceId, vinInfoHistory) = raws
        val vehicleOptions = vinInfoHistory.getOptionsList.asScala
        val mark = vinInfoHistory.getTtx.getMark
        val model = vinInfoHistory.getTtx.getModel
        val unlabeledOptionsWithAllOption = vehicleOptions.map { protoOption =>
          selector.unifyOption(s"${mark.toLowerCase}-${protoOption.getCode.toLowerCase}") match {
            case None =>
              (
                Some(
                  UnlabeledOptionForReport(
                    vin,
                    mark,
                    model,
                    protoOption.getCode,
                    sourceId,
                    protoOption.getRawName
                  )
                ),
                LabeledOption(mark.toLowerCase, protoOption.getCode.toLowerCase)
              )
            case _ => (None, LabeledOption(mark.toLowerCase, protoOption.getCode.toLowerCase))
          }
        }
        allOptionsCounter.addAndGet(vehicleOptions.size)
        (unlabeledOptionsWithAllOption.flatMap(_._1), unlabeledOptionsWithAllOption.map(_._2))
      })
      .grouped(WriteBatchSize)
      .map(res => {

        println(s"start upload unlabeled option to YT by path $unlabeledOptionSavePath")
        makeReportByUnlabeledOptionQueueDao
          .uploadUnlabeledOptionForReportToYt(res.flatMap(_._1), unlabeledOptionSavePath)
          .foreach { _ =>
            println(s"finish upload unlabeled option to YT by path $unlabeledOptionSavePath")
            println(s"start upload unlabeled option to YT by path $allOptionsSavePath")
            makeReportByUnlabeledOptionQueueDao
              .uploadLabeledOptionToYt(res.flatMap(_._2), allOptionsSavePath)
              .foreach(_ => println(s"finish upload unlabeled option to YT by path $allOptionsSavePath"))
          }

      })
  }

  private def makeReport(ytPath: String, unlabeledOptionSavePath: String, allOptionsSavePath: String): Future[Unit] = {

    implicit val system: ActorSystem = ActorSystem(
      s"MakeReportByUnlabeledOption-${System.currentTimeMillis()}-system"
    )
    implicit val ec: ExecutionContext = system.dispatcher

    val allOptionsCounter = new AtomicInteger(0)

    val done =
      listUnlabeledOption(ytPath, unlabeledOptionSavePath, allOptionsSavePath, allOptionsCounter).runWith(Sink.ignore)
    done.onComplete(_ => system.terminate())

    done.map(_ => println(s"count of all options: ${allOptionsCounter.get()}"))
  }

  def main(args: Array[String]): Unit = {
    val ytPath = args(0)
    val ytStartPath = args(1)
    val ytStartPath2 = args(2)
    Await.result(makeReport(ytPath, ytStartPath, ytStartPath2), Duration.Inf)
  }

}
