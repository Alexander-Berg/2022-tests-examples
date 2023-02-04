package ru.yandex.vertis.passport.service.user.social

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import slick.jdbc.MySQLProfile.api._

/**
  *
  * @author zvez
  */
class AutoruSocialLinkDeciderSpec extends WordSpec with SpecBase with MySqlSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val decider = new AutoruSocialLinkDecider(dbs.legacyUsers, dbs.legacyOffice)

  "AutoruSocialLinkDecider" should {
    "allow for common user" in {
      val userId = ModelGenerators.userId.next
      decider.allowLink(userId, allowClient = false, allowStaff = false).futureValue shouldBe true
    }

    "deny for our users" in {
      val userId = ModelGenerators.userId.next
      dbs.legacyUsers
        .run(
          sqlu"""INSERT INTO user (id, new_email, password_date, active, active_code, remind_code, set_date, `delete`, `log_ip`,`is_yandex_employee`) 
            VALUES ($userId, 'u@yandex-team.ru','2022-01-01T00:00:01',1,'act','remind','2022-01-01T00:00:01','2030-01-01','0.0.0.0',1)"""
        )
        .futureValue

      decider.allowLink(userId, false, false).futureValue shouldBe false
    }

    "deny for client users" in {
      val userId = ModelGenerators.userId.next
      dbs.legacyOffice
        .run(
          sqlu"INSERT INTO client_users(user_id, client_id) VALUES ($userId, 123)"
        )
        .futureValue

      decider.allowLink(userId, false, false).futureValue shouldBe false
    }
  }

}
