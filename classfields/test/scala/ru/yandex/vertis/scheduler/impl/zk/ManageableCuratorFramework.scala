package ru.yandex.vertis.scheduler.impl.zk

import java.util.concurrent.{CompletableFuture, TimeUnit}

import org.apache.curator.CuratorZookeeperClient
import org.apache.curator.framework.api._
import org.apache.curator.framework.api.transaction.{CuratorMultiTransaction, CuratorTransaction, TransactionOp}
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.listen.Listenable
import org.apache.curator.framework.schema.SchemaSet
import org.apache.curator.framework.state.{ConnectionStateErrorPolicy, ConnectionStateListener}
import org.apache.curator.framework.{CuratorFramework, WatcherRemoveCuratorFramework}
import org.apache.curator.utils.EnsurePath
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.server.quorum.flexible.QuorumVerifier

/**
 * [[CuratorFramework]] with ability of switch client
 *
 * @author alesavin
 */
case class ManageableCuratorFramework(valid: CuratorFramework,
                                      broken: CuratorFramework)
  extends CuratorFramework {

  var brokeGetData = false
  def delegate = if (brokeGetData) broken else valid

  override def checkExists(): ExistsBuilder =
    delegate.checkExists()

  override def getConnectionStateListenable: Listenable[ConnectionStateListener] =
    delegate.getConnectionStateListenable

  override def getUnhandledErrorListenable: Listenable[UnhandledErrorListener] =
    delegate.getUnhandledErrorListenable

  override def sync(s: String, o: scala.Any): Unit =
    delegate.sync(s, o)

  override def sync(): SyncBuilder =
    delegate.sync()

  override def nonNamespaceView(): CuratorFramework =
    delegate.nonNamespaceView()

  override def blockUntilConnected(i: Int, timeUnit: TimeUnit): Boolean = 
    delegate.blockUntilConnected(i, timeUnit)

  override def blockUntilConnected(): Unit = 
    delegate.blockUntilConnected()

  override def getData: GetDataBuilder = {
    if (brokeGetData)
      broken.getData
    else
      delegate.getData
  }

  override def newNamespaceAwareEnsurePath(s: String): EnsurePath =
    delegate.newNamespaceAwareEnsurePath(s)

  override def getNamespace: String =
    delegate.getNamespace

  override def getZookeeperClient: CuratorZookeeperClient =
    delegate.getZookeeperClient

  override def isStarted: Boolean =
    delegate.isStarted

  override def delete(): DeleteBuilder =
    delegate.delete()

  override def setData(): SetDataBuilder =
    delegate.setData()

  override def getState: CuratorFrameworkState =
    delegate.getState

  override def getACL: GetACLBuilder =
    delegate.getACL

  override def close(): Unit =
    delegate.close()

  override def setACL(): SetACLBuilder =
    delegate.setACL()

  override def getCuratorListenable: Listenable[CuratorListener] =
    delegate.getCuratorListenable

  override def getChildren: GetChildrenBuilder =
    delegate.getChildren

  override def clearWatcherReferences(watcher: Watcher): Unit =
    delegate.clearWatcherReferences(watcher)

  override def usingNamespace(s: String): CuratorFramework =
    delegate.usingNamespace(s)

  override def create(): CreateBuilder =
    delegate.create()

  override def start(): Unit =
    delegate.start()

  override def inTransaction(): CuratorTransaction =
    delegate.inTransaction()

  override def reconfig(): ReconfigBuilder = delegate.reconfig()

  override def getConfig: GetConfigBuilder = delegate.getConfig

  override def transaction(): CuratorMultiTransaction = delegate.transaction()

  override def transactionOp(): TransactionOp = delegate.transactionOp()

  override def createContainers(path: String): Unit = delegate.createContainers(path)

  override def watches(): RemoveWatchesBuilder = delegate.watches()

  override def newWatcherRemoveCuratorFramework(): WatcherRemoveCuratorFramework =
    delegate.newWatcherRemoveCuratorFramework()

  override def getConnectionStateErrorPolicy: ConnectionStateErrorPolicy =
    delegate.getConnectionStateErrorPolicy

  override def getCurrentConfig: QuorumVerifier = delegate.getCurrentConfig

  override def getSchemaSet: SchemaSet = delegate.getSchemaSet

  override def isZk34CompatibilityMode: Boolean = delegate.isZk34CompatibilityMode

  override def runSafe(runnable: Runnable): CompletableFuture[Void] = delegate.runSafe(runnable)
}
