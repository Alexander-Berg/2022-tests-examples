package ru.yandex.vertis.passport.service.session

import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.{BeforeAndAfter, OptionValues, WordSpec}
import ru.yandex.vertis.passport.dao.UserWasSeenDao
import ru.yandex.vertis.passport.model.visits.{UserWasSeenCounter, UserWasSeenCounters}
import ru.yandex.vertis.passport.model.{Platform, Platforms}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}

import scala.util.Random

/**
  *
  * @author zvez
  */
trait UserWasSeenDaoSpec extends WordSpec with SpecBase with BeforeAndAfter with OptionValues {

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

  def dao: UserWasSeenDao

  "UserWasSeenDao.addWasSeenCounters" should {
    "add counters to user without counters entry" in {
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.getMillis)
      val userId = ModelGenerators.userId.next
      dao
        .addWasSeenCounters(
          userId,
          UserWasSeenCounters(
            ios = UserWasSeenCounter(5, moment.minusDays(1).toInstant, moment.minusDays(1).plusHours(1).toInstant)
          )
        )
        .futureValue
      val counters = dao.getWasSeenCounters(userId).futureValue.value
      counters.overall.visits shouldBe 5
      counters.overall.firstSeen shouldBe moment.minusDays(1).toInstant
      counters.overall.lastSeen shouldBe moment.minusDays(1).plusHours(1).toInstant
      counters.android.visits shouldBe 0
      counters.android.firstSeen.getMillis shouldBe 0L
      counters.android.lastSeen.getMillis shouldBe 0L
      counters.ios.visits shouldBe 5
      counters.ios.firstSeen shouldBe moment.minusDays(1).toInstant
      counters.ios.lastSeen shouldBe moment.minusDays(1).plusHours(1).toInstant
    }

    "add counters to user with existing counters entry for the same platform" in {
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.minusDays(1).plusHours(1).getMillis)
      val userId = ModelGenerators.userId.next
      dao.touch(userId, Platforms.Ios).futureValue
      dao
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
    }

    "add counters to user with existing counters entry" in {
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.getMillis)
      val userId = ModelGenerators.userId.next
      dao.touch(userId, Platforms.Android).futureValue
      dao
        .addWasSeenCounters(
          userId,
          UserWasSeenCounters(
            ios = UserWasSeenCounter(5, moment.minusDays(1).toInstant, moment.minusDays(1).plusHours(1).toInstant)
          )
        )
        .futureValue
      // второй добавление должно оставить все без изменений
      dao
        .addWasSeenCounters(
          userId,
          UserWasSeenCounters(
            ios = UserWasSeenCounter(5, moment.minusDays(1).toInstant, moment.minusDays(1).plusHours(1).toInstant)
          )
        )
        .futureValue
      val counters = dao.getWasSeenCounters(userId).futureValue.value
      counters.overall.visits shouldBe 6
      counters.overall.firstSeen shouldBe moment.minusDays(1).toInstant
      counters.overall.lastSeen shouldBe moment.toInstant
      counters.android.visits shouldBe 1
      counters.android.firstSeen shouldBe moment.toInstant
      counters.android.lastSeen shouldBe moment.toInstant
      counters.ios.visits shouldBe 5
      counters.ios.firstSeen shouldBe moment.minusDays(1).toInstant
      counters.ios.lastSeen shouldBe moment.minusDays(1).plusHours(1).toInstant
    }
  }

  "UserWasSeenDao.touch" should {
    "update last seen ts" in {
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.getMillis)
      val userId = ModelGenerators.userId.next
      dao.touch(userId, Platforms.Android).futureValue
      val lastSeenO = dao.getLastSeen(userId).futureValue
      lastSeenO shouldBe defined
      val Some(lastSeen) = lastSeenO
      lastSeen shouldBe moment
    }

    "update last seen ts twice" in {
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.getMillis)
      val userId = ModelGenerators.userId.next
      dao.touch(userId, Platforms.Android).futureValue
      DateTimeUtils.setCurrentMillisFixed(moment.plusSeconds(1).getMillis)
      dao.touch(userId, Platforms.Android).futureValue
      dao.getLastSeen(userId).futureValue.value shouldBe moment.plusSeconds(1)
    }

    "perform concurrent updates" in {
      val start = DateTime.now().plusSeconds(2).getMillis
      val userId = ModelGenerators.userId.next
      def thread(platform: Platform): Thread = {
        val t = new Thread(() => {
          if (DateTime.now().getMillis - start > 0) Thread.sleep(DateTime.now().getMillis - start)
          var count = 5
          while (count > 0) {
            dao.touch(userId, platform).futureValue
            count -= 1
            Thread.sleep(Random.nextInt(10) + 1)
          }
        })
        t.start()
        t
      }
      val threads = (1 to 12).map {
        case 1 => thread(Platforms.Android)
        case 2 => thread(Platforms.Android)
        case 3 => thread(Platforms.Ios)
        case 4 => thread(Platforms.Ios)
        case 5 => thread(Platforms.Desktop)
        case 6 => thread(Platforms.Desktop)
        case 7 => thread(Platforms.FrontendMobile)
        case 8 => thread(Platforms.FrontendMobile)
        case 9 => thread(Platforms.Partner)
        case 10 => thread(Platforms.Partner)
        case 11 => thread(Platforms.Unknown)
        case 12 => thread(Platforms.Unknown)
        case i => sys.error(s"unexpected i=$i")
      }
      threads.foreach(_.join())
      val counters = dao.getWasSeenCounters(userId).futureValue.value
      counters.android.visits shouldBe 10
      counters.ios.visits shouldBe 10
      counters.desktop.visits shouldBe 10
      counters.mobile.visits shouldBe 10
      counters.partner.visits shouldBe 10
      counters.unknown.visits shouldBe 10
      counters.overall.visits shouldBe 60
      counters.overall.lastSeen.getMillis should be > counters.overall.firstSeen.getMillis
      counters.overall.lastSeen.getMillis shouldBe counters.countersList.map(_.lastSeen.getMillis).max
      counters.overall.firstSeen.getMillis shouldBe counters.countersList
        .filterNot(_.firstSeen.getMillis == 0)
        .map(_.firstSeen.getMillis)
        .min
    }
  }

  "UserWasSeenDao.getLastActive" should {
    "return None if user has never been seen" in {
      val userId = ModelGenerators.userId.next
      dao.getLastSeen(userId).futureValue shouldBe None
    }
  }

  "UserWasSeenCounters.overall" should {
    "return default values" in {
      UserWasSeenCounters().overall.firstSeen.getMillis shouldBe 0L
      UserWasSeenCounters().overall.lastSeen.getMillis shouldBe 0L
    }
  }
}
