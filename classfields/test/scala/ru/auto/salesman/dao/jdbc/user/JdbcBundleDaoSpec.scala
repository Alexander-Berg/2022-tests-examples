package ru.auto.salesman.dao.jdbc.user

import java.sql.Types

import ru.auto.salesman.dao.impl.jdbc._
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.dao.impl.jdbc.user.JdbcBundleDao
import ru.auto.salesman.dao.jdbc.user.JdbcBundleDaoSpec.DaoForBundleDaoTests
import ru.auto.salesman.dao.user.{BundleDao, BundleDaoSpec}
import ru.auto.salesman.model.DeprecatedDomain
import ru.auto.salesman.model.user.Bundle
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

import scala.slick.jdbc.{PositionedParameters, StaticQuery}
import scala.util.Try

class JdbcBundleDaoSpec extends BundleDaoSpec with SalesmanUserJdbcSpecTemplate {
  private val dao = new DaoForBundleDaoTests(database)

  def newDao(data: Iterable[Bundle]): BundleDao = {
    dao.clean()
    if (data.nonEmpty) dao.insert(data).success
    dao
  }

}

object JdbcBundleDaoSpec {

  class DaoForBundleDaoTests(database: Database)(
      implicit domain: DeprecatedDomain
  ) extends JdbcBundleDao(database) {

    def insert(bundles: Iterable[Bundle]): Try[Unit] = Try {
      database.withTransaction { implicit session =>
        val stmt = session.conn.prepareStatement(
          "insert into bundle" +
          "(bundle_id, offer_id, user_id, product, amount, status, transaction_id, context, activated_at, deadline, epoch)" +
          s"values (${Iterator.continually("?").take(11).mkString(", ")})"
        )

        bundles.foreach { b =>
          val pp = new PositionedParameters(stmt)
          pp.setString(b.id)
          pp.setString(b.offer.toString)
          pp.setString(b.user)
          pp.setString(b.product.name)
          pp.setInt(b.amount.toInt)
          pp.setString(b.status.toString)
          pp.setString(b.transactionId)
          pp.>>(b.context)
          pp.setTimestamp(new java.sql.Timestamp(b.activated.getMillis))
          pp.setTimestamp(new java.sql.Timestamp(b.deadline.getMillis))
          if (Option(b.epoch).nonEmpty)
            pp.setTimestamp(new java.sql.Timestamp(b.epoch))
          else
            pp.setNull(Types.TIMESTAMP)
          stmt.addBatch()
        }
        stmt.executeBatch()
      }

    }

    def clean(): Unit =
      database.withSession { implicit session =>
        StaticQuery.queryNA[Int]("delete from bundle").execute
      }
  }

}
