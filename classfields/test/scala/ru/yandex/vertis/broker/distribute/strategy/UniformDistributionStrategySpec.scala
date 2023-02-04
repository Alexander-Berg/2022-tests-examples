package ru.yandex.vertis.broker.distribute.strategy

import ru.yandex.vertis.broker.distribute.test.TestJob
import vertis.core.model.DataCenters
import vertis.zio.test.ZioSpecBase
import vertis.zio.zk.jobs.distribution.model.InstanceNode

/**
  */
class UniformDistributionStrategySpec extends ZioSpecBase {

  private val jobs = (1 to 5).map(i => TestJob(i.toString))
  private val nodes = (1 to 5).map(i => node(i.toString))

  "UniformDistributionStrategy" should {
    "not fail on empty jobs" in {
      distribute(
        Set.empty,
        Map(nodes(0) -> Set.empty)
      )
    }

    "not fail on empty nodes" in {
      val work = Set(jobs(0))
      val result = distribute(
        work,
        Map.empty
      )
      result.notDistributed should contain theSameElementsAs work
    }

    "distribute evenly" in {
      val result = distribute(
        Set(jobs(0), jobs(1)),
        Map(nodes(0) -> Set.empty, nodes(1) -> Set.empty)
      )
      result.notDistributed shouldBe empty
      result.distribution.size shouldBe 2
      result.distribution.values.foreach(_.size shouldBe 1)
    }

    "leave some jobs undistributed if no more space" in {
      val result = distribute(
        Set(jobs(0), jobs(1), jobs(2)),
        Map(nodes(0) -> Set.empty, nodes(1) -> Set(jobs(2))),
        1
      )
      result.notDistributed.size shouldBe 1
      result.notDistributed.head should not be jobs(2)
      result.distribution.values.map(_.size) should contain theSameElementsAs Seq(1, 1)
    }

    "not redistribute balanced jobs" in {
      val initial = Map(nodes(0) -> Set(jobs(0), jobs(1)), nodes(1) -> Set(jobs(2)))
      val result = distribute(
        Set(jobs(0), jobs(1), jobs(2)),
        initial
      )
      result.notDistributed shouldBe empty
      result.distribution shouldBe initial
    }

    "redistribute jobs if imbalance exceeds threshold" in {
      val initial = Map(nodes(0) -> Set(jobs(0), jobs(1), jobs(2), jobs(3)), nodes(1) -> Set.empty[TestJob])
      val result = distribute(
        Set(jobs(0), jobs(1), jobs(2), jobs(3)),
        initial
      )
      result.notDistributed shouldBe empty
      result.distribution(nodes(0)) should contain theSameElementsAs (Set(jobs(2), jobs(3)))
      result.distribution(nodes(1)) should contain theSameElementsAs (Set(jobs(0), jobs(1)))
    }

    "redistribute changed job" in {
      val initial = Map(nodes(0) -> Set(jobs(0), jobs(1), jobs(2)), nodes(1) -> Set.empty[TestJob]);
      val changedJob = jobs(1).copy(someConfig = "Changed!!!")
      val result = distribute(
        Set(jobs(0), changedJob, jobs(2)),
        initial
      )
      result.notDistributed shouldBe empty
      result.distribution(nodes(0)) should contain theSameElementsAs (Set(jobs(0), jobs(2)))
      result.distribution(nodes(1)) should contain theSameElementsAs (Set(changedJob))
    }
  }

  private def node(id: String) = InstanceNode(id, DataCenters.Vla)

  private def distribute(
      allJobs: Set[TestJob],
      currentState: Map[InstanceNode, Set[TestJob]],
      maxWeightPerNode: Double = 100): DistributionResult[InstanceNode, TestJob] = {
    runSync {
      new UniformDistributionStrategy(maxWeightPerNode).run(allJobs, currentState)
    }.get
  }
}
