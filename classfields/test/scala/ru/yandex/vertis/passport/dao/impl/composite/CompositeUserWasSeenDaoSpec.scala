package ru.yandex.vertis.passport.dao.impl.composite

import org.joda.time.{DateTime, DateTimeUtils}
import ru.yandex.vertis.passport.dao.UserWasSeenDao
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryUserWasSeenDao
import ru.yandex.vertis.passport.model.Platforms
import ru.yandex.vertis.passport.model.visits.{UserWasSeenCounter, UserWasSeenCounters}
import ru.yandex.vertis.passport.service.session.UserWasSeenDaoSpec
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.ExecutionContext

class CompositeUserWasSeenDaoSpec extends UserWasSeenDaoSpec {

  implicit protected val ec: ExecutionContext = ExecutionContext.global
  val oldDao = new InMemoryUserWasSeenDao
  val newDao = new InMemoryUserWasSeenDao

  override val dao: UserWasSeenDao = new CompositeUserWasSeenDao(oldDao, newDao)

  "CompositeUserWasSeenDao" should {
    "transfer data from old to new" in {
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.minusDays(1).plusHours(1).getMillis)
      val userId = ModelGenerators.userId.next
      oldDao.touch(userId, Platforms.Ios).futureValue
      oldDao
        .addWasSeenCounters(
          userId,
          UserWasSeenCounters(
            ios = UserWasSeenCounter(5, moment.minusDays(1).toInstant, moment.toInstant)
          )
        )
        .futureValue
      val counters = dao.getWasSeenCounters(userId).futureValue.value
      counters.overall.visits shouldBe 6
      counters.overall.firstSeen shouldBe moment.minusDays(1).toInstant
      counters.overall.lastSeen shouldBe moment.toInstant
      counters.android.visits shouldBe 0
      counters.android.firstSeen.getMillis shouldBe 0L
      counters.android.lastSeen.getMillis shouldBe 0L
      counters.ios.visits shouldBe 6
      counters.ios.firstSeen shouldBe moment.minusDays(1).toInstant
      counters.ios.lastSeen shouldBe moment.toInstant
      Thread.sleep(1000); //writing to new dao is async
      val newcounters = newDao.getWasSeenCounters(userId).futureValue.value
      newcounters.overall.visits shouldBe 6
      newcounters.overall.firstSeen shouldBe moment.minusDays(1).toInstant
      newcounters.overall.lastSeen shouldBe moment.toInstant
      newcounters.android.visits shouldBe 0
      newcounters.android.firstSeen.getMillis shouldBe 0L
      newcounters.android.lastSeen.getMillis shouldBe 0L
      newcounters.ios.visits shouldBe 6
      newcounters.ios.firstSeen shouldBe moment.minusDays(1).toInstant
      newcounters.ios.lastSeen shouldBe moment.toInstant
    }
  }
}
