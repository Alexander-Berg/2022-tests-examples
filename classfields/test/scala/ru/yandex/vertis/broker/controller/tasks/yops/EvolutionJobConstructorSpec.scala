package ru.yandex.vertis.broker.controller.tasks.yops

import broker.controller.inner_api.YopInnerConfig._
import broker.controller.inner_api.{StreamInnerConfig, YopInnerConfig, YtInnerConfig}
import broker.core.common.PartitionPeriod.BY_YEAR
import broker.core.inner_api.EvolutionJobConfig
import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import ru.yandex.vertis.broker.controller.model.BrokerDeliveries
import ru.yandex.vertis.broker.distribute.ProtoJob.EvolutionControllerJob
import zio.test.Assertion._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

/**
 * @author Ratskevich Natalia reimai@yandex-team.ru
 */
object EvolutionJobConstructorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[zio.test.environment.TestEnvironment, Any] =
    suite("EvolutionJobConstructor") {
      testM("create job with all data paths") {
        assertM(
          EvolutionJobConstructor
            .createJobs(BrokerDeliveries("test", Set(stream)))
        )(equalTo(Set(expectedJob)))
      }.provideCustomLayer(Prometheus.live ++ Logging.live)
    }

  val stream: StreamInnerConfig = StreamInnerConfig(
    schemaVersion = "0.0.1",
    name = "test/my_events/raw",
    messageType = "vertis.TestEvent",
    yt = YtInnerConfig(
      enabled = true,
      yop = YopInnerConfig(
        spawn = Some(Spawn(daysToWatch = 2)),
        repartition = Some(Repartition(dstTimestampColumn = "dst_ts", dstStreamName = "test/my_events/events")),
        archive = Some(Archive(enabled = true, delay = 1, targetPartitioning = BY_YEAR)),
        holocron = Some(Holocron(enabled = true)),
        evolution = Some(Evolution(enabled = true))
      )
    )
  )

  val expectedJob: EvolutionControllerJob =
    EvolutionControllerJob(
      EvolutionJobConfig(
        id = "test/my_events/raw",
        messageType = "vertis.TestEvent",
        basePath = "test/my_events/raw",
        dataPaths = Seq(
          "test/my_events/events/1d",
          "test/my_events/raw/1y",
          "test/my_events/eod/1d"
        ),
        isHolocron = true
      )
    )

}
