package ru.yandex.vertis.broker.distribute

import ru.yandex.vertis.broker.distribute.strategy.DataLocalDistributionStrategy
import ru.yandex.vertis.broker.distribute.test.TestJob
import vertis.core.model.{DataCenter, DataCenters}
import vertis.zio.test.ZioSpecBase
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.zk.jobs.distribution.model.{InstanceNode, NodeId}

/**
  */
class WorkDistributorSpec extends ZioSpecBase {

  private val n = 5
  private val jobs = (1 to n).map(i => TestJob(i.toString))
  private val sasNodes = (1 to n).map(dcNode(DataCenters.Sas))
  private val vlaNodes = (1 to n).map(dcNode(DataCenters.Vla))

  "WorkDistributor2" should {
    "assign jobs" in test() { (distributor, storage) =>
      for {
        _ <- distributor.distribute(Set(jobs(0)))
        work <- storage.assignedWork
        _ <- check {
          work.size shouldBe 1
          work(sasNodes(0).id) should contain theSameElementsAs Set(jobs(0))
        }
      } yield ()
    }

    "remove jobs" in test() { (distributor, storage) =>
      for {
        _ <- distributor.distribute(Set(jobs(0)))
        _ <- distributor.distribute(Set.empty)
        work <- storage.assignedWork
        _ <- check {
          work.size shouldBe 1
          work(sasNodes(0).id) shouldBe empty
        }
      } yield ()
    }

    "reassign job if node is dead" in test() { (distributor, storage) =>
      for {
        _ <- distributor.distribute(Set(jobs(0)))
        _ <- storage.provideAliveNodes(Set(sasNodes(1)))
        _ <- distributor.distribute(Set(jobs(0)))
        work <- storage.assignedWork
        _ <- check {
          work.size shouldBe 1
          work(sasNodes(1).id) should contain theSameElementsAs Set(jobs(0))
        }
      } yield ()
    }

    "not reassign unchanged splitted jobs" in test(sasNodes.toSet ++ vlaNodes.toSet) { (distributor, storage) =>
      for {
        _ <- distributor.distribute(jobs.toSet)
        distributed <- storage.assignedWork
        _ <- check("Distribution is splitted") {
          distributed.values.map(_.view.map(_.weight).sum).toSeq shouldBe Seq.fill(n * 2)(0.5)
        }
        newNode = dcNode(DataCenters.Sas)(n + 1)
        _ <- storage.provideAliveNodes((sasNodes ++ vlaNodes).toSet ++ Set(newNode))
        _ <- distributor.distribute(jobs.toSet)
        redistributed <- storage.assignedWork
        _ <- check("Distribution is stable") {
          val expected: Map[NodeId, Set[TestJob]] = distributed ++ Map(newNode.id -> Set.empty[TestJob])
          redistributed should be(expected)
        }
      } yield ()
    }
  }

  private def test(
      aliveNodes: Set[InstanceNode] = Set(sasNodes(0)),
      jobs: Map[NodeId, Set[TestJob]] = Map.empty
    )(f: (WorkDistributor[TestJob], NodesJobsInMemoryStorage[InstanceNode, TestJob]) => TestBody): Unit = {
    ioTest {
      NodesJobsInMemoryStorage.build(NodesJobsInMemoryStorage.State(aliveNodes, jobs)).flatMap { storage =>
        WorkDistributorImpl
          .create("test", storage, new DataLocalDistributionStrategy[TestJob](10))
          .flatMap { distributor =>
            f(distributor, storage)
          }
      }
    }
  }

  private def dcNode(dc: DataCenter)(i: Int): InstanceNode =
    InstanceNode(s"${i}_$dc", dc)
}
