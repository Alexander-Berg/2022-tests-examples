package vertistraf.notification_center.events_broker.main.test.services

import cats.data.Const
import ru.vertistraf.notification_center.events_broker.model.Event.OfferChangeEvent
import ru.vertistraf.notification_center.events_broker.services.MindboxClient.{
  EndpointId,
  MindboxClient,
  MindboxConfig,
  MindboxEventData
}
import ru.vertistraf.notification_center.events_broker.services.impl.MindboxSttpClient
import ru.vertistraf.notification_center.events_broker.model.IdentifiedEvent._
import ru.vertistraf.notification_center.events_broker.model.mindbox.MindboxEventBodySerializers
import ru.vertistraf.notification_center.events_broker.model.mindbox.MindboxResponse.MindboxInterpretedResponse.Error.{
  NonRetryableError,
  RetryableError
}
import zio._
import zio.magic._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.Assertion._
import ru.vertistraf.notification_center.events_broker.model.{Event, IdentifiedEvent}
import ru.vertistraf.notification_center.events_broker.newtypes.Mindbox
import ru.vertistraf.notification_center.events_broker.newtypes.Mindbox.{AndroidSecret, AutoruSecret, IosSecret}
import ru.vertistraf.notification_center.events_broker.services.MindboxClient
import sttp.client3.{BodySerializer, HttpError, Response}
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{DefaultRunnableSpec, ZSpec}
import sttp.client3.asynchttpclient.zio
import sttp.model.{StatusCode, Uri}

object MindboxClientSpec extends DefaultRunnableSpec {

  val validationError: String =
    "{\"status\":\"ValidationError\",\"validationMessages\":[{\"message\":\"\\\"mymail\\\" не является корректным адресом электронной почты.\",\"location\":\"/customer/email\"},{\"message\":\"Заполните хотя бы один контакт.\"}]}"

  val transactionAlreadyProcessed =
    "{\"status\":\"TransactionAlreadyProcessed\"}"

  val protocolError =
    "{\"status\":\"ProtocolError\",\"errorMessage\":\"{текст ошибки}\",\"errorId\":\"{UUID ошибки}\",\"httpStatusCode\":\"{http-код ошибки}\"}"

  val internalServerError =
    "{\"status\":\"InternalServerError\",\"errorMessage\":\"{текст ошибки}\",\"errorId\":\"{UUID ошибки}\",\"httpStatusCode\":\"{http-код ошибки}\"}"

  type IncorrectClientData = Response[Right[Any, NonRetryableError]]

  type TransactionAlreadyProcessed = Response[Right[Any, NonRetryableError]]

  type IncorrectAuthenticationData = Response[Left[HttpError[NonRetryableError], Any]]

  type IncorrectRequest = Response[Left[HttpError[NonRetryableError], Any]]

  type DailyRequestLimit = Response[Left[HttpError[NonRetryableError], Any]]

  type AuthenticationDataNotExists = Response[Left[HttpError[NonRetryableError], Any]]

  type IncorrectRequestUrl = Response[StatusCode]

  type MindboxDoesntWorkWithBody = Response[Left[HttpError[RetryableError], Any]]

  type MindboxDoesntWorkWithoutBody = Response[StatusCode]

  val test: Right[Int, String] = Right("s")

  private val eventGen: Gen[Random with Sized, OfferChangeEvent] = DeriveGen[OfferChangeEvent]

  private val offerChangedReportEventLayer: ZLayer[Any, Throwable, Has[BodySerializer[OfferChangeEvent]]] =
    ZLayer.wire[Has[BodySerializer[Event.OfferChangeEvent]]](
      MindboxEventBodySerializers.offerChangeEventBodySerializer
    )

  val mindboxConfigLayer: ULayer[Has[MindboxConfig]] = ZLayer.succeed {
    MindboxConfig(
      Uri("http://localhost:8080"),
      AndroidSecret("secret"),
      IosSecret("secret"),
      AutoruSecret("secret")
    )
  }

  private def mindboxClientLayer(sttpClient: ULayer[zio.SttpClient]) = {
    ZLayer.wireSome[TestEnvironment, MindboxClient[MindboxSttpClient.ResponseType]](
      sttpClient,
      mindboxConfigLayer,
      MindboxSttpClient.layer
    )
  }

  def stubClient(rawResponse: String, statusCode: Int): ULayer[zio.SttpClient] = ZLayer.succeed {
    zio.AsyncHttpClientZioBackend.stub.whenAnyRequest
      .thenRespond(rawResponse, StatusCode(statusCode))
  }

  def stubClient(statusCode: Int): ULayer[zio.SttpClient] = ZLayer.succeed {
    zio.AsyncHttpClientZioBackend.stub.whenAnyRequest
      .thenRespond(StatusCode(statusCode))
  }

