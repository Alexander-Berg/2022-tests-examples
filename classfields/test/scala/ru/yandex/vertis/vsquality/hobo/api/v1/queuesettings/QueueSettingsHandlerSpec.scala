package ru.yandex.vertis.vsquality.hobo.api.v1.queuesettings

import akka.http.scaladsl.model.StatusCodes._

import ru.yandex.vertis.vsquality.hobo.QueueSettingsUpdateRequest
import ru.yandex.vertis.vsquality.hobo.model.QueueSettings
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.vsquality.hobo.service.OperatorContext
import ru.yandex.vertis.vsquality.hobo.util.{HandlerSpecBase, Use}
import ru.yandex.vertis.vsquality.hobo.view.DomainMarshalling._

class QueueSettingsHandlerSpec extends HandlerSpecBase {

  private val queueId = QueueIdGen.next

  private val queueSettingsService = backend.queueSettingsService

  override def basePath: String = "/api/1.x/queue-settings"

  private val updateRequest = QueueSettingsUpdateRequest(defaultCost = Use(10))

  "getQueueSettings" should {

    "invoke correct method" in {

      implicit val oc: OperatorContext = OperatorContextGen.next

      Get(url(s"/$queueId")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[QueueSettings]
        there.was(one(queueSettingsService).get(queueId)(oc))
      }
    }
    "return 404 for nonexistent queue" in {

      implicit val oc: OperatorContext = OperatorContextGen.next

      Get(url(s"/nonexistent"), updateRequest) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "getAllQueueSettings" should {

    "invoke correct method" in {

      implicit val oc: OperatorContext = OperatorContextGen.next

      Get(url("/")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[Map[QueueId, QueueSettings]]
        there.was(one(queueSettingsService).getAll(oc))
      }
    }

  }

  "updateQueueSettings" should {

    "invoke correct method" in {

      implicit val oc: OperatorContext = OperatorContextGen.next

      Patch(url(s"/$queueId"), updateRequest) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[QueueSettings]
        there.was(one(queueSettingsService).update(queueId, updateRequest)(oc))
      }
    }
    "return 404 for nonexistent queue" in {

      implicit val oc: OperatorContext = OperatorContextGen.next

      Patch(url(s"/nonexistent"), updateRequest) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

  }

}
