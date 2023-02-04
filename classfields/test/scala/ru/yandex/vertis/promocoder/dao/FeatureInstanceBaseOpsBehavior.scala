package ru.yandex.vertis.promocoder.dao

import org.scalatest.BeforeAndAfterEach
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.promocoder.model.FeatureInstance
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.service.FeatureInstanceService.Filter.{
  ActiveById,
  ActiveForUser,
  ActiveForUsersIn,
  ById,
  ByOrigin,
  ForUserByOrigin
}
import ru.yandex.vertis.promocoder.util.DateTimeInterval
import ru.yandex.vertis.util.time.DateTimeUtil.now

import scala.concurrent.Future

/** @author alex-kovalenko
  */
trait FeatureInstanceBaseOpsBehavior extends DaoSpecBase with BeforeAndAfterEach with ModelGenerators {

  type D <: FeatureInstanceBaseOps

  import scala.concurrent.ExecutionContext.Implicits.global

  import FeatureInstanceBaseOpsBehavior._

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(scaled(Span(20, Seconds)))

  override protected def beforeEach(): Unit = {
    dao.clean.futureValue
    super.beforeEach()
  }

  def dao: D with CleanableDao

  def daoWithUpsert(): Unit = {
    "upsert feature instances" in {
      val features = uniqueFeatureInstanceGen(2).next
      val head = features.head
      asyncTest {
        for {
          _ <- dao.upsert(Iterable(head))
          _ <- dao
            .get(ById(head.id))
            .map { fs =>
              fs.size shouldBe 1
              fs.head shouldBe head
            }

          _ <- dao.upsert(features)
          _ <- Future.traverse(features) { f =>
            for {
              got <- dao.get(ById(f.id))
              _ = got should (have size 1 and contain(f))
            } yield ()
          }
          _ <- dao.get(ById(head.id)).map(fs => fs should contain(head))
        } yield ()
      }
    }
  }

