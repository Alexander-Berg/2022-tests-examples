package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.autoru

import ru.auto.api.ApiOfferModel.Section
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.sale.AutoruFormatSaleRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.InvalidVinError
import ru.yandex.auto.vin.decoder.raw.validation.ValidationRecordError

import java.io.{File, FileInputStream}

object AutoruSalesParser extends App {

  val manager = new AutoruFormatSaleRawModelManager(EventType.BRIGHT_PARK_SALES, FileFormats.Json)

  val dir = new File("~/Downloads/CARFAX-1677")

  val start = System.currentTimeMillis()

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

  val finish = System.currentTimeMillis()

  println(list.size)
  println(s"${finish - start} ms")

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
    s"success records with warnings - $recordsWithWarnings (${(recordsWithWarnings * 1.0 / success.size * 100).toInt})"
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
  val hasIsNewFlagCount = success.count(_.model.info.flatMap(_.isNew).nonEmpty)
  println(s"has is new flag count - $hasIsNewFlagCount (${(hasIsNewFlagCount * 1.0 / success.size * 100).toInt} %)")
  val hasIsCreditFlagCount = success.count(_.model.info.flatMap(_.isCredit).nonEmpty)

  println(
    s"has is credit flag count - $hasIsCreditFlagCount (${(hasIsCreditFlagCount * 1.0 / success.size * 100).toInt} %)"
  )
  val hasAmount = success.count(_.model.info.flatMap(_.amount).exists(_ > 0))
  println(s"has amount - $hasAmount (${(hasAmount * 1.0 / success.size * 100).toInt} %)")

  val newCount = success.count(_.model.info.map(_.section).contains(Section.NEW))
  val usedCount = success.count(_.model.info.map(_.section).contains(Section.USED))
  println(s"new cars  - $newCount. used cars - $usedCount")

  val creditCount = success.count(_.model.info.flatMap(_.isCredit).contains(true))
  val notCreditCount = success.count(_.model.info.flatMap(_.isCredit).contains(false))
  println(s"credit cars  - $creditCount. not credit cars - $notCreditCount")

  val newCarsWithNoZeromileage = success.count(m =>
    m.model.info.map(_.section).contains(Section.NEW) && m.model.info.flatMap(_.mileage).exists(_ > 1000)
  )

  val x = success.filter(m =>
    m.model.info.map(_.section).contains(Section.NEW) && m.model.info.flatMap(_.mileage).exists(_ > 1000)
  )
  val zz = 3

  println(s"new cars with mileage > 1000  - $newCarsWithNoZeromileage")

  println()
  println()

  /*val maxDate = {
    val max = success.maxBy(_.model.info.map(_.eventTimestamp).getOrElse(0L))
    println(s"max timestamp - ${max.model.eventTimestamp} data = ${max.raw}")
  }

  val minDate = {
    val min = success.minBy(_.model.info.map(_.eventTimestamp).getOrElse(0L))
    println(s"min timestamp - ${min.model.eventTimestamp} data = ${min.raw}")
  }*/

  println()
  println()
  println("ERRORS LIST (without invalid vin):")

  errors
    .take(100)
    .filterNot(_.isInstanceOf[InvalidVinError])
    .foreach(e => {
      println(e.humanText)
      println(e.asInstanceOf[ValidationRecordError].recordContext)
    })

  println()
  println()
  println("WARNINGS LIST:")
  val warnings = success.flatMap(_.warnings).take(100)

  warnings.foreach(w => {
    println(w.humanText)
    // println(w.asInstanceOf[ValidationRecordError].recordContext)
  })

  /*println(
    success
      .filter(s => s.model.yearManufactured.nonEmpty && s.model.mileage.nonEmpty)
      .count(s => {
        val years = new DateTime(s.model.eventTimestamp).getYear - s.model.yearManufactured.get + 1
        val mileage = s.model.mileage.get

        val r = years > 4 && mileage / years > 40000 || mileage / years < 5000

        if (r) {
          println(years + " " + mileage + " " + s.raw)
        }

        r
      })
  )*/

  /*success.groupBy(a => (a.vin -> a.groupId)).foreach {
    case (g, l) if l.size > 1 => {

      println(s"vin = ${g._1.vin}; id = ${g._2}")
      println("duplicates:")
      l.foreach(p => println(p.raw))
      println()
      println()

      acnt += 1
      /*l.foreach(p => println(p.raw))
      println()*/
    }
    case _ =>
  }*/

}
