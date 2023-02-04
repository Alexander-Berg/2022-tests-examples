package auto.carfax.common.clients.promocoder.test

import auto.carfax.common.clients.promocoder.PromocoderClient
import auto.carfax.common.clients.promocoder.model._
import auto.carfax.common.clients.promocoder.promocoder._
import auto.carfax.common.utils.http.TestHttpUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.vin.decoder.model.UserRef
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.duration._
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture

import java.util.UUID

@Ignore
class PromocoderClientIntTest extends AsyncFunSuite {

  val remoteService = new RemoteHttpService(
    "promocoder",
    new HttpEndpoint("promocoder-api-http-api.vrts-slb.test.vertis.yandex.net", 80, "http"),
    client = TestHttpUtils.DefaultHttpClient
  )

  implicit val t: Traced = Traced.empty
  val client = new PromocoderClient(remoteService)

  test("release promocode") {
    val response = client
      .releaseDiscount(
        PromocoderUser.apply(UserRef.user(123L)),
        UUID.randomUUID().toString,
        FeatureInstanceRequest(
          FeatureTags.OneReport,
          1.days,
          3,
          FeaturePayload(isPersonal = true, discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 100)))
        )
      )
      .await

    assert(response.size == 1)
  }

}
