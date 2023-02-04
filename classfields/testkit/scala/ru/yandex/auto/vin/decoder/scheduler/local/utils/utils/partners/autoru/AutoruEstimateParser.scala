package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.autoru

import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.estimate.AutoruFormatEstimateRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.InvalidVinError

import java.io.{File, FileInputStream}

object AutoruEstimateParser extends App {

  val manager = new AutoruFormatEstimateRawModelManager(EventType.BRIGHT_PARK_SALES, FileFormats.Json)

  val dir = new File("~/Downloads/CARFAX-1677")

  val iterator = dir
    .listFiles()
    .iterator
    .flatMap(file => {
      manager.nameManager
        .getDataTimestamp(file.getName, 123L)
        .map(ts => {
          manager.parseFile(new FileInputStream(file), file.getName)
        })
        .getOrElse(Iterator.empty)
    })

  val list = iterator.toList

  val success = list.collect { case Right(v) =>
    v
  }

  val successWithData = success.filterNot(_.model.info.isEmpty)

  val errors = list.collect { case Left(e) =>
    e
  }

  println(s"total - ${list.size}")
  println(s"correct - ${success.size} (${(success.size * 1.0 / list.size * 100).toInt} %) ")
  println(s"errors - ${errors.size} (${(errors.size * 1.0 / list.size * 100).toInt} %)")
  println(s"records with invalid vin - ${errors.count(_.isInstanceOf[InvalidVinError])}")
  val recordsWithWarnings = success.count(_.warnings.nonEmpty)

  println(
    s"success records with warnings - $recordsWithWarnings (${(recordsWithWarnings * 1.0 / success.size * 100).toInt}) %"
  )
  println(s"Success with data - ${successWithData.size} (${(successWithData.size * 1.0 / success.size * 100).toInt}) %")

  println()
  println(s"unique vins - ${success.map(_.identifier.toString).distinct.size}")
  println(s"unique ids - ${success.map(_.groupId).distinct.size}")
  val uniqueIdsAndVins = success.map(a => (a.identifier.toString -> a.groupId)).distinct.size

  val messageUniqueIdsAndVins = if (uniqueIdsAndVins == success.size) {
    "Unique (vin + id) count = success count. OK"
  } else {
    "ERROR! Unique (vin + id) count != success count. FAIL"
  }

  println(
    s"unique (id + vin) - ${success.map(a => (a.identifier.toString -> a.groupId)).distinct.size}. $messageUniqueIdsAndVins"
  )
  println()

  val mileagesCount = success.count(_.model.info.flatMap(_.mileage).exists(_ > 0))
  println(s"mileages count - $mileagesCount (${(mileagesCount * 1.0 / success.size * 100).toInt} %)")

  val recordsWithPhotoCount = successWithData.count(_.model.info.exists(_.images.nonEmpty))
  val recordsWithDamagesCount = successWithData.count(_.model.info.exists(_.damages.nonEmpty))

  val recordsWithInspectionDetailsCount =
    successWithData.count(_.model.info.exists(_.results.exists(_.inspectionDetails.nonEmpty)))

  val recordsWithRepairCount =
    successWithData.count(_.model.info.exists(_.results.exists(_.repair.nonEmpty)))

  // валидный формат урлов чекать
  val totalPhotoCount =
    successWithData.flatMap(_.model.info.map(_.images).getOrElse(Seq.empty)).count(_.origUrl.nonEmpty)

  val recordsWithVideoCount = successWithData.count(_.model.info.exists(_.videos.nonEmpty))
  val totalVideoCount = successWithData.flatMap(_.model.info.map(_.videos).getOrElse(Seq.empty)).count(_.url.nonEmpty)

  println(
    s"records with photo count - $recordsWithPhotoCount (${(recordsWithPhotoCount * 1.0 / success.size * 100).toInt} %)"
  )
  println(s"Summary photo count - $totalPhotoCount")

  println(
    s"records with video count - $recordsWithVideoCount (${(recordsWithVideoCount * 1.0 / success.size * 100).toInt} %)"
  )
  println(s"Summary video count - $totalVideoCount")

  println(
    s"records with damages count - $recordsWithDamagesCount (${(recordsWithDamagesCount * 1.0 / success.size * 100).toInt} %)"
  )

  println(
    s"records with inspection details count - $recordsWithInspectionDetailsCount (${(recordsWithInspectionDetailsCount * 1.0 / success.size * 100).toInt} %)"
  )

  println(
    s"records with repairs  count - $recordsWithRepairCount (${(recordsWithRepairCount * 1.0 / success.size * 100).toInt} %)"
  )

  println("Finish")
}
