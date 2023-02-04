package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.autoteka

import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoteka.insurance.AutotekaInsuranceRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.InvalidVinError

import java.io.{File, FileInputStream}

object AutotekaInsuranceParser extends App {

  val manager = AutotekaInsuranceRawModelManager(FileFormats.Csv, EventType.BRIGHT_PARK_SALES)

  val dir = new File("~/Desktop/dealers/autoteka/insurance")

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

  val mileagesCount = success.count(_.model.common.mileage.exists(_ > 0))
  println(s"mileages count - $mileagesCount (${(mileagesCount * 1.0 / success.size * 100).toInt} %)")

  val kaskoCount = success.count(_.model.insuranceType == InsuranceType.KASKO)
  val osagoCount = success.count(_.model.insuranceType == InsuranceType.OSAGO)
  val otherCount = success.count(_.model.insuranceType == InsuranceType.UNKNOWN_INSURANCE)

  println(s"kasko count - $kaskoCount. osago count - $osagoCount. other count - $otherCount")

  println()
  println()
  println("ERRORS LIST (without invalid vin):")
  errors.take(100).filterNot(_.isInstanceOf[InvalidVinError]).foreach(e => println(e.humanText))

  println()
  println()
  println("WARNINGS LIST:")
  val warnings = success.flatMap(_.warnings).take(100)
  warnings.foreach(w => println(w.humanText))

  /*var acnt = 0
  success.groupBy(a => (a.identifier -> a.groupId)).foreach {
    case (g, l) if l.size > 1 => {

      fileWriter.println(s"vin = ${g._1.toString}; id = ${g._2}")
      fileWriter.println("duplicates:")
      l.foreach(p => fileWriter.println(p.raw))
      fileWriter.println()
      fileWriter.println()

      acnt += 1
      l.foreach(p => println(p.raw))
      println()
    }
    case _ =>
  }*/

  // println(acnt)

}
