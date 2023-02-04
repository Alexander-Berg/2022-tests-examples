package ru.yandex.vertis.moderation.api.v1.service.hobo

import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{any => argAny}
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hobo.proto.Model.{QueueId, TrueFalseResolution}
import ru.yandex.vertis.hobo.proto.{Model => HoboModel, ModelFactory => HoboModelFactory}
import ru.yandex.vertis.moderation.model.ModerationRequest
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.HandlerSpecBase
import ru.yandex.vertis.moderation.view.ViewCompanion.MarshallingContext

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class HoboHandlerSpec extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.OK

  private val service: Service = ServiceGen.next
  private val requestHandler = environmentRegistry(service).requestHandler
  private val instanceDao = environmentRegistry(service).instanceDao

  override def basePath: String = s"/api/1.x/$service/hobo"

  "bindTask" should {

    "invoke correct methods" in {
      val task = HoboSignalTaskGen.next
      val hoboTask =
        HoboModelFactory.newTaskBuilder
          .setKey(task.key)
          .setQueue(QueueId.valueOf(task.queue))
          .setState(HoboModel.Task.State.COMPLETED)
          .setResolution(
            HoboModel.Resolution
              .newBuilder()
              .setVersion(1)
              .setTrueFalse(TrueFalseResolution.newBuilder().setVersion(1).build())
              .build()
          )
          .build
      val instance =
        InstanceGen.next.copy(
          signals = SignalSet(HoboSignalGen.withoutSwitchOff.withoutMarker.next.copy(task = Some(task)))
        )
      updateInstanceDao(instance, instanceDao)()
      Post(url(s"/task/${instance.id}"), hoboTask) ~> route ~> check {
        status shouldBe OK
        there.was(one(instanceDao).getMaybeExpiredInstance(instance.externalId))
        there.was(two(requestHandler).handle(argAny[ModerationRequest]))
      }
    }
  }

  implicit override def marshallingContext: MarshallingContext = MarshallingContext(service)
}
