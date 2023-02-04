package ru.yandex.veratis.general.gost.stages.photo.logic.test

import com.google.protobuf.duration.Duration
import com.google.protobuf.empty.Empty
import common.scalapb.ScalaProtobuf.instantToTimestamp
import general.common.image_model.MDSImage
import general.common.price_model
import general.common.price_model.Price
import general.common.price_model.Price.Price.PriceInCurrency
import general.common.seller_model.SellerId
import general.common.seller_model.SellerId.SellerId.UserId
import general.gost.offer_model.AttributeValue.Value.Number
import general.gost.offer_model.OfferStatusEnum.OfferStatus
import general.gost.offer_model._
import general.gost.seller_model.Seller
import ru.yandex.vertis.general.gost.stages.photo.logic.PhotoStage
import ru.yandex.vertis.general.gost.stages.photo.logic.testkit.TestPicaService
import ru.yandex.vertis.general.gost.stages.public.logic.Stage.ProcessResult
import ru.yandex.vertis.pica.service.pica_service.ApiModel.{GetOrPutRequest, ImageContext}
import ru.yandex.vertis.pica.model.model.ProcessingResult.{Error => PicaError, Result => PResult, _}
import ru.yandex.vertis.pica.model.model.{Image, ImageResult, ProcessingResult}
import zio.test.environment.TestEnvironment
import zio.test.mock._
import zio.test._
import zio.{Has, UIO, ULayer, ZLayer}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.util.Random
import scala.util.chaining._
import scala.concurrent.duration.DurationInt

object PhotoStageSpec extends DefaultRunnableSpec {

  import Assertion._
  import Expectation._

  private type TestData = (ULayer[Has[Map[GetOrPutRequest, ProcessingResult]]], OfferView, ProcessResult)

  private val now = Instant.ofEpochMilli(System.currentTimeMillis())
  private val oneHour = now.plus(1, ChronoUnit.HOURS)
  private val tenMinutes = now.plus(10, ChronoUnit.MINUTES)
  private val fiveMinutes = now.plus(5, ChronoUnit.MINUTES)
  private val thirtyMinutes = now.plus(30, ChronoUnit.MINUTES)

