package ru.yandex.vertis.moderation.api.v1.service.consumer

import org.apache.kafka.common.TopicPartition
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.api.view.ChangeOffsetsRequest
import ru.yandex.vertis.moderation.kafka.{ConsumerOffsets, ConsumerStates}
import ru.yandex.vertis.moderation.kafka.registry.ConsumerRegistry
import ru.yandex.vertis.moderation.kafka.registry.ConsumerRegistry.{ConsumerInfo, ConsumerPatch}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.HandlerSpecBase

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class ConsumerHandlerSpec extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.OK

  private val service: Service = CoreGenerators.ServiceGen.next
  private val consumerRegistry: ConsumerRegistry = environmentRegistry(service).consumerRegistry

  override def basePath: String = s"/api/1.x/$service/consumer"

  "getConsumerOffsetsMap" should {

    import ru.yandex.vertis.moderation.api.TmsMarshalling.ConsumerOffsetsMapUnmarshaller

    "invoke correct method" in {
      Get(url("/offset")) ~> route ~> check {
        status shouldBe OK
        there.was(one(consumerRegistry).getOffsets)
        responseAs[Map[String, ConsumerOffsets]]
      }
    }
  }

  "getConsumerInfoList" should {

    import ru.yandex.vertis.moderation.api.TmsMarshalling.SeqConsumerInfoUnmarshaller

    "invoke correct method" in {
      Get(url("/info")) ~> route ~> check {
        status shouldBe OK
        there.was(one(consumerRegistry).getConsumerInfoList)
        responseAs[Seq[ConsumerInfo]]
      }
    }
  }

  "updateConsumer" should {

    import ru.yandex.vertis.moderation.api.TmsMarshalling.ConsumerPatchMarshaller
    import ru.yandex.vertis.moderation.api.TmsMarshalling.ConsumerInfoUnmarshaller

    "invoke correct method" in {
      val groupId = "group"
      val patch = ConsumerPatch(state = Some(ConsumerStates.Skip), maxBatchSize = None, timestamp = None)
      Patch(url(s"/$groupId"), patch) ~> route ~> check {
        status shouldBe OK
        there.was(one(consumerRegistry).updateConsumer(groupId, patch, None, None))
        responseAs[ConsumerInfo]
      }
    }
  }

  "updateConsumerWithAuthorAndDescription" should {

    import ru.yandex.vertis.moderation.api.TmsMarshalling.ConsumerPatchMarshaller
    import ru.yandex.vertis.moderation.api.TmsMarshalling.ConsumerInfoUnmarshaller

    "invoke correct method" in {
      val groupId = "group"
      val patch = ConsumerPatch(state = Some(ConsumerStates.Skip), maxBatchSize = None, timestamp = None)
      Patch(url(s"/$groupId/?author=JohnDoe&description=Whatever"), patch) ~> route ~> check {
        status shouldBe OK
        there.was(one(consumerRegistry).updateConsumer(groupId, patch, Some("JohnDoe"), Some("Whatever")))
        responseAs[ConsumerInfo]
      }
    }
  }

  "updateConsumerOffsetsMap" should {

    import ru.yandex.vertis.moderation.api.TmsMarshalling.ConsumerChangeOffsetsRequestMarshaller

    "invoke correct method when request contains offsets" in {
      val updateMap: Map[String, ConsumerOffsets] =
        Map(
          "group1" -> Map(new TopicPartition("t1", 1) -> 5L, new TopicPartition("t1", 2) -> 10L),
          "group2" -> Map(new TopicPartition("t2", 1) -> 15L)
        )
      val changeOffsetsRequest =
        ChangeOffsetsRequest(
          offsets = Some(updateMap),
          timestamps = None
        )
      Patch(url("/offset"), changeOffsetsRequest) ~> route ~> check {
        status shouldBe OK
        there.was(one(consumerRegistry).updateOffsets(updateMap))
      }
    }

    "invoke correct method when request contains timestamps" in {
      val timestamps = Map("group1" -> new DateTime())
      val changeOffsetsRequest =
        ChangeOffsetsRequest(
          offsets = None,
          timestamps = Some(timestamps)
        )
      Patch(url("/offset"), changeOffsetsRequest) ~> route ~> check {
        status shouldBe OK
        there.was(one(consumerRegistry).setOffsetsToTimestamp(timestamps))
      }
    }
  }
}
