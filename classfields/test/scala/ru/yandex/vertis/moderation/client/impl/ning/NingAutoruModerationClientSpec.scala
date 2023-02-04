package ru.yandex.vertis.moderation.client.impl.ning

import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.client.Generators.ExternalIdGen
import ru.yandex.vertis.moderation.client.{ModerationClientFactory, ModerationClientSpecBase, SpecBase}
import ru.yandex.vertis.moderation.proto.Model.{InstanceSource, Opinion, Service}
import ru.yandex.vertis.moderation.proto.ModelFactory

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Impl of [[ModerationClientSpecBase]] for autoru
  *
  * @author alesavin
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class NingAutoruModerationClientSpec
  extends SpecBase {


  val moderationClientFactory: ModerationClientFactory =
    NingModerationClientFactory(new DefaultAsyncHttpClientConfig.Builder().build())
  val moderationClient = moderationClientFactory.client(
    "moderation-push-api-01-sas.test.vertis.yandex.net" ,
    37158,
    Service.AUTORU
//    Service.USERS_AUTORU
  )


  "opinions" should {
    "correctly work for one unknown external id" in {
      val user = ModelFactory.newUserBuilder().
        setAutoruUser("24886370").
//        setAutoruUser("19084139").
        build()

      val externalId = ModelFactory.newExternalIdBuilder.
          setUser(user).
//          setObjectId("auto_ru_19084139").
          setObjectId("1053049958-840a").
          build
      val opinions = moderationClient.opinions(Seq(externalId)).futureValue
      info(opinions.toList.toString)
      val op = moderationClient.domainOpinion(externalId).futureValue
      info(op.toString)
    }
  }



}
