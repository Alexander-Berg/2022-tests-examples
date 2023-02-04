package ru.yandex.auto.vin.decoder.partners.audatex

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.CarfaxPalma.AudatexDetailDescription
import ru.auto.api.vin.VinResolutionEnums.Status
import ru.yandex.auto.vin.decoder.proto.VinHistory.AdaperioAudatex
import ru.yandex.auto.vin.decoder.providers.palma.{AudatexDictionaries, AudatexDictionaryProvider}
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.audatex.PreparedAudatexData
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.history.entities.AudatexPreparedReportHistoryEntity
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.IterableHasAsJava

class AudatexTextCleanerSpec extends AnyFunSuite with MockitoSupport {

  private val TrueFeature = new Feature[Boolean] {
    override def name: String = ""
    override def value: Boolean = true
  }

  private val FalseFeature = new Feature[Boolean] {
    override def name: String = ""
    override def value: Boolean = false
  }

  test("should clean different cases") {
    val cleaner: AudatexTextCleaner = {
      val locations = {
        val raw = ResourceUtils.getStringFromResources(s"/audatex/dictionaries/placements.csv")
        raw
          .split("\r\n")
          .toList
          .flatMap(_.split(",", -1).toList match {
            case clean :: rawLocations :: Nil =>
              rawLocations.split("@@").map(_ -> clean).toList
            case _ => throw new IllegalArgumentException("not allowed")

          })
          .foldLeft(ListMap[String, String]()) { case (acc, (k, v)) =>
            acc + (k -> v)
          }
      }

      val descriptions = {
        val raw = ResourceUtils.getStringFromResources(s"/audatex/dictionaries/descriptions.csv")
        val rows = raw.split("\r\n").toList
        val parsedValues = rows.map { row =>
          var cols = row.split(",", -1).toList
          if (row.contains("\"")) {
            if (cols.head.contains("\"")) {
              val col0 = (cols.head + cols(1)).replaceAll("\"", "")
              cols = col0 :: cols.drop(2)
            }
          }
          cols match {
            case rawDescription :: prettyDescription :: operation :: location :: Nil =>
              AudatexDetailDescription
                .newBuilder()
                .setRawDescription(rawDescription)
                .setPrettyDescription(prettyDescription)
                .setOperation(operation)
                .setLocation(location)
                .build()
            case _ => throw new IllegalArgumentException("not allowed")
          }
        }
        parsedValues.map(v => v.getRawDescription -> v).toMap
      }

      val dictionary = AudatexDictionaries(locations = locations, descriptions = descriptions)
      val provider = mock[AudatexDictionaryProvider]
      when(provider.get()).thenReturn(dictionary)
      new AudatexTextCleaner(provider, TrueFeature)
    }
    val raw = ResourceUtils.getStringFromResources(s"/audatex/dictionaries/expected_mapping.csv")
    raw.split("\n").foreach { row =>
      row.split(",").toList match {
        case rawDescription :: expectedResult :: Nil =>
          println(s"Test: $rawDescription -> $expectedResult")
          assert(cleaner.prettify(rawDescription).await == expectedResult)
        case _ => throw new IllegalArgumentException("not allowed")

      }
    }
  }

  test("should simply capitalize words in not enabled") {
    val provider = mock[AudatexDictionaryProvider]
    val cleaner = new AudatexTextCleaner(provider, FalseFeature)
    val testablePairs = List(
      ("Б", "Б"),
      ("ЗАМЕНА МАСЛА", "Замена масла"),
      ("R.2 РАДИАТОР", "R.2 радиатор"),
      ("пОрШнЕвЫе., ДиСКИ", "Поршневые., диски")
    )
    val data = buildAudatexData(testablePairs.map(_._1))
    val processor = cleaner.buildProcessor(Some(data)).await.get
    for {
      (input, expectedOutput) <- testablePairs
    } yield {
      assert(processor(input) == expectedOutput)
    }
  }

  test("should capitalize words for records not found in dictionary") {
    val provider = mock[AudatexDictionaryProvider]
    when(provider.get()).thenReturn(AudatexDictionaries(Map.empty, Map.empty))
    val cleaner = new AudatexTextCleaner(provider, TrueFeature)
    val testablePairs = List(
      ("Б", "Б"),
      ("ЗАМЕНА МАСЛА", "Замена масла"),
      ("R.2 РАДИАТОР", "R.2 радиатор"),
      ("пОрШнЕвЫе., ДиСКИ", "Поршневые., диски")
    )
    val data = buildAudatexData(testablePairs.map(_._1))
    val processor = cleaner.buildProcessor(Some(data)).await.get
    for {
      (input, expectedOutput) <- testablePairs
    } yield {
      assert(processor(input) == expectedOutput)
    }
  }

  private def buildAudatexData(works: List[String]): PreparedAudatexData = {
    def toWork(description: String): AdaperioAudatex.Work =
      AdaperioAudatex.Work.newBuilder().setDescription(description).build()
    val entity: AudatexPreparedReportHistoryEntity = AudatexPreparedReportHistoryEntity(
      1,
      AdaperioAudatex.Report.getDefaultInstance.toBuilder
        .addAllWorks(works.map(toWork).asJava)
        .build(),
      None,
      isRed = false,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      "",
      Seq.empty,
      None
    )
    PreparedAudatexData(isUpdating = false, status = Status.OK, isReady = true, entities = List(entity))
  }
}
