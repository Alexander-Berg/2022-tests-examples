package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.autoteka

import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.autoteka.estimate.AutotekaEstimateRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.InvalidVinError
import ru.yandex.auto.vin.decoder.scheduler.local.utils.LocalScript
import auto.carfax.common.utils.misc.StringUtils.RichString

import java.io.{File, FileInputStream}
import scala.concurrent.Future

object AutotekaEstimateParser extends LocalScript {

  /* PARAMS */

  private val dir = new File("/home/plomovtsev/Desktop/Novocar")
  override protected val itemsWord: String = "files"

  /* DEPENDENCIES */

  private val manager = AutotekaEstimateRawModelManager(EventType.UNRECOGNIZED)
  private val nameManager = manager.nameManager

  override def action: Future[Any] = {

    val okNamedFiles = dir.listFiles
      .filter(file => nameManager.getDataTimestamp(file.getName, 123L).nonEmpty)

    progressBar.start(totalItems = okNamedFiles.size)

    val estimates = okNamedFiles.flatMap { file =>
      val res = manager.parseFile(new FileInputStream(file), file.getName)
      progressBar.inc(1)
      res
    }

    val success = estimates.collect { case Right(v) => v }
    val errors = estimates.collect { case Left(e) => e }

    def printlnPercentOfSuc(title: String, count: Int): Unit = {
      println(s"$title - $count (${(count * 1.0 / success.length * 100).toInt}%)")
    }

    println(s"total - ${estimates.size}")
    println(s"correct - ${success.size} (${(success.size * 1.0 / estimates.size * 100).toInt}%) ")
    println(s"errors - ${errors.size} (${(errors.size * 1.0 / estimates.size * 100).toInt}%)")
    println(s"records with invalid vin - ${errors.count(_.isInstanceOf[InvalidVinError])}")
    printlnPercentOfSuc("success records with warnings", success.count(_.warnings.nonEmpty))
    println()
    println(s"unique vins - ${success.map(_.identifier.toString).distinct.size}")
    println(s"unique ids - ${success.map(_.groupId).distinct.size}")
    val uniqueIdsAndVins = success.map(a => (a.identifier.toString -> a.groupId)).distinct.size

    val messageUniqueIdsAndVins = if (uniqueIdsAndVins == success.size) {
      "Unique (vin + id) count = success count. OK"
    } else {
      "ERROR! Unique (vin + id) count != success count"
    }
    println(
      s"unique (id + vin) - ${success.map(a => (a.identifier.toString -> a.groupId)).distinct.size}. $messageUniqueIdsAndVins"
    )
    println()
    printlnPercentOfSuc("records with mileages", success.count(_.model.common.mileage.exists(_ > 0)))
    printlnPercentOfSuc("records with photos", success.count(_.model.photos.exists(_.url.nonEmpty)))
    printlnPercentOfSuc(
      "records with issues (carfax: results.summary)",
      success.count(_.model.condition.exists(_.issues.exists(_.nonEmpty)))
    )
    printlnPercentOfSuc(
      "records with stars (carfax: results.inspectionDetails)",
      success.count(_.model.condition.exists(_.stars.exists(_.toString.nonEmpty)))
    )
    printlnPercentOfSuc("records with damages", success.count(_.model.damages.nonEmpty))
    printlnPercentOfSuc(
      "records with damages photos",
      success.count(_.model.damages.exists(_.photoUrl.flatMap(_.toOption).nonEmpty))
    )
    printlnGroupSizes("damaged parts", success.flatMap(_.model.damages).groupBy(_.part))
    printlnGroupSizes("damage types", success.flatMap(_.model.damages).groupBy(_.`type`))

    val errorz = errors.filterNot(_.isInstanceOf[InvalidVinError]).take(100)
    if (errorz.size > 0) {
      println()
      println()
      println("ERRORS LIST (without invalid vin):")
      errorz.map(_.humanText).foreach(println)
    }

    val warnings = success.flatMap(_.warnings).take(100)
    if (warnings.size > 0) {
      println()
      println()
      println("WARNINGS LIST:")
      warnings.map(_.humanText).foreach(println)
    }

    println()
    println()

    Future.unit
  }

  private def printlnGroupSizes[T](groupingName: String, groups: Map[String, Array[T]]): Unit = {
    if (groups.nonEmpty) {
      println()
      val biggest = groups.map { case (name, items) => name -> items.size }.toSeq.sortBy(-_._2).take(10)
      println(s"$groupingName (${biggest.size} distinct values):")
      biggest.foreach { case (name, count) => println(s"$count - $name") }
      if (biggest.size != groups.size) {
        println("... (10 most frequent shown) ...")
      }
    }
  }
}
