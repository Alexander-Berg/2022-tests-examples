package ru.yandex.vertis.passport.dao.impl.mysql

import org.joda.time.DateTime
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.util.mysql.DualDatabase
import ru.yandex.vertis.passport.util.DateTimeUtils.DateTimeOrdering

import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  * @author zvez
  */
class AutoruUsersQuotaDaoSpec extends WordSpec with SpecBase with MySqlSupport {

  private val service: AutoruUsersQuotaDao = new AutoruUsersQuotaDao(DualDatabase(dbs.legacyUsers))

  private val CarsModerationDomain: String = "CARS"
  private val SpecialModerationDomain: String = "SPECIAL"

  "AutoruUsersQuotaService.removeQuota" should {
    "take away user's quota" in {
      val userId = ModelGenerators.userId.next
      service.removeQuota(userId, CarsModerationDomain).futureValue
      val actual = service.userNoQuotaModerationDomains(userId).futureValue.keySet
      actual.contains(CarsModerationDomain) shouldBe true
    }

    "allow remove quota multiple" in {
      val userId = ModelGenerators.userId.next
      service.removeQuota(userId, CarsModerationDomain).futureValue
      service.removeQuota(userId, CarsModerationDomain).futureValue
      service.removeQuota(userId, CarsModerationDomain).futureValue
      service.removeQuota(userId, SpecialModerationDomain).futureValue
      val actual = service.userNoQuotaModerationDomains(userId).futureValue.keySet
      val expeted = Set(CarsModerationDomain, SpecialModerationDomain)
      actual shouldBe expeted
    }

    "remove restored quota" in {
      val userId = ModelGenerators.userId.next
      service.removeQuota(userId, CarsModerationDomain).futureValue
      service.restoreQuota(userId, CarsModerationDomain).futureValue
      service.removeQuota(userId, CarsModerationDomain).futureValue
      val actual = service.userNoQuotaModerationDomains(userId).futureValue.keySet
      actual.contains(CarsModerationDomain) shouldBe true
    }
  }

  "AutoruUsersQuotaService.restoreQuota" should {
    "do nothing if quota wasn't removed" in {
      val userId = ModelGenerators.userId.next
      service.userNoQuotaModerationDomains(userId).futureValue.keySet shouldBe Set.empty
      service.restoreQuota(userId, CarsModerationDomain).futureValue
      service.userNoQuotaModerationDomains(userId).futureValue.keySet shouldBe Set.empty
    }

    "restore previously removed quota" in {
      val userId = ModelGenerators.userId.next
      service.removeQuota(userId, CarsModerationDomain).futureValue
      service.restoreQuota(userId, CarsModerationDomain).futureValue
      service.userNoQuotaModerationDomains(userId).futureValue.keySet shouldBe Set.empty
    }
  }

  "AutoruUsersQuotaService.getNewRecordsSince" in {
    val from = DateTime.now().minusSeconds(3)
    val userId = ModelGenerators.userId.next
    service.removeQuota(userId, CarsModerationDomain).futureValue

    val (userIds, marker) = service.getNewRecordsSince(from, 1000).futureValue
    userIds should contain(userId)
    marker should be > from
  }
}
