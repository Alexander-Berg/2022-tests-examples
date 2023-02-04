package ru.yandex.realty.seller.service.impl

import org.junit.runner.RunWith
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.user.{PassportUser, UserRef}
import ru.yandex.realty.proto.offer.PaymentType
import ru.yandex.realty.proto.offer.vos.OfferIdx.{IdxRequest, IdxRequestDelete, IxdRequestCreate}
import ru.yandex.realty.seller.dao.{ProductScheduleStateDao, PurchasedProductDao}
import ru.yandex.realty.seller.model.{ProductType, PurchasedProductStatus}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._
import ru.yandex.realty.seller.model.schedule.{
  FeedSchedulePatch,
  ProductSchedulePatch,
  ScheduleEnabledPatch,
  ScheduleMultiPatch
}
import ru.yandex.realty.seller.service.IdxRequestProcessor._
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.vertis.scalamock.util._

import scala.concurrent.Future

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class IdxRequestProcessorImplSpec
  extends AsyncSpecBase
  with ProtobufMessageGenerators
  with SellerModelGenerators
  with ProtoInstanceProvider
  with ShrinkLowPriority {

  private val productsDao: PurchasedProductDao = mock[PurchasedProductDao]
  private val scheduleDao: ProductScheduleStateDao = mock[ProductScheduleStateDao]
  private val processor = new IdxRequestProcessorImpl(productsDao, scheduleDao)

  private val allowedStatuses = Set(PurchasedProductStatuses.Pending, PurchasedProductStatuses.Active)

  private def idxCreateRequestPaymentTypeGen(paymentType: PaymentType, fromFeed: Boolean) =
    for {
      create <- generate[IxdRequestCreate]()
      uid <- Gen.posNum[Long]
      partnerId <- if (fromFeed) Gen.posNum[Long] else Gen.const(0L)
    } yield {
      val builder = create.toBuilder
      val contentBuilder = builder.getOfferBuilder.getContentBuilder
      contentBuilder.setUid(uid.toString)
      contentBuilder.getSalesAgentBuilder.setPaymentType(paymentType)
      contentBuilder.setPartnerId(partnerId)
      IdxRequest.newBuilder().setCreate(builder).build()
    }

  private val idxDeleteRequestPaymentTypeGen =
    for {
      delete <- generate[IdxRequestDelete]()
      uid <- Gen.posNum[Long]
    } yield {
      val builder = delete.toBuilder
      builder.getIdentityBuilder.setUid(uid.toString)
      IdxRequest.newBuilder().setDelete(builder).build()
    }

  "IdxRequestProcessorImpl" should {
    "ignore create for non-juridical person" in {
      val req = idxCreateRequestPaymentTypeGen(PaymentType.PT_NATURAL_PERSON, fromFeed = true).next
      processor.process(req).futureValue shouldBe Ignored
    }

    "ignore create from vos offer" in {
      val req = idxCreateRequestPaymentTypeGen(PaymentType.PT_JURIDICAL_PERSON, fromFeed = false).next
      processor.process(req).futureValue shouldBe Ignored
    }

    "process correct create" in {
      val req = idxCreateRequestPaymentTypeGen(PaymentType.PT_JURIDICAL_PERSON, fromFeed = true).next
      processor.process(req).futureValue shouldBe Ignored
    }

    "set stop expiration policy on delete event" in {
      val req = idxDeleteRequestPaymentTypeGen.next
      val target = OfferTarget(req.getDelete.getIdentity.getOfferId)

      val products = listUnique(1, 5, purchasedProductGen)(_.id).next
        .map(_.copy(owner = PassportUser(1L), target = target))

      (productsDao
        .getProducts(_: PurchaseTarget, _: Set[PurchasedProductStatus]))
        .expects(target, allowedStatuses)
        .returningF(products)

      (productsDao
        .updateProducts(_: Iterable[ProductUpdate]))
        .expects(where { updates: Iterable[ProductUpdate] =>
          updates.forall(_.patch.expirationPolicy == Stop)
        })
        .returning(Future.unit)

      val patch = ScheduleMultiPatch(
        ScheduleEnabledPatch(false),
        FeedSchedulePatch(Seq())
      )

      products.groupBy(_.product).map(_._2.head).foreach { p =>
        (scheduleDao
          .updateSchedule(_: UserRef, _: String, _: ProductType, _: ProductSchedulePatch))
          .expects(p.owner, target.offerId, p.product, patch)
          .returningF(scheduleStateGen.next)
      }

      val result = processor.process(req).futureValue

      result match {
        case BatchUpdate(Nil, updated, Nil) =>
          updated should have size products.size
          updated.map(_.oldState) should contain allElementsOf products
      }
    }

  }

}
