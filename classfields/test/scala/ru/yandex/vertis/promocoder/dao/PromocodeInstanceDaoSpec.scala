package ru.yandex.vertis.promocoder.dao

import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.promocoder.dao.PromocodeInstanceDao.{Patch, Source}
import ru.yandex.vertis.promocoder.model.PromocodeInstance.{ReferringStatuses, Statuses}
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{Constraints, Promocode, PromocodeId}
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.Filter.{
  ById,
  ByPromocode,
  ByReferringStatus,
  ByStatus,
  ByUser
}
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.PromocodeConstraintViolationException
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.PromocodeConstraintViolationException.Codes
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.annotation.nowarn
import scala.collection.immutable.::
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/** Specs on [[PromocodeInstanceDao]]
  *
  * @author alex-kovalenko
  */
@nowarn
trait PromocodeInstanceDaoSpec extends DaoSpecBase with ModelGenerators with BeforeAndAfterEach {

  import ExecutionContext.Implicits.global

  override protected def beforeEach(): Unit = {
    dao.clean.futureValue
    super.beforeEach()
  }

  def promocodeDao: PromocodeDao
  def dao: PromocodeInstanceDao with CleanableDao

  val user = "user"
  val user1 = "user1"
  val user2 = "user2"

  val EmptyConstraints = Constraints(
    deadline = DateTimeUtil.now().plusDays(2),
    totalActivations = Int.MaxValue,
    userActivations = Int.MaxValue
  )