  def sendEvent[E: MindboxEventData: IdentifiedEvent](
      event: E,
      endpointId: EndpointId,
      stub: ULayer[zio.SttpClient]): RIO[TestEnvironment, MindboxSttpClient.ResponseType] = {
    ZIO
      .accessM[MindboxClient[MindboxSttpClient.ResponseType]](_.get.sendEvent[E](event, endpointId))
      .provideLayer(mindboxClientLayer(stub))
  }

  def environment: ZIO[TestEnvironment, Throwable, MindboxEventData[OfferChangeEvent]] = ZIO
    .service[BodySerializer[Event.OfferChangeEvent]]
    .map(serializer =>
      MindboxClient.MindboxEventData(
        Const(Mindbox.Operation("operation")),
        serializer
      )
    )
    .provideLayer(offerChangedReportEventLayer)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("mindbox client tests")(
      testM("Incorrect client data")(
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(validationError, 200))
            }
          assertM(actual) {
            isSubtype[IncorrectClientData] {
              hasField[IncorrectClientData, String](
                "status",
                _.body.value.status,
                equalTo {
                  "ValidationError"
                }
              )
            }
          }
        }
      ),
      testM("Transaction already processed") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(transactionAlreadyProcessed, 200))
            }
          assertM(actual) {
            isSubtype[TransactionAlreadyProcessed] {
              hasField[TransactionAlreadyProcessed, String](
                "status",
                _.body.value.status,
                equalTo {
                  "TransactionAlreadyProcessed"
                }
              )
            }
          }
        }
      },
      testM("Incorrect request") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(protocolError, 400))
            }
          assertM(actual) {
            isSubtype[IncorrectRequest] {
              hasField[IncorrectRequest, String](
                "status",
                _.body.value.body.status,
                equalTo {
                  "ProtocolError"
                }
              )
            }
          }
        }
      },
      testM("Authentication data not exists") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(protocolError, 401))
            }
          assertM(actual) {
            isSubtype[AuthenticationDataNotExists] {
              hasField[AuthenticationDataNotExists, String](
                "status",
                _.body.value.body.status,
                equalTo {
                  "ProtocolError"
                }
              )
            }
          }
        }
      },
      testM("Incorrect authentication data") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(protocolError, 403))
            }
          assertM(actual) {
            isSubtype[IncorrectAuthenticationData] {
              hasField[IncorrectAuthenticationData, String](
                "status",
                _.body.value.body.status,
                equalTo {
                  "ProtocolError"
                }
              )
            }
          }
        }
      },
      testM("Incorrect request url") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(404))
            }
          assertM(actual) {
            isSubtype[IncorrectRequestUrl] {
              hasField[IncorrectRequestUrl, StatusCode](
                "body",
                _.body,
                equalTo {
                  StatusCode.NotFound
                }
              )
            }
          }
        }
      },
      testM("daily request limit per 1 operation") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(protocolError, 429))
            }
          assertM(actual) {
            isSubtype[DailyRequestLimit] {
              hasField[DailyRequestLimit, String](
                "status",
                _.body.value.body.status,
                equalTo {
                  "ProtocolError"
                }
              )
            }
          }
        }
      },
      testM("Mindbox doesnt work") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(internalServerError, 500))
            }
          assertM(actual) {
            isSubtype[MindboxDoesntWorkWithBody] {
              hasField[MindboxDoesntWorkWithBody, String](
                "status",
                _.body.value.body.status,
                equalTo {
                  "InternalServerError"
                }
              )
            }
          }
        }
      },
      testM("Mindbox doesnt work") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(502))
            }
          assertM(actual) {
            isSubtype[MindboxDoesntWorkWithoutBody] {
              hasField[MindboxDoesntWorkWithoutBody, StatusCode](
                "body",
                _.body,
                equalTo {
                  StatusCode.BadGateway
                }
              )
            }
          }
        }
      },
      testM("Mindbox doesnt work") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(504))
            }
          assertM(actual) {
            isSubtype[MindboxDoesntWorkWithoutBody] {
              hasField[MindboxDoesntWorkWithoutBody, StatusCode](
                "body",
                _.body,
                equalTo {
                  StatusCode.GatewayTimeout
                }
              )
            }
          }
        }
      },
      testM("Mindbox doesnt work") {
        checkM(eventGen) { event =>
          val actual = environment
            .flatMap { implicit eventData =>
              sendEvent(event, EndpointId.Autoru, stubClient(internalServerError, 503))
            }
          assertM(actual) {
            isSubtype[MindboxDoesntWorkWithBody] {
              hasField[MindboxDoesntWorkWithBody, String](
                "status",
                _.body.value.body.status,
                equalTo {
                  "InternalServerError"
                }
              )
            }
          }
        }
      }
    )
}
