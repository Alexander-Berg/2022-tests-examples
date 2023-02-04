package vertis.yt.util

import java.util.concurrent.CompletableFuture
import ru.yandex.inside.yt.kosher.async.{Yt => YtAsync}
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.transactions.async.{YtTransactions => YtAsyncTransactions}
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import vertis.yt.test.{TestYtAsync, TestYtAsyncTransactions}

import java.time.{Duration, Instant}
import java.util
import javax.annotation.Nullable

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait YtFailures {

  def failingOnAbort(ytClient: YtAsync)(e: Throwable): YtAsync = {
    val failedFuture: CompletableFuture[Void] = CompletableFuture.failedFuture(e)

    val tx = new TestYtAsyncTransactions {

      override def start(
          @Nullable transactionId: GUID,
          pingAncestorTransactions: Boolean,
          timeout: Duration,
          @Nullable deadline: Instant,
          @Nullable prerequisiteTransactions: util.List[GUID],
          attributes: util.Map[String, YTreeNode]): CompletableFuture[GUID] =
        ytClient
          .transactions()
          .start(transactionId, pingAncestorTransactions, timeout, deadline, prerequisiteTransactions, attributes)

      override def abort(transactionId: GUID, pingAncestorTransactions: Boolean): CompletableFuture[Void] =
        failedFuture
    }

    new TestYtAsync {
      override def transactions(): YtAsyncTransactions = tx
    }
  }
}