  private val name = s"name_${Random.nextString(5)}"
  private val offerId = s"offerId_${Random.nextInt(4)}"
  private val groupId = Random.nextInt(4)
  private val namespace = "photo_stage"
  private val ttl = Some(30.minutes)
  private val configLayer = ZLayer.succeed(PhotoStage.PhotoConfig(namespace, ttl))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PhotoStage")(
      testM("queued") {
        val picaResults = Seq(
          PResult.Queued(Queued(Some(instantToTimestamp(fiveMinutes)))),
          PResult.Queued(Queued(Some(instantToTimestamp(tenMinutes)))),
          PResult.Queued(Queued(Some(instantToTimestamp(oneHour))))
        )

        testing(generateTestData(expectedScheduledAt = Some(fiveMinutes), picaResults: _*))
      },
      testM("reloading") {
        testing(generateTestData(expectedScheduledAt = Some(oneHour), PResult.Reloading(Reloading(None))))
      },
      testM("processed") {
        testing(
          generateTestData(expectedScheduledAt = Some(thirtyMinutes), newProcessed(namespace, groupId.toString, name))
        )
      },
      testM("failure") {
        testing(generateTestData(expectedScheduledAt = Some(fiveMinutes), ProcessingResult.Result.Empty))
      },
      testM("failed") {
        testing(generateTestData(expectedScheduledAt = None, PResult.Error(PicaError(Seq.empty))))
      },
      testM("mixed") {
        val picaResults = Seq(
          newProcessed(namespace, groupId.toString, name),
          newProcessed(namespace, groupId.toString, name),
          PResult.Error((PicaError(Seq.empty))),
          PResult.Queued(Queued(Some(instantToTimestamp(tenMinutes)))),
          PResult.Reloading(Reloading(None)),
          ProcessingResult.Result.Empty
        )
        testing(generateTestData(expectedScheduledAt = Some(fiveMinutes), picaResults: _*))
      }
    )

  private def clockLayer =
    MockClock
      .CurrentTime(equalTo(TimeUnit.MILLISECONDS), value(now.toEpochMilli))

  private def testing(testData: TestData): UIO[TestResult] = {
    val (inputData, offerView, processResult) = testData
    val picaLayer = inputData >>> TestPicaService.layer
    val photoStage = picaLayer ++ clockLayer ++ configLayer >>> PhotoStage.live
    assertM(photoStage.build.use(_.get.process(offerView, state = None))) {
      hasField("newOffer", (_: ProcessResult).newOffer, equalTo(processResult.newOffer)) &&
      hasField("newState", (_: ProcessResult).newState, equalTo(processResult.newState)) &&
      hasField("newTimer", (_: ProcessResult).newTimer, equalTo(processResult.newTimer))
    }
  }

  private def generateTestData(
      expectedScheduledAt: Option[Instant],
      picaResults: ProcessingResult.Result*): (ULayer[Has[Map[GetOrPutRequest, ProcessingResult]]], OfferView, ProcessResult) = {

    val photos = getRandomPhotos

    picaResults
      .map {
        case picaResult: PResult.Processed =>
          val url = s"url/${Random.nextString(8)}"
          (newGetOrPutRequest(namespace, url) -> picaResult, Photo(None, url), newPhoto(url, namespace, groupId, name))
        case picaResult: PResult.Error =>
          val url = s"url/${Random.nextString(8)}"
          (
            newGetOrPutRequest(namespace, url) -> picaResult,
            Photo(None, url),
            Photo(None, url, Some(Empty.defaultInstance))
          )
        case picaResult =>
          val url = s"url/${Random.nextString(8)}"
          (newGetOrPutRequest(namespace, url) -> picaResult, Photo(None, url), Photo(None, url))
      }
      .unzip3
      .pipe { case (picaMock, inputPhotos, photosResult) =>
        val offerView = getValidOfferView(offerId, "title").update(_.offer.photos := photos ++ inputPhotos)
        val offerViewResult = offerView.update(_.offer.photos := photos ++ photosResult)
        (
          ZLayer.succeed(picaMock.map { case (k, v) => (k, ProcessingResult(v)) }.toMap),
          offerView,
          ProcessResult(offerViewResult, None, expectedScheduledAt)
        )
      }
  }

  private def getRandomPhotos = {
    def name = s"name_${Random.nextString(5)}"
    def url = s"url/${Random.nextString(8)}"
    def groupId = Random.nextInt(4)
    Seq.fill(Random.nextInt(1))(newPhoto(url, namespace, groupId, name))
  }

  private def newGetOrPutRequest(namespace: String, url: String) =
    GetOrPutRequest(Some(ImageContext(namespace, url)), ttl = ttl.map(d => Duration(d.toSeconds)))

  private def newProcessed(namespace: String, groupId: String, name: String) = {
    PResult.Processed(
      new Processed(
        Some(
          ImageResult(
            Some(
              Image(
                namespace = namespace,
                groupId = groupId,
                name = name
              )
            )
          )
        )
      )
    )
  }

  private def newPhoto(url: String, namespace: String, groupId: Int, name: String) =
    Photo(
      Some(
        MDSImage(
          namespace = namespace,
          groupId = groupId,
          name = name
        )
      ),
      url,
      expireTime = ttl.map(d => instantToTimestamp(now.plusSeconds(d.toSeconds)))
    )

  private def getValidOfferView(offerId: String, title: String) = OfferView(
    sellerId = Some(SellerId(UserId(123))),
    offerId = offerId,
    offer = Some(
      Offer(
        title = title,
        description = "DESCRIPTION",
        price = Some(Price(PriceInCurrency(price_model.PriceInCurrency(100)))),
        category = Some(Category("category_id", 1)),
        attributes = Seq(Attribute("attribute_id", 2, Some(AttributeValue(Number(3))))),
        seller = Some(Seller())
      )
    ),
    status = OfferStatus.ACTIVE,
    version = 1
  )
}
