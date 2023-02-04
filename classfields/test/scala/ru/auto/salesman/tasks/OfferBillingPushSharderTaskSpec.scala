package ru.auto.salesman.tasks

import java.util.concurrent.Executors

import org.scalacheck.Gen
import org.scalatest.time.{Seconds, Span}
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.GoodsDao.Record
import ru.auto.salesman.model._
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.push.AsyncPushClient
import ru.auto.salesman.push.AsyncPushClient.OfferBilling
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.tasks.OfferBillingPushSharderTaskSpec._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens._
import ru.auto.salesman.test.model.gens._
import ru.yandex.vertis.generators.ProducerProvider.asProducer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class OfferBillingPushSharderTaskSpec extends BaseSpec {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(5, Seconds)))

  implicit val oc =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  def newTask(records: Iterable[Record]): OfferBillingPushSharderTask =
    new OfferBillingPushSharderTask(
      MockAsyncPushClient,
      goodsDao(records),
      10,
      MockEpochService,
      Markers.OfferBillingPushSharderEpoch
    )(oc)

  def drop(): Unit = {
    MockAsyncPushClient.drop()
    MockEpochService.drop()
  }

  "OfferBillingPushSharderTask" should {

    "work on empty set of records" in {
      val t = newTask(Iterable.empty)

      t.run().futureValue

      MockEpochService.EpochValue should be(0L)
      MockAsyncPushClient.skip should be(0L)
      MockAsyncPushClient.push should be(0L)
      drop()
    }

    "correctly work with active billings" in {
      val records = recordGen().next(100).toList.map { r =>
        r.copy(status = GoodStatuses.Active)
      }
      val maxEpoch = records.flatMap(_.epoch).max

      val t = newTask(records)

      t.run().futureValue

      MockEpochService.EpochValue should be(maxEpoch)
      MockAsyncPushClient.skip should be(0L)
      MockAsyncPushClient.push should be(records.size)
      drop()
    }

    "correctly work with different billings" in {
      val records = {
        val disabled = (1 to 5).flatMap { i =>
          recordGen(s"Disabled$i").next(5).toList.map { r =>
            r.copy(status = GoodStatuses.Inactive)
          }
        }
        val enabled = recordGen().next(100).toList.map { r =>
          r.copy(status = GoodStatuses.Active)
        }
        val random = (1 to 1000).map(_ => recordGen().next)

        disabled ++ enabled ++ random
      }
      val maxEpoch = records.flatMap(_.epoch).max
      val t = newTask(records)
      val toSkip =
        records.groupBy(r => AutoruOfferId(r.offerId, r.offerHash)).count {
          case (_, group) =>
            !group.exists(_.status == GoodStatuses.Active)
        }

      val toPush = records.count(_.status == GoodStatuses.Active)
      t.run().futureValue

      MockEpochService.EpochValue should be(maxEpoch)
      MockAsyncPushClient.skip should be(toSkip)
      MockAsyncPushClient.push should be(toPush)
      drop()
    }

  }

}

object OfferBillingPushSharderTaskSpec {

  import ru.yandex.vertis.mockito.MockitoSupport.{?, mock, when}

  def recordGen(id: String = randomAlphanumericString(10)): Gen[Record] =
    for {
      record <- GoodRecordGen
      billing = randomAlphanumericString(5).getBytes
      epoch <- Gen.chooseNum[Long](1000, 1000000)
    } yield record.copy(offerBilling = Some(billing), category = OfferCategories.Cars)

  object MockEpochService extends EpochService {
    var EpochValue = 0L

    def get(marker: String): Try[Epoch] =
      Success(EpochValue)

    def set(marker: String, epoch: Epoch): Try[Unit] = {
      EpochValue = epoch
      Success(())
    }

    def drop(): Unit = EpochValue = 0
  }

  def goodsDao(records: Iterable[Record]): GoodsDao = {
    val m = mock[GoodsDao]
    when(m.get(?)).thenReturn(Success(records))
    m
  }

  object MockAsyncPushClient extends AsyncPushClient {
    var skip = 0
    var push = 0

    def get(offer: PartnerOfferId)(
        implicit ec: ExecutionContext
    ): Future[Iterable[OfferBilling]] = ???

    def set(offer: PartnerOfferId, billings: Iterable[OfferBilling])(
        implicit ec: ExecutionContext
    ): Future[Unit] = {
      push += billings.size
      Future.successful(())
    }

    def skip(
        offer: PartnerOfferId
    )(implicit ec: ExecutionContext): Future[Unit] = {
      skip += 1
      Future.successful(())
    }

    def drop(): Unit = {
      skip = 0
      push = 0
    }
  }

}
