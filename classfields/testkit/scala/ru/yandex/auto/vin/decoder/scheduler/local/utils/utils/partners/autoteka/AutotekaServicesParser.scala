package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.autoteka

import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoteka.services.AutotekaServicesRawModelManager
import ru.yandex.auto.vin.decoder.raw.autoteka.services.model.AutotekaServicesRawModel
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.InvalidVinError

import java.io.{File, FileInputStream}

object AutotekaServicesParser extends App {

  val manager = AutotekaServicesRawModelManager(FileFormats.Csv, ",", EventType.NOVOCAR_SALES)

  val dir = new File("/home/plomovtsev/Desktop/rolf_services")

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
  val zeroMileagesCount = success.count(_.model.common.mileage.exists(_ == 1))
  println(s"zero mileages count - $zeroMileagesCount (${(zeroMileagesCount * 1.0 / success.size * 100).toInt} %)")

  val recordsWithServicesCount: Int = success.count(_.model.services.nonEmpty)

  println(
    s"records with services list - $recordsWithServicesCount (${(recordsWithServicesCount * 1.0 / success.size * 100).toInt} %)"
  )
  val recordsWithServicesMoreONeCount: Int = success.count(_.model.services.length > 1)

  println(
    s"records with services list size > 1 - $recordsWithServicesMoreONeCount (${(recordsWithServicesMoreONeCount * 1.0 / success.size * 100).toInt} %)"
  )

  println()
  println()

  val maxDate = {
    val max = success.maxByOption(_.model.event.timestamp)
    println(s"max timestamp - ${max.map(_.model.event.timestamp)} data = ${max.map(_.raw)}")
  }

  val minDate = {
    val min = success.minByOption(_.model.event.timestamp)
    println(s"min timestamp - ${min.map(_.model.event.timestamp)} data = ${min.map(_.raw)}")
  }

  println()
  println()
  println("ERRORS LIST (without invalid vin):")
  errors.take(100).filterNot(_.isInstanceOf[InvalidVinError]).foreach(e => println(e.humanText))

  println()
  println()
  println("WARNINGS LIST:")
  val warnings = success.flatMap(_.warnings).take(100)
  warnings.foreach(w => println(w.humanText))

  private val isAllTheSame = (l: List[AutotekaServicesRawModel]) => {
    val dropFilename: String => String = _.replaceAll("\"filename\":\".*?\",", "")
    l.forall(cur => dropFilename(cur.raw) == dropFilename(l.head.raw))
  }

  println()
  println()

  success.groupBy(a => (a.identifier -> a.groupId)).foreach {
    case (g, l) if l.size > 1 && !isAllTheSame(l) => {

      println(s"vin = ${g._1}; id = ${g._2}")
      println("duplicates:")
      l.foreach(p => println(p.raw))
      println()
      println()

      /*l.foreach(p => println(p.raw))
      println()*/
    }
    case _ =>
  }
}
