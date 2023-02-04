package ru.yandex.vertis.passport.dao

import org.joda.time.Instant

import java.util.NoSuchElementException
import org.scalacheck.Gen
import org.scalatest.{FreeSpec, WordSpec}
import ru.yandex.vertis.passport.model.{Session, SessionOwner, SessionUser}
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.concurrent.Futures

import scala.collection.immutable.Seq
import scala.util.Random

/**
  * Tests for [[SessionDao]]
  *
  * @author zvez
  */
trait SessionDaoSpec extends WordSpec with SpecBase {

  import scala.concurrent.ExecutionContext.Implicits.global

  def sessionDao: SessionDao

  val userId = ModelGenerators.userId.next
  val owner = SessionUser(userId)

  val someSession = ModelGenerators.session.next.copy(userId = Some(userId))
  val someSession2 = ModelGenerators.session.next.copy(userId = Some(userId))

  val someFakeSidsGen = for {
    count <- Gen.choose(0, 50)
    ids <- Gen.listOfN(count, ModelGenerators.simpleSessionId)
  } yield ids

  "SessionDao" should {
    "get should return None if session doesn't exist" in {
      sessionDao.get(someSession.id).failed.futureValue shouldBe a[NoSuchElementException]
    }

    "getUserSessions should return nothing if user doesn't have any session" in {
      sessionDao.getUserSessionIds(owner).futureValue should be(empty)
    }

    "getUserSessionIds with multiple sessions" in {
      val owner = ModelGenerators.sessionOwner.next
      val createdSessions = createSomeSessions(owner)

      val result = sessionDao.getUserSessionIds(owner).futureValue
      result should contain theSameElementsAs createdSessions.map(_.id)
    }

    "delete should affect user->sessions links" in {
      val owner = ModelGenerators.sessionOwner.next
      val Seq(s1, s2) = createSomeSessions(owner, 2)
      sessionDao.delete(s1.id).futureValue
      val result = sessionDao.getUserSessionIds(owner).futureValue
      result should contain theSameElementsAs Seq(s2.id)
    }

    "getBulk" should {
      "empty ids" in {
        sessionDao.getBulk(Nil).futureValue shouldBe Nil
      }

      "non existent id" in {
        sessionDao.getBulk(Seq(ModelGenerators.simpleSessionId.next)).futureValue shouldBe Nil
      }

      "real loading" in {
        val owner = ModelGenerators.sessionOwner.next
        val createdSessions = createSomeSessions(owner)

        sessionDao.getBulk(createdSessions.map(_.id)).futureValue should contain theSameElementsAs createdSessions
      }

      "mixed" in {
        val owner = ModelGenerators.sessionOwner.next
        val createdSessions = createSomeSessions(owner, Gen.choose(0, 50).next)
          .map(s => s.id -> s)
          .toMap
        val fakeIds = someFakeSidsGen.next

        val reqIds = Random.shuffle(createdSessions.keys ++ fakeIds).toSeq
        val result = sessionDao.getBulk(reqIds).futureValue
        result should contain theSameElementsAs createdSessions.values

//        val expectedInOrder = reqIds.flatMap(createdSessions.get)
//        result should contain theSameElementsInOrderAs expectedInOrder
      }
    }

    "delete bulk" should {
      "empty ids" in {
        sessionDao.deleteBulk(Nil).futureValue
      }

      "non existent id" in {
        sessionDao.deleteBulk(Seq(ModelGenerators.simpleSessionId.next)).futureValue
      }

      "real deletion" in {
        val owner = ModelGenerators.sessionOwner.next
        val createdSessions = createSomeSessions(owner)

        sessionDao.deleteBulk(createdSessions.map(_.id)).futureValue

        sessionDao.getBulk(createdSessions.map(_.id)).futureValue shouldBe empty
      }

      "mixed" in {
        val owner = ModelGenerators.sessionOwner.next
        val createdSessions = createSomeSessions(owner, Gen.choose(0, 50).next)
        val fakeIds = someFakeSidsGen.next

        val reqIds = Random.shuffle(createdSessions.map(_.id) ++ fakeIds)
        sessionDao.deleteBulk(reqIds).futureValue

        sessionDao.getBulk(reqIds).futureValue shouldBe empty
      }
    }

    "messing with sessions doesn't break links" in {
      val owner = ModelGenerators.sessionOwner.next
      val initialSessions = createSomeSessions(owner, 5)
      (1 to 5).foldLeft(initialSessions.map(_.id)) { (sessions, _) =>
        val (toDelete, toLive) = sessions.splitAt(Random.nextInt(sessions.length))
        Futures.traverseSequential(toDelete)(sessionDao.delete).futureValue
        val newSessions = createSomeSessions(owner, 3).map(_.id)
        val allSessions = toLive ++ newSessions
        sessionDao.getUserSessionIds(owner).futureValue should contain theSameElementsAs allSessions
        allSessions
      }
    }

    "messing with sessions doesn't break links (bulk edition)" in {
      val owner = ModelGenerators.sessionOwner.next
      val initialSessions = createSomeSessions(owner)
      (1 to 5).foldLeft(initialSessions.map(_.id)) { (sessions, _) =>
        val (toDelete, toLive) = sessions.splitAt(Random.nextInt(sessions.length))
        sessionDao.deleteBulk(toDelete).futureValue
        val newSessions = createSomeSessions(owner).map(_.id)
        val allSessions = toLive ++ newSessions
        sessionDao.getUserSessionIds(owner).futureValue should contain theSameElementsAs allSessions
        allSessions
      }
    }

    "deleteAllUserSessions" should {

      "non existent user" in {
        sessionDao.deleteAllUserSessions(ModelGenerators.sessionOwner.next)._2.futureValue
      }

      "really delete something" in {
        val owner = ModelGenerators.sessionOwner.next
        val sessions = createSomeSessions(owner)
        val (fast, slow) = sessionDao.deleteAllUserSessions(owner)
        fast.futureValue
        sessionDao.getUserSessionIds(owner).futureValue shouldBe empty

        slow.futureValue
        sessionDao.getBulk(sessions.map(_.id)).futureValue shouldBe empty
      }
    }

    "session lifecycle" should {
      "create" in {
        sessionDao.create(someSession).futureValue should be(())
      }

      "get created" in {
        sessionDao.get(someSession.id).futureValue should be(someSession)
      }

      "getUserSessionIds should contain created session" in {
        val result = sessionDao.getUserSessionIds(owner).futureValue
        result should contain theSameElementsAs Seq(someSession.id)
      }

      "trying to create session with the same id should fail" in {
        sessionDao.create(someSession).failed.futureValue shouldBe a[Exception]
      }

      "delete" in {
        sessionDao.delete(someSession.id).futureValue should be(())
        sessionDao.get(someSession.id).failed.futureValue shouldBe a[NoSuchElementException]
      }
      "session should be updatable" should {
        "update" in {
          sessionDao.create(someSession2).futureValue
          val newSession = someSession2.copy(creationDate = Instant.now())
          sessionDao.update(newSession).futureValue
          val fromDb = sessionDao.get(someSession2.id).futureValue
          sessionDao.delete(fromDb.id).futureValue
          fromDb should be(newSession)
        }
      }

    }
  }

  def createSomeSessions(owner: SessionOwner, count: Int = 10): Seq[Session] = {
    (1 to count).map { _ =>
      val sid = ModelGenerators.richSessionId.next.copy(owner = owner)
      val session = ModelGenerators.session.next.withOwner(owner).copy(id = sid)
      sessionDao.create(session).futureValue
      session
    }
  }
}
