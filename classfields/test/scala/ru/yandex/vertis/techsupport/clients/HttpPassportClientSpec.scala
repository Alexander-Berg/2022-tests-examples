package ru.yandex.vertis.vsquality.techsupport.clients

import sttp.client3.{Request, SttpBackend}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.softwaremill.tagging._
import org.scalatest.Ignore
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import ru.yandex.passport.model.api.api_model.UserResult
import ru.yandex.passport.model.common.common_model.UserModerationStatus
import ru.yandex.vertis.vsquality.utils.cats_utils.MonadErr
import ru.yandex.vertis.vsquality.techsupport.clients.impl.HttpPassportClient
import ru.yandex.vertis.vsquality.techsupport.model.{Tags, Url, UserId}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.http_client_utils.HttpClientUtils.DelegateSttpBackend
import ru.yandex.vertis.vsquality.utils.cached_utils.config.MethodCachingConfig.Enabled
import ru.yandex.vertis.vsquality.utils.cached_utils.model._
import ru.yandex.vertis.vsquality.utils.cached_utils.{CacheF, Layouts}
import ru.yandex.vertis.vsquality.utils.cached_utils.config.{CacheConfig, MethodCachingConfig}
import ru.yandex.vertis.vsquality.utils.cached_utils.factory.CacheFactory
import ru.yandex.vertis.vsquality.utils.cached_utils.model.MethodNameTag
import ru.yandex.vertis.vsquality.utils.cached_utils.model.Tags.MethodName
import ru.yandex.vertis.vsquality.utils.http_client_utils.cached.CachedSttpBackend
import sttp.capabilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author devreggs
  */
@Ignore
class HttpPassportClientSpec extends SpecBase {

  private val basePassportUrl: Url =
    "http://passport-api.vrts-slb.test.vertis.yandex.net/api/2.x".taggedWith[Tags.Url]
  private val backend: SttpBackend[F, _] = AsyncHttpClientCatsBackend[F]().await

  private val stubBackend = mock[SttpBackend[F, _]]
  stub(stubBackend.send(_: Request[_, Any])) { case r: Request[_, Any] => backend.send(r) }

  private val cache: BackendCache[F] =
    new CacheF[F, HttpBytesRequest, SerializableHttpBytesResponse](
      CacheFactory(CacheConfig.InMemory, Layouts.SttpBackendInfoLayout)
    )

  implicit private val cachedBackend: SttpBackend[F, _] = new DelegateSttpBackend(stubBackend)
    with CachedSttpBackend[F, Any] {
    implicit override def F: MonadErr[F] = implicitly
    override protected def backendCache: BackendCache[F] = cache

    override protected def methodCachingConfigs: Map[MethodNameTag, MethodCachingConfig] = Map(
      "get-user".taggedWith[MethodName] -> Enabled(1.minutes),
      "get-moderation".taggedWith[MethodName] -> Enabled(1.minutes)
    )
  }

  private val client: PassportClient[F] = new HttpPassportClient(basePassportUrl)
  private val user = UserId.Client.Autoru.PrivatePerson(42911777L.taggedWith[Tags.AutoruPrivatePersonId])

  "PassportClient.getUser" should {
    "get user info with cache" in {
      client.user(user).await shouldBe a[UserResult]
      verify(stubBackend).send(?)
      client.user(user).await shouldBe a[UserResult]
      verifyNoMoreInteractions(stubBackend)
    }
  }

  "PassportClient.getUser" should {
    "get moderation info with cache" in {
      client.moderation(user).await shouldBe a[UserModerationStatus]
    }
  }
}
