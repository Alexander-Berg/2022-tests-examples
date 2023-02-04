package ru.yandex.vertis.ydb.skypper

import com.yandex.ydb.core.{StatusCode, UnexpectedResultException}
import com.yandex.ydb.table.transaction.TxControl
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced

@RunWith(classOf[JUnitRunner])
class TxInOtherSessionTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  test("put transaction to request from other session") {
    val txControl = TxControl.serializableRw().setCommitTx(false).asInstanceOf[TxControlType]
    ydb.rawExecute("1") { session =>
      val result =
        session.executeDataQuery("select title from series where series_id = 1", txControl).join().expect("error")
      val txId = result.getTxId
      val txControl2 = TxControl.id(txId).setCommitTx(false).asInstanceOf[TxControlType]
      ydb.rawExecute("2") { session2 =>
        session should not be session2
        val ex = intercept[UnexpectedResultException] {
          session2.executeDataQuery("select title from test2 where series_id = 1", txControl2).join().expect("error")
        }
        ex.getStatusCode shouldBe StatusCode.NOT_FOUND
      }
    }
  }
}