  "PromocodeInstanceDao (simple)" should {

    def createPromos(n: Int, prefix: PromocodeId): Future[List[Promocode]] = {
      val ps = uniquePromocodesGen(n).next
      val promos = ps.map(p => p.copy(code = s"$prefix${p.code}", constraints = EmptyConstraints))
      Future.traverse(promos)(promocodeDao.upsert).map(_ => promos)
    }

    "create new instance" in {
      asyncTest {
        for {
          p1 :: p2 :: Nil <- createPromos(2, "create")
          instance <- dao.insert(Source(user1, p1))
          _ = instance.user shouldBe user1
          _ = instance.code shouldBe p1.code
          _ = instance.status shouldBe Statuses.Created

          other <- dao.insert(Source(user1, p1))
          _ = other.id should not be instance.id

          _ <- Future
            .sequence(
              Iterable(
                dao.insert(Source(user1, p2)),
                dao.insert(Source(user2, p1)),
                dao.insert(Source(user2, p2))
              )
            )
            .map(_.map(_.id).toSet.size shouldBe 3)
        } yield ()
      }
    }

    "get instance" when {
      "got filter by promocode" in {
        asyncTest {
          for {
            p :: Nil <- createPromos(1, "f_by_code")
            _ <- dao.insert(Source(user1, p))
            _ <- dao.insert(Source(user1, p))
            _ <- dao.insert(Source(user2, p))

            _ <- dao
              .count(ByPromocode(p.code))
              .map(_ shouldBe 3)
            instances <- dao.get(ByPromocode(p.code))
            _ = instances should have size 3
            _ = instances.count(_.user == user1) shouldBe 2
            _ = instances.map(_.code).toSet should (have size 1 and contain(p.code))
            _ = instances.map(_.user).toSet should
              (have size 2 and contain(user1) and contain(user2))
          } yield ()
        }
      }

      "got filter by user" in {
        asyncTest {
          for {
            p1 :: p2 :: Nil <- createPromos(2, "f_by_user")
            _ <- dao.insert(Source(user1, p1))
            _ <- dao.insert(Source(user1, p1))
            _ <- dao.insert(Source(user1, p2))

            instances <- dao.get(ByUser(user1))
            _ = instances should have size 3
            _ = instances.map(_.user).toSet should (have size 1 and contain(user1))
            _ = instances.map(_.code).toSet should (have size 2 and contain(p1.code) and contain(p2.code))
          } yield ()
        }
      }

      "got filter by status" in {
        asyncTest {
          for {
            p1 :: p2 :: Nil <- createPromos(2, "f_by_status")
            _ <- dao.insert(Source(user1, p1))
            _ <- dao.insert(Source(user1, p2))

            _ <- dao
              .count(ByStatus(Statuses.Created))
              .map(_ shouldBe 2)
            created <- dao.get(ByStatus(Statuses.Created))
            _ = created.map(_.code).toSet should (contain(p1.code) and contain(p2.code))

            _ <- dao
              .count(ByStatus(Statuses.Shipped))
              .map(_ shouldBe 0)
            _ <- dao
              .get(ByStatus(Statuses.Shipped))
              .map(_.size shouldBe 0)
            _ <- dao.update(created.head.id, Patch(status = Some(Statuses.Shipped)))

            _ <- dao
              .count(ByStatus(Statuses.Shipped))
              .map(_ shouldBe 1)
            _ <- dao
              .get(ByStatus(Statuses.Shipped))
              .map(_.toList match {
                case h :: Nil if h.id == created.head.id =>
                case other => fail(s"Unexpected $other")
              })
          } yield ()
        }
      }

      "got filter by id" in {
        asyncTest {
          for {
            p1 :: Nil <- createPromos(1, "f_by_id")
            _ <- dao.insert(Source(user1, p1))
            instance <- dao.get(ByPromocode(p1.code)).map(_.head)
            _ <- dao
              .count(ById(instance.id))
              .map(_ shouldBe 1)
            instances <- dao.get(ById(instance.id))
            _ = instances should have size 1
            _ = instances.head shouldBe instance
          } yield ()
        }
      }

      "got filter by referring status" in {
        asyncTest {
          for {
            p1 :: p2 :: Nil <- createPromos(2, "f_by_ref_st")
            pRef = p1.copy(features = Iterable(FeatureGen.next.copy(referring = Some(ReferringGen.next))))
            pNonRef = p2.copy(features = p2.features.map(_.copy(referring = None)))

            iRef <- dao.insert(Source(user1, pRef))
            _ <- dao.insert(Source(user1, pNonRef))

            _ <- dao
              .count(ByReferringStatus(ReferringStatuses.Waiting))
              .map(_ shouldBe 1)
            _ <- dao
              .get(ByReferringStatus(ReferringStatuses.Waiting))
              .map(r =>
                r.map(_.code).toSet should
                  (contain(pRef.code) and not contain pNonRef.code)
              )
            _ <- dao
              .count(ByReferringStatus(ReferringStatuses.Shipped))
              .map(_ shouldBe 0)
            _ <- dao
              .get(ByReferringStatus(ReferringStatuses.Shipped))
              .map(r => r.size shouldBe 0)

            updated <- dao.update(iRef.id, Patch(refStatus = Some(ReferringStatuses.Shipped)))
            _ <- dao
              .count(ByReferringStatus(ReferringStatuses.Shipped))
              .map(_ shouldBe 1)
            _ <- dao
              .get(ByReferringStatus(ReferringStatuses.Shipped))
              .map(r => r.map(_.id).toSet should (have size 1 and contain(updated.id)))
          } yield ()
        }
      }
    }

    "update instance" when {
      "got patch with status" in {
        asyncTest {
          for {
            p1 :: Nil <- createPromos(1, "upd_status")
            _ <- dao.insert(Source(user1, p1))

            instance <- dao.get(ByPromocode(p1.code)).map(_.head)

            updated <- dao.update(instance.id, Patch(Some(Statuses.Shipped)))
            _ = updated.id shouldBe instance.id
            _ = updated.status shouldBe Statuses.Shipped

            _ <- dao
              .get(ById(instance.id))
              .map(_.toList should matchPattern { case `updated` :: Nil =>
              })
            _ = shouldFailWith[NoSuchElementException] {
              dao.update("", Patch(Some(Statuses.Shipped)))
            }
          } yield ()
        }
      }

      "got patch with referring status" in {
        asyncTest {
          for {
            p1 :: p2 :: Nil <- createPromos(2, "upd_ref_status")
            pRef = p1.copy(features = Iterable(FeatureGen.next.copy(referring = Some(ReferringGen.next))))
            pNonRef = p2.copy(features = p2.features.map(_.copy(referring = None)))

            iRef <- dao.insert(Source(user1, pRef))
            iNonRef <- dao.insert(Source(user1, pNonRef))

            _ <- dao
              .update(iNonRef.id, Patch(refStatus = Some(ReferringStatuses.Shipped)))
              .map(_.referringStatus shouldBe None)
            updated <- dao.update(iRef.id, Patch(refStatus = Some(ReferringStatuses.Shipped)))
            _ = updated.referringStatus shouldBe Some(ReferringStatuses.Shipped)
            _ <- dao
              .get(ById(iRef.id))
              .map(_.toList should matchPattern { case `updated` :: Nil =>
              })

          } yield ()
        }
      }
    }

    "delete instance" in {
      asyncTest {
        for {
          p1 :: p2 :: Nil <- createPromos(2, "delete")
            .map(_.map(p => p.copy(features = p.features.map(_.copy(referring = None)))))
          pi1 <- dao.insert(Source(user1, p1))
          pi2 <- dao.insert(Source(user1, p2))
          _ <- dao.get(ById(pi1.id)).map(_ should contain(pi1))
          _ <- dao.get(ById(pi2.id)).map(_ should contain(pi2))
          _ <- dao.delete(pi1.id)
          _ <- dao.get(ById(pi1.id)).map(_ shouldBe empty)
          _ <- dao.get(ById(pi2.id)).map(_ should contain(pi2))
          _ <- dao.delete(pi2.id)
          _ <- dao.get(ById(pi2.id)).map(_ shouldBe empty)
        } yield ()
      }
    }
  }

