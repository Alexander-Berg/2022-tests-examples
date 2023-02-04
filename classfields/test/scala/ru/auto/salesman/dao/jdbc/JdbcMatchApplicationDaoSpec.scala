package ru.auto.salesman.dao.jdbc

import cats.data.NonEmptyList
import ru.auto.salesman.Task
import ru.auto.salesman.dao.MatchApplicationDaoSpec
import ru.auto.salesman.dao.MatchApplicationDaoSpec.TestMatchApplicationDao
import ru.auto.salesman.dao.impl.jdbc.JdbcMatchApplicationDao
import ru.auto.salesman.dao.impl.jdbc.JdbcMatchApplicationDao.baseSelectQuery
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate
import zio.ZIO
import zio.blocking._

class JdbcMatchApplicationDaoSpec
    extends MatchApplicationDaoSpec
    with SalesmanJdbcSpecTemplate {

  def dao: TestMatchApplicationDao =
    new JdbcMatchApplicationDao(database) with TestMatchApplicationDao {

      def findAll(): Task[List[MatchApplicationCreateRequest]] =
        effectBlocking {
          database.withSession { implicit session =>
            baseSelectQuery.list
          }
        }

      def create(
          records: NonEmptyList[MatchApplicationCreateRequest]
      ): Task[Unit] =
        blocking {
          database.withTransaction { implicit transaction =>
            ZIO.foreach_(records.toList)(createIfNotExists)
          }
        }
    }

}
