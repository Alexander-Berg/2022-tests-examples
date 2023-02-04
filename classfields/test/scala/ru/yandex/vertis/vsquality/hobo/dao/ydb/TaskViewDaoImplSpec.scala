package ru.yandex.vertis.vsquality.hobo.dao.ydb

import cats.effect.{ContextShift, IO}
import com.dimafeng.testcontainers.ForAllTestContainer
import ru.yandex.vertis.vsquality.hobo.dao.TaskViewDao
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.vsquality.utils.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.vsquality.utils.ydb_utils.{DefaultYdbWrapper, WithTransaction}

import scala.concurrent.ExecutionContext.global
import scala.io.{Codec, Source}

class TaskViewDaoImplSpec extends TaskViewDaoSpecBase with ForAllTestContainer {

  type F[X] = IO[X]

  implicit val ydbCs: ContextShift[IO] = IO.contextShift(global)

  override lazy val container: YdbWrapperContainer[IO] = YdbWrapperContainer.stable

  lazy val ydb: DefaultYdbWrapper[IO] = {
    val database = container.ydb
    val stream = getClass.getResourceAsStream("/ydb.sql")
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    database.executeSchema(schema).await
    database
  }

  before {
    ydb.runTx(ydb.execute("DELETE FROM task_views;")).await
  }
  override lazy val taskViewDao: TaskViewDao = new YdbTaskViewDaoImpl[IO, WithTransaction[IO, *]](ydb)
}
