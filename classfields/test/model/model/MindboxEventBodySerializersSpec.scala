package vertistraf.notification_center.events_broker.main.test.model.mindbox

import ru.vertistraf.notification_center.events_broker.model.Event
import ru.vertistraf.notification_center.events_broker.model.Event.{BoughtReportEvent, NewOfferForSavedSearchEvent}
import ru.vertistraf.notification_center.events_broker.model.mindbox.MindboxEventBodySerializers
import ru.vertistraf.notification_center.events_broker.services.EnvPrefixService
import sttp.client3.{BasicRequestBody, BodySerializer, StringBody}
import zio.{Has, Task, ZIO, ZLayer}
import zio.test.environment.TestEnvironment
import zio.test.{assert, Assertion, DefaultRunnableSpec, ZSpec}
import zio._
import zio.magic._

import java.time.Instant

object MindboxEventBodySerializersSpec extends DefaultRunnableSpec {

  import Assertion._

  type Env = TestEnvironment with Has[BodySerializer[Event.BoughtReportEvent]]

  private val Now = Instant.now()

  private val BoughtReportEventSample = BoughtReportEvent("user:123456", "autoru-567890", Now)

  private val BoughtReportEventJson =
    "{\"customer\":{\"ids\":{\"appId\":\"test:user:123456\"}},\"order\":{\"ids\":{\"ads\":\"autoru-567890:test:user:123456\"},\"totalPrice\":\"0\",\"customFields\":{\"boughtReport\":true},\"lines\":[{\"basePricePerItem\":\"0\",\"quantity\":\"1\",\"product\":{\"ids\":{\"ads\":\"autoru-567890\"}},\"status\":\"Bought\"}]}}"

  private val testEnvService = new EnvPrefixService.Service {
    override def getEnvPrefix: Task[String] = Task("test")
  }

  private val boughtReportEventLayer: ZLayer[Any, Throwable, Has[BodySerializer[BoughtReportEvent]]] =
    ZLayer.wire[Has[BodySerializer[Event.BoughtReportEvent]]](
      (() => testEnvService).toLayer,
      MindboxEventBodySerializers.boughtReportEventBodySerializer
    )

  private def extractJson(body: BasicRequestBody): String = body match {
    case StringBody(s, _, _) => s
    case _ => ""
  }

  override def spec: ZSpec[TestEnvironment, Any] = suite("MindboxEventBodySerializers")(
    suite("BoughtReportEventBodySerializer")(
      testM("print BoughtReportEvent into json")(
        (for {
          serializer <- ZIO.service[BodySerializer[Event.BoughtReportEvent]]
        } yield assert(extractJson(serializer(BoughtReportEventSample)))(equalTo(BoughtReportEventJson)))
          .provideLayer(boughtReportEventLayer)
      )
    )
  )
}
