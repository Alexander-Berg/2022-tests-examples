package ru.auto.api.managers.comeback

import org.scalatest.enablers.Emptiness.emptinessOfGenTraversable
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.ResponseStatus
import ru.auto.api.exceptions.{ComebackExportPeriodFilterTooLong, RegularPaymentsRequired}
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.{CategorySelector, OfferID}
import ru.auto.api.services.carfax.CarfaxClient
import ru.auto.api.services.comeback.ComebackClient
import ru.auto.comeback.proto.ComebackServiceOuterClass
import ru.auto.comeback.proto.ComebackServiceOuterClass.{ComebackExportResponse, ComebackListResponse}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class ComebackManagerSpec extends BaseSpec with MockitoSupport with TestRequest with ScalaCheckPropertyChecks {

  val comebackClient: ComebackClient = mock[ComebackClient]
  val carfaxClient: CarfaxClient = mock[CarfaxClient]
  val offerLoader: EnrichedOfferLoader = mock[EnrichedOfferLoader]

  val comebackManager = new ComebackManager(
    comebackClient,
    carfaxClient,
    offerLoader
  )

  "ComebackManagerSpec" should {
    val request = comebackListingRequestGen.next

    val downloadUri = "https://auto-export.s3.yandex.net/comeback_export.xlsx"

    val exportSuccesResponse =
      ComebackExportResponse
        .newBuilder()
        .setDownloadUri(downloadUri)
        .setStatus(ComebackServiceOuterClass.ResponseStatus.SUCCESS)
        .build

    def exportFailedResponse(code: ComebackServiceOuterClass.ErrorCode) =
      ComebackExportResponse
        .newBuilder()
        .setError(code)
        .setStatus(ComebackServiceOuterClass.ResponseStatus.ERROR)
        .build

    def listingFailedResponse(code: ComebackServiceOuterClass.ErrorCode) =
      ComebackListResponse
        .newBuilder()
        .setError(code)
        .setStatus(ComebackServiceOuterClass.ResponseStatus.ERROR)
        .build

    "getComebacks complete work" in {
      val comebackListingResponse = comebackListResponseGen.next
      val dealerRequest = dealerRequestGen.next
      val rawEssentialsReportResponse = rawEssentialsReportResponseGen.next

      val shouldResultComeBacks = comebackListingResponse.getComebacksList.asScala.map { comeback =>
        val offer = enrichOfferByDocuments(comeback.getOffer)
        val offerID = OfferID.parse(offer.getId)
        val category = CategorySelector.from(offer.getCategory)
        when(offerLoader.getOffer(eq(category), eq(offerID), ?, ?, ?, ?)(?)).thenReturnF(offer)
        val builder = comeback.toBuilder.setOffer(offer)
        builder.getMetaBuilder.setVinReportItemsCount(5)
        builder.build()
      }

      comebackListingResponse.getComebacksList.asScala.filter { x =>
        !x.getOffer.getDocuments.getVin.isEmpty
      } shouldBe empty
      comebackListingResponse.getComebacksList.asScala.filter(_.getMeta.getVinReportItemsCount != 0) shouldBe empty

      val dealer = dealerRequest.user.ref.asDealer
      when(comebackClient.getComebacks(?, ?, ?, ?)(?)).thenReturnF(comebackListingResponse)
      when(carfaxClient.getRawEssentialsReport(?)(?)).thenReturnF(rawEssentialsReportResponse)

      val response = comebackManager.getComebacks(request)(dealerRequest).await

      response.getComebacksList.asScala should contain theSameElementsAs shouldResultComeBacks
    }

    // фронт ожидает status == SUCCESS в ответе
    // https://github.com/YandexClassifieds/autoru-frontend/blob/e52d49876a0cfe53405d27ca40924769ce48e9e2/auto-core/react/lib/gateApiClass.js#L180-L183
    "getComebacks should return status = SUCCESS" in {
      val response = comebackManager.getComebacks(request)(dealerRequestGen.next).await
      response.hasStatus shouldBe true
      response.getStatus shouldBe ResponseStatus.SUCCESS
    }

    "getComebacks should fail with 402, because payment is irregular" in {
      val dealerRequest = dealerRequestGen.next

      when(comebackClient.getComebacks(?, ?, ?, ?)(?))
        .thenReturnF(listingFailedResponse(ComebackServiceOuterClass.ErrorCode.COMEBACK_REGULAR_PAYMENT_REQUIRED))

      assertThrows[RegularPaymentsRequired] {
        comebackManager.getComebacks(request)(dealerRequest).await
      }
    }

    "getComebacks return without enrich" in {
      val comebackListingResponse = comebackListResponseGen.next
      val dealerRequest = dealerRequestGen.next
      val rawEssentialsReportResponse = rawEssentialsReportResponseGen.next

      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.failed(new Exception))

      comebackListingResponse.getComebacksList.asScala.filter { x =>
        !x.getOffer.getDocuments.getVin.isEmpty
      } shouldBe empty
      comebackListingResponse.getComebacksList.asScala.filter(_.getMeta.getVinReportItemsCount != 0) shouldBe empty

      when(comebackClient.getComebacks(?, ?, ?, ?)(?)).thenReturnF(comebackListingResponse)
      when(carfaxClient.getRawEssentialsReport(?)(?)).thenReturnF(rawEssentialsReportResponse)

      val response = comebackManager.getComebacks(request)(dealerRequest).await

      response.getComebacksList.asScala should contain theSameElementsAs
        comebackListingResponse.getComebacksList.asScala
    }

    "getComebacks return with updated offer but without VIN report" in {
      val comebackListingResponse = comebackListResponseGen.next
      val dealerRequest = dealerRequestGen.next

      val shouldResultComeBacks = comebackListingResponse.getComebacksList.asScala.map { comeback =>
        val offer = enrichOfferByDocuments(comeback.getOffer)
        val offerID = OfferID.parse(offer.getId)
        val category = CategorySelector.from(offer.getCategory)
        when(offerLoader.getOffer(eq(category), eq(offerID), ?, ?, ?, ?)(?)).thenReturnF(offer)
        val builder = comeback.toBuilder.setOffer(offer)
        builder.build()
      }

      comebackListingResponse.getComebacksList.asScala.filter { x =>
        !x.getOffer.getDocuments.getVin.isEmpty
      } shouldBe empty
      comebackListingResponse.getComebacksList.asScala.filter(_.getMeta.getVinReportItemsCount != 0) shouldBe empty

      when(comebackClient.getComebacks(?, ?, ?, ?)(?)).thenReturnF(comebackListingResponse)
      when(carfaxClient.getRawEssentialsReport(?)(?)).thenReturn(Future.failed(new Exception))

      val response = comebackManager.getComebacks(request)(dealerRequest).await

      response.getComebacksList.asScala should contain theSameElementsAs shouldResultComeBacks
    }

    "exportComebacks" in {
      when(comebackClient.exportComebacks(?, ?, ?)(?)).thenReturnF(exportSuccesResponse)
      val response = comebackManager.exportComebacks(comebackExportRequestGen.next)(dealerRequest).await

      response.hasStatus shouldBe true
      response.getStatus shouldBe ResponseStatus.SUCCESS
      response.hasDownloadUri shouldBe true
      response.getDownloadUri shouldBe downloadUri
      response.hasError shouldBe false
    }

    "exportComebacks should fail with 402, because payment is irregular" in {
      val dealerRequest = dealerRequestGen.next

      when(comebackClient.exportComebacks(?, ?, ?)(?))
        .thenReturnF(exportFailedResponse(ComebackServiceOuterClass.ErrorCode.COMEBACK_REGULAR_PAYMENT_REQUIRED))

      assertThrows[RegularPaymentsRequired] {
        comebackManager.exportComebacks(comebackExportRequestGen.next)(dealerRequest).await
      }
    }

    "exportComebacks should fail with 400, because long date period filter" in {
      val dealerRequest = dealerRequestGen.next

      when(comebackClient.exportComebacks(?, ?, ?)(?)).thenReturnF(
        exportFailedResponse(ComebackServiceOuterClass.ErrorCode.COMEBACK_EXPORT_PERIOD_FILTER_IS_TOO_LONG)
      )

      assertThrows[ComebackExportPeriodFilterTooLong] {
        comebackManager.exportComebacks(comebackExportRequestGen.next)(dealerRequest).await
      }
    }
  }
}
