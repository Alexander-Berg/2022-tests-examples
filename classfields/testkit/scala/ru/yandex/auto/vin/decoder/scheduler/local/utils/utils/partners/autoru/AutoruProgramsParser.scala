package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.autoru

import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.programs.AutoruFormatProgramsRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldError
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.InvalidVinError

import java.io.{File, FileInputStream}

object AutoruProgramsParser extends App {

  val manager = new AutoruFormatProgramsRawModelManager(EventType.ROLF_PROGRAMS, FileFormats.Json)

  val dir = new File("/Users/degtev-o/Downloads/rolf")

  val start = System.currentTimeMillis()

  val iterator = dir
    .listFiles()
    .iterator
    .flatMap(file => {
      manager.nameManager
        .getDataTimestamp(file.getName, 123L)
        .map(ts => {
          println(file.getName)
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

  success.groupBy(a => (a.identifier -> a.groupId)).map {
    case (_, l) if l.size > 1 =>
      l.foreach(a => println(a.raw))
      println()
      println()
    case _ =>
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
  println()
  println("ERRORS LIST:")

  errors.collect { case e: ValidationFieldError =>
    println(s">>> ${e.humanText}")
    println(e.context)
    println()
  }

  println()
  println()
  println("WARNINGS LIST:")
  val warnings = success.flatMap(_.warnings).take(100)
  warnings.foreach(w => println(w.humanText))
}
