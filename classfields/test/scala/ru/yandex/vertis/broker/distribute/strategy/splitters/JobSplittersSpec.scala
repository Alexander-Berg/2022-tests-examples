package ru.yandex.vertis.broker.distribute.strategy.splitters

import ru.yandex.vertis.broker.model.convert.BrokerModelConverters._
import broker.core.inner_api._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.broker.distribute.ProtoJob.{KafkaControllerJob, LbControllerJob, LbSourcedJob, YtControllerJob}
import vertis.core.model.DataCenters

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class JobSplittersSpec extends AnyWordSpec with Matchers {

  val source = StreamLbSource("topic", DataCenters.logbrokerDcs.map(_.toBroker), partitions = Set(0, 1))

  "Splitters" should {
    "split YtControllerJob" in checkPipelineToYtConfig(
      YtControllerJob(
        YtJobConfig(
          source,
          StreamYtTarget("table"),
          broker.core.inner_api.ConsumerConfig("name")
        )
      )
    )

    "split KafkaControllerJob" in checkSplitAndMerge[KafkaControllerJob](
      KafkaControllerJob(
        KafkaJobConfig(source, StreamKafkaTarget("target"), broker.core.inner_api.ConsumerConfig("name"))
      )
    )

    "split and not merge LbConfig" in checkPipelineToLbConfig(
      LbControllerJob(
        LbJobConfig(
          source,
          StreamLbTarget("topic", broker.core.common.LbCompression.RAW, Set(0)),
          broker.core.inner_api.ConsumerConfig("name"),
          broker.core.common.MessageFormat.JSON
        )
      )
    )
  }

  /** Checks that split-merge is an identity operation
    */
  private def checkSplitAndMerge[W <: LbSourcedJob: JobSplitter](input: W) = {
    val splitter = implicitly[JobSplitter[W]]
    val splitted = splitter.subTasks(input.source.dcs.map(_.toScala))(input)
    val merged = splitter.merge(splitted.toSet)
    merged.map(_.source) should be(Set(input.source))
  }

  private def checkPipelineToYtConfig(input: YtControllerJob) = {
    val splitter = YtJobSplitter
    val expected = input.source.partitions.map(p => input.source.copy(partitions = Set(p)))
    val splitted = splitter.subTasks(input.source.dcs.map(_.toScala))(input)
    val merged = splitter.merge(splitted.toSet)
    merged.map(_.source) should be(expected)
  }

  private def checkPipelineToLbConfig(input: LbControllerJob) = {
    val splitter = LbJobSplitter
    val splitted = splitter.subTasks(DataCenters.logbrokerDcs)(input).toSet
    splitted.forall(_.src.source.partitions.size == 1) shouldBe true
    val merged = splitter.merge(splitted)
    merged should be(splitted)
  }
}
