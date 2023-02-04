package ru.yandex.vertis.moderation.api.v1.service.opinion

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.HandlerSpecBase

/**
  * @author semkagtn
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class OpinionHandlerSpec extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.OK

  private val service: Service = ServiceGen.next
  private val apiService = environmentRegistry(service).pushApiInstanceService

  override def basePath: String = s"/api/1.x/$service/opinion"

  "getOpinions" should {

    "invoke correct method" in {
      val externalId = ExternalIdGen.next
      val externalIds = Seq(externalId)

      Post(url("/"), externalIds) ~> route ~> check {
        status shouldBe OK
        there.was(one(apiService).singleOpinions(externalIds))
      }
    }
  }
}
