package ru.yandex.vertis.vsquality.techsupport.api.v1

import cats.data.{Kleisli, Validated}
import cats.effect.{Blocker, IO}
import cats.syntax.applicative._
import com.softwaremill.tagging._
import io.circe.syntax._
import io.circe.{Json, Printer}
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.headers._
import org.scalacheck.Gen
import org.scalatest.Assertion
import ru.yandex.common.monitoring.ping.SignalSwitchingDecider
import ru.yandex.vertis.feature.impl.InMemoryFeatureRegistry
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.feature_registry_utils.FeatureRegistryF
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoSyntax._
import ru.yandex.vertis.vsquality.utils.test_utils.MockitoUtil._
import ru.yandex.vertis.vsquality.techsupport.api.protocol._
import ru.yandex.vertis.vsquality.techsupport.api.{ApiScenariosController, ApiTechsupportController, RoutesBuilder}
import ru.yandex.vertis.vsquality.techsupport.builder.ComponentsFactory.TechsupportFeatureTypes
import ru.yandex.vertis.vsquality.techsupport.config.MdsConfig
import ru.yandex.vertis.vsquality.techsupport.conversion.vertischat.VertisChatConversionInstances
import ru.yandex.vertis.vsquality.techsupport.dao.scenario.ScenarioDao
import ru.yandex.vertis.vsquality.techsupport.model
import ru.yandex.vertis.vsquality.techsupport.model.Arbitraries._
import ru.yandex.vertis.vsquality.techsupport.model.api.{ApiResponse, RequestMeta}
import ru.yandex.vertis.vsquality.techsupport.model.external.jivosite
import ru.yandex.vertis.vsquality.techsupport.model.{Domain, ExternalScenarioId, Tags, UserId}
import ru.yandex.vertis.techsupport.proto.model.Api.GetScenariosResponse.messageCompanion
import ru.yandex.vertis.techsupport.proto.model.{Api, ExternalGraphScenario => ProtoScenario}
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.Action.CompleteConversation
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario.Node
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances._
import scalapb_circe.codec.generatedMessageEncoderWithPrinter
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances.parser
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances.printer
import ru.yandex.vertis.vsquality.techsupport.dao.scenario.ScenarioDao.ScenarioKey

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author devreggs
  */
class ApiSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._

  private val validScenarioProto: ProtoScenario = {
    val startFrom =
      Node(
        id = "start".taggedWith[Tags.BotStateId],
        action = generate[CompleteConversation]()
      )
    val graphScenario =
      ExternalGraphScenario(
        startFrom = startFrom.id,
        fallbackTo = Some(startFrom.id),
        nodesMap = Map(startFrom.id -> startFrom),
        edges = Seq.empty
      )
    graphScenario match {
      case Validated.Valid(a)   => ExternalGraphScenarioFormat.toProto(a)
      case Validated.Invalid(_) => fail("Unexpected invalid graph")
    }
  }

  private val entry =
    Api.UpdateScenarioEntry(
      scenarioId = Some("id"),
      version = Some(3),
      scenario = Some(validScenarioProto)
    )

  private val validUpdateScenarioRequest =
    Api.UpdateScenarioRequest(
      scenarios = Seq(entry)
    )

  import ApiSpec._

  private val mockedApiTechController: ApiTechsupportController[F] = mock[ApiTechsupportController[F]]
  private val mockedApiScenarioController: ApiScenariosController[F] = mock[ApiScenariosController[F]]

  private val featureRegistry = new FeatureRegistryF(new InMemoryFeatureRegistry(TechsupportFeatureTypes))

  implicit private val httpApp: HttpApp[F] =
    new RoutesBuilder(
      "pls don't test swagger",
      mockedApiTechController,
      mockedApiScenarioController,
      new SignalSwitchingDecider(),
      Blocker.liftExecutionContext(global),
      featureRegistry,
      MdsConfig("http://avatars.mdst.yandex.net")
    ).httpApp

  "Api handlers" should {
    when(mockedApiTechController.request(?)).thenReturnOnly(IO.pure(ApiResponse.Ok))
    def checkCorrectJsonHandling(jsonProto: String) = {
      val actualResponse =
        runRequest(
          Method.POST,
          chatMessagePath(model.Domain.Autoru, model.ChatProvider.VertisChats),
          body = EntityEncoder.stringEncoder.toEntity(jsonProto).body
        )
      check(actualResponse, expectedStatus = Status.Ok)
    }

    def checkCorrectProtoHandling(binaryProto: Array[Byte]) = {
      val actualResponse =
        runRequest(
          Method.POST,
          chatMessagePath(model.Domain.Autoru, model.ChatProvider.VertisChats),
          body = EntityEncoder.byteArrayEncoder.toEntity(binaryProto).body,
          headers = Headers.of(`Content-Type`.apply(ProtobufMediaType))
        )
      check(actualResponse, expectedStatus = Status.Ok)
    }

    "receive chat message" in {
      val rm = RequestMeta(Instant.now, "some_req_id".taggedWith[Tags.RequestId], None)
      forAll(clientProcessMessage(rm)) { request =>
        val proto = VertisChatConversionInstances.VertisChatWrites.serialize(request) match {
          case Validated.Valid(a)   => a
          case Validated.Invalid(e) => throw new IllegalArgumentException(s"Failed serialization $e")
        }
        checkCorrectJsonHandling(proto.asJson.printWith(Printer.noSpaces))
        checkCorrectProtoHandling(proto.toByteArray)
      }
    }

    "fail process incorrect message" in {
      import io.circe.generic.auto._
      val request = jivosite.Request(None, None, None)
      val actualResponse =
        runRequest(
          Method.POST,
          chatMessagePath(model.Domain.Autoru, model.ChatProvider.VertisChats),
          body = EntityEncoder[IO, jivosite.Request].toEntity(request).body
        )
      check(actualResponse, expectedStatus = Status.BadRequest)
    }

    "receive techsupport message" in {
      import io.circe.generic.auto._
      forAll(techsupportChatMessage.arbitrary) { request =>
        val actualResponse =
          runRequest(
            Method.POST,
            techMessagePath(model.Domain.Autoru, model.TechsupportProvider.Jivosite),
            body = implicitly[EntityEncoder[F, jivosite.Request]].toEntity(request).body
          )
        check(actualResponse, expectedStatus = Status.Ok)
      }
    }

  }

  "Add scenarios" should {
    stub(
      mockedApiScenarioController
        .update(_: String, _: Seq[(ScenarioDao.ScenarioKey, ExternalGraphScenario)])(_: RequestMeta)
    ) { case _ =>
      ApiResponse.Ok.pure[F]
    }

    "return 200 on correct request with proto body" in {
      val actualResponse = runRequest(
        method = Method.POST,
        path = addScenarioPath(model.Domain.Autoru, "author"),
        body = EntityEncoders
          .proto[F, Api.UpdateScenarioRequest, Api.UpdateScenarioRequest]
          .toEntity(validUpdateScenarioRequest)
          .body,
        headers = Headers.of(`Content-Type`(ProtobufMediaType))
      )
      check(actualResponse, expectedStatus = Status.Ok)
    }

    "return 200 on correct request with json body" in {
      val body = jsonEncoderOf[F, Api.UpdateScenarioRequest].toEntity(validUpdateScenarioRequest).body
      val actualResponse = runRequest(
        method = Method.POST,
        path = addScenarioPath(model.Domain.Autoru, "author"),
        body = body,
        headers = Headers.of(`Content-Type`(MediaType.application.json))
      )
      check(actualResponse, expectedStatus = Status.Ok)
    }

    "return 500 if request validation failed" in {
      val invalidRequestJson: Json = io.circe.parser
        .parse("""
          |{
          |   "author": "user"
          |}
          |""".stripMargin)
        .toTry
        .get
      val actualResponse = runRequest(
        method = Method.POST,
        path = addScenarioPath(model.Domain.Autoru, "author"),
        body = jsonEncoderOf[F, Json].toEntity(invalidRequestJson).body,
        headers = Headers.of(
          Header("x-operator-yandex-id", "test_operator"),
          `Content-Type`(MediaType.application.json)
        )
      )
      check(actualResponse, expectedStatus = Status.InternalServerError)
    }
  }

  "Get scenario" should {
    val scenarioId = "test_scenario"
    val version = 1
    val key = Api.ScenarioKey(Some(scenarioId), ru.yandex.vertis.techsupport.proto.model.Domain.AUTORU, Some(version))
    val scenarioEntry =
      Api
        .ScenarioEntry()
        .withScenario(validScenarioProto)
        .withUpdatedBy("me")
        .withKey(key)
        .withUpdateTime(generate[Instant]().toProtoMessage)
    val scenariosResponse = ApiResponse.GetScenarios(Api.GetScenariosResponse(Seq(scenarioEntry)))
    val keys = Seq(ScenarioKey(scenarioId, model.Domain.Autoru, version))
    when(mockedApiScenarioController.get(keys)).thenReturn(IO.pure(scenariosResponse))

    "return 200 on correct request" in {
      val actualResponse =
        runRequest(
          method = Method.GET,
          path = scenarioPath(model.Domain.Autoru, scenarioId, version),
          headers = Headers.of(Accept(ProtobufMediaType))
        )
      check(
        actualResponse,
        expectedStatus = Status.Ok,
        expectedBody = scenariosResponse.result
      )(EntityDecoders.proto[F, Api.GetScenariosResponse, Api.GetScenariosResponse])
    }

    "encode response to json by default" in {
      import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._
      val actualResponse = runRequest(
        method = Method.GET,
        path = scenarioPath(model.Domain.Autoru, scenarioId, version)
      )
      check(
        actualResponse,
        expectedStatus = Status.Ok,
        expectedBody = scenariosResponse.result
      )(jsonOf[F, Api.GetScenariosResponse])
    }

    "return bad request if no encoders for 'accept' content type" in {
      val actualResponse = runRequest(
        method = Method.GET,
        path = scenarioPath(model.Domain.Autoru, scenarioId, version),
        headers = Headers.of(
          Accept(MediaType.application.xml)
        )
      )
      check(
        actualResponse,
        expectedStatus = Status.BadRequest
      )
    }
  }

  private def check[A](
      actual: IO[Response[IO]],
      expectedStatus: Status,
      expectedBody: A
    )(implicit ev: EntityDecoder[IO, A]): Assertion = {
    val actualResp = actual.await

    actualResp.status shouldBe expectedStatus
    actualResp.as[A].await shouldBe expectedBody
  }

  private def check[A](
      actual: IO[Response[IO]],
      expectedStatus: Status): Assertion = {
    val actualResp = actual.await
    println(new String(actualResp.body.compile.toList.await.toArray))

    actualResp.status shouldBe expectedStatus
  }

}

