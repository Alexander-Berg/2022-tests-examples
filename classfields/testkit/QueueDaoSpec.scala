package ru.yandex.vertis.general.gost.storage.testkit

import java.time.temporal.ChronoUnit
import common.zio.ydb.Ydb.HasTxRunner
import common.zio.ydb.testkit.TestYdb.runTx
import general.feed.model.FeedSourceEnum
import ru.yandex.vertis.general.common.model.editor.testkit.Editors
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.gost.model.Offer.{ExternalOfferId, FeedInfo, OfferId}
import ru.yandex.vertis.general.gost.model.OfferExport
import ru.yandex.vertis.general.gost.model.testkit.OfferGen
import ru.yandex.vertis.general.gost.storage.QueueDao
import ru.yandex.vertis.general.gost.storage.QueueDao.QueueDao
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test._

object QueueDaoSpec {

  def spec(
      label: String): Spec[QueueDao with Clock with HasTxRunner with Random with Sized with TestConfig, TestFailure[Throwable], TestSuccess] = {
    suite(label)(
      testM("push & pull offers") {
        checkNM(1)(OfferGen.anyOffer) { offer =>
          for {
            shardId <- zio.random.nextInt
            now <- zio.clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))
            offerId <- zio.random.nextLong.map(x => OfferId(x.toString))
            export1 = OfferExport(shardId, now, offerId, offer, Editors.automatic("some-editor"))
            export2 = OfferExport(
              shardId,
              now.plusSeconds(1),
              offerId,
              offer,
              Editors.seller(SellerId.UserId(12343L))
            )
            _ <- runTx(QueueDao.push(export1))
            _ <- runTx(QueueDao.push(export2))
            saved <- runTx(QueueDao.pull(shardId, 10))
          } yield assert(saved)(equalTo(Seq(export1, export2)))
        }
      },
      testM("remove offers from queue") {
        checkNM(1)(OfferGen.anyOffer) { offer =>
          for {
            shardId <- zio.random.nextInt
            now <- zio.clock.instant
            offerId <- zio.random.nextLong.map(x => OfferId(x.toString))
            export = OfferExport(shardId, now, offerId, offer, Editors.seller(SellerId.UserId(4321L)))
            _ <- runTx(QueueDao.push(export))
            _ <- runTx(QueueDao.remove(export :: Nil))
            saved <- runTx(QueueDao.pull(shardId, 10))
          } yield assert(saved)(isEmpty)
        }
      },
      testM("Получить метрики очереди") {
        checkNM(1)(OfferGen.anyOffer) { offer =>
          for {
            shardId <- zio.random.nextInt
            now <- zio.clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))
            offerId <- zio.random.nextLong.map(x => OfferId(x.toString))
            export = OfferExport(shardId, now, offerId, offer, Editors.automatic("for-testing"))
            initialQueueMetrics <- runTx(QueueDao.getQueueMetrics)
            _ <- runTx(QueueDao.push(export))
            queueMetricsAfterPush <- runTx(QueueDao.getQueueMetrics)
          } yield assert(queueMetricsAfterPush.size)(equalTo(initialQueueMetrics.size + 1))
        }
      },
      testM("push & pull offer with feed info") {
        checkNM(1)(OfferGen.anyOffer) { offer =>
          for {
            shardId <- zio.random.nextInt
            now <- zio.clock.instant.map(_.truncatedTo(ChronoUnit.MILLIS))
            offerId <- zio.random.nextLong.map(x => OfferId(x.toString))
            export1 = OfferExport(
              shardId,
              now,
              offerId,
              offer.copy(feedInfo =
                Some(
                  FeedInfo(
                    namespaceId = None,
                    externalId = ExternalOfferId("1"),
                    taskId = 1L,
                    rawOffer = None,
                    source = FeedSourceEnum.FeedSource.FEED
                  )
                )
              ),
              Editors.automatic("some-editor")
            )

            _ <- runTx(QueueDao.push(export1))
            saved <- runTx(QueueDao.pull(shardId, 10))
          } yield assert(saved)(equalTo(Seq(export1)))
        }
      }
    )
  }
}
