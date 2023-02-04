package ru.yandex.vertis.broker.distribute.strategy

import broker.core.common.PartitionPeriod
import broker.core.inner_api.{ConsumerConfig, StreamLbSource, StreamYtTarget, YtJobConfig}
import ru.yandex.vertis.broker.distribute.ProtoJob.YtControllerJob
import ru.yandex.vertis.broker.distribute.test.ConditionallySplittableJob
import vertis.core.model.DataCenters._
import vertis.core.model.{DataCenter, DataCenters}
import vertis.zio.test.ZioSpecBase
import ru.yandex.vertis.broker.distribute.strategy.splitters.JobSplitter._
import ru.yandex.vertis.broker.model.convert.BrokerModelConverters._

/** A separate test for some corner cases of the DataLocalDistributionStrategy
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
class ExtraDataLocalDistributionStrategySpec extends ZioSpecBase {

  "DataLocalDistributionStrategy" should {

    "distribute non-splittable jobs" in ioTest {
      for {
        res <- new DataLocalDistributionStrategy[ConditionallySplittableJob](100500)
          .run(Set(ConditionallySplittableJob("one"), ConditionallySplittableJob("two")), stateForNodes(allVlaSasNodes))
        _ <- check("all distributed")(res.notDistributed shouldBe empty)
        _ <- check("distributed 2 jobs")(res.distribution.values.map(_.size).sum shouldBe 2)
      } yield ()
    }

    "distribute splittable jobs alongside with non-splittable" in ioTest {
      for {
        res <- new DataLocalDistributionStrategy[ConditionallySplittableJob](100500)
          .run(
            Set(
              ConditionallySplittableJob("one"),
              ConditionallySplittableJob("two", partitions = Set(0, 1)),
              ConditionallySplittableJob("three")
            ),
            stateForNodes(vlaSasNodes)
          )
        _ <- check("all distributed")(res.notDistributed shouldBe empty)
        _ <- check("distributed 4 jobs")(res.distribution.values.map(_.size).sum shouldBe 4)
        _ <- check("distribution is uniform")(all(res.distribution.values) should have size 2)
      } yield ()
    }

    "distribute splittable jobs which used to be non-splittable" in ioTest {
      val state = Map(
        vlaNode -> Set(ConditionallySplittableJob("one")),
        sasNode -> Set(ConditionallySplittableJob("two"), ConditionallySplittableJob("three"))
      )
      val newJobs = Set(
        ConditionallySplittableJob("one"),
        ConditionallySplittableJob("two", partitions = Set(0, 1)),
        ConditionallySplittableJob("three", partitions = Set(0, 1))
      )
      for {
        res <- new DataLocalDistributionStrategy[ConditionallySplittableJob](100500)
          .run(newJobs, state)
        _ <- check("all distributed")(res.notDistributed shouldBe empty)
        _ <- check("distributed 5 jobs")(res.distribution.values.map(_.size).sum shouldBe 5)
        _ <- check("with a total weight of 3")(res.distribution.values.map(totalWeight).sum shouldBe 3)
      } yield ()
    }

    "split a job within one dc" in ioTest {
      val state = Map(
        vlaNode -> Set.empty[YtControllerJob],
        otherVlaNode -> Set.empty[YtControllerJob]
      )
      val newJobs = Set(lbYtConf(DataCenters.logbrokerDcs, Set(0, 1, 2, 3)))
      for {
        res <- new DataLocalDistributionStrategy[YtControllerJob](100500).run(newJobs, state)
        _ <- check("all distributed")(res.notDistributed shouldBe empty)
        mytRes = res.distribution.getOrElse(vlaNode, Set.empty)
        otherMytRes = res.distribution.getOrElse(otherVlaNode, Set.empty)
        _ <- check("distributed evenly") {
          mytRes.size should be(otherMytRes.size)
        }
        _ <- check("distributed as 4 jobs with 1 partitions") {
          mytRes.headOption.toSeq.flatMap(_.source.partitions).size shouldBe 1
        }
      } yield ()
    }

    "re-split jobs between dc" in ioTest {
      val state = Map(
        vlaNode -> Set(lbYtConf(DataCenters.logbrokerDcs, Set(0, 1))),
        sasNode -> Set.empty[YtControllerJob]
      )
      val newJobs = Set(lbYtConf(DataCenters.logbrokerDcs, Set(0, 1, 2, 3)))
      val expectedState = Map(
        vlaNode -> Set(
          lbYtConf(Set(Vla, Vlx), Set(0)),
          lbYtConf(Set(Vla, Vlx), Set(1)),
          lbYtConf(Set(Vla, Vlx), Set(2)),
          lbYtConf(Set(Vla, Vlx), Set(3))
        ),
        sasNode -> Set(
          lbYtConf(Set(Sas, Iva), Set(0)),
          lbYtConf(Set(Sas, Iva), Set(1)),
          lbYtConf(Set(Sas, Iva), Set(2)),
          lbYtConf(Set(Sas, Iva), Set(3))
        )
      )
      for {
        res <- new DataLocalDistributionStrategy[YtControllerJob](100500).run(newJobs, state)
        _ <- check("all distributed")(res.notDistributed shouldBe empty)
        _ <- check("splitted by dc")(res.distribution should be(expectedState))
      } yield ()
    }

    "be stable" in ioTest {
      val state = Map(
        vlaNode -> Set(lbYtConf(DataCenters.logbrokerDcs, Set(0))),
        otherVlaNode -> Set.empty[YtControllerJob]
      )
      for {
        res <- new DataLocalDistributionStrategy[YtControllerJob](100500)
          .run(state.values.flatten.toSet, state)
        _ <- check("all distributed")(res.notDistributed shouldBe empty)
        _ <- check("distribution is stable")(res.distribution should be(state))
      } yield ()
    }
  }

  private def lbYtConf(datacenters: Set[DataCenter], partitions: Set[Int], id: String = "id"): YtControllerJob =
    YtControllerJob(
      YtJobConfig(
        source = StreamLbSource(id, datacenters.map(_.toBroker), partitions),
        target = StreamYtTarget(id, PartitionPeriod.BY_DAY),
        consumer = ConsumerConfig("consumer"),
        messageType = "MessageType"
      )
    )
}
