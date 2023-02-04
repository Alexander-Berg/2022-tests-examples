package ru.yandex.auto.vin.decoder.scheduler.local.utils

import auto.carfax.common.utils.config.Environment
import auto.carfax.common.utils.tracing.Traced
import ru.auto.api.vin.event.VinReportEventType
import ru.yandex.auto.vin.decoder.components.DefaultCoreComponents
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.utils.collections.RichIterable
import ru.yandex.auto.vin.decoder.yql.{YQLJdbc, YqlConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Утилита, очищающая YDB от записей для указанного партнера и списка VIN. Порядок действий:
  *
  *  - Формируем [[https://yql.yandex-team.ru/Operations/X_8q8lPzVIU9Wlt62fQKXmhZIkplLuSMHd8yE9jLWqQ= список винов]]
  *  - Указываем верные реквизиты доступа в секциях `auto-vin-decoder.ydb` и `auto-vin-decoder.yql` в файле `auto-vin-decoder-core.testing.conf`
  *  - Указываем параметры `eventType и ytVinsPath`
  *  - Удаляем отметки о запусках в PostgreSQL: `DELETE FROM public.import_stats WHERE source_id = 'AVTOBAN_SERVICES'`
  *  - Запускаем [[http://localhost:3000/vin-scheduler/run?id=AVTOBAN_SERVICES_files_import_task повторный импорт]]
  *  - Проверяем [[https://yql.yandex-team.ru/Operations/YAV7RgPTTlEKc6w9SBs09RhSj1b5ReXOdMEFcEfIMpY= наличие данных]]
  *  - Удаляем [[https://yt.yandex-team.ru/hahn/navigation?path=//home/verticals/_home/nuklea/all_avtoban_vins список идентификаторов]]
  */
object YdbCleaner extends LocalScript {

  /* PARAMS */

  implicit val t: Traced = Traced.empty
  private val eventType = VinReportEventType.EventType.INCHCAPE_SERVICES
  private val ytVinsPath = "hahn.`home/verticals/_home/plomovtsev/inchcape_services`"
  private val defaultBatchSize = 500
  private val deletionDelaySeconds = 10

  /* DEPENDENCIES */

  private val yqlJdbc = createYqlJdbc // конфиг: auto-vin-decoder.yql
  private val ydbContext = new DefaultCoreComponents().ydbContext // конфиг: auto-vin-decoder.ydb
  private val rawStorageService = ydbContext.rawStorageService

  private val deletionTablePath = ydbContext.ydbConfig.endpoint + ydbContext.ydbConfig.root +
    ydbContext.rootFolder.map("/" + _).getOrElse("") + "/vin_raw_storage"

  def action: Future[Any] = {
    println(s"Selecting vins from $ytVinsPath...")
    val vins = yqlJdbc.execute(s"select vin from $ytVinsPath;", _.getString("vin"))
    println(s"${vins.size} vins selected")
    pause(
      deletionDelaySeconds,
      s"Start deletion from '$deletionTablePath'"
    ) // время, чтобы вручную остановить скрипт, если в консоли оказалась не та таблица
    progressBar.start(totalItems = vins.size)
    delete(vins)
  }

  private def delete(vins: Vector[String]): Future[Any] = {
    val vinBatches = vins.sorted.map(VinCode(_)).grouped(defaultBatchSize).toVector
    vinBatches.runSequential { vinBatch =>
      rawStorageService
        .deleteBatchByVin(eventType, vinBatch)
        .map(_ => progressBar.inc(vinBatch.size))
    }
  }

  // Есть в SchedulerComponents, но не хочется инициализировать всё остальное, что там есть
  private def createYqlJdbc: YQLJdbc = {
    val yqlConfig: YqlConfig = YqlConfig.fromConfig(Environment.config.getConfig("auto-vin-decoder.yql.config"))
    new YQLJdbc(yqlConfig)
  }
}
