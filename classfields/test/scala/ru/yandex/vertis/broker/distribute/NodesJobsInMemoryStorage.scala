package ru.yandex.vertis.broker.distribute

import ru.yandex.vertis.broker.distribute.NodesJobsInMemoryStorage.State
import ru.yandex.vertis.broker.distribute.storage.NodesJobsStorage
import vertis.zio.BaseEnv
import vertis.zio.zk.jobs.distribution.model.{Node, NodeId}
import zio.{RIO, Ref, UIO}

/**
  */
class NodesJobsInMemoryStorage[N <: Node, W](stateRef: Ref[State[N, W]]) extends NodesJobsStorage[N, W] {

  override def assignWork(node: N, jobs: Set[W]): RIO[BaseEnv, Unit] = stateRef.update { s =>
    s.copy(jobs = s.jobs + (node.id -> jobs))
  }

  override def freeNode(nodeId: NodeId): RIO[BaseEnv, Unit] = stateRef.update { s =>
    s.copy(jobs = s.jobs - nodeId)
  }

  override def assignedWork: RIO[BaseEnv, Map[NodeId, Set[W]]] = stateRef.get.map(_.jobs)

  override def aliveNodes: RIO[BaseEnv, Set[N]] = stateRef.get.map(_.aliveNodes)

  def provideAliveNodes(xs: Set[N]): UIO[Unit] = stateRef.update { s =>
    s.copy(aliveNodes = xs)
  }
}

object NodesJobsInMemoryStorage {
  case class State[N <: Node, W](aliveNodes: Set[N] = Set.empty, jobs: Map[NodeId, Set[W]] = Map.empty)

  def build[N <: Node, W](initialState: State[N, W]): UIO[NodesJobsInMemoryStorage[N, W]] =
    Ref
      .make(initialState)
      .map(new NodesJobsInMemoryStorage[N, W](_) with LoggedNodesJobsStorage[N, W])
}
