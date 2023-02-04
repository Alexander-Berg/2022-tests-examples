package ru.auto.api.managers.enrich

import ru.auto.api.ApiOfferModel.{Offer, PaidServicePrice}
import ru.auto.api.managers.price.UserPriceManager
import ru.auto.api.model.{ActivationPrice, AutoruProduct, OfferID}
import ru.auto.api.util.Request

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class SlowlyFailingUserPriceManager(timeout: Duration) extends UserPriceManager {

  override def getMultipleOffersPrices(
      offers: Seq[Offer],
      products: Seq[AutoruProduct],
      applyMoneyFeature: Boolean,
      isNewDraft: Boolean
  )(implicit req: Request): Future[Map[OfferID, Seq[PaidServicePrice]]] =
    Future {
      Thread.sleep(timeout.toMillis)
      ???
    } {
      // Важно не использовать здесь дефолтный тред-пул для тестов из одного треда, чтобы можно
      // было в параллель ждать здесь и поллить фьючу в тесте. Иначе тест, ждущий тест, не сфейлится,
      // т.к. не отвалится по таймауту раньше, чем эта фьюча завершится.
      ExecutionContext.global
    }

  override def getActivationPrice(offer: Offer)(implicit req: Request): Future[ActivationPrice] = ???

  override def getPrices(offer: Offer,
                         products: Seq[AutoruProduct],
                         applyMoneyFeature: Boolean,
                         isNewDraft: Boolean,
                         isValid: Boolean)(implicit req: Request): Future[Seq[PaidServicePrice]] = ???
}
