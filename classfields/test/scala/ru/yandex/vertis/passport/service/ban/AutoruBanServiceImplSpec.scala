package ru.yandex.vertis.passport.service.ban

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.passport.dao.impl.mysql.AutoruBlacklistDao
import ru.yandex.vertis.passport.dao.impl.mysql.AutoruBlacklistDao.BlacklistRecord
import ru.yandex.vertis.passport.service.ban.BanService.UserBan
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport}
import ru.yandex.vertis.passport.util.mysql.DualDatabase
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.util.DateTimeUtils.toJodaDuration

/**
  * @author zvez
  */
class AutoruBanServiceImplSpec extends BanServiceSpec with MySqlSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val blacklistDao = new AutoruBlacklistDao(DualDatabase(dbs.legacyUsers))

  override val service = new AutoruBanServiceImpl(blacklistDao)

  val autoruAllGen = Gen.oneOf(
    "all/all",
    "all/cars",
    "all/moto",
    "all/special",
    "all/trucks",
    "all7",
    "all"
  )

  val autoruUserGen = Gen.oneOf(
    "users",
    "users8"
  )

  val domainGen = Gen.oneOf(AutoruBanServiceImpl.DomainUsers, AutoruBanServiceImpl.DomainAll)

  "AutoruBanServiceImpl" should {

    "map autoru 'all' server do domain" in {
      val userId = ModelGenerators.userId.next
      val server = autoruAllGen.next
      blacklistDao.insert(blacklistRecord(userId, server)).futureValue

      service.getUserBans(userId).futureValue shouldBe Seq(UserBan(AutoruBanServiceImpl.DomainAll))
    }

    "map autoru 'users' server do domain" in {
      val userId = ModelGenerators.userId.next
      val server = autoruUserGen.next
      blacklistDao.insert(blacklistRecord(userId, server)).futureValue

      service.getUserBans(userId).futureValue shouldBe Seq(UserBan(AutoruBanServiceImpl.DomainUsers))
    }

    "ban user" in {
      val user = ModelGenerators.legacyUser.next
      val domain = domainGen.next

      service.getUserBans(user.id).futureValue shouldBe empty

      service.banUser(user, domain, "test").futureValue
      service.getUserBans(user.id).futureValue shouldBe Seq(UserBan(domain))
    }

    "ban phones with different ttl" in {
      val user = ModelGenerators.legacyUser.filter(_.phones.nonEmpty).next
      val domain = domainGen.next
      service.banUser(user, domain, "test").futureValue

      val banRecords = blacklistDao.listForUser(user.id).futureValue
      banRecords.size shouldBe user.phones.size + 1

      val recordsByPhone = banRecords.map(r => r.phone.getOrElse("") -> r).toMap

      recordsByPhone.contains("") shouldBe true

      recordsByPhone("").expireDate.toDateTimeAtStartOfDay
        .minus(AutoruBanServiceImpl.BanUserTtl)
        .isBeforeNow shouldBe true

      user.phones.foreach { phone =>
        recordsByPhone.contains(phone.phone) shouldBe true
        recordsByPhone(phone.phone).expireDate.toDateTimeAtStartOfDay
          .minus(AutoruBanServiceImpl.BanPhoneTtl)
          .isBeforeNow
      }
    }

    "checkPhoneBanned" in {
      val user = ModelGenerators.legacyUser.filter(_.phones.nonEmpty).next
      val domain = domainGen.next
      service.banUser(user, domain, "").futureValue

      user.phones.foreach { phone =>
        service.checkPhoneBanned(phone.phone).futureValue shouldBe true
      }
    }

  }

  private def blacklistRecord(userId: String, server: String) =
    BlacklistRecord(
      server = server,
      userId = Some(userId),
      email = "",
      phone = None,
      setDate = DateTime.now(),
      expireDate = DateTime.now().plusDays(1).toLocalDate,
      command = "",
      reasonId = None,
      reasonComment = ""
    )

}
