package vertis.yt.util.transactions

import org.scalatest.{ParallelTestExecution, TryValues}
import ru.yandex.inside.yt.kosher.impl.common.YtErrorMapping
import vertis.yt.util.YtFailures
import vertis.yt.util.matchers.YtMatchers
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.wrappers.YtZioTransactions
import zio.duration.Duration.fromScala
import zio.{Task, ZIO}

import scala.concurrent.duration._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class YtTxManagerIntSpec extends YtZioTest with YtMatchers with YtFailures with TryValues with ParallelTestExecution {

  private lazy val txSupport = new YtZioTransactions(ytClientAsync)

  private def createTxManager(
      txTimeout: FiniteDuration,
      keepAlivePeriod: FiniteDuration,
      txSupport: YtZioTransactions = txSupport) = {
    val manager = new YtTxManager(txSupport, txTimeout, keepAlivePeriod)
    runSync(manager.init)
    manager
  }

  "tx manager" should {

    "keep tx alive" in {
      val txManager = createTxManager(5.second, 1.seconds)
      val txId = runSync(
        {
          txManager
            .startTransaction("my_transaction")
        } <* ZIO.sleep(fromScala(10.seconds))
      ).get

      txId should not be closed
      runSync(txManager.close)
    }

    "abort transaction on close" in {
      val txManager = createTxManager(10.minutes, 10.seconds)
      val tx = runSync(txManager.startTransaction("tx"))
      tx should not be closed
      runSync(txManager.close)
      tx.get shouldBe closed
    }
  }

  "not accept new requests after being closed" in {
    val txManager = createTxManager(10.minutes, 10.seconds)
    runSync(txManager.close)
    intercept[IllegalStateException] {
      val startRes = runSync(txManager.startTransaction("wait for me!"))
      startRes.get
    }
  }

  // transaction created in parallel with TxManager being closed should not survive
  // it's either rejected or aborted, but not left for txTimeout (up to an hour) to rot
  s"close transaction if racing" in {
    val txManager = createTxManager(1.hour, 10.seconds)
    val txTitle = s"gonna catch that last train"
    val closeAndStart = txManager
      .startTransaction(txTitle)
      .zipParLeft(txManager.close)
    val tx = runSync(closeAndStart)
    withClue("txManager closed")(txManager.isClosed shouldBe true)
    tx.get shouldBe closed
  }

  "stop keeping tx alive if abort has failed" in ioTest {
    val failingClient = failingOnAbort(ytClientAsync)(new YtErrorMapping.UnknownError("Ooops"))
    val txManager = new YtTxManager(new YtZioTransactions(failingClient), 3.seconds, 500.millis)
    for {
      txId <- txManager.startTransaction("my_transaction")
      _ <- Task(txId should not be closed)
      abortionFailure <- txManager.abortTransaction(txId).either.map(_.swap.toOption)
      _ = abortionFailure should not be empty
      _ <- ZIO.sleep(fromScala(4.seconds))
      _ <- Task(txId shouldBe closed)
    } yield ()
  }
}

/*
    "abort children" in {
      val txManager = new YtTxManager(txSupport, 1.hour, 10.seconds)
      val parent = txManager.startTransaction("parent").unsafeRunSync()
      val child = txManager.startTransaction("child", Some(parent)).unsafeRunSync()
      val transactions = Seq(parent, child)
      every(transactions) should not be closed
      txManager.abortTransaction(parent).unsafeRunSync()
      every(transactions) shouldBe closed
    }

    "not abort parent" in {
      val txManager = new YtTxManager(txSupport, 1.hour, 10.seconds)
      val parent = txManager.startTransaction("parent").unsafeRunSync()
      val child = txManager.startTransaction("child", Some(parent)).unsafeRunSync()
      val transactions = Seq(parent, child)
      every(transactions) should not be closed
      txManager.abortTransaction(child).unsafeRunSync()
      child shouldBe closed
      parent should not be closed
    }*/
