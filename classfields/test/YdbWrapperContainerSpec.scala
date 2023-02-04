package ru.yandex.vertis.vsquality.utils.ydb_utils

import java.util.concurrent.{ExecutorService, Executors}

import cats.effect.{ContextShift, IO}
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.vsquality.utils.test_utils.ydb.YdbWrapperContainer

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-07-05
  */
class YdbWrapperContainerSpec extends AnyWordSpec with Matchers with ForAllTestContainer {
  val pool: ExecutorService = Executors.newCachedThreadPool()
  val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(pool)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  override val container: YdbWrapperContainer[IO] = YdbWrapperContainer.stable[IO]

  "YdbWrapperContainer" should {
    "work as easy to use container" in {
      val ydb = container.ydb
      val result =
        ydb
          .runTx {
            ydb.execute("select 1 + 1")
          }
          .unsafeRunSync()
      result.resultSet.getRowCount shouldBe 1
      result.resultSet.next shouldBe true
      result.resultSet.getColumn(0).getInt32 shouldBe 2
    }
  }
}
