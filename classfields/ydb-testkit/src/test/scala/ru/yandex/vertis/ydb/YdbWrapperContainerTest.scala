package ru.yandex.vertis.ydb

import _root_.zio.Runtime
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-07-05
  */
class YdbWrapperContainerTest extends AnyWordSpec with Matchers with ForAllTestContainer {
  override val container: YdbWrapperContainer = YdbWrapperContainer.stable

  val r = Runtime.default

  "YdbWrapperContainer" should {
    "work as easy to use container" in {
      val ydb = container.ydb
      val result = r.unsafeRun(ydb.runTx {
        ydb.execute("select 1 + 1")
      })
      result.resultSet.getRowCount shouldBe 1
      result.resultSet.next shouldBe true
      result.resultSet.getColumn(0).getInt32 shouldBe 2
    }
  }
}
