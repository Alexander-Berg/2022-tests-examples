package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import org.scalamock.function.StubFunction5
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.dao.GoodsDao.Record
import ru.auto.salesman.dao.OfferDao.Condition.OfferIdCategory
import ru.auto.salesman.dao.OfferDao.OfferPatch
import ru.auto.salesman.dao.{GoodsDao, OfferDao}
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.OfferCategories.Cars
import ru.auto.salesman.model.OfferStatuses.{Hidden, Show}
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{
  AutoruDealer,
  FirstActivateDate,
  Funds,
  GoodStatuses,
  Offer,
  OfferCategories,
  ProductId
}
import ru.auto.salesman.service.DealerProductApplyService.ProductApplyResult
import ru.auto.salesman.service.DealerProductApplyService.ProductApplyResult.Applied
import ru.auto.salesman.service.{
  CallOfferUpdater,
  DealerProductApplyService,
  GoodsDaoProvider
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.carsNewActiveOfferGen
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.generators.BasicGenerators.asProducer

import scala.util.Try

class CallOfferUpdaterSpec extends BaseSpec {

  "CallOfferUpdater" should {

    "just bill offer placement that should be activated" in {
      stubApplyProductReturning(Applied)
      stubOfferUpdate()
      stubGoodsDaoProvider(hasActivePlacement = false)

      callOfferUpdater.billOrApplyPatch(offer, activate).success

      stubApplyProduct.verify(
        offerId,
        Placement,
        dealer,
        /* customPrice = */ None,
        /* rc = */ *
      )
      // actually product service updates offer, but does it internally
      // we check here, that offer updater doesn't update offer itself somehow
      // (e.g., doesn't do some deactivate)
      (offerDao.update _).verify(*, *).never()
    }

    "not activate offer that has active placement" in {
      stubGoodsDaoProvider(hasActivePlacement = true)

      callOfferUpdater.billOrApplyPatch(offer, activate).success

      (offerDao.update _).verify(*, *).never()
    }

    "just apply patch if it isn't activation patch, don't try to bill offer placement" in {
      stubOfferUpdate()

      callOfferUpdater.billOrApplyPatch(offer, hide).success

      (offerDao.update _).verify(OfferIdCategory(1079350984L, Cars), hide)
    }
  }

  private val offerId = AutoruOfferId("1079350984-49fe0858")

  private val offer: Offer =
    carsNewActiveOfferGen(
      offerIdGen = 1079350984L,
      offerHashGen = "49fe0858",
      clientIdGen = 88L
    ).next
  private val dealer = AutoruDealer(88)
  private val activate = OfferPatch(status = Some(Show))
  private val hide = OfferPatch(status = Some(Hidden))
  implicit private val rc: RequestContext = AutomatedContext("test")

  private val productApplyService: DealerProductApplyService =
    stub[DealerProductApplyService]
  private val offerDao: OfferDao = stub[OfferDao]
  private val goodsDaoProvider = stub[GoodsDaoProvider]
  private val goodsDao = stub[GoodsDao]

  private val callOfferUpdater =
    new CallOfferUpdater(productApplyService, offerDao, goodsDaoProvider)

  private def stubApplyProduct: StubFunction5[AutoruOfferId, ProductId, AutoruDealer, Option[Funds], RequestContext, Try[ProductApplyResult]] =
    toStubFunction5(
      productApplyService.applyProduct(
        _: AutoruOfferId,
        _: ProductId,
        _: AutoruDealer,
        _: Option[Funds]
      )(_: RequestContext)
    )

  private def stubApplyProductReturning(result: ProductApplyResult): Unit =
    stubApplyProduct.when(*, *, *, *, *).returningT(result)

  private def stubOfferUpdate(): Unit =
    (offerDao.update _).when(*, *).returningT(())

  private def stubGoodsDaoProvider(hasActivePlacement: Boolean): Unit = {
    (goodsDao.get _)
      .when(*)
      .returningT(
        Iterable(
          Record(
            primaryKeyId = 1L,
            offerId = 1234,
            offerHash = "ads",
            category = OfferCategories.Cars,
            section = Section.NEW,
            clientId = 20101,
            product = ProductId.Placement,
            status = GoodStatuses.Active,
            createDate = DateTime.now(),
            extraData = "",
            expireDate = None,
            FirstActivateDate(now()),
            offerBilling = None,
            offerBillingDeadline = None,
            holdTransactionId = None,
            epoch = None
          )
        ).filter(_ => hasActivePlacement)
      )
    (goodsDaoProvider.chooseDao _).when(*).returning(goodsDao)
  }
}
