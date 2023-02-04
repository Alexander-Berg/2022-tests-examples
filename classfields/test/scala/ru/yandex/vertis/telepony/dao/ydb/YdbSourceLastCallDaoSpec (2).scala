package ru.yandex.vertis.telepony.dao.ydb

import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.generator.TeleponyCallGenerator._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.SourceLastCallService.SourceLastCallsRequest
import ru.yandex.vertis.telepony.util.ydb.YdbSupport.Env
import vertis.zio.test.ZioSpecBase
import vertis.ydb.test.YdbTest
import zio.{RIO, ZIO}

class YdbSourceLastCallDaoSpec extends ZioSpecBase with YdbTest with Suite with SpecBase {

  private val dao: YdbSourceLastCallDao = YdbSourceLastCallDao(ydbWrapper)

  override def beforeAll(): Unit = {
    super.beforeAll()
    runSync(dao.createTable).get
  }

  override def afterAll(): Unit = {
    runSync(ydbWrapper.dropTable("source_last_calls"))
    super.afterAll()
  }

  private def buildSourceLastCall(call: TeleponyCall) =
    SourceLastCall(call.id, call.objectId, call.tag, call.time, call.callType, SimplifiedCallResults.getResult(call))

  private def buildCheckIO(
      callsToUpload: Iterable[TeleponyCall],
      domain: TypedDomain,
      request: SourceLastCallsRequest,
      expectedCalls: Iterable[TeleponyCall]) = {
    val io: Iterable[RIO[Env, Unit]] = callsToUpload.map(dao.upsert)
    for {
      _ <- ZIO.collectAll(io)
      res <- dao.getCalls(domain, request)
      expected = expectedCalls.map(buildSourceLastCall)
      _ <- check {
        res should contain theSameElementsAs expected
      }
    } yield ()
  }

  private def modifyCall(call: TeleponyCall)(objectIdPrefix: String): TeleponyCall = {
    call.copy(objectId = ObjectId(s"$objectIdPrefix---${call.objectId.value}"))
  }

  private def genCall(objectIdPrefix: String): TeleponyCall = {
    val call = TeleponyCallGen.suchThat { call =>
      call.callInfo match {
        case _: TeleponyCall.AppCallInfo => true
        case _ if call.source.isDefined => true
        case _ => false
      }
    }.next
    modifyCall(call)(objectIdPrefix)
  }

  private def genApp2AppCall(objectIdPrefix: String): TeleponyCall = {
    val call = TeleponyCallGen.suchThat { call =>
      call.callInfo match {
        case _: TeleponyCall.AppCallInfo => true
        case _ => false
      }
    }.next
    modifyCall(call)(objectIdPrefix)
  }

  private def genPhoneCall(objectIdPrefix: String): TeleponyCall = {
    val call = TeleponyCallGen.suchThat { call =>
      call.callInfo match {
        case _: TeleponyCall.AppCallInfo => false
        case _ if call.source.isDefined => true
        case _ => false
      }
    }.next
    modifyCall(call)(objectIdPrefix)
  }

  private def extractSourceUsername(call: TeleponyCall): Option[Username] =
    call.callInfo match {
      case TeleponyCall.AppCallInfo(sourceUsername, _, _, _, _, _, _) => Some(sourceUsername)
      case _ => None
    }

  "YdbSourceLastCallDao" should {
    "skip unexpected call" in ioTest {
      val call1 = genPhoneCall("skip").copy(source = None)
      val call2 = genApp2AppCall("skip")
      val username = extractSourceUsername(call2)
      val modifiedCall2 = call2.copy(
        domain = call1.domain,
        objectId = call1.objectId,
        callInfo = call2.callInfo match {
          case aci: TeleponyCall.AppCallInfo => aci.copy(sourceUsername = s"skip$username")
          case x => x
        }
      )

      val request = SourceLastCallsRequest(call1.source, username, Iterable(call1.objectId))

      buildCheckIO(Iterable(call1, modifiedCall2), call1.domain, request, expectedCalls = Iterable())
    }

    "upsert and return one call" in ioTest {
      val call = genCall("one")
      val username = extractSourceUsername(call)
      val request = SourceLastCallsRequest(call.source, username, Iterable(call.objectId))

      buildCheckIO(Iterable(call), call.domain, request, expectedCalls = Iterable(call))
    }

    "return few various calls" in ioTest {
      val phoneCall1 = genPhoneCall("few")
      val source = phoneCall1.source
      val domain = phoneCall1.domain
      val phoneCall2 = genPhoneCall("few").copy(domain = domain, source = source)

      val appCall = genApp2AppCall("few").copy(domain = domain)
      val username = extractSourceUsername(appCall)

      val request =
        SourceLastCallsRequest(source, username, Iterable(phoneCall1.objectId, appCall.objectId, phoneCall2.objectId))

      buildCheckIO(
        Iterable(phoneCall1, appCall, phoneCall2),
        domain,
        request,
        expectedCalls = Iterable(phoneCall1, appCall, phoneCall2)
      )
    }

    "return only last call" in ioTest {
      val firstCall = genPhoneCall("last")
      val source = firstCall.source
      val domain = firstCall.domain
      val username = extractSourceUsername(firstCall)
      val objectId = firstCall.objectId
      val lastCall = genPhoneCall("last")
        .copy(time = firstCall.time.plusHours(1), domain = domain, source = source, objectId = objectId)

      val request = SourceLastCallsRequest(source, username, Iterable(objectId))

      buildCheckIO(Iterable(firstCall, lastCall), domain, request, expectedCalls = Iterable(lastCall))
    }

    "return only last call (with app2app)" in ioTest {
      val phoneCall = genPhoneCall("last_app")
      val source = phoneCall.source
      val domain = phoneCall.domain
      val objectId = phoneCall.objectId
      val appCall =
        genApp2AppCall("last_app").copy(domain = domain, objectId = objectId, time = phoneCall.time.plusHours(1))
      val username = extractSourceUsername(appCall)

      val request = SourceLastCallsRequest(source, username, Iterable(objectId))

      buildCheckIO(Iterable(phoneCall, appCall), domain, request, expectedCalls = Iterable(appCall))
    }

    "return only correct phone call" in ioTest {
      val correctCall = genPhoneCall("correct")
      val source = correctCall.source
      val domain = correctCall.domain
      val objectId = correctCall.objectId
      val callWithAnotherObjectId = genPhoneCall("correct").copy(domain = domain, source = source)
      val callWithAnotherSource = genPhoneCall("correct").copy(domain = domain, objectId = objectId)

      val callsToUpload = Iterable(correctCall, callWithAnotherObjectId, callWithAnotherSource)
      val request = SourceLastCallsRequest(source, None, Iterable(objectId))

      buildCheckIO(callsToUpload, domain, request, expectedCalls = Iterable(correctCall))
    }

    "return only correct app2app call" in ioTest {
      val correctCall = genApp2AppCall("correct_app")
      val username = extractSourceUsername(correctCall)
      val callInfo = correctCall.callInfo
      val callType = correctCall.callType
      val domain = correctCall.domain
      val objectId = correctCall.objectId
      val callWithAnotherObjectId =
        genApp2AppCall("correct_app").copy(domain = domain, callInfo = callInfo, callType = callType)
      val callWithAnotherSource = genApp2AppCall("correct_app").copy(domain = domain, objectId = objectId)

      val callsToUpload = Iterable(correctCall, callWithAnotherObjectId, callWithAnotherSource)
      val request = SourceLastCallsRequest(None, username, Iterable(objectId))

      buildCheckIO(callsToUpload, domain, request, expectedCalls = Iterable(correctCall))
    }
  }

}
