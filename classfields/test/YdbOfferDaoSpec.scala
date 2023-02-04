package ru.yandex.vertis.general.gost.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.gost.model.Offer
import ru.yandex.vertis.general.gost.model.Offer.OfferId
import ru.yandex.vertis.general.gost.model.testkit.OfferGen
import ru.yandex.vertis.general.gost.storage.OfferDao
import ru.yandex.vertis.general.gost.storage.ydb.offer.YdbOfferDao
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.{assert, checkNM, suite, testM, DefaultRunnableSpec}

object YdbOfferDaoSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("YdbOfferDao")(
      testM("создание оффера") {
        checkNM(1)(OfferGen.anyOffer) { offer =>
          for {
            offerId <- zio.random.nextLong.map(x => OfferId(x.toString))
            offerWithCorrectId = offer.copy(offerId = offerId)
            _ <- runTx(OfferDao.createOrUpdateOffer(offerWithCorrectId))
            saved <- runTx(OfferDao.getOffer[Offer](offerId))

          } yield assert(saved)(isSome(equalTo(offerWithCorrectId)))
        }
      },
      testM("вернуть None если оффера не существует") {
        for {
          offerId <- zio.random.nextLong.map(x => OfferId(x.toString))
          saved <- runTx(OfferDao.getOffer[Offer](offerId))
        } yield assert(saved)(isNone)
      },
      testM("чтение офферов с курсором") {
        checkNM(1)(OfferGen.anyOffer) { offer =>
          for {
            offerId1 <- zio.random.nextLong.map(x => OfferId(x.toString))
            offerId2 <- zio.random.nextLong.map(x => OfferId(x.toString))
            _ <- runTx(OfferDao.createOrUpdateOffer(offer.copy(offerId = offerId1)))
            _ <- runTx(OfferDao.createOrUpdateOffer(offer.copy(offerId = offerId2)))
            res1 <- OfferDao.getOffers(None).take(1).runCollect
            res2 <- OfferDao.getOffers(from = res1.lastOption.map(_.cursor)).take(100).runCollect
          } yield assert(res1)(hasSize(equalTo(1))) && assert(res2)(hasSize(isGreaterThanEqualTo(1))) &&
            assert((res1 ++ res2).map(_.value.offerId))(hasSubset(List(offerId1, offerId2))) &&
            assert(res1.map(_.value.offerId))(hasNoneOf(res2.map(_.value.offerId)))
        }
      }
    ) @@ shrinks(0) @@ sequential
  }.provideCustomLayerShared {
    TestYdb.ydb >>> (YdbOfferDao.live ++ Ydb.txRunner) ++ Clock.live
  }
}
