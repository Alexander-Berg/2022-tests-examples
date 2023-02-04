package ru.yandex.vertis.broker.distribute.strategy

import ru.yandex.vertis.broker.distribute.Job
import ru.yandex.vertis.broker.distribute.strategy.splitters.JobSplitter
import ru.yandex.vertis.broker.distribute.test.TestJob
import vertis.core.model.DataCenters
import vertis.zio.test.ZioSpecBase
import vertis.zio.zk.jobs.distribution.model.InstanceNode

class DataLocalDistributionStrategySpec extends ZioSpecBase {

  private val testJobs = (1 to 5).map(i => TestJob(s"job$i")).toSet

  "DataLocalDistributionStrategy" should {
    "not fail on empty jobs" in {
      distribute(
        Set.empty,
        stateForNodes[TestJob](allVlaSasNodes)
      )
    }

    "leave jobs unassigned when passed no nodes" in {
      val result = distribute(
        testJobs,
        Map.empty
      )
      result.notDistributed should contain theSameElementsAs testJobs
    }

    "when passed 1 node, assign all jobs to it" in {
      val result = distribute(
        testJobs,
        stateForNode(vlaNode)
      )
      result.notDistributed shouldBe empty
      result.distribution.size shouldBe 1
      result.distribution.values.head should contain theSameElementsAs testJobs
    }

    "distribute jobs evenly" in {
      val result = distribute(testJobs, stateForNodes(allVlaSasNodes))
      result.notDistributed shouldBe empty
      result.distribution.forall(_._2.size == 1) shouldBe true
    }

    "distribute jobs evenly for an exact capacity" in {
      val result =
        distribute(testJobs.take(4), stateForNodes(vlaSasNodes), maxWeightPerNode = 2.0 * DataCenters.logbrokerDcs.size)
      result.notDistributed shouldBe empty
      result.distribution.forall(_._2.size == 4) shouldBe true
    }

    "not exceed `maxWeightPerNode`" in {
      val result = distribute(
        testJobs.take(3),
        stateForNodes(vlaSasNodes),
        maxWeightPerNode = 1
      )
      result.notDistributed.iterator.map(_.weight).sum shouldBe 1
      result.distribution(vlaNode).iterator.map(_.weight).sum shouldBe 1
      result.distribution(sasNode).iterator.map(_.weight).sum shouldBe 1
    }

    "not redistribute jobs" in {
      val distr0 = distribute(
        testJobs,
        stateForNodes(allVlaSasNodes)
      )
      distr0.notDistributed shouldBe empty

      val distr1 = distribute(
        testJobs,
        distr0.distribution
      )
      distr1 should equal(distr0)
    }

    "throw IllegalArgumentException for an unexpected set of dcs" in {
      intercept[IllegalArgumentException] {
        distribute(testJobs, stateForNodes(ivaNodes ++ vlaSasNodes))
      }
    }

    "reassign all jobs when a datacenter goes offline" in {
      val distr2dc = distribute(testJobs, stateForNodes(vlaSasNodes)).distribution
      distr2dc.size shouldBe 2

      val distr1dc = distribute(testJobs, distr2dc.filter(_._1 == vlaNode)).distribution
      distr1dc.size shouldBe 1
      distr1dc.values.flatten.map(_.weight).sum shouldBe testJobs.toSeq.map(_.weight).sum
    }

    "reassign all jobs when a datacenter goes back online" in {
      val distr1dc = distribute(testJobs, stateForNode(sasNode)).distribution
      distr1dc.size shouldBe 1

      val distr2dc = distribute(testJobs, distr1dc ++ stateForNode(vlaNode)).distribution
      distr2dc.size shouldBe 2

      val (sasJobs, mytJobs) = distr2dc.partition(_._1.dc == DataCenters.Sas)
      mytJobs.values.flatten.map(_.weight).sum shouldBe testJobs.size / 2d
      sasJobs.values.flatten.map(_.weight).sum shouldBe testJobs.size / 2d
    }
  }

  private def distribute[T <: Job](
      allJobs: Set[T],
      currentState: Map[InstanceNode, Set[T]],
      maxWeightPerNode: Double = 100
    )(implicit jobSplitter: JobSplitter[T]): DistributionResult[InstanceNode, T] =
    runSync {
      new DataLocalDistributionStrategy[T](maxWeightPerNode).run(allJobs, currentState)
    }.get
}
