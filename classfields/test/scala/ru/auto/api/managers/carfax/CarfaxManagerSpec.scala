package ru.auto.api.managers.carfax

import org.mockito.Mockito.{reset, verify}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.{CreateVinHistoryScoreResponse, ResponseStatus}
import ru.auto.api.Validations.ValidationError
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.{CreateScoreForNotBoughtHistory, HistoryScoreValidationException}
import ru.auto.api.managers.carfax.report.EssentialsReportManager
import ru.auto.api.managers.offers.OfferCardManager
import ru.auto.api.managers.vin.HistoryReportPriceManager.createPaidServicePrice
import ru.auto.api.model.ModelGenerators.{AnonymousUserRefGen, PrivateUserRefGen}
import ru.auto.api.model.RequestParams
import ru.auto.api.services.carfax.CarfaxClient
import ru.auto.api.util.RequestImpl
import ru.auto.api.vin.Common.IdentifierType
import ru.auto.api.vin.ResponseModel.{ReportResponse, VinDecoderError, VinResponse}
import ru.auto.api.vin.VinApiModel.{Report, ReportParams, VinHistoryScore}
import ru.auto.api.vin.VinReportModel.RawVinEssentialsReport
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionModel.{Block, KmAgeHistory, Resolution}
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.auto.salesman.model.user.ApiModel._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

class CarfaxManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with AsyncTasksSupport {
  val carfaxClient: CarfaxClient = mock[CarfaxClient]
  val walletManager: CarfaxWalletManager = mock[CarfaxWalletManager]
  val offerCardManager: OfferCardManager = mock[OfferCardManager]
  val essentialsReportManager = mock[EssentialsReportManager]

  before {
    reset(carfaxClient, walletManager)
  }

  override def beforeAll(): Unit = {
    reset(carfaxClient, walletManager)
  }

  val manager =
    new CarfaxManager(
      carfaxClient,
      walletManager,
      essentialsReportManager
    )

  val vin = "Z8T4C5S19BM005269"
  val maskedVin = "Z8T**************"
  val licensePlate = "A777AA777"

  implicit private val trace: Traced = Traced.empty

  private def generateAnonymReq: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.desktop)
    r.setUser(AnonymousUserRefGen.next)
    r.setTrace(trace)
    r
  }

  private def generatePrivateReq: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r.setUser(PrivateUserRefGen.next)
    r.setTrace(trace)
    r
  }

  val FALSE = BooleanResponse.newBuilder().setResult(false).build()
  val TRUE = BooleanResponse.newBuilder().setResult(true).build()

  val vinResolution = Resolution
    .newBuilder()
    .setVin(vin)
    .setKmAgeHistory(
      KmAgeHistory.newBuilder().addBlocks(Block.newBuilder())
    )
    .build()

  val vinResponse = VinResponse.newBuilder().setVin(vin).build()

  val vinInProgressResponse = VinResponse
    .newBuilder()
    .setError(
      VinDecoderError
        .newBuilder()
        .setErrorCode(VinDecoderError.Code.IN_PROGRESS)
    )
    .build()

  val report = Report
    .newBuilder()
    .setParams(ReportParams.newBuilder().build)
    .setResolution(vinResolution)
    .build

  val reportResponse = ReportResponse
    .newBuilder()
    .setReport(
      report
    )
    .build()

  val reportInProgress = ReportResponse
    .newBuilder()
    .setError(
      VinDecoderError
        .newBuilder()
        .setErrorCode(VinDecoderError.Code.IN_PROGRESS)
    )
    .build()

  val billing = List(
    createPaidServicePrice(
      ProductPrice
        .newBuilder()
        .setProductPriceInfo(
          ProductPriceInfo
            .newBuilder()
            .addAliases("offers-history-reports-10")
            .build
        )
        .setPrice(
          Price
            .newBuilder()
            .setBasePrice(1000)
            .setEffectivePrice(1000)
            .build
        )
        .build()
    )
  )

  "CarfaxManager.createVinHistoryScore" should {

    "successfully create vin history score" in {
      when(walletManager.getPurchaseInfo(?)(?)).thenReturnF(PurchaseInfo(true, None))
      when(carfaxClient.createVinHistoryScore(?)(?))
        .thenReturnF(CreateVinHistoryScoreResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build())
      when(essentialsReportManager.getReport(?)(?))
        .thenReturnF(RawVinEssentialsReport.newBuilder().setStatus(VinResolutionEnums.Status.OK).build())

      val req = generatePrivateReq
      val score = VinHistoryScore
        .newBuilder()
        .setUserId(req.user.userRef.toPlain)
        .setVin(vin)
        .setScore(VinHistoryScore.Score.POSITIVE)
        .build()
      val res = manager.createVinHistoryScore(score)(req)

      verify(carfaxClient).createVinHistoryScore(?)(?)
      assert(res.await.getStatus == ResponseStatus.SUCCESS)
      assert(res.await.getValidationErrorsCount == 0)
    }

    "create vin history score: not bought history" in {
      when(walletManager.getPurchaseInfo(?)(?)).thenReturnF(PurchaseInfo.NotBought)
      when(carfaxClient.createVinHistoryScore(?)(?))
        .thenReturnF(CreateVinHistoryScoreResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build())
      val req = generatePrivateReq
      val score = VinHistoryScore
        .newBuilder()
        .setUserId(req.user.userRef.toPlain)
        .setVin(vin)
        .setScore(VinHistoryScore.Score.POSITIVE)
        .build()

      assertThrows[CreateScoreForNotBoughtHistory] {
        manager.createVinHistoryScore(score)(req).await
      }
    }

    "create vin history score: validation errors" in {
      when(walletManager.getPurchaseInfo(?)(?)).thenReturnF(PurchaseInfo(true, None))
      when(carfaxClient.createVinHistoryScore(?)(?))
        .thenReturnF(
          CreateVinHistoryScoreResponse
            .newBuilder()
            .setStatus(ResponseStatus.ERROR)
            .addValidationErrors(ValidationError.newBuilder())
            .build()
        )
      val req = generatePrivateReq
      val score = VinHistoryScore
        .newBuilder()
        .setUserId(req.user.userRef.toPlain)
        .setVin(vin)
        .setScore(VinHistoryScore.Score.POSITIVE)
        .build()

      assertThrows[HistoryScoreValidationException] {
        manager.createVinHistoryScore(score)(req).await
      }
      verify(carfaxClient).createVinHistoryScore(?)(?)
    }
  }

  "CarfaxManager.recognizeIdentifier" should {
    "return license plate" in {
      val rawLp = "A 123 AA 77  "
      val lp = "А123АА77"

      val res = manager.recognizeIdentifier(rawLp)

      res._1 shouldBe IdentifierType.LICENSE_PLATE
      res._2 shouldBe lp
    }

    "return vin" in {
      val rawVin = "WAUZZZF24LN008 861 "
      val vin = "WAUZZZF24LN008861"

      val res = manager.recognizeIdentifier(rawVin)

      res._1 shouldBe IdentifierType.VIN
      res._2 shouldBe vin
    }

    "return unknown" in {
      val identifier = "WAU "

      val res = manager.recognizeIdentifier(identifier)

      res._1 shouldBe IdentifierType.UNKNOWN_IDENTIFIER
      res._2 shouldBe identifier
    }
  }

}
