package ru.yandex.auto.search.indexing.ydb

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import zio.test.TestAspect._
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._

import scala.util.Random

object YdbOfferChangesDaoSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("offers crud") {
      testM("insert offer") {
        val offerInfo = buildOfferInfo()

        for {
          _ <- runTx(OfferChangesDao.upsertOffer(offerInfo))
          offerStatus <- runTx(OfferChangesDao.findOffer(offerInfo.offerId))

        } yield {
          assert(offerStatus)(equalTo(Some(offerInfo)))
        }
      }

      testM("update offer status") {
        val offerInfo = buildOfferInfo()

        for {
          _ <- runTx(OfferChangesDao.upsertOffer(offerInfo))
          _ <- runTx(OfferChangesDao.updateOfferStatus(offerInfo.offerId, OfferIndexingStatus.Indexed))
          offerFound <- runTx(OfferChangesDao.findOffer(offerInfo.offerId))

        } yield {
          assert(offerFound.map(_.status))(equalTo(Some(OfferIndexingStatus.Indexed)))
        }
      }

    } @@ sequential @@ before(TestYdb.clean(YdbOfferChangesDao.tableName))
  }.provideCustomLayerShared {
    TestYdb.ydb >+> YdbOfferChangesDao.live ++ Ydb.txRunner
  }

  def buildOfferInfo(status: OfferIndexingStatus = OfferIndexingStatus.Waiting): IndexedOfferInfo = {
    val offerId = Random.alphanumeric.take(15).mkString
    IndexedOfferInfo(
      offerId = offerId,
      version = Random.nextInt(100),
      hash = Random.nextLong(10000),
      status
    )

  }

}
