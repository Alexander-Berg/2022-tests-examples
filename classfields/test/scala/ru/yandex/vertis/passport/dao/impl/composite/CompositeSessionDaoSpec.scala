package ru.yandex.vertis.passport.dao.impl.composite

import ru.yandex.vertis.passport.dao.impl.memory.InMemorySessionDao
import ru.yandex.vertis.passport.dao.{SessionDao, SessionDaoSpec}
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.ExecutionContext

class CompositeSessionDaoSpec extends SessionDaoSpec {

  implicit protected val ec: ExecutionContext = ExecutionContext.global

  val oldDao = new InMemorySessionDao
  val newDao = new InMemorySessionDao

  override val sessionDao: SessionDao = new CompositeSessionDao(oldDao, newDao)

  "CompositeSessionDao" should {
    "transfer session data from old to new" in {
      val owner = ModelGenerators.sessionOwner.next
      val sessions = createSomeSessions(owner)
      for {
        session <- sessions
      } yield {
        oldDao.create(session).futureValue
      }
      sessionDao.getUserSessionIds(owner).futureValue.toSet shouldBe (sessions.map(_.id)).toSet
      sessionDao.get(sessions.head.id).futureValue shouldBe sessions.head
      Thread.sleep(1000); //writing to new dao is async
      newDao.getUserSessionIds(owner).futureValue.toSet shouldBe (sessions.map(_.id)).toSet
      newDao.get(sessions.head.id).futureValue shouldBe sessions.head
    }
  }
}
