package ru.yandex.realty.mocks.yt

import org.joda.time.Duration
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.operations.{Operation, OperationProgress, OperationStatus, YtOperations}
import ru.yandex.inside.yt.kosher.operations.specs.{
  EraseSpec,
  JoinReduceSpec,
  MapReduceSpec,
  MapSpec,
  MergeSpec,
  ReduceSpec,
  RemoteCopySpec,
  SortSpec,
  VanillaSpec
}
import ru.yandex.inside.yt.kosher.ytree.YTreeNode

import java.util
import java.util.Optional

/**
  * @author azakharov
  */
object MockYtOperations extends YtOperations {
  override def status(operationId: GUID): OperationStatus = ???

  override def progress(operationId: GUID): OperationProgress = ???

  override def result(operationId: GUID): YTreeNode = ???

  override def merge(transactionId: Optional[GUID], pingAncestorTransactions: Boolean, spec: MergeSpec): GUID =
    transactionId.get()

  override def erase(transactionId: Optional[GUID], pingAncestorTransactions: Boolean, spec: EraseSpec): GUID = ???

  override def map(transactionId: Optional[GUID], pingAncestorTransactions: Boolean, spec: MapSpec): GUID = ???

  override def reduce(transactionId: Optional[GUID], pingAncestorTransactions: Boolean, spec: ReduceSpec): GUID = ???

  override def joinReduce(
    transactionId: Optional[GUID],
    pingAncestorTransactions: Boolean,
    spec: JoinReduceSpec
  ): GUID = ???

  override def sort(transactionId: Optional[GUID], pingAncestorTransactions: Boolean, spec: SortSpec): GUID = ???

  override def mapReduce(transactionId: Optional[GUID], pingAncestorTransactions: Boolean, spec: MapReduceSpec): GUID =
    ???

  override def remoteCopy(spec: RemoteCopySpec): GUID = ???

  override def abort(operationId: GUID): Unit = ???

  override def getOperation(operationId: GUID): Operation = MockOperation

  override def getOperationInfo(
    operationId: GUID,
    timeout: Duration,
    attributes: util.List[String]
  ): util.Map[String, YTreeNode] = ???

  override def vanilla(transactionId: Optional[GUID], pingAncestorTransactions: Boolean, spec: VanillaSpec): GUID = ???

  override def listJobs(operationId: GUID, attributes: util.Map[String, YTreeNode]): util.Map[String, YTreeNode] = ???

  override def getJobStderr(operationId: GUID, jobId: GUID): String = ???
}
