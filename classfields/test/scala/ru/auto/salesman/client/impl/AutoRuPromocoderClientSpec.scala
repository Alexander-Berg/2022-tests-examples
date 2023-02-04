package ru.auto.salesman.client.impl

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import ru.auto.salesman.client.PromocoderClient.Services.AutoRuUsers
import ru.auto.salesman.client.fallback.FallbackPromocoderClient
import ru.auto.salesman.model.UserTypes._
import ru.auto.salesman.model.{
  AutoruUser,
  FeatureCount,
  FeatureInstance,
  FeatureInstanceRequest,
  FeatureOrigin,
  FeaturePayload,
  FeatureTypes,
  FeatureUnits,
  PromocoderUser
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.environment.now
import ru.auto.salesman.test.docker.PromocoderContainer
import ru.auto.salesman.util.sttp.SttpClientImpl
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.duration._
import spray.json._
import zio.ZIO

class AutoRuPromocoderClientSpec extends BaseSpec {

  import AutoRuJsonProtocol._

  private val featureInstanceJsonWriter: JsonWriter[FeatureInstance] =
    (obj: FeatureInstance) =>
      JsObject(
        "id" -> obj.id.toJson,
        "origin" -> obj.origin.toJson,
        "tag" -> obj.tag.toJson,
        "user" -> obj.user.toJson,
        "count" -> obj.count.count.toJson,
        "payload" -> obj.count.unit.toJson,
        "createTs" -> obj.createTs.toJson,
        "deadline" -> obj.deadline.toJson,
        "jsonPayload" -> obj.payload.toJson
      )

  private val currentTime = now()

  private val user = PromocoderUser(1, ClientUser)
  private val failingUser = PromocoderUser(2, ClientUser)
  private val request = createFeatureRequest(3)
  private val feature = createFeature("1")

  private val failingClient = new AutoRuPromocoderClient(
    "test",
    runServer(
      concat(
        getFeaturesBadResultRoute,
        getFeaturesNotFoundRoute,
        changeFeatureCountBadRequestRoute,
        changeFeatureCountIdempotentBadRequestRoute,
        createFeaturesNotFoundRoute,
        deleteFeaturesInternalServerErrorRoute
      )
    ).toString,
    SttpClientImpl(TestOperationalSupport)
  )

  private val promocoderClient = new AutoRuPromocoderClient(
    AutoRuUsers,
    PromocoderContainer.address,
    SttpClientImpl(TestOperationalSupport)
  )

  "AutoRuPromocoderClient.getFeatures" should {
    "return features" in {
      val test =
        for {
          _ <- ensureNoFeaturesBefore("1", user)
          created <- promocoderClient.createFeatures("1", user, List(request))
          received <- promocoderClient.getFeatures(user)
        } yield {
          val feature = received.loneElement
          feature shouldBe created.loneElement

          feature.tag shouldBe request.tag
          feature.count.count shouldBe request.count
          feature.payload shouldBe request.jsonPayload
        }

      test.success.value
    }

    "return feature by id" in {
      val test =
        for {
          _ <- ensureNoFeaturesBefore("1", user)
          created <- promocoderClient.createFeatures("1", user, List(request))
          received <- promocoderClient
            .getFeature(created.loneElement.id)
            .someOrFailException
        } yield {
          received shouldBe created.loneElement

          received.tag shouldBe request.tag
          received.count.count shouldBe request.count
          received.payload shouldBe request.jsonPayload
        }

      test.success.value
    }

    "ignore bad features" in {
      failingClient
        .getFeatures(user)
        .success
        .value
        .shouldBe(List(feature))
    }

    "fallback to empty features list on slow response" in {
      val getFeaturesSlowRoute: Route =
        (get & path(
          "api" / "1.x" / "service" / "autoru-users" / "feature" / "user" / "autoru_common_1"
        ))(complete {
          Thread.sleep(10000)
          JsArray(feature.toJson(featureInstanceJsonWriter)).prettyPrint
        })
      val client = new AutoRuPromocoderClient(
        "autoru-users",
        runServer(getFeaturesSlowRoute).toString,
        SttpClientImpl(TestOperationalSupport)
      ) with FallbackPromocoderClient
      client
        .getFeatures(PromocoderUser(AutoruUser(1)))
        .success
        .value shouldBe Nil
    }

    "fail on error" in {
      failingClient
        .getFeatures(failingUser)
        .failure
    }
  }

  "AutoRuPromocoderClient.changeFeatureCount" should {
    "change feature count" in {
      val test =
        for {
          _ <- ensureNoFeaturesBefore("1", user)
          created <- promocoderClient.createFeatures(
            "1",
            user,
            List(request)
          )
          changed <- promocoderClient.changeFeatureCount(
            created.loneElement.id,
            FeatureCount(1, FeatureUnits.Items)
          )
        } yield changed.count.count shouldBe 2

      test.success.value
    }

    "fail on error" in {
      val featureId = "featureInstanceId"
      val count = FeatureCount(5, FeatureUnits.Items)
      failingClient
        .changeFeatureCount(featureId, count)
        .failure
    }
  }

  "AutoRuPromocoderClient.changeFeatureCountIdempotent" should {
    "change feature count idempotent" in {
      val test =
        for {
          _ <- ensureNoFeaturesBefore("1", user)
          created <- promocoderClient.createFeatures("1", user, List(request))
          _ <- ZIO.foreachPar_(0 to 4) { _ =>
            promocoderClient.changeFeatureCountIdempotent(
              created.loneElement.id,
              "key1",
              FeatureCount(1, FeatureUnits.Items)
            )
          }
          changed <- promocoderClient.getFeatures(user)
        } yield changed.loneElement.count.count shouldBe 2

      test.success.value
    }

    "fail on error" in {
      val featureId = "featureInstanceId"
      val key = "key"
      val count = FeatureCount(5, FeatureUnits.Items)
      failingClient
        .changeFeatureCountIdempotent(featureId, key, count)
        .failure
    }
  }

  "AutoRuPromocoderClient.createFeatures" should {
    "create features" in {
      val test =
        for {
          _ <- ensureNoFeaturesBefore("1", user)
          created <- promocoderClient.createFeatures("1", user, List(request))
        } yield {
          val feature = created.loneElement
          feature.tag shouldBe request.tag
          feature.count.count shouldBe request.count
          feature.payload shouldBe request.jsonPayload
        }

      test.success.value
    }

    "fail on error" in {
      failingClient
        .createFeatures("1", failingUser, List(request))
        .failure
    }
  }

  "AutoRuPromocoderClient.delete" should {
    "delete features" in {
      val test =
        for {
          _ <- ensureNoFeaturesBefore("1", user)
          _ <- promocoderClient.createFeatures("1", user, List(request))
          _ <- promocoderClient.deleteFeatures("1", user)
          features <- promocoderClient.getFeatures(user)
        } yield features shouldBe empty

      test.success.value
    }

    "fail on error" in {
      failingClient
        .deleteFeatures("1", failingUser)
        .failure
    }
  }

  private def getFeaturesBadResultRoute: Route =
    (get & path(
      "api" / "1.x" / "service" / "test" / "feature" / "user" / "autoru_client_1"
    )) {
      complete {
        JsArray(
          feature.toJson(featureInstanceJsonWriter),
          """{"featureType":"autoprolongation","metadata":{"author":"berchenko"}}""".parseJson
        ).prettyPrint
      }
    }

  private def getFeaturesNotFoundRoute: Route =
    (get & path(
      "api" / "1.x" / "service" / "test" / "feature" / "user" / "autoru_client_2"
    ))(complete(NotFound))

  private def changeFeatureCountBadRequestRoute: Route =
    (post & path(
      "api" / "1.x" / "service" / "test" / "feature" / "featureInstanceId" / "5"
    ))(complete(BadRequest))

  private def changeFeatureCountIdempotentBadRequestRoute: Route =
    (put & path(
      "api" / "1.x" / "service" / "test" / "feature" / "featureInstanceId" / "5"
    ))(parameter("key")(_ => complete(BadRequest)))

  private def createFeaturesNotFoundRoute: Route =
    (post & path(
      "api" / "1.x" / "service" / "test" / "feature" / "batch" / "1" / "user" / "autoru_client_2"
    ))(entity(as[String])(_ => complete(NotFound)))

  private def deleteFeaturesInternalServerErrorRoute: Route =
    (delete & path(
      "api" / "1.x" / "service" / "test" / "internal" / "feature" / "Api" / "1"
    ))(parameter("user")(_ => complete(InternalServerError)))

  private def createFeature(id: String) = FeatureInstance(
    id = id,
    origin = FeatureOrigin("origin"),
    tag = "loyalty",
    user = "123",
    count = FeatureCount(3L, FeatureUnits.Money),
    createTs = currentTime,
    deadline = currentTime.plusDays(2),
    FeaturePayload(FeatureUnits.Money).copy(featureType = FeatureTypes.Loyalty)
  )

  private def createFeatureRequest(count: Int) = FeatureInstanceRequest(
    tag = "loyalty",
    startTime = Some(currentTime),
    lifetime = 2.days,
    count = count,
    jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
  )

  private def ensureNoFeaturesBefore(batchId: String, user: PromocoderUser) =
    for {
      _ <- promocoderClient.deleteFeatures(batchId, user)
      features <- promocoderClient.getFeatures(user)
    } yield features shouldBe empty

}
