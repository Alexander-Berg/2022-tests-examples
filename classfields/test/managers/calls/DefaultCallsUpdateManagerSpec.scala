package auto.dealers.calltracking.logic.test.managers.calls

import java.time.Instant
import java.util.UUID
import com.google.protobuf.timestamp.Timestamp
import auto.common.clients.vos.testkit.DummyVosClient
import ru.auto.api.api_offer_model.{Category, Section}
import auto.dealers.calltracking.logic.CallsEnricher
import auto.dealers.calltracking.logic.managers.SettingsManager
import auto.dealers.calltracking.logic.managers.calls.CallsUpdateManager
import auto.dealers.calltracking.logic.managers.calls.CallsUpdateManager._
import auto.dealers.calltracking.logic.repository.callinfo.CallInfoRepository
import auto.dealers.calltracking.model.{Call, CallInfo}
import auto.dealers.calltracking.model.Call.{Billing, Complaint, ResolutionVector => InternalResolution, Telepony}
import auto.dealers.calltracking.model.ExternalId.TeleponyId
import auto.dealers.calltracking.model.testkit.CallGen._
import ru.auto.calltracking.proto.model.Call.CallResult
import auto.dealers.calltracking.storage.testkit.{TestCalltrackingDao, TestSettingsDao}
import auto.dealers.calltracking.storage.testkit.TestCalltrackingDao.TestService
import ru.yandex.vertis.billing.billing_event._
import ru.yandex.vertis.billing.model._
import ru.yandex.vertis.telepony.model.proto.call_model.{CallbackInfo, RedirectCallInfo, TeleponyCall}
import ru.yandex.vertis.telepony.model.proto.call_model.CallbackInfo.CallbackStage
import ru.yandex.vertis.telepony.model.proto.model.CallResultEnum.{CallResult => TeleponyCallResult}
import common.zio.ops.prometheus.Prometheus
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

object DefaultCallsUpdateManagerSpec extends DefaultRunnableSpec {

