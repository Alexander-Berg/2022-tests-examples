package ru.yandex.realty.managers.products

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.managers.products.JuridicalProductsRequestValidator.RequestValidationResult
import ru.yandex.realty.managers.products.status.ProductData
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.proto.api.v2.JuridicalProducts.{ProcessedOffer, ProcessedProduct, ProductProcessingError}
import ru.yandex.realty.proto.api.v2.request.JuridicalProductsRequest.ProductsEntry
import ru.yandex.realty.proto.api.v2.request.JuridicalProductsRequest
import ru.yandex.realty.proto.offer.ProductSource
import ru.yandex.realty.proto.products.Products.AggregatedProductStatus.{
  ActiveStatus,
  InactiveStatus,
  WaitingForActivationStatus,
  WaitingForDeactivationStatus,
  WaitingForPaymentStatus
}
import ru.yandex.realty.proto.products.Products.{AggregatedProductInfo, AggregatedProductStatus, ProductsInfo}
import ru.yandex.realty.proto.seller.ProductTypes
import ru.yandex.vertis.protobuf.ProtobufUtils

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class JuridicalProductsRequestValidatorSpec extends AsyncSpecBase with OfferModelGenerators with PropertyChecks {

  implicit private val offerId: String = "8276828729382938"

  implicit private class RichAggregatedJuridicalProduct(info: AggregatedProductInfo) {

    private def withSource(x: AggregatedProductInfo, s: ProductSource): AggregatedProductInfo =
      x.toBuilder.setSource(s).build()

    def feedProduct: AggregatedProductInfo =
      withSource(info, ProductSource.PRODUCT_SOURCE_FEED)

    def manualProduct: AggregatedProductInfo =
      withSource(info, ProductSource.PRODUCT_SOURCE_MANUAL)

    def raisingType: AggregatedProductInfo =
      info.toBuilder.setProductType(ProductTypes.PRODUCT_TYPE_RAISING).build()

    def premiumType: AggregatedProductInfo =
      info.toBuilder.setProductType(ProductTypes.PRODUCT_TYPE_PREMIUM).build()

    def waitingForPaymentStatus: AggregatedProductInfo =
      info.toBuilder
        .setAggregatedStatus(
          AggregatedProductStatus.newBuilder().setWaitingForPayment(WaitingForPaymentStatus.getDefaultInstance)
        )
        .build()

    def waitingForActivationStatus: AggregatedProductInfo =
      info.toBuilder
        .setAggregatedStatus(
          AggregatedProductStatus.newBuilder().setWaitingForActivation(WaitingForActivationStatus.getDefaultInstance)
        )
        .build()

    def waitingForDeactivationStatus: AggregatedProductInfo =
      info.toBuilder
        .setAggregatedStatus(
          AggregatedProductStatus
            .newBuilder()
            .setWaitingForDeactivation(WaitingForDeactivationStatus.getDefaultInstance)
        )
        .build()

    def activeStatus: AggregatedProductInfo =
      info.toBuilder
        .setAggregatedStatus(
          AggregatedProductStatus.newBuilder().setActiveStatus(ActiveStatus.getDefaultInstance)
        )
        .build()

    def inactiveStatus: AggregatedProductInfo =
      info.toBuilder
        .setAggregatedStatus(
          AggregatedProductStatus.newBuilder().setInactiveStatus(InactiveStatus.getDefaultInstance)
        )
        .build()

    def withBoughtByNaturalPerson: AggregatedProductInfo = {
      val status = info.getAggregatedStatus
      val newStatus = if (status.hasWaitingForDeactivation) {
        status.toBuilder
          .setWaitingForDeactivation(
            status.getWaitingForDeactivation.toBuilder.setBoughtByNaturalPerson(true)
          )
          .build()
      } else {
        status
      }
      info.toBuilder.setAggregatedStatus(newStatus).build()
    }
  }

  private def retrieveErrors(validationResult: RequestValidationResult): Iterable[ProcessedOffer] =
    validationResult.response.getOffersList.asScala
      .map { processedOffer =>
        processedOffer.toBuilder
          .clearProducts()
          .addAllProducts {
            processedOffer.getProductsList.asScala.filter(_.getError != ProductProcessingError.UNKNOWN).asJava
          }
          .build()
      }
      .filter(_.getProductsList.asScala.nonEmpty)

  private def productEntries(entries: (String, Seq[ProductTypes])*): Seq[ProductsEntry] =
    entries.map {
      case (offerId, types) =>
        ProductsEntry.newBuilder().setOfferId(offerId).addAllProductTypes(types.asJava).build()
    }

  private def assertRequestsEquals(r1: JuridicalProductsRequest, r2: JuridicalProductsRequest) {
    r1.getToStopList should contain theSameElementsAs r2.getToStopList.asScala
    r1.getToCreateList should contain theSameElementsAs r2.getToCreateList.asScala
  }

  private val raisingProductsEntry = productEntries(offerId -> Seq(ProductTypes.PRODUCT_TYPE_RAISING)).head

  private def defaultStopRequest =
    JuridicalProductsRequest
      .newBuilder()
      .addToStop(raisingProductsEntry)
      .build()

  private def defaultCreateRequest =
    JuridicalProductsRequest
      .newBuilder()
      .addToCreate(raisingProductsEntry)
      .build()

  private def infoMapFor(info: AggregatedProductInfo*)(implicit offerId: String): Map[String, ProductData] =
    Map(
      offerId -> ProductData(
        ProductsInfo
          .newBuilder()
          .addAllProducts(info.asJava)
          .build(),
        Map.empty
      )
    )

  private def expectedValidResponse(request: JuridicalProductsRequest, info: Map[String, ProductData]) {
    val res = JuridicalProductsRequestValidator.validate(request, info)
    retrieveErrors(res) shouldBe empty
    res.validRequest shouldBe request
  }

  private def expectedSingleErrorCode(code: ProductProcessingError)(res: RequestValidationResult) {
    val errorOffers = retrieveErrors(res)
    errorOffers should have size 1

    val errorOffer = errorOffers.head
    errorOffer.getOfferId shouldBe raisingProductsEntry.getOfferId
    println(ProtobufUtils.toJson(errorOffer))
    println(errorOffer.getProductsList.asScala)
    errorOffer.getProductsList.asScala should have size 1

    val errorProduct = errorOffer.getProductsList.asScala.head
    errorProduct.getProductType shouldBe ProductTypes.PRODUCT_TYPE_RAISING
    errorProduct.getError shouldBe code
  }

  "JuridicalProductsRequestValidator" should {

    "throw error" when {

      "product from feed" in {
        val info = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.feedProduct.activeStatus
        )

        expectedSingleErrorCode(ProductProcessingError.FEED_PRODUCT) {
          JuridicalProductsRequestValidator.validate(defaultCreateRequest, info)
        }

        expectedSingleErrorCode(ProductProcessingError.FEED_PRODUCT) {
          JuridicalProductsRequestValidator.validate(defaultStopRequest, info)
        }
      }

      "create/stop product with waiting for payment or activation" in {
        val infoWFP = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.manualProduct.waitingForPaymentStatus
        )
        val infoWFA = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.manualProduct.waitingForActivationStatus
        )

        for {
          info <- Seq(infoWFA, infoWFP)
          request <- Seq(defaultStopRequest, defaultCreateRequest)
        } yield {
          expectedSingleErrorCode(ProductProcessingError.PRODUCT_IN_PROCESS) {
            JuridicalProductsRequestValidator.validate(request, info)
          }
        }
      }

      "stop inactive product" in {
        val info = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.manualProduct.inactiveStatus
        )
        expectedSingleErrorCode(ProductProcessingError.INACTIVE_PRODUCT) {
          JuridicalProductsRequestValidator.validate(defaultStopRequest, info)
        }
      }

      "create already active product" in {
        val info = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.manualProduct.activeStatus
        )
        expectedSingleErrorCode(ProductProcessingError.PRODUCT_IN_PROCESS) {
          JuridicalProductsRequestValidator.validate(defaultCreateRequest, info)
        }
      }

      "create new product with existing natural waiting for deactivation product" in {
        val info = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.manualProduct.waitingForDeactivationStatus.withBoughtByNaturalPerson
        )
        expectedSingleErrorCode(ProductProcessingError.NATURAL_PRODUCT_EXCEPTION) {
          JuridicalProductsRequestValidator.validate(defaultCreateRequest, info)
        }

      }
    }

    "return single valid response" when {
      "stop waitingForDeactivation or active product" in {
        val infoWFD = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.manualProduct.waitingForDeactivationStatus
        )

        val infoActive = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.manualProduct.waitingForDeactivationStatus
        )

        for (info <- Seq(infoActive, infoWFD)) {
          expectedValidResponse(defaultStopRequest, info)
        }

      }

      "buy inactive products" in {
        val info = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.raisingType.manualProduct.inactiveStatus
        )

        expectedValidResponse(defaultCreateRequest, info)
      }
    }

    "return mix response" when {
      "process batch stop" in {
        val offer1 = readableString.next
        val offer2 = readableString.next

        val stopRequest = JuridicalProductsRequest
          .newBuilder()
          .addAllToStop(
            productEntries(
              offer1 -> Seq(ProductTypes.PRODUCT_TYPE_RAISING, ProductTypes.PRODUCT_TYPE_PREMIUM),
              offer2 -> Seq(ProductTypes.PRODUCT_TYPE_RAISING, ProductTypes.PRODUCT_TYPE_PREMIUM)
            ).asJava
          )
          .build()

        val info1 = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.manualProduct.activeStatus.premiumType,
          AggregatedProductInfo.getDefaultInstance.feedProduct.activeStatus.raisingType
        )(offer1)

        val info2 = infoMapFor(
          AggregatedProductInfo.getDefaultInstance.manualProduct.activeStatus.premiumType,
          AggregatedProductInfo.getDefaultInstance.manualProduct.activeStatus.raisingType
        )(offer2)

        val info = info1 ++ info2

        val result = JuridicalProductsRequestValidator.validate(stopRequest, info)

        val expectedValidPart =
          JuridicalProductsRequest
            .newBuilder()
            .addAllToStop(
              productEntries(
                offer1 -> Seq(ProductTypes.PRODUCT_TYPE_PREMIUM),
                offer2 -> Seq(ProductTypes.PRODUCT_TYPE_RAISING, ProductTypes.PRODUCT_TYPE_PREMIUM)
              ).asJava
            )
            .build()

        val expectedErrorPart =
          ProcessedOffer
            .newBuilder()
            .setOfferId(offer1)
            .addProducts(
              ProcessedProduct
                .newBuilder()
                .setProductType(ProductTypes.PRODUCT_TYPE_RAISING)
                .setError(ProductProcessingError.FEED_PRODUCT)
            )
            .build()

        assertRequestsEquals(result.validRequest, expectedValidPart)

        val errors = retrieveErrors(result)
        errors should have size 1
        errors.head shouldBe expectedErrorPart
      }

      "process batch create" in {
        val offers = readableString.next(3).toSeq

        val request = JuridicalProductsRequest
          .newBuilder()
          .addAllToCreate(
            productEntries(
              offers.map(_ -> Seq(ProductTypes.PRODUCT_TYPE_RAISING)): _*
            ).asJava
          )
          .build()

        val info12 =
          offers
            .take(2)
            .map { offerId =>
              infoMapFor(
                AggregatedProductInfo.getDefaultInstance.inactiveStatus.raisingType
              )(offerId)
            }
            .flatMap(_.toSeq)
            .toMap

        val info3 =
          infoMapFor(
            AggregatedProductInfo.getDefaultInstance.activeStatus.raisingType
          )(offers.last)

        val info = info12 ++ info3
        val result = JuridicalProductsRequestValidator.validate(request, info)

        val expectedRequest = JuridicalProductsRequest
          .newBuilder()
          .addAllToCreate(
            productEntries(
              offers.take(2).map(_ -> Seq(ProductTypes.PRODUCT_TYPE_RAISING)): _*
            ).asJava
          )
          .build()

        assertRequestsEquals(result.validRequest, expectedRequest)

        val errors = retrieveErrors(result)
        errors should have size 1
        val actualError = errors.head

        actualError shouldBe ProcessedOffer
          .newBuilder()
          .setOfferId(offers.last)
          .addProducts(
            ProcessedProduct
              .newBuilder()
              .setProductType(ProductTypes.PRODUCT_TYPE_RAISING)
              .setError(ProductProcessingError.PRODUCT_IN_PROCESS)
          )
          .build()
      }
    }
  }
}
