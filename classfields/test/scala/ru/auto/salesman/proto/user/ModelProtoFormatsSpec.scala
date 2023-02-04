package ru.auto.salesman.proto.user

import com.google
import org.scalacheck.Gen
import ru.auto.api.CommonModel.PaidService.PaymentReason.UNKNOWN_PAYMENT_REASON
import ru.auto.salesman.model.user.ProductContext.GoodsContext
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  Fields,
  PaymentSystem,
  ProductDuration
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.slicedResultGen
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.SlicedResult
import ru.yandex.vertis.protobuf
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat
import ru.yandex.vertis.protobuf.{ProtoFormat, ProtoWriter}
import ru.auto.salesman.environment._
import ru.auto.salesman.model.user.product.Products
import com.google.protobuf.StringValue
import scala.collection.JavaConverters._

class ModelProtoFormatsSpec
    extends BaseSpec
    with UserModelGenerators
    with ModelProtoFormats {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  "UserPaysysFormat" should {

    val fmt = implicitly[ProtoFormat[PaymentSystem, ApiModel.UserPaysys]]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.UserPaysys.getDefaultInstance
    }

    "write and read proto" in {
      forAll(paymentSystemGen) { obj =>
        val proto = ApiModel.UserPaysys
          .newBuilder()
          .setPaysys(ApiModel.PaymentSystem.forNumber(obj.id))
          .build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "PriceModifierFeatureFormat" should {

    val fmt = implicitly[
      ProtoFormat[PriceModifier.Feature, ApiModel.PriceModifier.Feature]
    ]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.PriceModifier.Feature.getDefaultInstance
    }

    "write and read proto" in {
      forAll(FeatureGen) { obj =>
        val proto =
          ApiModel.PriceModifier.Feature
            .newBuilder()
            .setId(obj.featureInstanceId)
            .setDeadline(protobuf.asProto(obj.deadline))
            .setPayload(protobuf.asProto(obj.payload))
            .setCount(obj.count)
            .build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "PriceModifierPeriodicalDiscountFormat" should {

    val fmt = implicitly[ProtoFormat[
      PriceModifier.PeriodicalDiscount,
      ApiModel.PriceModifier.PeriodicalDiscount
    ]]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.PriceModifier.PeriodicalDiscount.getDefaultInstance
    }

    "write and read proto" in {
      forAll(PeriodicalDiscountModifierGen) { obj =>
        val proto =
          ApiModel.PriceModifier.PeriodicalDiscount
            .newBuilder()
            .setId(obj.discountId)
            .setDiscount(obj.discount)
            .setDeadline(protobuf.asProto(obj.deadline))
            .build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "ProlongIntervalFormat" should {
    val fmt = implicitly[ProtoFormat[
      PriceModifier.ProlongInterval,
      ApiModel.PriceModifier.ProlongInterval
    ]]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.PriceModifier.ProlongInterval.getDefaultInstance
    }

    "write and read proto" in {
      forAll(ProlongIntervalGen) { obj =>
        val proto = ApiModel.PriceModifier.ProlongInterval
          .newBuilder()
          .setProlongPrice(obj.prolongPrice)
          .setWillExpire(obj.willExpire.asTimestamp)
          .build
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "PriceModifierFormat" should {

    val fmt = implicitly[ProtoFormat[PriceModifier, ApiModel.PriceModifier]]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.PriceModifier.getDefaultInstance
    }

    "write and read proto" in {
      forAll(PriceModifierGen) { obj =>
        val b = ApiModel.PriceModifier.newBuilder()
        obj.feature.map(protobuf.asProto(_)).foreach(b.setPromocoderFeature)
        obj.bundleId.foreach(b.setBundleId)
        obj.experimentInfo.foreach { exp =>
          b.setUaasBoxes(exp.expBoxes)
          exp.activeExperimentId.foreach(b.setExperimentId)
        }
        obj.appliedExperimentId.foreach { appliedExperimentId =>
          b.setAppliedExperimentId(StringValue.newBuilder().setValue(appliedExperimentId))
        }
        obj.periodicalDiscount.map(protobuf.asProto(_)).foreach(b.setDiscount)
        obj.prolongInterval
          .map(protobuf.asProto(_))
          .foreach(b.setProlongInterval)
        val proto = b.build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "PriceFormat" should {

    val fmt = implicitly[ProtoFormat[Price, ApiModel.Price]]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.Price.getDefaultInstance
    }

    "write and read proto" in {
      forAll(PriceGen) { obj =>
        val b = ApiModel.Price
          .newBuilder()
          .setBasePrice(obj.basePrice)
          .setEffectivePrice(obj.effectivePrice)
        obj.policyId.foreach(policyId =>
          b.setPolicyId(StringValue.newBuilder().setValue(policyId))
        )
        obj.prolongPrice.foreach(b.setProlongPrice)
        obj.modifier.map(protobuf.asProto(_)).foreach(b.setModifier)
        val proto = b.build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }

    "return scala object with empty prolongPrice if protobuf.prolongPrice is empty" in {
      val proto = ApiModel.Price.getDefaultInstance
      val result = fmt.read(proto)
      result.prolongPrice shouldBe None
    }

    "return scala object with prolongPrice if protobuf.prolongPrice is set" in {
      forAll(Gen.choose(1, 1000000)) { prolongPrice =>
        val proto =
          ApiModel.Price.newBuilder().setProlongPrice(prolongPrice).build()
        val result = fmt.read(proto)
        result.prolongPrice.value shouldBe prolongPrice
      }
    }

    "return proto object with empty prolongPrice if scala.prolongPrice is empty" in {
      forAll(PriceGen) { base =>
        val price = base.copy(prolongPrice = None)
        val result = fmt.write(price)
        result.getProlongPrice shouldBe 0
      }
    }

    "return proto object with prolongPrice if scala.prolongPrice is set" in {
      forAll(PriceGen, Gen.choose(1, 100000)) { (base, prolongPrice) =>
        val price = base.copy(prolongPrice = Some(prolongPrice))
        val result = fmt.write(price)
        result.getProlongPrice shouldBe prolongPrice
      }
    }
  }

  "ProductPriceFormat" should {

    val fmt = implicitly[ProtoFormat[ProductPrice, ApiModel.ProductPrice]]

    "return default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductPrice.getDefaultInstance
    }

    "write and read proto" in {
      forAll(ProductPriceGen) { obj =>
        val b = ApiModel.ProductPrice
          .newBuilder()
          .setPrice(protobuf.asProto(obj.price))

        obj.product match {
          case s: Products.Subscription =>
            b.setProduct(s.name)
            b.setCounter(s.counter)

          case _ =>
            b.setProduct(obj.product.toString)
        }

        b.setDuration(protobuf.asProto(obj.duration))
          .setDays(obj.duration.days.getDays)
        obj.paymentReason.foreach(pr => b.setPaymentReasonValue(pr.id))

        b.setProlongationAllowed(obj.prolongationAllowed)
        b.setProlongationForced(obj.prolongationForced)
        b.setProlongationForcedNotTogglable(obj.prolongationForcedNotTogglable)

        obj.productPriceInfo
          .map(productPriceInfoFormat.write)
          .foreach(b.setProductPriceInfo)

        obj.analytics.map(analyticsFormat.write).foreach(b.setAnalytics)

        b.setScheduleFreeBoost(obj.scheduleFreeBoost)

        val proto = b.build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }

    "set days" in {
      forAll(productPriceGen(duration = ProductDuration.days(365))) { obj =>
        val proto = fmt.write(obj)
        proto.getDays shouldBe 365
      }
    }
  }

  "ProductPriceInfoFormat" should {

    val fmt =
      implicitly[ProtoFormat[ProductPriceInfo, ApiModel.ProductPriceInfo]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductPriceInfo.getDefaultInstance
    }

    "write and read proto" in {
      forAll(ProductPriceInfoGen) { obj =>
        val b = ApiModel.ProductPriceInfo.newBuilder()
        obj.name.foreach(b.setName)
        obj.title.foreach(b.setTitle)
        obj.description.foreach(b.setDescription)
        obj.multiplier.foreach(b.setMultiplier)
        b.addAllAliases(obj.aliases.asJava)
        b.addAllPackageContent(protobuf.asProtoList(obj.packageContent))
        obj.autoApplyPrice.foreach(b.setAutoApplyPrice)
        b.setPurchaseForbidden(obj.purchaseForbidden)
        obj.quotaLeft.map(_.toInt).foreach(b.setQuotaLeft)
        val proto = b.build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "PackageContentFormat" should {

    val fmt = implicitly[ProtoFormat[PackageContent, ApiModel.PackageContent]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.PackageContent.getDefaultInstance
    }

    "write and read proto" in {
      forAll(PackageContentGen) { obj =>
        val b = ApiModel.PackageContent.newBuilder()
        b.setAlias(obj.alias)
        obj.name.foreach(b.setName)
        b.setDuration(protobuf.asProto(obj.duration))
        val proto = b.build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "ProductPricesFormat" should {

    val fmt = implicitly[ProtoFormat[ProductPrices, ApiModel.ProductPrices]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductPrices.getDefaultInstance
    }

    "write and read proto" in {
      forAll(ProductPricesGen) { obj =>
        val b = ApiModel.ProductPrices.newBuilder().setOfferId(obj.offerId)
        val prices = obj.prices.map(c => productPriceFormat.write(c))
        b.addAllProductPrices(prices.asJava)
        val proto = b.build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "GoodsContextFormat" should {

    val fmt = implicitly[ProtoFormat[
      ProductContext.GoodsContext,
      ApiModel.ProductContext.GoodsContext
    ]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductContext.GoodsContext.getDefaultInstance
    }

    "write and read proto" in {
      forAll(GoodsContextGen) { obj =>
        val proto = ApiModel.ProductContext.GoodsContext
          .newBuilder()
          .setProductPrice(protobuf.asProto(obj.productPrice))
          .build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "BundleContextFormat" should {

    val fmt = implicitly[ProtoFormat[
      ProductContext.BundleContext,
      ApiModel.ProductContext.BundleContext
    ]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductContext.BundleContext.getDefaultInstance
    }

    "write and read proto" in {
      forAll(BundleContextGen) { obj =>
        val proto = ApiModel.ProductContext.BundleContext
          .newBuilder()
          .setProductPrice(protobuf.asProto(obj.productPrice))
          .build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "SubscriptionContextFormat" should {

    val fmt = implicitly[ProtoFormat[
      ProductContext.SubscriptionContext,
      ApiModel.ProductContext.SubscriptionContext
    ]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductContext.SubscriptionContext.getDefaultInstance
    }

    "write and read proto without vin" in {
      forAll(SubscriptionContextGen) { obj =>
        val proto = ApiModel.ProductContext.SubscriptionContext
          .newBuilder()
          .setProductPrice(protobuf.asProto(obj.productPrice))
          .build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }

    "write and read proto with vin and garageId" in {
      val vin = "vin"
      val garageId = "garage_id"
      forAll(SubscriptionContextGen) { obj =>
        val testObj =
          obj.copy(vinOrPlate = Some(vin), garageId = Some(garageId))
        val proto = ApiModel.ProductContext.SubscriptionContext
          .newBuilder()
          .setProductPrice(protobuf.asProto(testObj.productPrice))
          .setVinOrLicensePlate(vin)
          .setGarageId(garageId)
          .build()
        fmt.write(testObj) shouldBe proto
        fmt.read(proto) shouldBe testObj
      }
    }
  }

  "ProductContextFormat" should {

    val fmt = implicitly[ProtoFormat[ProductContext, ApiModel.ProductContext]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductContext.getDefaultInstance
    }

    "write and read proto" in {
      forAll(
        Gen.oneOf(
          productContextGen(ProductType.Goods),
          productContextGen(ProductType.Bundle),
          productContextGen(ProductType.Subscription)
        )
      ) { obj =>
        val b = ApiModel.ProductContext.newBuilder()
        obj match {
          case c: ProductContext.GoodsContext => b.setGoods(protobuf.asProto(c))
          case c: ProductContext.BundleContext =>
            b.setBundle(protobuf.asProto(c))
          case c: ProductContext.SubscriptionContext =>
            b.setSubscription(protobuf.asProto(c))
        }
        val proto = b.build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "ProductRequestFormat" should {

    val fmt = implicitly[ProtoFormat[ProductRequest, ApiModel.ProductRequest]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.ProductRequest.getDefaultInstance
    }

    "write and read proto" in {
      forAll(ProductRequestGen) { obj =>
        val b = ApiModel.ProductRequest
          .newBuilder()
          .setProduct(obj.product.toString)
          .setAmount(obj.amount)
          .setProlongable(obj.prolongable.value)
        obj.offer.map(_.value).foreach(b.setOffer)
        b.setContext(protobuf.asProto(obj.context))
        val proto = b.build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }

    "convert ProductRequest#context into protobuf correctly" in {
      forAll(productRequestGen(Placement)) { base =>
        val policyId = "test-policy"
        val duration = ProductDuration.days(7)
        val basePrice = 100
        val effectivePrice = 99
        val prolongPrice = 90
        val prolongationAllowed = true
        val prolongationForced = false
        val prolongationForcedNotTogglable = true
        val scheduleFreeBoost = true
        val context = GoodsContext(
          ProductPrice(
            Placement,
            duration,
            paymentReason = None,
            Price(
              basePrice,
              effectivePrice,
              Some(prolongPrice),
              modifier = None,
              policyId = Some(policyId)
            ),
            prolongationAllowed,
            prolongationForced,
            prolongationForcedNotTogglable,
            productPriceInfo = None,
            analytics = None,
            scheduleFreeBoost
          )
        )
        val productRequest = base.copy(context = context)

        val result =
          fmt.write(productRequest).getContext.getGoods.getProductPrice

        result.getProduct shouldBe "placement"
        result.getDuration shouldBe google.protobuf.Duration
          .newBuilder()
          .setSeconds(604800) // 7 days in seconds
          .build()
        result.getPaymentReason shouldBe UNKNOWN_PAYMENT_REASON
        result.getPrice.getPolicyId.getValue shouldBe policyId
        result.getPrice.getBasePrice shouldBe basePrice
        result.getPrice.getEffectivePrice shouldBe effectivePrice
        result.getPrice.getProlongPrice shouldBe prolongPrice
        result.getPrice.hasModifier shouldBe false
        result.getProlongationAllowed shouldBe prolongationAllowed
        result.getProlongationForced shouldBe prolongationForced
        result.getProlongationForcedNotTogglable shouldBe prolongationForcedNotTogglable
        result.getScheduleFreeBoost shouldBe scheduleFreeBoost
      }
    }
  }

  "FieldsFormat" should {

    val fmt =
      implicitly[ProtoFormat[Fields, ApiModel.TransactionRequest.Fields]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.TransactionRequest.Fields.getDefaultInstance
    }

    "write and read proto" in {
      forAll(Gen.listOf(FieldGen)) { obj =>
        val fields = obj.map { case (k, v) =>
          ApiModel.TransactionRequest.Field
            .newBuilder()
            .setKey(k)
            .setValue(v)
            .build()
        }
        val proto = ApiModel.TransactionRequest.Fields
          .newBuilder()
          .addAllFields(fields.asJava)
          .build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "TransactionRequestFormat" should {

    val fmt =
      implicitly[ProtoFormat[TransactionRequest, ApiModel.TransactionRequest]]

    "get default instance" in {
      fmt.defaultInstance shouldBe ApiModel.TransactionRequest.getDefaultInstance
    }

    "write and read proto" in {
      forAll(TransactionRequestGen) { obj =>
        val proto = ApiModel.TransactionRequest
          .newBuilder()
          .setUser(obj.user)
          .setAmount(obj.amount)
          .addAllPayload(protobuf.asProtoList(obj.payload.toSeq))
          .setPayloadFields(protobuf.asProto(obj.fields))
          .build()
        fmt.write(obj) shouldBe proto
        fmt.read(proto) shouldBe obj
      }
    }
  }

  "TransactionFormat" should {

    val fmt = implicitly[ProtoWriter[Transaction, ApiModel.Transaction]]

    "write proto" in {
      forAll(TransactionGen) { obj =>
        val b = ApiModel.Transaction
          .newBuilder()
          .setTransactionId(obj.transactionId)
          .setUser(obj.user)
          .setAmount(obj.amount)
          .setStatusValue(obj.status.id)
          .addAllPayload(protobuf.asProtoList(obj.payload.toSeq))
          .setCreatedAt(protobuf.asProto(obj.createdAt))
          .setId(obj.id)
        obj.paidAt.map(protobuf.asProto(_)).foreach(b.setPaidAt)
        val proto = b.build()
        fmt.write(obj) shouldBe proto
      }
    }
  }

  "TransactionPageFormat" should {

    val fmt = implicitly[
      ProtoWriter[SlicedResult[Transaction], ApiModel.TransactionPage]
    ]

    "write proto" in {
      forAll(slicedResultGen(TransactionGen)) { obj =>
        val proto = ApiModel.TransactionPage
          .newBuilder()
          .addAllTransactions(protobuf.asProtoList(obj.values.toSeq))
          .setTotal(obj.total)
          .setSlice(protobuf.asProto(obj.slice))
          .build()
        fmt.write(obj) shouldBe proto
      }
    }
  }
}