object ApiSpec {

  private val apiPath: String = "/api/v1"

  private def chatMessagePath(domain: model.Domain, chatProvider: model.ChatProvider): String =
    s"$apiPath/chat/${domain.entryName}/${chatProvider.entryName}/message"

  private def techMessagePath(domain: model.Domain, techsupportProvider: model.TechsupportProvider): String =
    s"$apiPath/tech/${domain.entryName}/${techsupportProvider.entryName}/${model.ChatProvider.VertisChats.entryName}//message"

  private def addScenarioPath(domain: model.Domain, author: String): String =
    s"$apiPath/$domain/scenario?author=$author"

  private def scenarioInfoPath(domain: model.Domain, scenarioId: String): String =
    s"$apiPath/$domain/scenario/$scenarioId"

  private def scenarioPath(domain: model.Domain, scenarioId: String, version: Int): String =
    s"$apiPath/$domain/scenario/$scenarioId/$version"

  private def uri(s: String): Uri = Uri.fromString(s).toOption.get

  private def runRequest[F[_]](
      method: Method,
      path: String,
      body: EntityBody[F] = EmptyBody,
      headers: Headers = Headers.empty
    )(implicit httpApp: Kleisli[F, Request[F], Response[F]]): F[Response[F]] =
    httpApp.run(Request(method, uri(path), body = body, headers = headers))
}
