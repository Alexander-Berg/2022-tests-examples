package ru.yandex.vertis.billing.dao

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.model_core.gens.{OfferBillingGen, Producer}
import ru.yandex.vertis.billing.model_core.proto.{Conversions => ModelConversions}
import ru.yandex.vertis.billing.util.CacheControl
import ru.yandex.vertis.billing.util.CacheControl.Cache

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Spec on [[OfferBillingStorageDao]]
  *
  * @author alesavin
  */
class OfferBillingStorageDaoSpec extends AnyWordSpec with Matchers with ScalaFutures {

  implicit val context = ExecutionContext.global

  private val offerBillingStorage: OfferBillingStorageDao =
    new OfferBillingStorageDao {

      private val _values =
        mutable.Map.empty[Model.OfferId, List[Model.OfferBilling]]

      private def append(record: Model.OfferBilling) = {
        _values.get(record.getOfferId) match {
          case Some(records) =>
            _values.put(record.getOfferId, records ++ List(record))
          case None =>
            _values.put(record.getOfferId, List(record))
        }
      }

      override def upsert(records: Iterable[Model.OfferBilling]): Future[Unit] =
        Future.successful(records.foreach(append))

      override def delete(offers: Iterable[Model.OfferId]): Future[Unit] =
        Future.successful(offers.foreach(_values.remove))

      override def get(cacheControl: CacheControl = Cache): Future[Iterable[Model.OfferBilling]] =
        Future.successful(_values.values.flatten)

      override def get(offer: Model.OfferId): Future[Iterable[Model.OfferBilling]] =
        Future.successful(_values.getOrElse(offer, List()))

      override def flush = ()

      implicit override protected def ec: ExecutionContext = concurrent.ExecutionContext.Implicits.global
    }

  /**
    * Default value for futures [[PatienceConfig]].
    */
  private val DefaultPatienceConfig =
    PatienceConfig(Span(10, Seconds), Span(1, Seconds))

  implicit override def patienceConfig: PatienceConfig =
    DefaultPatienceConfig

  "OfferBillingStorageDao" should {
    var offerBillings = List.empty[Model.OfferBilling]

    "scan empty" in {
      offerBillingStorage.get().futureValue should be(empty)
    }

    "get empty" in {
      val offer = OfferBillingGen.next(1).map(ModelConversions.toMessage).head.getOfferId
      intercept[NoSuchElementException] {
        offerBillingStorage.get(offer).futureValue.head
      }
    }

    "upsert" in {
      offerBillings = OfferBillingGen.next(10).map(ModelConversions.toMessage).toList
      offerBillingStorage.upsert(offerBillings).futureValue should be(())
    }

    "scan upserted" in {
      val result = offerBillingStorage.get().futureValue
      result.size should be > 0
      result.foreach {
        offerBillings.contains(_) should be(true)
      }
    }

    "get upserted" in {
      offerBillings.foreach { b =>
        offerBillingStorage.get(b.getOfferId).futureValue should not be None
      }
    }

    "upsert already exist" in {
      offerBillingStorage.upsert(offerBillings.take(5)).futureValue
    }

    "scan upserted by duplication" in {
      val result = offerBillingStorage.get().futureValue
      result.size should be > 0
      result.foreach {
        offerBillings.contains(_) should be(true)
      }
    }

    "delete" in {
      offerBillingStorage.delete(offerBillings.take(5).map(_.getOfferId)).futureValue
    }

    "scan after delete" in {
      offerBillingStorage.get().futureValue.size should be(5)
    }

    "delete full amount" in {
      offerBillingStorage.delete(offerBillings.map(_.getOfferId)).futureValue
    }
    "scan empty after delete" in {
      offerBillingStorage.get().futureValue.size should be(0)
    }
  }

}
