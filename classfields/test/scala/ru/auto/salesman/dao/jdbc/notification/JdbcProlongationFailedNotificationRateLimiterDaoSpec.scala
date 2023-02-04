package ru.auto.salesman.dao.jdbc.notification

import doobie.implicits._
import ru.auto.salesman.dao.impl.jdbc.database.doobie.Transactor._
import ru.auto.salesman.dao.impl.jdbc.database.doobie.Mappings._
import ru.auto.salesman.dao.impl.jdbc.notification.JdbcProlongationFailedNotificationRateLimiterDao
import ru.auto.salesman.model.notification.LastProlongationFailedNotificationSentAt
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.auto.salesman.model.AutoruUser

class JdbcProlongationFailedNotificationRateLimiterDaoSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate {

  val dao =
    new JdbcProlongationFailedNotificationRateLimiterDao(transactor, transactor)

  "JdbcProlongationFailedNotificationRateLimiterDao" should {
    "save and read row" in {
      clearTable()
      val userId = AutoruUser(123)
      dao.save(userId).success.value shouldBe 1
      val a = dao.get(userId).success.value.get.userId
      a shouldBe userId

    }

    "save and update row" in {
      clearTable()
      val firstUser = AutoruUser(123)
      val secondUser = AutoruUser(345)
      val thirdUser = AutoruUser(678)

      dao.save(firstUser).success.value shouldBe 1
      dao.save(secondUser).success.value shouldBe 1
      dao.save(thirdUser).success.value shouldBe 1
      dao.get(secondUser).success.value.get.userId shouldBe secondUser
      dao.get(firstUser).success.value.get.userId shouldBe firstUser
      dao.get(thirdUser).success.value.get.userId shouldBe thirdUser

      dao.save(firstUser).success.value shouldBe 2
      dao.get(firstUser).success.value.get.userId shouldBe firstUser
      getAllRow().size shouldBe 3

      val lastSendTimeFirstUser =
        dao.get(firstUser).success.value.get.lastSentAt.getMillis
      val lastSendTimeThirdUser =
        dao.get(thirdUser).success.value.get.lastSentAt.getMillis

      assert(lastSendTimeFirstUser > lastSendTimeThirdUser)

    }
  }

  private def getAllRow(): List[LastProlongationFailedNotificationSentAt] = {
    val sql = sql"""
        select user_id, last_sent_at
        from prolongation_failed_notification_rate_limiter
       """
    sql
      .query[LastProlongationFailedNotificationSentAt]
      .to[List]
      .transact(transactor)
      .success
      .value
  }

  private def clearTable(): Unit = {
    val deleteSql =
      sql"""
        delete from prolongation_failed_notification_rate_limiter;
      """

    deleteSql.update.run
      .transact(this.transactor)
      .unit
      .success
      .value
  }

}
