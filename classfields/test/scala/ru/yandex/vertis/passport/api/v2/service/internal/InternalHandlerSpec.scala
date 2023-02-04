package ru.yandex.vertis.passport.api.v2.service.internal

import akka.http.scaladsl.model.StatusCodes._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.mockito.Mockito
import org.scalatest.WordSpec
import play.api.libs.json._
import ru.yandex.vertis.moderation.proto.Dsl
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru
import ru.yandex.vertis.moderation.proto.Model.{InstanceOpinion, Opinions}
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.model.RequestContext
import ru.yandex.vertis.passport.service.ban.AutoruModerationBinding
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class InternalHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with PlayJsonSupport
  with V2Spec
  with NoTvmAuthorization {

  val base = "/api/2.x/auto/internal"

  val instanceOpinionGen = for {
    userId <- ModelGenerators.userId
  } yield {
    val opinions = Opinions
      .newBuilder()
      .addEntries(
        Opinions.Entry.newBuilder().setDomain(Dsl.Domain.usersAutoRu(UsersAutoru.BUS)).setVersion(1)
      )
      .setVersion(1)
    InstanceOpinion
      .newBuilder()
      .setExternalId(AutoruModerationBinding.userExternalId(userId))
      .setOpinions(opinions)
      .setVersion(1)
      .build()
  }

  "dynamic properties" should {
    "get" in {
      when(dynamicPropertiesService.getRaw("foo")).thenReturn(JsBoolean(true))
      Get(s"$base/dynamic-properties/foo") ~>
        route ~>
        check {
          status shouldBe OK
          responseAs[JsValue] shouldBe JsBoolean(true)
        }
    }
    "change" in {
      Mockito.doNothing().when(dynamicPropertiesService).setRaw("foo", JsBoolean(true))
      implicit val writes = marshaller(Writes.BooleanWrites)
      Post(s"$base/dynamic-properties/foo", true) ~>
        route ~>
        check {
          status shouldBe OK
        }
    }
  }

}
