package vertis.yt.zio.wrappers

import common.yt.schema.YtTypes
import org.scalatest.ParallelTestExecution
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import vertis.yt.model.attributes.YtAttributes
import vertis.yt.model.{YtColumn, YtSchema, YtTable}
import vertis.yt.util.matchers.YtMatchers
import vertis.yt.util.support.YsonSupport
import vertis.yt.util.transactions.YtTx
import vertis.yt.zio.YtZioTest

/** @author kusaeva
  */
class YtZioCypressIntSpec extends YtZioTest with YtMatchers with YsonSupport with ParallelTestExecution {

  "yt cypress" should {

    "list path with attributes" in ioTest {
      ytResources.use { r =>
        import r.yt
        import yt.cypress

        val optimize = YtAttributes.OptimizeForScan
        val userAttr = "some_user_attribute" -> YTree.stringNode("random_value")

        val attrs: Seq[(String, YTreeNode)] = Seq(optimize, userAttr)

        val tableName = "attribute_table"
        val path = testBasePath.child(tableName)
        val table = YtTable(tableName, path, YtSchema(Seq(YtColumn("payload", YtTypes.string))), None, attrs)

        yt.tx.withTx("test attrs") {
          for {
            txId <- YtTx.txId
            someTxId = Some(txId)
            _ <- cypress
              .createTable(someTxId, table, ignoreExisting = false)
            _ <- cypress
              .list(someTxId, table.path.parent(), attrs.map(_._1))
              .map {
                _.collect {
                  case node if node.name == tableName =>
                    attrs.map { case (k, v) =>
                      node.attributes[YTreeNode](k) shouldBe v
                    }
                }
              }
          } yield ()
        }
      }
    }
  }
}
