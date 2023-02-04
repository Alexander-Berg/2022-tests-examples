package ru.yandex.vertis.telepony.service.impl

import org.mockito.Mockito
import ru.yandex.vertis.hobo.client.HoboClient
import ru.yandex.vertis.hobo.client.exception.AlreadyExistException
import ru.yandex.vertis.hobo.proto.Model.{Resolution, Task, TeleponyMarkingResolution}
import ru.yandex.vertis.hobo.proto.ModelFactory
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.HoboCallCheckTaskDao
import ru.yandex.vertis.telepony.dao.HoboCallCheckTaskDao.HoboCallCheckTask
import ru.yandex.vertis.telepony.exception.CallNotFoundException
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.service.CallCheckService.CheckSource
import ru.yandex.vertis.telepony.service.logging.LoggingHoboCallCheckService
import ru.yandex.vertis.telepony.service.{ActualCallService, BlacklistService, CallCheckService}
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext}

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
  * @author ponydev
  */
class HoboCallCheckServiceImplSpec extends SpecBase with MockitoSupport {

  val protoTask = {
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

  implicit val requestContext: RequestContext = AutomatedContext(id = "id")

  private def createHoboCallCheckService(
      hoboCallCheckTaskDao: HoboCallCheckTaskDao,
      callServiceV2: ActualCallService,
      blacklistService: BlacklistService,
      mockedHoboClient: HoboClient): CallCheckService =
    new HoboCallCheckServiceImpl(
      hoboCallCheckTaskDao,
      callServiceV2,
      blacklistService,
      "base-response-url",
      TypedDomains.autoru_def,
      mockedHoboClient
    ) with LoggingHoboCallCheckService

  "HoboCallCheckService" should {
    "store callId and send task to hobo" in {
      val call = CallV2Gen.next
      val callId = call.id
      val mockedCS = mock[ActualCallService]
      when(mockedCS.get(eq(callId))(?))
        .thenReturn(Future.successful(call))
      val mockedMCT = mock[HoboCallCheckTaskDao]
      when(mockedMCT.store(eq(callId), eq(protoTask.getKey)))
        .thenReturn(Future.successful(()))
      val mockedBS = mock[BlacklistService]
      when(mockedBS.add(?)).thenReturn(Future.successful(()))
      val mockedHC = mock[HoboClient]
      when(mockedHC.createTask(?, ?, ?)).thenReturn(Future.successful(protoTask))
      val service = createHoboCallCheckService(mockedMCT, mockedCS, mockedBS, mockedHC)
      service.store(CheckSource.Manual, callId).futureValue
      Mockito.verify(mockedCS).get(eq(callId))(?)
      Mockito.verify(mockedMCT).store(eq(callId), eq(protoTask.getKey))
      Mockito.verify(mockedHC).createTask(?, ?, ?)
    }
    "fail store when no such call" in {
      val call = CallV2Gen.next
      val callId = call.id
      val mockedCS = mock[ActualCallService]
      when(mockedCS.get(eq(callId))(?))
        .thenReturn(Future.failed(CallNotFoundException(callId)))
      val mockedMCT = mock[HoboCallCheckTaskDao]
      when(mockedMCT.store(eq(callId), eq(protoTask.getKey)))
        .thenReturn(Future.successful(()))
      val mockedBS = mock[BlacklistService]
      when(mockedBS.add(?)).thenReturn(Future.successful(()))
      val mockedHC = mock[HoboClient]
      when(mockedHC.createTask(?, ?, ?)).thenReturn(Future.successful(protoTask))
      val service = createHoboCallCheckService(mockedMCT, mockedCS, mockedBS, mockedHC)
      service.store(CheckSource.Manual, callId).failed.futureValue shouldEqual CallNotFoundException(callId)
      Mockito.verify(mockedCS).get(eq(callId))(?)
      Mockito.verify(mockedMCT, Mockito.never()).store(eq(callId), eq(protoTask.getKey))
      Mockito.verify(mockedHC, Mockito.never()).createTask(?, ?, ?)
    }

    "do not store when already exists in hobo" in {
      val taskId = ShortStr.next
      val call = CallV2Gen.next
      val callId = call.id
      val mockedCS = mock[ActualCallService]
      when(mockedCS.get(eq(callId))(?))
        .thenReturn(Future.successful(call))
      val mockedMCT = mock[HoboCallCheckTaskDao]
      when(mockedMCT.store(eq(callId), eq(taskId)))
        .thenReturn(Future.successful(()))
      val mockedBS = mock[BlacklistService]
      when(mockedBS.add(?)).thenReturn(Future.successful(()))
      val mockedHC = mock[HoboClient]
      val hoboEx = AlreadyExistException("hoboKey", "Hobo message")
      when(mockedHC.createTask(?, ?, ?)).thenReturn(Future.failed(hoboEx))
      val service = createHoboCallCheckService(mockedMCT, mockedCS, mockedBS, mockedHC)
      service.store(CheckSource.Manual, callId).failed.futureValue shouldEqual AlreadyExistException(
        "hoboKey",
        "Hobo message"
      )
      Mockito.verify(mockedCS).get(eq(callId))(?)
      Mockito.verify(mockedHC).createTask(?, ?, ?)
      Mockito.verify(mockedMCT, Mockito.never()).store(eq(callId), eq(protoTask.getKey))
    }

    "update check task and get" in {
      val taskId = ShortStr.next
      val call = CallV2Gen.next
      val callId = call.id
      val mockedCS = mock[ActualCallService]
      when(mockedCS.get(eq(callId))(?))
        .thenReturn(Future.successful(call))
      val resolution = CallMarkGen.next
      val mockedMCT = mock[HoboCallCheckTaskDao]
      when(mockedMCT.setResolution(eq(taskId), eq(Some(resolution))))
        .thenReturn(Future.successful(()))
      val mockedBS = mock[BlacklistService]
      when(mockedBS.add(?)).thenReturn(Future.successful(()))
      val service = createHoboCallCheckService(mockedMCT, mockedCS, mockedBS, mock[HoboClient])
      service.update(HoboCallCheckTask(callId, taskId, isClosed = true, Some(resolution))).futureValue
      Mockito.verify(mockedCS).get(eq(callId))(?)
      Mockito.verify(mockedMCT).setResolution(eq(taskId), eq(Some(resolution)))
    }

    "fail update check task and get if no such callId" in {
      val taskId = ShortStr.next
      val call = CallV2Gen.next
      val callId = call.id
      val mockedCS = mock[ActualCallService]
      when(mockedCS.get(eq(callId))(?))
        .thenReturn(Future.failed(CallNotFoundException(callId)))
      val resolution = CallMarkGen.next
      val mockedMCT = mock[HoboCallCheckTaskDao]
      when(mockedMCT.setResolution(eq(taskId), eq(Some(resolution))))
        .thenReturn(Future.successful(()))
      val mockedBS = mock[BlacklistService]
      when(mockedBS.add(?)).thenReturn(Future.successful(()))
      val service = createHoboCallCheckService(mockedMCT, mockedCS, mockedBS, mock[HoboClient])
      service
        .update(
          HoboCallCheckTask(callId, taskId, isClosed = true, Some(resolution))
        )
        .failed
        .futureValue shouldEqual CallNotFoundException(callId)

      Mockito.verify(mockedCS).get(eq(callId))(?)
      Mockito.verify(mockedMCT, Mockito.never()).setResolution(eq(taskId), eq(Some(resolution)))
    }
  }
}
