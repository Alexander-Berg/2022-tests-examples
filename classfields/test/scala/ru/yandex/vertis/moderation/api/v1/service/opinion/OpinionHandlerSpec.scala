package ru.yandex.vertis.moderation.api.v1.service.opinion

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.Opinion
import ru.yandex.vertis.moderation.util.HandlerSpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.view.ViewCompanion
import ru.yandex.vertis.moderation.view.ViewCompanion.MarshallingContext

import scala.concurrent.Future

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class OpinionHandlerSpec extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}

  private val service: Service = ServiceGen.next
  private val apiService = environmentRegistry(service).apiService

  override def basePath: String = s"/api/1.x/$service/opinion"

  "getOpinion" should {

    "invoke correct method" in {
      val instanceId = InstanceIdGen.next
      val opinion = OpinionGen.next
      doReturn(Future.successful(opinion)).when(apiService).opinion(instanceId)

      Get(url(s"/$instanceId")) ~> route ~> check {
        status shouldBe OK
        responseAs[Opinion] shouldBe opinion
      }
    }

    "return 404 if no such instance" in {
      val instanceId = InstanceIdGen.next

      doReturn(Future.failed(new NoSuchElementException)).when(apiService).opinion(instanceId)

      Get(url(s"/$instanceId")) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  implicit override def marshallingContext: ViewCompanion.MarshallingContext = MarshallingContext(service)
}
