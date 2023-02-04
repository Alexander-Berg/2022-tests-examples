package common.db.migrations.test

import com.dimafeng.testcontainers.PostgreSQLContainer
import common.db.migrations.liquibase.{LiquibaseConfig, LiquibaseMigrator}
import common.zio.app.Application
import common.zio.doobie.testkit.TestPostgresql
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._
import zio.{Has, URLayer, ZIO, ZLayer}

import java.sql.DriverManager

object LiquibaseMigratorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[environment.TestEnvironment, Any] = {
    suite("LiquibaseScriptRunnerTest")(
      testM("""Container has started""") {
        for {
          container <- ZIO.service[PostgreSQLContainer]
          containerId = container.containerId
        } yield assert(containerId)(not(isNull))
      },
      testM("""Migrations completed""") {
        for {
          migrator <- LiquibaseMigrator(_.migrate())
        } yield assertCompletes
      },
      testM("""Database was updated""") {
        for {
          container <- ZIO.service[PostgreSQLContainer]
          connection = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
          resultSet = connection
            .prepareStatement(
              "select count(1) as cnt from offers"
            )
            .executeQuery()
          _ = resultSet.next()
        } yield assert(resultSet.getLong("cnt"))(equalTo(2L))
      }
    ) @@ sequential @@ shrinks(0)
  }.provideCustomLayerShared {
    val postgres = TestPostgresql.managedContainer
    val config: URLayer[Has[PostgreSQLContainer], Has[LiquibaseConfig]] = ZLayer.fromService(container => {
      new LiquibaseConfig(
        container.driverClassName,
        container.jdbcUrl,
        container.username,
        container.password,
        "public"
      )
    })
    val scriptRunner = Application.live ++ (postgres >>> config) >>> LiquibaseMigrator.live
    postgres ++ scriptRunner.orDie
  }
}
