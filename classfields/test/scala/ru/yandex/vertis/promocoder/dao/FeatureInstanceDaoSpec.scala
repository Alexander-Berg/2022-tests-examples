package ru.yandex.vertis.promocoder.dao

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.promocoder.dao.FeatureInstanceDao.{CounterPatch, FinishNowPatch}
import ru.yandex.vertis.promocoder.model.{FeatureInstance, Value}
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Filter.{ById, ByOrigin, ForUserByOrigin}
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.{FeatureKeyAlreadyUsed, FeatureNotAvailableException}
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.util.time.DateTimeUtil.now

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/** Specs on [[FeatureInstanceDao]]
  *
  * @author alex-kovalenko
  */
trait FeatureInstanceDaoSpec extends FeatureInstanceBaseOpsBehavior {

  type D = FeatureInstanceDao

  import ExecutionContext.Implicits.global

  val StableNow = DateTimeUtil.now()

  "FeatureInstanceDao" should {
    behave.like(daoWithUpsert())
    behave.like(daoWithGet())

    "insert features" in {
      val f1 :: f2 :: Nil = uniqueFeatureInstanceGen(2).next
      dao.insert(f1).futureValue
      dao.get(ById(f1.id)).futureValue.toList shouldBe List(f1)
      intercept[IllegalArgumentException] {
        dao.insert(f1).await
      }
      dao.insert(f2).futureValue
      dao.get(ById(f2.id)).futureValue.toList shouldBe List(f2)
    }

    "update counter" in {
      val feature = FeatureInstanceGen.next.copy(count = 10)
      val expired = FeatureInstanceGen.next
        .copy(createTs = now().minusDays(2), deadline = now().minusDays(1), startTime = Some(now().minusDays(2)))
      val notActiveYet = FeatureInstanceGen.next.copy(
        createTs = now().minusDays(1),
        startTime = Some(now().plusDays(1)),
        deadline = now().plusDays(2)
      )

      dao.insert(feature).futureValue

      intercept[FeatureNotAvailableException] {
        dao.update(feature.id, CounterPatch(Some(100))).await
      }.feature shouldBe feature

      dao.update(feature.id, CounterPatch(Some(10))).futureValue.count shouldBe 0
      dao.get(ById(feature.id)).futureValue.head.count shouldBe 0
      dao.update(feature.id, CounterPatch(Some(-1))).futureValue.count shouldBe 1
      intercept[NoSuchElementException] {
        dao.update(expired.id, CounterPatch(Some(1))).await
      }

      dao.insert(notActiveYet).futureValue
      intercept[FeatureNotAvailableException] {
        dao.update(notActiveYet.id, CounterPatch(Some(-1))).await
      }

      dao.insert(expired).futureValue
      intercept[FeatureNotAvailableException] {
        dao.update(expired.id, CounterPatch(Some(1))).await
      }.feature shouldBe expired
    }

    "update deadline" in {
      // prepare
      val feature = FeatureInstanceGen.next
      val expired = FeatureInstanceGen.next
        .copy(createTs = now().minusDays(2), deadline = now().minusDays(1), startTime = Some(now().minusDays(2)))
      val notActiveYet = FeatureInstanceGen.next.copy(
        createTs = now().minusDays(1),
        startTime = Some(now().plusDays(1)),
        deadline = now().plusDays(2)
      )
      dao.insert(feature).futureValue

      // test logic
      dao.update(feature.id, FinishNowPatch).futureValue.deadline shouldBe StableNow
      dao.get(ById(feature.id)).futureValue.head.deadline shouldBe StableNow
      intercept[NoSuchElementException] {
        dao.update(expired.id, FinishNowPatch).await
      }

      dao.insert(notActiveYet).futureValue
      intercept[FeatureNotAvailableException] {
        dao.update(notActiveYet.id, FinishNowPatch).await
      }

      dao.insert(expired).futureValue
      intercept[FeatureNotAvailableException] {
        dao.update(expired.id, FinishNowPatch).await
      }.feature shouldBe expired
    }

    "respect key during update" in {
      val feature = FeatureInstanceGen.next.copy(count = 10)
      val key = readableString.next
      asyncTest {
        for {
          _ <- dao.insert(feature)
          _ <- dao
            .update(feature.id, CounterPatch(Some(1)), Some(key))
            .map(_.count shouldBe 9)

          _ = shouldFailWith[FeatureKeyAlreadyUsed] {
            dao.update(feature.id, CounterPatch(Some(1)), Some(key))
          }

          _ <- dao
            .get(ById(feature.id))
            .map(_.head.count shouldBe 9)

        } yield ()

      }
    }

    "upsert" when {
      "increase total and count on upsert" in {
        val feature = FeatureInstanceGen.next

        dao.upsert(Iterable(feature)).futureValue
        dao.get(ById(feature.id)).futureValue.toList shouldBe List(feature)

        dao.upsert(Iterable(feature)).futureValue
        dao.get(ById(feature.id)).futureValue.toList shouldBe List(feature)

        val updateFeature = feature.copy(total = feature.total.map(_ + 10), count = feature.count + 10)
        dao.upsert(Iterable(updateFeature)).futureValue
        dao.get(ById(updateFeature.id)).futureValue.toList shouldBe List(updateFeature)
      }

      "decrease total and count on upsert" in {
        val feature = FeatureInstanceGen.next.copy(count = 50, total = Some(100))

        dao.upsert(Iterable(feature)).futureValue
        dao.get(ById(feature.id)).futureValue.toList shouldBe List(feature)

        val updateFeature = feature.copy(total = feature.total.map(_ - 10), count = feature.count - 10)
        dao.upsert(Iterable(updateFeature)).futureValue
        dao.get(ById(updateFeature.id)).futureValue.toList shouldBe List(updateFeature)
      }

      "not decrease to negative count" in {
        val feature = FeatureInstanceGen.next.copy(count = 50, total = Some(100))

        dao.upsert(Iterable(feature)).futureValue
        dao.get(ById(feature.id)).futureValue.toList shouldBe List(feature)

        val updateFeature = feature.copy(total = feature.total.map(_ - 60), count = 0)
        dao.upsert(Iterable(updateFeature)).futureValue
        dao.get(ById(updateFeature.id)).futureValue.toList shouldBe List(updateFeature)
      }

      "correctly upsert if no total yet" in {
        val feature = FeatureInstanceGen.next.copy(count = 50, total = None)

        dao.upsert(Iterable(feature)).futureValue
        dao.get(ById(feature.id)).futureValue.toList shouldBe List(feature)

        val updateFeature = feature.copy(total = Some(100))
        dao.upsert(Iterable(updateFeature)).futureValue
        dao.get(ById(updateFeature.id)).futureValue.toList shouldBe List(updateFeature)
      }

      "do not increase features" in {
        val feature = FeatureInstanceGen.next.copy(count = 50, total = Some(100))

        dao.upsert(Iterable(feature)).futureValue
        dao.get(ById(feature.id)).futureValue.toList shouldBe List(feature)

        val delta = 20
        dao.update(feature.id, CounterPatch(Some(delta))).futureValue
        dao.get(ById(feature.id)).futureValue.toList shouldBe List(feature.copy(count = feature.count - delta))

        dao.upsert(Iterable(feature)).futureValue
        dao.get(ById(feature.id)).futureValue.toList shouldBe List(feature.copy(count = feature.count - delta))
      }

    }

    "delete" when {
      import FeatureInstanceBaseOpsBehavior._
      "got ForUserByOrigin filter" in {
        val u2WithPromo = u2Features.map(_.copy(origin = originPromo))
        val features =
          u2WithPromo :+
            promoFeature :+
            refFeature :+
            apiFeature
        asyncTest {
          for {
            _ <- dao.upsert(features)
            _ <- dao.delete(ForUserByOrigin(user1, originPromo))
            _ <- dao
              .get(ByOrigin(originPromo))
              .map(_.size shouldBe 3)
            _ <- dao.delete(ForUserByOrigin(user2, originPromo))
            _ <- dao
              .get(ByOrigin(originPromo))
              .map(_.toSet shouldBe Set(promoFeature))
            _ <- dao.delete(ForUserByOrigin(promoFeature.user, promoFeature.origin))
            _ <- dao
              .get(ByOrigin(originPromo))
              .map(_ shouldBe empty)
            _ <- dao
              .get(ByOrigin(originRef))
              .map(_ should not be empty)
            _ <- dao.delete(ForUserByOrigin(refFeature.user, originRef))
            _ <- dao
              .get(ByOrigin(originRef))
              .map(_ shouldBe empty)
            _ <- dao
              .get(ByOrigin(originApi))
              .map(_ should not be empty)
            _ <- dao.delete(ForUserByOrigin(apiFeature.user, apiFeature.origin))
            _ <- dao
              .get(ForUserByOrigin(apiFeature.user, apiFeature.origin))
              .map(_ shouldBe empty)
          } yield ()
        }
      }

      "got ByOrigin filter" in {
        val u2WithPromo = u2Features.map(_.copy(origin = originPromo))
        val features =
          u2WithPromo :+
            promoFeature :+
            refFeature :+
            apiFeature
        asyncTest {
          for {
            _ <- dao.upsert(features)
            _ <- dao
              .get(ByOrigin(originPromo))
              .map(_.size shouldBe 3)
            _ <- dao.delete(ByOrigin(originPromo))
            _ <- dao
              .get(ByOrigin(originPromo))
              .map(_ shouldBe empty)
            _ <- dao
              .get(ByOrigin(originRef))
              .map(_ should not be empty)
            _ <- dao.delete(ByOrigin(originRef))
            _ <- dao
              .get(ByOrigin(originRef))
              .map(_ shouldBe empty)
            _ <- dao
              .get(ByOrigin(originApi))
              .map(_ should not be empty)
            _ <- dao.delete(ByOrigin(originApi))
            _ <- dao
              .get(ByOrigin(originApi))
              .map(_ shouldBe empty)
          } yield ()
        }
      }
    }
  }

  "FeatureInstanceDao (special cases)" should {
    "correctly update count" when {
      "has concurrent requests" in {
        val concurrency = 4
        val iterations = 50
        val count: Value = iterations

        val id = {
          val feature = FeatureInstanceGen.next
            .copy(id = "concurrent_decrement", count = count)
          dao.insert(feature).futureValue
          feature.id
        }

        implicit val executor: ExecutionContextExecutor = ExecutionContext
          .fromExecutor(Executors.newFixedThreadPool(concurrency))

        def action: Future[FeatureInstance] = dao.update(id, CounterPatch(Some(1)))

        Future
          .sequence {
            (1 to iterations).map(_ => action)
          }
          .futureValue(Timeout(Span(10, Seconds)))

        val feature = dao.get(ById(id)).futureValue.head
        feature.count shouldBe 0

      }
    }
  }
}
