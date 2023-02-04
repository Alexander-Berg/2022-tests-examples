package ru.yandex.vertis.broker.distribute

import vertis.core.model.{DataCenter, DataCenters}
import vertis.zio.zk.jobs.distribution.model.InstanceNode

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
package object strategy {

  private[strategy] val sasNodes = dcNodes(DataCenters.Sas)
  private[strategy] val vlaNodes = dcNodes(DataCenters.Vla)
  private[strategy] val ivaNodes = dcNodes(DataCenters.Iva)

  private[strategy] val allVlaSasNodes = vlaNodes ++ sasNodes

  private[strategy] val vlaNode = vlaNodes.head
  private[strategy] val otherVlaNode = vlaNodes.drop(1).head
  private[strategy] val sasNode = sasNodes.head
  private[strategy] val vlaSasNodes = Seq(vlaNode, sasNode)

  private[strategy] val allDcNodes =
    DataCenters.logbrokerDcs.iterator.map(dcNode(_)(1)).toSet

  private[strategy] def stateForNode[T <: Job](node: InstanceNode): Map[InstanceNode, Set[T]] =
    stateForNodes[T](Seq(node))

  private[strategy] def stateForNodes[T](nodes: Iterable[InstanceNode]): Map[InstanceNode, Set[T]] =
    nodes.map(_ -> Set.empty[T]).toMap

  def totalWeight(jobs: Iterable[Job]): Double =
    jobs.iterator.map(_.weight).sum

  private def dcNode(dc: DataCenter)(i: Int) = InstanceNode(s"${dc.toString.toLowerCase}$i", dc)
  private def dcNodes(dc: DataCenter, n: Int = 5): Set[InstanceNode] = (1 to n).map(dcNode(dc)).toSet
}
