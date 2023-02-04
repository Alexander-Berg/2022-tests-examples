package ru.yandex.realty.seller.processing.products

import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.kafka.producer.SimpleKafkaProducer
import ru.yandex.realty.proto.offer.event.SellerEvent
import ru.yandex.realty.seller.dao.PurchasedProductDao
import ru.yandex.realty.seller.model.event.SellerEventConverter
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{
  ProductTypes,
  PurchaseProductDeliveryStatuses,
  PurchasedProduct,
  PurchasedProductStatuses
}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success}

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class DeliveryStageSpec extends AsyncSpecBase with PropertyChecks with OptionValues {

  import DeliveryStageSpec._

  private val producer: SimpleKafkaProducer[SellerEvent] = mock[SimpleKafkaProducer[SellerEvent]]
  private val converter: SellerEventConverter = mock[SellerEventConverter]
  private val productsDao: PurchasedProductDao = mock[PurchasedProductDao]

  private val productEventLogger: ProductEventLogger = mock[ProductEventLogger]
  private val stage: DeliveryStage = new DeliveryStage(producer, productsDao, productEventLogger, converter)
  implicit val traced: Traced = Traced.empty
  "DeliveryStage" should {
    "mark product as delivered" in {
      forAll(NotDeliveredProductGen) { product =>
        (productEventLogger
          .sendProductEvent(_: PurchasedProduct))
          .expects(*)
          .returns(Future.successful(()))
          .once()

        val productEvent = SellerEvent.getDefaultInstance

        val productsInPackage = getProductsInPackage(product)
        if (productsInPackage.nonEmpty) {
          (productsDao.getProductsInPackage _)
            .expects(product.withoutVisitTime)
            .returning(Future.successful(productsInPackage))
        }
        (converter.toMessage _)
          .expects(product.withoutVisitTime, productsInPackage)
          .returning(Success(productEvent))
        (producer.send _)
          .expects(productEvent)
          .returning(Future.successful(ProducerResult))

        val state = ProcessingState(product)
        val result = stage.process(state).futureValue
        val nextVisit = product.status match {
          case PurchasedProductStatuses.Pending => product.startTime
          case PurchasedProductStatuses.Active => product.endTime
          case _ => None
        }
        result.entry.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Delivered
        result.entry.visitTime shouldBe nextVisit
      }
    }

    "reschedule product" when {
      "dao fails" in {
        forAll(NotDeliveredPackageGen) { product =>
          (productEventLogger
            .sendProductEvent(_: PurchasedProduct))
            .expects(*)
            .returns(Future.successful(()))
            .once()
          (productsDao.getProductsInPackage _)
            .expects(product.withoutVisitTime)
            .returning(Future.failed(new IllegalArgumentException("artificial") with NoStackTrace))
          (producer.topic _)
            .expects()
            .returning("topic")

          val state = ProcessingState(product)
          val result = stage.process(state).futureValue
          result.entry.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Failed
          result.entry.visitTime.value.getMillis should be > DateTimeUtil.now().getMillis
          result.entry.visitTime.value.getMillis should be < DateTimeUtil.now().plusMinutes(15).getMillis
        }
      }
      "converter fails" in {

        forAll(NotDeliveredProductGen) { product =>
          (productEventLogger
            .sendProductEvent(_: PurchasedProduct))
            .expects(*)
            .returns(Future.successful(()))
            .once()
          val productsInPackage = getProductsInPackage(product)
          if (productsInPackage.nonEmpty) {
            (productsDao.getProductsInPackage _)
              .expects(product.withoutVisitTime)
              .returning(Future.successful(productsInPackage))
          }
          (converter.toMessage _)
            .expects(product.withoutVisitTime, productsInPackage)
            .returning(Failure(new IllegalArgumentException("artificial") with NoStackTrace))
          (producer.topic _)
            .expects()
            .returning("topic")

          val state = ProcessingState(product)
          val result = stage.process(state).futureValue
          result.entry.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Failed
          result.entry.visitTime.value.getMillis should be > DateTimeUtil.now().getMillis
          result.entry.visitTime.value.getMillis should be < DateTimeUtil.now().plusMinutes(15).getMillis
        }
      }
      "producer fails" in {
        forAll(NotDeliveredProductGen) { product =>
          (productEventLogger
            .sendProductEvent(_: PurchasedProduct))
            .expects(*)
            .returns(Future.successful(()))
            .once()
          val productEvent = SellerEvent.getDefaultInstance

          val productsInPackage = getProductsInPackage(product)
          if (productsInPackage.nonEmpty) {
            (productsDao.getProductsInPackage _)
              .expects(product.withoutVisitTime)
              .returning(Future.successful(productsInPackage))
          }
          (converter.toMessage _)
            .expects(product.withoutVisitTime, productsInPackage)
            .returning(Success(productEvent))
          (producer.send _)
            .expects(productEvent)
            .returning(Future.failed(new IllegalStateException("artificial") with NoStackTrace))
          (producer.topic _)
            .expects()
            .returning("topic")

          val state = ProcessingState(product)
          val result = stage.process(state).futureValue
          result.entry.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.Failed
          result.entry.visitTime.value.getMillis should be > DateTimeUtil.now().getMillis
          result.entry.visitTime.value.getMillis should be < DateTimeUtil.now().plusMinutes(15).getMillis
        }
      }
    }

    "do noting" in {
      forAll(DeliveredProductGen) { product =>
        val state = ProcessingState(product)
        stage.process(state).futureValue shouldBe state
      }
    }
  }

  private def getProductsInPackage(product: PurchasedProduct) = {
    if (product.product == ProductTypes.PackageTurbo || product.product == ProductTypes.PackageRaising) {
      listUnique(1, 5, purchasedProductGen)(_.id).next
    } else {
      Seq.empty
    }
  }
}

object DeliveryStageSpec extends SellerModelGenerators {

  val NotDeliveredProductGen: Gen[PurchasedProduct] = for {
    product <- Gen.oneOf(purchasedProductGen, purchasedPackageGen)
    deliveryStatus <- Gen.oneOf(PurchaseProductDeliveryStatuses.Pending, PurchaseProductDeliveryStatuses.Failed)
  } yield product.copy(deliveryStatus = deliveryStatus)

  val NotDeliveredPackageGen: Gen[PurchasedProduct] = for {
    product <- purchasedPackageGen
    deliveryStatus <- Gen.oneOf(PurchaseProductDeliveryStatuses.Pending, PurchaseProductDeliveryStatuses.Failed)
  } yield product.copy(deliveryStatus = deliveryStatus)

  val DeliveredProductGen: Gen[PurchasedProduct] = for {
    product <- Gen.oneOf(purchasedProductGen, purchasedPackageGen)
    deliveryStatus <- Gen.oneOf(
      PurchaseProductDeliveryStatuses.Delivered,
      PurchaseProductDeliveryStatuses.NoOp,
      PurchaseProductDeliveryStatuses.Unknown
    )
  } yield product.copy(deliveryStatus = deliveryStatus)

  val ProducerResult = new RecordMetadata(new TopicPartition("topic", 0), 1, 2, 3, long2Long(4), 5, 6)

}
