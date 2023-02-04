package ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator

import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.{FailureMessage, OfferMessage}
import ru.yandex.vertis.feedprocessor.autoru.model.{Generators, Messages}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.ModificationString
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator.CatalogXmlIterator.CatalogData

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by sievmi on 20.04.18
  */

object UnificatorChecker extends App with TestApplication {

  /*
    https://helpdesk.auto.ru/catalog/cars

    auto-searcher-01-sas.test.vertis.yandex.net
    sievmi-01-sas.dev.vertis.yandex.net:34389

    /Users/sievmi/Desktop/
   */

  private val bucketSize = 40
  private val statisticsBucket = 150
  private val awaitTime = 10.seconds

  private var cntHttpErrors = 0
  private var cntUnificatorErrors = 0
  private var cntTechParamIdErrors = 0

  val dataPreparer = new DataPreparer(parseArgs(args.toIndexedSeq))

  val time = System.currentTimeMillis()

  val carsUnificator = dataPreparer.createCarsUnificator
  val parsedCatalog = dataPreparer.getParsedCatalog
  val writer = dataPreparer.createCSVWriter()

  writer.startLine()

  for (i <- 0 to (parsedCatalog.size + bucketSize - 1) / bucketSize) {

    printStatistics(i)
    val batch: ListBuffer[CatalogData] = ListBuffer.empty

    for (j <- 0 to Math.min(bucketSize, parsedCatalog.size - bucketSize * i - 1)) {
      batch += parsedCatalog(bucketSize * i + j)
    }

    val unifyDataSeq = batch
      .flatMap(data => {
        val generatedOffer = AutoruGenerators.carExternalOfferGen(Generators.newTasksGen).next
        try {
          // пробуем унифицировать со всеми возможными годами производства
          for (year <- data.startYear.toInt to data.endYear.toInt)
            yield UnifyData(
              data,
              generatedOffer.copy(
                modification = ModificationString(data.modification),
                mark = data.mark,
                model = data.model,
                bodyType = data.bodyType,
                year = year
              )
            )
        } catch {
          case _: NumberFormatException =>
            Seq(
              UnifyData(
                data,
                generatedOffer.copy(
                  modification = ModificationString(data.modification),
                  mark = data.mark,
                  model = data.model,
                  bodyType = data.bodyType
                )
              )
            )
        }
      })
      .toSeq

    val offers = unifyDataSeq.map(_.offer)

    try {
      val results = Await.result(carsUnificator.unify(offers), awaitTime)
      checkResult(writer, unifyDataSeq, results)
    } catch {
      case _: Exception =>
        // отправляем запросы по одному
        unifyDataSeq.foreach(data => {
          try {
            val result = Await.result(carsUnificator.unify(Seq(data.offer)), awaitTime)
            checkResult(writer, Seq(data), result)
          } catch {
            case e: Exception =>
              cntHttpErrors += 1
              writer.writeLine(data, "", "", e.toString)
          }
        })
    }
  }

  dataPreparer.closeResources()

  println("---------")
  println(s"finish 100% in ${System.currentTimeMillis() - time} ms ${parsedCatalog.size}/${parsedCatalog.size}")
  println(
    s"Unificator errors: $cntUnificatorErrors. Tech param id errors: " +
      s"$cntTechParamIdErrors. Http errors: $cntHttpErrors."
  )

  private def printStatistics(i: Int): Unit = {
    if (i > 0 && i % statisticsBucket == 0) {
      println(
        f"${(i * bucketSize + 0.0) / parsedCatalog.size * 100}%.2f %% finished in " +
          f"${System.currentTimeMillis() - time} ms. ${i * bucketSize}/${parsedCatalog.size}"
      )
      println(
        s"Unificator errors: $cntUnificatorErrors. Tech param id errors: " +
          s"$cntTechParamIdErrors. Http errors: $cntHttpErrors."
      )
    }
  }

  def checkResult(
      writer: CSVWriter,
      unifyData: Seq[UnifyData],
      results: Seq[Messages.OutgoingMessage[CarExternalOffer]]): Unit = {

    unifyData
      .zip(results)
      .foreach(pair => {
        val isSuccess = pair._2.isInstanceOf[OfferMessage[CarExternalOffer]]
        val unificatorTechParamId = if (isSuccess) {
          pair._2.asInstanceOf[OfferMessage[CarExternalOffer]].offer.unification.get.techParamId.get
        } else -1

        val unificatorTechParamIdString = {
          if (unificatorTechParamId == -1) "" else unificatorTechParamId.toString
        }

        val error = if (!isSuccess) {
          pair._2.asInstanceOf[FailureMessage[CarExternalOffer]].error.toString
        } else ""

        if (!isSuccess) {
          cntUnificatorErrors += 1
          writer.writeLine(pair._1, unificatorTechParamIdString, error, "")
        } else if (pair._1.catalogData.techParamId.toLong != unificatorTechParamId) {
          cntTechParamIdErrors += 1
          writer.writeLine(pair._1, unificatorTechParamIdString, "", "")
        }
      })
  }

  case class UnifyData(catalogData: CatalogData, offer: CarExternalOffer)

  case class Arguments(catalogPath: Option[String], unificatorUrl: Option[String], outPath: Option[String])

  def parseArgs(args: Seq[String]): Arguments = {
    var catalogPath: Option[String] = None
    var unificatorUrl: Option[String] = None
    var outPath: Option[String] = None
    args.sliding(2, 2).toList.collect {
      case Seq("--catalog", value: String) => catalogPath = Some(value)
      case Seq("--unificator", value: String) => unificatorUrl = Some(value)
      case Seq("--out", value: String) => outPath = Some(value)
    }

    Arguments(catalogPath, unificatorUrl, outPath)
  }
}
