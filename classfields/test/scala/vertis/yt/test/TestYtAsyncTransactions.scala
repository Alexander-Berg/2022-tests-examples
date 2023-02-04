package vertis.yt.test

import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.transactions.async.{Transaction, YtTransactions}
import ru.yandex.inside.yt.kosher.ytree.YTreeNode

import java.time.{Duration, Instant}
import java.util
import java.util.concurrent.CompletableFuture
import javax.annotation.Nullable

/** @author kusaeva
  */
class TestYtAsyncTransactions extends YtTransactions {

  override def start(
      @Nullable transactionId: GUID,
      pingAncestorTransactions: Boolean,
      timeout: Duration,
      @Nullable deadline: Instant,
      @Nullable prerequisiteTransactions: util.List[GUID],
      attributes: util.Map[String, YTreeNode]): CompletableFuture[GUID] = ???

  override def ping(transactionId: GUID, pingAncestorTransactions: Boolean): CompletableFuture[Void] = ???

  override def commit(
      transactionId: GUID,
      pingAncestorTransactions: Boolean,
      prerequisiteTransactionIds: util.List[GUID]): CompletableFuture[Void] = ???

  override def abort(transactionId: GUID, pingAncestorTransactions: Boolean): CompletableFuture[Void] = ???

  override def getTransaction(transactionId: GUID): CompletableFuture[Transaction] = ???
}
