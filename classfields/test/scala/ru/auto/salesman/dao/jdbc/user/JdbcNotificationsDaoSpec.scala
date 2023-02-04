package ru.auto.salesman.dao.jdbc.user

import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, Inspectors}
import org.scalatest.concurrent.IntegrationPatience
import ru.auto.salesman.dao.impl.jdbc.user.JdbcNotificationsDao
import ru.auto.salesman.dao.user.{NotificationsDao, UserPush}
import ru.auto.salesman.model.DeprecatedDomain
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.auto.salesman.dao.slick.invariant
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators
import ru.auto.salesman.dao.impl.jdbc._
import ru.auto.salesman.dao.slick.invariant.GetResult
import ru.auto.salesman.environment.TimeZone
import ru.auto.salesman.model.DeprecatedDomains.AutoRu

import scala.slick.jdbc.{PositionedParameters, StaticQuery}
import cats.implicits._
import ru.auto.salesman.dao.user.NotificationsDao.Filter.UserAndFireAtAfter
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

class JdbcNotificationsDaoSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate
    with UserDaoGenerators
    with IntegrationPatience
    with BeforeAndAfter
    with IntegrationPropertyCheckConfig {

  after {
    clean()
  }

  "NotificationsDao" should {
    "insert entries" in {
      forAll(requestsNotificationsDaoGen, requestsNotificationsDaoGen) {
        (initialTableData, requests) =>
          insert(initialTableData)

          requests.traverse(notificationDao.insert).success.value

          Inspectors.forEvery(requests) { req =>
            database.withSession { implicit session =>
              invariant.StaticQuery
                .queryNA[UserPush](
                  s"SELECT user_id, fired_at FROM notifications WHERE user_id = '${req.userId}'"
                )
                .list should contain(UserPush(req.userId, req.firedAt))
            }
          }
      }

    }

    "select entries by filter if all entries in the future" in {
      forAll(requestsNotificationDaoInFutureGen) { reqInFuture =>
        insert(reqInFuture)
        val now = DateTime.now()

        Inspectors.forEvery(reqInFuture.map(_.firedAt)) { time =>
          time.isAfter(now) shouldBe true
        }

        Inspectors.forEvery(reqInFuture) { req =>
          val reqResult =
            notificationDao
              .get(UserAndFireAtAfter(req.userId, now))
              .success
              .value
          reqResult.nonEmpty shouldBe true
          reqResult.foreach { userPush =>
            userPush.user shouldBe req.userId
            userPush.firedAt.isAfter(now) shouldBe true
          }
        }

      }
    }

    "select entries by filter if all entries in the past" in {
      forAll(requestsNotificationsDaoInPastGen) { reqInPast =>
        insert(reqInPast)
        val now = DateTime.now()

        Inspectors.forEvery(reqInPast.map(_.firedAt)) { time =>
          time.isBefore(now) shouldBe true
        }

        Inspectors.forEvery(reqInPast) { req =>
          notificationDao
            .get(UserAndFireAtAfter(req.userId, now))
            .success
            .value
            .size shouldBe 0
        }

      }
    }

  }

  implicit override def domain: DeprecatedDomain = AutoRu

  val notificationDao = new JdbcNotificationsDao(database)

  implicit val userPushResult: GetResult[UserPush] = GetResult[UserPush] { r =>
    val userId = r.<<[String]
    UserPush(userId, new DateTime(r.nextTimestamp(), TimeZone))
  }

  def clean(): Unit =
    database.withSession { implicit session =>
      StaticQuery.queryNA[Int]("delete from products_apply_schedule").execute
    }

  def insert(initialTableData: List[NotificationsDao.Request]): Unit =
    database.withTransaction { implicit session =>
      val stmt = session.conn.prepareStatement(
        "INSERT INTO notifications (user_id, fired_at) VALUES (?, ?)"
      )

      initialTableData.foreach { data =>
        val pp = new PositionedParameters(stmt)
        pp.setString(data.userId)
        pp.setTimestamp(jodaDateTimeAsSqlTimestamp(data.firedAt))
        stmt.addBatch()
      }

      stmt.executeBatch()
    }

}
