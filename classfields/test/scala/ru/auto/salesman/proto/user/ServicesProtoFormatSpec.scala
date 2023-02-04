package ru.auto.salesman.proto.user

import ru.auto.salesman.model.user.ApiModel
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.user.UserProductService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserProductServiceGenerators
import ru.yandex.vertis.protobuf.ProtoFormat
import ru.auto.salesman.environment._
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.test.model.gens.HandlerModelGenerators
import ru.yandex.vertis.protobuf
import collection.JavaConverters._

class ServicesProtoFormatSpec
    extends BaseSpec
    with UserProductServiceGenerators
    with ServicesProtoFormat
    with HandlerModelGenerators {
  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  "ActiveOfferProductsRequestFormat" should {
    val fmt = implicitly[ProtoFormat[
      UserProductService.ActiveOfferProductsRequest,
      ApiModel.ActiveOfferProductsRequest
    ]]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ActiveOfferProductsRequest.getDefaultInstance
    }

    "write and read proto" in {
      forAll(activeOfferProductsRequestProtoGen()) { proto =>
        val obj = UserProductService.ActiveOfferProductsRequest(
          OfferIdentity(proto.getOfferId),
          proto.getGeoId
        )
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }

  }

  "ActiveOffersProductsRequestFormat" should {
    val fmt = implicitly[ProtoFormat[
      UserProductService.ActiveOffersProductsRequests,
      ApiModel.ActiveOffersProductsRequest
    ]]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ActiveOffersProductsRequest.getDefaultInstance
    }

    "read and write proto" in {
      forAll(activeOffersProductsRequestsProtoGen()) { proto =>
        val reqList = proto.getActiveOfferProductRequestsList.asScala.map { p =>
          UserProductService.ActiveOfferProductsRequest(
            OfferIdentity(p.getOfferId),
            p.getGeoId
          )
        }.toList
        val obj = UserProductService.ActiveOffersProductsRequests(reqList)

        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }

  }

  "ProductResponseFormat" should {
    val fmt = implicitly[
      ProtoFormat[UserProductService.Response, ApiModel.ProductResponse]
    ]
    "write and read proto" in {
      forAll(productResponseGen()) { obj =>
        val b = ApiModel.ProductResponse.newBuilder
          .setId(obj.id)
          .setUser(obj.user)
          .setProduct(obj.product.name)
          .setAmount(obj.amount)
          .setStatus(ApiModel.ProductResponse.Status.forNumber(obj.status.id))
          .setTransactionId(obj.transactionId)
          .setContext(protobuf.asProto(obj.context))
          .setActivated(obj.activated.asTimestamp)
          .setDeadline(obj.deadline.asTimestamp)
          .setRecoverable(obj.product.recoverable)
          .setProlongable(obj.prolongable.value)

        obj match {
          case bundle: UserProductService.Response.Bundle =>
            b.setOffer(bundle.offerId.value)
          case goods: UserProductService.Response.Goods =>
            b.setOffer(goods.offerId.value)
          case subscription: UserProductService.Response.Subscription =>
            b.setCounter(subscription.counter)
        }

        val proto = b.build

        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }

    "return defaul instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductResponse.getDefaultInstance
    }
  }
}
