package vertis.yt.util.transactions

import common.yt.{NetworkError, YtError}
import org.scalatest.{EitherValues, ParallelTestExecution}
import ru.yandex.inside.yt.kosher.common.GUID
import vertis.yt.util.YtFailures
import vertis.yt.util.matchers.YtMatchers
import vertis.yt.zio.Aliases.YtTask
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.wrappers.YtZioTransactions
import zio.ZIO

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class YtTxWrapperIntSpec
  extends YtZioTest
  with YtMatchers
  with YtFailures
  with ParallelTestExecution
  with EitherValues {

  "tx wrapper" should {
    "fail on a failed commit" in ioTest {
      ytZio.use { yt =>
        val testDir = testBasePath.child("should_not_be_created")
        YtTxWrapper.make(commitFailingTxSupport).use { txWrapper =>
          for {
            res <- (txWrapper
              .withTx("test_tx") {
                logger.info("Working hard")
              } *> yt.cypressNoTx.touchDir(testDir)).either
            _ <- check("tx failed with NetworkError")(res.left.value shouldBe netError)
            _ <- checkM("Directory was not created")(yt.cypressNoTx.exists(testDir).map(_ shouldBe false))
          } yield ()
        }
      }
    }

    "keep original exception in a failed tx with tx abort failure" in ioTest {
      ytZio.use { yt =>
        val testDir = testBasePath.child("should_not_be_created_either")
        val error = new UnsupportedOperationException("Siesta time")
        val expectedError = YtError.toYtError(error)
        YtTxWrapper.make(commitAndAbortFailingTxSupport).use { txWrapper =>
          for {
            res <- (txWrapper
              .withTx("test_tx") {
                ZIO.fail(error)
              } *> yt.cypressNoTx.touchDir(testDir)).either
            _ <- check("tx failed with NetworkError")(res.left.value shouldBe expectedError)
            _ <- checkM("Directory was not created")(yt.cypressNoTx.exists(testDir).map(_ shouldBe false))
          } yield ()
        }
      }
    }
  }

  private val netError = NetworkError(new RuntimeException("wifi failed me, my Lord"))
  private val failNet = ZIO.fail(netError)

  private def notCommitTransaction(txId: GUID): YtTask[Unit] = {
    logger.info(s"Not gonna commit $txId") *> failNet
  }

  private def notAbortTransaction(txId: GUID, title: Option[String], cause: Option[Throwable]): YtTask[Unit] =
    logger.info(s"Not gonna abort $txId") *> failNet

  private lazy val commitFailingTxSupport = new YtZioTransactions(ytClientAsync) {
    override def commitTransaction(txId: GUID): YtTask[Unit] = notCommitTransaction(txId)
  }

  private lazy val commitAndAbortFailingTxSupport = new YtZioTransactions(ytClientAsync) {
    override def commitTransaction(txId: GUID): YtTask[Unit] = notCommitTransaction(txId)

    override def abortTransaction(txId: GUID, title: Option[String], cause: Option[Throwable]): YtTask[Unit] =
      notAbortTransaction(txId, title, cause)
  }
}
