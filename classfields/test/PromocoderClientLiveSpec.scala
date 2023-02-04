package auto.common.clients.promocoder.test

import auto.common.clients.promocoder.PromocoderClient._
import auto.common.clients.promocoder.model._
import auto.common.clients.promocoder.model.features._
import auto.common.clients.promocoder.model.users._
import auto.common.clients.promocoder.model.PromocoderService
import auto.common.clients.promocoder.{PromocoderClient, PromocoderClientLive, PromocoderJsonProtocol}
import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.Method
import zio.ZLayer
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import eu.timepit.refined.auto._
import io.circe.{Encoder, Json}
import io.circe.syntax._
import org.joda.time.DateTime
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.duration.FiniteDuration

object PromocoderClientLiveSpec extends DefaultRunnableSpec with PromocoderJsonProtocol {

  implicit private val featureInstanceJsonEncoderForTest: Encoder[FeatureInstance] = (obj: FeatureInstance) =>
    Json.obj(
      "id" -> Json.fromString(obj.id),
      "origin" -> obj.origin.asJson,
      "tag" -> Json.fromString(obj.tag.value),
      "user" -> Json.fromString(obj.user.value),
      "count" -> Json.fromLong(obj.count.count),
      "createTs" -> obj.createTs.asJson,
      "deadline" -> obj.deadline.asJson,
      "jsonPayload" -> obj.payload.asJson
    )

  private val batchId = "dummy"
  private val user = PromocoderUser(userId = 1, userType = UserType.CommonUser)

  private val tag: FeatureTag = "tag"
  private val count: PositiveCount = 1L

  private val payload = FeaturePayload.validateAndCreate(FeatureUnit.Money, FeatureType.Promocode, None, None) match {
    case Right(result) => result
    case Left(err) => throw err
  }

  private val dummyFeatureRequest = FeatureInstanceRequest(
    tag = tag,
    startTime = None,
    lifetime = FiniteDuration.apply(1, "d"),
    count = count,
    jsonPayload = payload
  )

  private val dummyFeatureInstance = FeatureInstance(
    id = "some_id",
    origin = FeatureOrigin("origin"),
    tag = tag,
    user = user.userId,
    count = FeatureCount.Money(count),
    createTs = DateTime.now().withZone(DateTimeUtil.DefaultTimeZone),
    deadline = DateTime.now().withZone(DateTimeUtil.DefaultTimeZone).plusDays(1),
    payload = payload
  )

  private val CreateFeaturesPath = s"api/1.x/service/autoru/feature/batch/$batchId/user/$user".split('/').toList

  private val DecrementFeaturePath =
    s"api/1.x/service/autoru/feature/${dummyFeatureInstance.id.value}/200".split('/').toList

  private val responseStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case r if r.uri.path.equals(CreateFeaturesPath) && r.method == Method.POST =>
      Response.ok(List(dummyFeatureInstance).asJson.noSpaces)
    case r
        if r.uri.path.equals(DecrementFeaturePath) && r.uri.params.toMap.equals(
          Map("key" -> "unique_key")
        ) && r.method == Method.PUT =>
      Response.ok(dummyFeatureInstance.asJson.noSpaces)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PromocoderClientLive")(
      testM("successfully create features")(
        for {
          response <- PromocoderClient.createFeatures(PromocoderService.AutoRu, batchId, user, Seq(dummyFeatureRequest))
        } yield assert(response)(equalTo(List(dummyFeatureInstance)))
      ),
      testM("successfully decrements features")(
        for {
          response <- PromocoderClient.decrementFeatureWithKey(
            PromocoderService.AutoRu,
            dummyFeatureInstance.id,
            200,
            "unique_key"
          )
        } yield assert(response)(equalTo(dummyFeatureInstance))
      )
    ).provideCustomLayer(createEnvironment(responseStub))
  }

  def createEnvironment(sttpBackendStub: Sttp.ZioSttpBackendStub): ZLayer[Any, Nothing, PromocoderClient] = {
    (Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpBackendStub)) >>> PromocoderClientLive.Live
  }
}