  // scalastyle:off
  def daoWithGet(): Unit = {
    "get features" when {
      "got ActiveForUser filter" in {
        asyncTest {
          for {
            _ <- dao.upsert(u2Features :+ u1Feature :+ expired :+ notActiveYet)
            _ <- dao
              .get(ActiveForUser(user1))
              .map(_.toSet should (have size 1 and contain(u1Feature)))
            _ <- dao
              .get(ActiveForUser(user2))
              .map(_.toSet should contain theSameElementsAs u2Features)

            _ <- dao
              .get(ActiveForUser(expired.user))
              .map(_ shouldBe empty)
            _ <- dao
              .get(ActiveForUser(notActiveYet.user))
              .map(_ shouldBe empty)
          } yield ()
        }
      }

      "got ActiveById filter" in {
        asyncTest {
          for {
            _ <- dao.upsert(u1Feature :: expired :: Nil)
            _ <- dao
              .get(ActiveById(u1Feature.id))
              .map(_.toSet shouldBe Set(u1Feature))
            _ <- dao
              .get(ActiveById(expired.id))
              .map(_ shouldBe empty)
            _ <- dao
              .get(ActiveById(notActiveYet.id))
              .map(_ shouldBe empty)
          } yield ()
        }
      }

      "got ById filter" in {
        asyncTest {
          for {
            _ <- dao.upsert(expired :: u1Feature :: notActiveYet :: Nil)
            _ <- dao
              .get(ById(expired.id))
              .map(_.toSet shouldBe Set(expired))
            _ <- dao
              .get(ById(notActiveYet.id))
              .map(_.size shouldBe 1)
          } yield ()
        }
      }

      "got ForUserByOrigin filter" in {
        val u2WithPromo = u2Features.map(_.copy(origin = originPromo))
        val features =
          u2WithPromo :+
            u1Feature :+
            promoFeature :+
            refFeature :+
            apiFeature
        asyncTest {
          for {
            _ <- dao.upsert(features)
            _ <- dao
              .get(ForUserByOrigin(user2, originPromo))
              .map(_.toSet should contain theSameElementsAs u2WithPromo)
            _ <- dao
              .get(ForUserByOrigin(promoFeature.user, promoFeature.origin))
              .map(_.toSet shouldBe Set(promoFeature))
            _ <- dao
              .get(ForUserByOrigin(refFeature.user, refFeature.origin))
              .map(_.toSet shouldBe Set(refFeature))
            _ <- dao
              .get(ForUserByOrigin(user1, promoFeature.origin))
              .map(_.size shouldBe 0)
          } yield ()
        }
      }

      "got ByOrigin filter" in {
        val withPromo = u2Features.map(_.copy(origin = originPromo)) :+ promoFeature
        val features =
          withPromo :+
            u1Feature :+
            promoFeature :+
            refFeature :+
            apiFeature
        asyncTest {
          for {
            _ <- dao.upsert(features)
            _ <- dao
              .get(ByOrigin(originPromo))
              .map(_.toSet should contain theSameElementsAs withPromo)
            _ <- dao
              .get(ByOrigin(refFeature.origin))
              .map(_.toSet shouldBe Set(refFeature))
            _ <- dao
              .get(ByOrigin(apiFeature.origin))
              .map(_.toSet shouldBe Set(apiFeature))
          } yield ()
        }
      }

      "got ActiveForUsersIn filter" in {
        val time = now()
        val interval = DateTimeInterval(time.minusDays(1), time)
        val alreadyExpired = FeatureInstanceGen.next.copy(
          user = user1,
          createTs = time.minusDays(3),
          startTime = None,
          deadline = time.minusDays(2)
        )
        val notStarted = FeatureInstanceGen.next.copy(
          user = user1,
          createTs = time.minusDays(1),
          startTime = Some(time.plusDays(1)),
          deadline = time.plusDays(2)
        )
        val fromFuture = FeatureInstanceGen.next.copy(
          user = user1,
          createTs = time.plusDays(1),
          startTime = None,
          deadline = time.plusDays(2)
        )
        val inByCreate =
          FeatureInstanceGen.next.copy(user = user1, createTs = interval.from, startTime = None, deadline = interval.to)
        val inByStart = FeatureInstanceGen.next.copy(
          user = user1,
          createTs = time.minusDays(1),
          startTime = Some(interval.from),
          deadline = interval.to
        )
        val intLeft = FeatureInstanceGen.next.copy(
          user = user1,
          createTs = time.minusDays(2),
          startTime = None,
          deadline = interval.from.plus(1)
        )
        val intRightByCreate = FeatureInstanceGen.next.copy(
          user = user1,
          createTs = interval.to.minus(1),
          startTime = None,
          deadline = time.plusDays(2)
        )
        val intRightByStart = FeatureInstanceGen.next.copy(
          user = user1,
          createTs = time.minusDays(2),
          startTime = Some(interval.to.minus(1)),
          deadline = time.plusDays(2)
        )
        val otherFeature =
          FeatureInstanceGen.next.copy(user = user2, createTs = interval.from, startTime = None, deadline = interval.to)
        val features: Iterable[FeatureInstance] = Iterable(
          alreadyExpired,
          notStarted,
          fromFuture,
          inByCreate,
          inByStart,
          intLeft,
          intRightByCreate,
          intRightByStart,
          otherFeature
        )
        asyncTest {
          for {
            _ <- dao.upsert(features)
            _ <- dao
              .get(ActiveForUsersIn(Set(user1), interval))
              .map(
                _ should contain theSameElementsAs Iterable(
                  inByCreate,
                  inByStart,
                  intLeft,
                  intRightByCreate,
                  intRightByStart
                )
              )
            _ <- dao.get(ActiveForUsersIn(Set(user2), interval)).map(_ should (have size 1 and contain(otherFeature)))
          } yield ()
        }
      }
    }
  }

}

object FeatureInstanceBaseOpsBehavior extends ModelGenerators {
  val (user, user1, user2) = ("user", "user1", "user2")
  val u1Feature = FeatureInstanceGen.next.copy(user = user1)

  val u2Features = uniqueFeatureInstanceGen(2).next
    .map(_.copy(user = user2))

  val expired =
    FeatureInstanceGen.next.copy(createTs = now().minusDays(2), startTime = None, deadline = now().minusDays(1))

  val notActiveYet = FeatureInstanceGen.next.copy(
    createTs = now().minusDays(1),
    startTime = Some(now().plusDays(1)),
    deadline = now().plusDays(2)
  )

  val originPromo = PromocodeOriginGen.next
  val originRef = ReferringOriginGen.next
  val originApi = ApiOriginGen.next

  val promoFeature = FeatureInstanceGen.next
    .copy(origin = originPromo, user = user)

  val refFeature = FeatureInstanceGen.next
    .copy(origin = originRef, user = user)

  val apiFeature = FeatureInstanceGen.next
    .copy(origin = originApi, user = user)

}