  private val clientId: Int = 777

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DefaultCallsUpdateManager")(
      testM("Sync upserting from different sources") {
        checkM(
          anyCall,
          anyTelepony(),
          anyResolutionVector,
          anyComplaint,
          anyBilling
        ) { case (call, telepony, resolution, complaint, billing) =>
          for {
            daoTestService <- ZIO.access[Has[TestService]](_.get)
            id <- ZIO.succeed(UUID.randomUUID().toString)
            curTime <- ZIO.succeed(Instant.now())

            calls <- ZIO.collectAll {
              getUpsertingOrders(id, call, telepony, complaint, resolution, billing).map { upserting =>
                for {
                  _ <- upserting
                  upsertedCall <- daoTestService.getCallByExternalId(TeleponyId(id))
                  _ <- daoTestService.removeCall(TeleponyId(id))
                } yield upsertedCall.copy(id = 0L, created = curTime)
              }.toSeq
            }
          } yield assert(calls)(forall(equalTo(calls.head)))
        }
      },
      testM("add tags to callbacks") {
        checkM(
          anyCall,
          anyTelepony()
        ) { case (call, telepony) =>
          for {
            daoTestService <- ZIO.access[Has[TestService]](_.get)
            ids <- ZIO.succeed((1 to 5).map(_ => UUID.randomUUID().toString))
            stages = CallbackStage.values
            calls <- ZIO.foreach(ids.zip(stages)) { case (id, stage) =>
              for {
                _ <- CallsUpdateManager.updateCallFromTelepony(
                  createTeleponyCallMessage(id, call, telepony, callbackStage = Some(stage))
                )
                upsertedCall <- daoTestService.getCallByExternalId(TeleponyId(id))
              } yield upsertedCall
            }
          } yield tagAssertion(calls)
        }
      },
      testM("considers callback from callkeeper") {
        checkM(
          anyCall,
          anyCall,
          anyTelepony()
        ) { case (firstCall, secondCall, telepony) =>
          for {
            daoTestService <- ZIO.access[Has[TestService]](_.get)
            id <- ZIO.succeed(UUID.randomUUID().toString)
            firstCallWithPhones = firstCall.copy(
              redirectPhone = Some("79991112233"),
              sourcePhone = Some("79992223344"),
              externalId = TeleponyId(id)
            )
            _ <- daoTestService.upsertCalls(Seq(firstCallWithPhones))
            callToSave = secondCall.copy(
              clientId = firstCallWithPhones.clientId,
              sourcePhone = firstCallWithPhones.sourcePhone,
              redirectPhone = firstCallWithPhones.redirectPhone,
              callTime = firstCallWithPhones.callTime
            )
            _ <- CallsUpdateManager.updateCallFromTelepony(
              createTeleponyCallMessage(id, callToSave, telepony, Some(CallbackStage.CALLING_SOURCE))
            )
            upsertedCall <- daoTestService.getCallByExternalId(TeleponyId(id))
          } yield assert(upsertedCall.isCallback)(isTrue)
        }
      }
    ).provideCustomLayerShared {
      val testCalltrackingDao = TestCalltrackingDao.live
      val callInfoNoopRepository = ZLayer.succeed(
        CallInfo(
          clientId = clientId,
          offerId = "",
          category = Category.CATEGORY_UNKNOWN,
          section = Section.SECTION_UNKNOWN,
          platform = ""
        )
      ) >>> CallInfoRepository.noop
      val callsEnricher = DummyVosClient.dummy ++ callInfoNoopRepository >>> CallsEnricher.live
      val settings = TestSettingsDao.live >>> SettingsManager.live
      testCalltrackingDao ++
        (callsEnricher ++ testCalltrackingDao ++ settings ++ Prometheus.live >>> CallsUpdateManager.live)
    } @@ sequential

  private def tagAssertion(calls: Seq[Call]): zio.test.TestResult = {
    val callbackResults = Seq(
      CallResult.CALLBACK_FAILED,
      CallResult.SUCCESS,
      CallResult.CALLBACK_MISSING,
      CallResult.CALLBACK_USER_NOT_RESPOND,
      CallResult.CALLBACK_NO_CONFIRMATION
    )

    assert(calls.map(_.isCallback))(forall(equalTo(true))) &&
    assert(calls.map(_.callResult))(hasSameElements(callbackResults)) &&
    assert(calls.flatMap(_.tags))(
      hasSameElements(Seq("Пропущенный", "Не обработан оператором", "Пользователь не ответил"))
    )
  }

  private def getUpsertingOrders(
      id: String,
      call: Call,
      telepony: Telepony,
      complaint: Complaint,
      resolution: InternalResolution,
      billing: Billing): Iterator[RIO[CallsUpdateManager, Unit]] = {
    val upsertScenarios = Seq(
      CallsUpdateManager.updateCallFromTelepony(createTeleponyCallMessage(id, call, telepony, callbackStage = None)),
      CallsUpdateManager.updateCallFromBillingCalls(
        createBillingCallsMessage(id, call.copy(telepony = Some(telepony)), resolution, complaint)
      ),
      CallsUpdateManager.updateCallFromBillingTransactions(
        createBillingTransactionInfoMessage(id, call.copy(telepony = Some(telepony)), billing)
      )
    ).permutations

    upsertScenarios.map(_.reduceLeft(_ *> _))
  }

  private def createTeleponyCallMessage(
      id: String,
      call: Call,
      telepony: Telepony,
      callbackStage: Option[CallbackInfo.CallbackStage]): TeleponyCall = {
    val info: TeleponyCall.Info = callbackStage
      .map(stage => TeleponyCall.Info.CallbackInfo(CallbackInfo(stage = stage)))
      .getOrElse(
        TeleponyCall.Info.RedirectCallInfo(
          RedirectCallInfo(
            proxyNumber = call.redirectPhone.getOrElse(""),
            callResult = convertCallResult(call.callResult)
          )
        )
      )
    TeleponyCall(
      callId = id,
      objectId = telepony.objectId,
      tag = telepony.tag,
      hasRecord = telepony.hasRecord,
      time = Some(Timestamp(call.callTime.getEpochSecond, call.callTime.getNano)),
      durationSeconds = call.callDuration.toSeconds.toInt,
      talkDurationSeconds = call.talkDuration.toSeconds.toInt,
      info = info,
      sourcePhone = call.sourcePhone.getOrElse(""),
      targetPhone = call.targetPhone,
      callType = callbackStage.map(_ => TeleponyCall.CallType.CALLBACK).getOrElse(TeleponyCall.CallType.REDIRECT_CALL)
    )
  }

  private def convertCallResult(callResult: CallResult): TeleponyCallResult =
    callResult match {
      case CallResult.BLOCKED => TeleponyCallResult.BLOCKED
      case CallResult.SUCCESS => TeleponyCallResult.SUCCESS
      case CallResult.NO_ANSWER => TeleponyCallResult.NO_ANSWER
      case CallResult.STOP_CALLER => TeleponyCallResult.STOP_CALLER
      case CallResult.BUSY_CALLEE => TeleponyCallResult.BUSY_CALLEE
      case CallResult.UNAVAILABLE_CALLEE => TeleponyCallResult.UNAVAILABLE_CALLEE
      case CallResult.INVALID_CALLEE => TeleponyCallResult.INVALID_CALLEE
      case CallResult.NO_REDIRECT => TeleponyCallResult.NO_REDIRECT
      case CallResult.ERROR => TeleponyCallResult.ERROR
      case CallResult.Unrecognized(value) => TeleponyCallResult.Unrecognized(value)
      case _ => TeleponyCallResult.UNKNOWN_RESULT
    }

  private def createBillingTransactionInfoMessage(id: String, call: Call, billing: Billing): BillingOperation =
    BillingOperation.defaultInstance
      .withWithdrawPayload {
        BillingOperation.WithdrawPayload.defaultInstance.withCallFact {
          CommonBillingInfo.CallFact.defaultInstance.withCallFact {
            CallFact(
              id = Some(id),
              timestamp = Some(call.callTime.toEpochMilli),
              duration = Some(call.talkDuration.toSeconds.toInt),
              waitDuration = Some(call.callDuration.toSeconds.toInt - call.talkDuration.toSeconds.toInt),
              tag = call.telepony.map(_.tag),
              incoming = call.sourcePhone,
              redirect = call.redirectPhone,
              internal = Some(call.targetPhone),
              objectId = call.telepony.map(_.objectId)
            )
          }
        }
      }
      .withTransactionInfo {
        CommonBillingInfo.TransactionInfo.defaultInstance
          .withCustomerId(CustomerId(1, call.clientId))
      }

  private def createBillingCallsMessage(
      id: String,
      call: Call,
      resolution: InternalResolution,
      complaint: Complaint): CallStateEvent =
    CallStateEvent(
      callFact = Some(
        CallFact(
          id = Some(id),
          timestamp = Some(call.callTime.toEpochMilli),
          duration = Some(call.talkDuration.toSeconds.toInt),
          waitDuration = Some(call.callDuration.toSeconds.toInt - call.talkDuration.toSeconds.toInt),
          redirect = call.redirectPhone,
          incoming = call.sourcePhone,
          internal = Some(call.targetPhone),
          tag = call.telepony.map(_.tag),
          objectId = call.telepony.map(_.objectId),
          result = Some(call.callResult.toString())
        )
      ),
      complaint = Some(
        CallComplaint(
          createTime = Some(complaint.createTime.toEpochMilli)
        )
      ),
      resolution = Some(
        ResolutionVector(
          automatic = Some(
            Resolution(message = resolution.automatic.map(_.message), status = resolution.automatic.map(_.status))
          ),
          manual =
            Some(Resolution(message = resolution.manual.map(_.message), status = resolution.manual.map(_.status)))
        )
      )
    )
}
