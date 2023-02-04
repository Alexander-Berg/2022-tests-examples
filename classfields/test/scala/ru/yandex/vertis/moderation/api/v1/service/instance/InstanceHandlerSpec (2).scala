package ru.yandex.vertis.moderation.api.v1.service.instance

import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{eq => meq}
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.RequestContext
import ru.yandex.vertis.moderation.model.ModerationRequest
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.{DateTimeUtil, HandlerSpecBase}

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class InstanceHandlerSpec extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.OK

  private val service: Service = ServiceGen.next
  private val pushApiService = environmentRegistry(service).pushApiInstanceService

  override def basePath: String = s"/api/1.x/$service/instance"

  "pushInstances" should {

    "invoke correct method" in {
      val instanceSource = InstanceSourceGen.next.copy(signals = Set.empty)
      Post(url("/"), Seq(instanceSource)) ~> route ~> check {
        status shouldBe OK
        there.was(one(pushApiService).upsert(meq(Seq(instanceSource)))(any[RequestContext]))
      }
    }
  }

  "getInstancesByExternalId" should {

    "invoke correct method" in {
      val externalId = ExternalIdGen.next
      Post(url("/current"), Seq(externalId)) ~> route ~> check {
        status shouldBe OK
        there.was(one(pushApiService).getAllCurrent(Set(externalId)))
      }
    }
  }

  "changeContext" should {

    "invoke correct method" in {
      val instance = InstanceGen.next

      val externalId = instance.externalId
      val contextSource = ContextSourceGen.next
      val request = ModerationRequest.ChangeContext.withInitialDepth(externalId, contextSource, DateTimeUtil.now())
      Put(url("/context"), request) ~> route ~> check {
        status shouldBe OK
        there.was(one(pushApiService).updateContext(meq(externalId), meq(contextSource))(any[RequestContext]))
      }
    }

    "return 200 if no such instance" in {
      val externalId = ExternalIdGen.next
      val contextSource = ContextSourceGen.next
      val request = ModerationRequest.ChangeContext.withInitialDepth(externalId, contextSource, DateTimeUtil.now())
      Put(url("/context"), request) ~> route ~> check {
        status shouldBe OK
      }
    }
  }
}
