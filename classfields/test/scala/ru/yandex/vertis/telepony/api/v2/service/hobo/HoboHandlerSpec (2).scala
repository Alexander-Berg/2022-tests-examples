package ru.yandex.vertis.telepony.api.v2.service.hobo

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import org.mockito.Mockito
import ru.yandex.vertis.hobo.client.exception.AlreadyExistException
import ru.yandex.vertis.hobo.proto.Model.{Resolution, Task, TeleponyMarkingResolution}
import ru.yandex.vertis.hobo.proto.ModelFactory
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.dao.HoboCallCheckTaskDao
import ru.yandex.vertis.telepony.exception.CallNotFoundException
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.service.CallCheckService
import ru.yandex.vertis.telepony.service.CallCheckService.CheckSource
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext}

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
  * @author neron
  */
class HoboHandlerSpec extends RouteTest with MockitoSupport {

  def createHandler(mockedCallCheckService: CallCheckService): Route =
    seal(
      new HoboHandler {
        override protected def hoboCallCheckService: CallCheckService = mockedCallCheckService
      }.route
    )

  implicit val requestContext: RequestContext = AutomatedContext(id = "id")

  private val callCheckProtoTask = {
    val payload = ModelFactory
      .newPayloadBuilder()
      .setExternal(
        ModelFactory.newExternalBuilder().addAllIds(Seq(ShortStr.next).asJava)
      )
    val teleponyMarkingResolution = TeleponyMarkingResolution
      .newBuilder()
      .setGoodCall(
        TeleponyMarkingResolution.GoodCall
          .newBuilder()
          .setTag("Reallty good call")
      )
      .build()
    val resolution = Resolution
      .newBuilder()
      .setVersion(1)
      .setTeleponyMarking(teleponyMarkingResolution)
      .build()
    Task
      .newBuilder()
      .setVersion(1)
      .setKey("key")
      .setState(Task.State.NEW)
      .setPayload(payload)
      .setResolution(resolution)
      .build()
  }

  "HoboHandler" should {
    "update callCheck from hobo task" in {
      val mockedMCCS = mock[CallCheckService]
      val handler = createHandler(mockedMCCS)
      val modelTask = HoboCallCheckTaskDao.from(callCheckProtoTask)
      import ru.yandex.vertis.telepony.api.v2.view.proto.Marshalling.hoboTaskMarshaller
      when(mockedMCCS.update(eq(modelTask))(?)).thenReturn(Future.successful(()))
      Post(Uri("/call/task"), callCheckProtoTask) ~> handler ~> check {
        status shouldEqual StatusCodes.OK
        Mockito.verify(mockedMCCS).update(eq(modelTask))(?)
      }
    }

    "fail to update callCheck from hobo task if task can't be processed" in {
      val mockedMCCS = mock[CallCheckService]
      val handler = createHandler(mockedMCCS)
      val badProtoTask = callCheckProtoTask.toBuilder.setResolution(Resolution.newBuilder().setVersion(1)).build()
      import ru.yandex.vertis.telepony.api.v2.view.proto.Marshalling.hoboTaskMarshaller
      when(mockedMCCS.update(?)(?)).thenReturn(Future.successful(()))
      Post(Uri("/call/task"), badProtoTask) ~> handler ~> check {
        status shouldEqual StatusCodes.OK
        Mockito.verify(mockedMCCS).update(?)(?)
      }
    }

    "store callCheck and send to hobo" in {
      val mockedMCCS = mock[CallCheckService]
      val testCallId = "superTest"
      val handler = createHandler(mockedMCCS)
      when(mockedMCCS.store(?, ?, ?)(?)).thenReturn(Future.successful(()))
      Put(s"/call/$testCallId/check") ~> handler ~> check {
        status shouldEqual StatusCodes.OK
        Mockito.verify(mockedMCCS).store(eq(CheckSource.Manual), eq(testCallId), ?)(?)
      }
    }

    "fail to store callCheck if no such callId" in {
      val mockedMCCS = mock[CallCheckService]
      val testCallId = "superTest"
      val handler = createHandler(mockedMCCS)
      when(mockedMCCS.store(?, ?, ?)(?)).thenReturn(
        Future.failed(CallNotFoundException(testCallId))
      )
      Put(s"/call/$testCallId/check") ~> handler ~> check {
        status shouldEqual StatusCodes.NotFound
        Mockito.verify(mockedMCCS).store(eq(CheckSource.Manual), eq(testCallId), ?)(?)
      }
    }

  }

}