  "PromocodeInstanceDao (special cases)" should {
    "fail to insert" when {
      "get unexistent promocode" in {
        val promocode = PromocodeGen.next.copy(code = "unexistent")
        shouldFailWith[NoSuchElementException] {
          dao.insert(Source(user, promocode))
        }
      }
      "get expired promocode" in {
        val promocode = PromocodeGen.next
          .copy(code = "expired", constraints = EmptyConstraints.copy(deadline = DateTimeUtil.now().minusDays(1)))
        promocodeDao.upsert(promocode).futureValue

        val e = shouldFailWith[PromocodeConstraintViolationException] {
          dao.insert(Source("user", promocode))
        }
        e.code shouldBe Codes.Expired.toString
      }

      "get user from blacklist" in {
        val promocode =
          PromocodeGen.next.copy(code = "blacklist", constraints = EmptyConstraints.copy(blacklist = Set(user)))
        promocodeDao.upsert(promocode).futureValue
        val e = shouldFailWith[PromocodeConstraintViolationException] {
          dao.insert(Source(user, promocode))
        }
        e.code shouldBe Codes.Blacklist.toString
      }

      "total activations limit exceed" in {
        val promocode = PromocodeGen.next
          .copy(code = "total-activations-limit-exceed", constraints = EmptyConstraints.copy(totalActivations = 1))
        promocodeDao.upsert(promocode).futureValue

        dao.insert(Source(user, promocode)).futureValue
        val e1 = shouldFailWith[PromocodeConstraintViolationException] {
          dao.insert(Source(user, promocode))
        }
        e1.code shouldBe Codes.TotalLimitExceeded.toString
        val e2 = shouldFailWith[PromocodeConstraintViolationException] {
          dao.insert(Source(s"another-$user", promocode))
        }
        e2.code shouldBe Codes.TotalLimitExceeded.toString
      }

      "per-user activations limit exceed" in {
        val promocode = PromocodeGen.next
          .copy(code = "per-user-limit-exceed", constraints = EmptyConstraints.copy(userActivations = 1))

        promocodeDao.upsert(promocode).futureValue
        dao.insert(Source(user, promocode)).futureValue
        val e = shouldFailWith[PromocodeConstraintViolationException] {
          dao.insert(Source(user, promocode))
        }
        e.code shouldBe Codes.UserLimitExceeded.toString

        dao.insert(Source(s"another-$user", promocode)).futureValue
      }
    }

    "concurrently create instances near total limit" in {
      val iterations = 8
      val limit = 5
      val promocode =
        PromocodeGen.next.copy(code = "concurrent_total", constraints = EmptyConstraints.copy(totalActivations = limit))
      promocodeDao.upsert(promocode).futureValue
      val users = listNUnique(2, UserGen)(identity).next
      val cdl = new CountDownLatch(iterations)
      implicit val ec: ExecutionContextExecutor = ExecutionContext
        .fromExecutor(Executors.newFixedThreadPool(4))

      val futures = for {
        i <- 1 to iterations
        user = users(i % 2)
        future = {
          val f = dao.insert(Source(user, promocode))
          f.onComplete(_ => cdl.countDown())
          f
        }
      } yield future

      cdl.await(10, TimeUnit.SECONDS)

      val resultsOrErrors = futures.flatMap(_.eitherValue)

      resultsOrErrors.size shouldBe iterations
      val (failed, success) = resultsOrErrors.partition(_.isLeft)
      success.size shouldBe limit
      failed.size shouldBe (iterations - limit)
    }

    "concurrently create instances near per-user limit" in {
      val iterations = 5
      val limit = 3
      val promocode = PromocodeGen.next
        .copy(code = "concurrent_per-user", constraints = EmptyConstraints.copy(userActivations = limit))
      promocodeDao.upsert(promocode).futureValue
      val user = UserGen.next
      val cdl = new CountDownLatch(iterations)
      implicit val ec: ExecutionContextExecutor = ExecutionContext
        .fromExecutor(Executors.newFixedThreadPool(4))

      val futures = for {
        _ <- 1 to iterations
        future = {
          val f = dao.insert(Source(user, promocode))
          f.onComplete(_ => cdl.countDown())
          f
        }
      } yield future

      cdl.await(10, TimeUnit.SECONDS)

      val resultsOrErrors = futures.flatMap(_.eitherValue)

      resultsOrErrors.size shouldBe iterations
      val (failed, success) = resultsOrErrors.partition(_.isLeft)
      success.size shouldBe limit
      failed.size shouldBe (iterations - limit)
    }
  }
}
