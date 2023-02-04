package ru.yandex.vertis.moderation.client.impl.ning

import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.client.{ModerationClientFactory, ModerationClientSpecBase}
import ru.yandex.vertis.moderation.proto.Model.{InstanceSource, Service}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Impl of [[ModerationClientSpecBase]] for realty
  *
  * @author semkagtn
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class NingRealtyModerationClientSpec
  extends ModerationClientSpecBase {

  override def service: Service = Service.REALTY

  override def instanceFilter: InstanceSource => Boolean = instance =>
    instance.getEssentials.hasRealty &&
      (instance.getExternalId.getUser.hasPartnerUser ||
        instance.getExternalId.getUser.hasYandexUser)

  override def moderationClientFactory: ModerationClientFactory =
    NingModerationClientFactory(new DefaultAsyncHttpClientConfig.Builder().build())
}
