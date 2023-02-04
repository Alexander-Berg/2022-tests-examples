package ru.yandex.vertis.telepony.api.v2.service.call

import akka.actor.ActorRef
import akka.http.scaladsl.model
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentType, MediaTypes, StatusCodes, Uri}
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe
import org.joda.time.DateTime
import org.mockito.Mockito
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.service.{BlockedCallServiceProbe, CallServiceProbeV2}
import ru.yandex.vertis.telepony.api.v2.view.json.call.{BlockedCallView, CallView, CallsCountByDayView, CallsStatsView}
import ru.yandex.vertis.telepony.api.{DomainExceptionHandler, RouteTest}
import ru.yandex.vertis.telepony.exception.CallNotFoundException
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.CallService.Filter
import ru.yandex.vertis.telepony.service.CallService.Filter.{ByObjectId, ByRedirectId, ByTag, ByTagPart}
import ru.yandex.vertis.telepony.service.{ActualCallService, BlockedCallService, SourceLastCallService}
import ru.yandex.vertis.telepony.util._
import ru.yandex.vertis.telepony.util.sliced.SlicedResult

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author evans
  */
class CallHandlerSpec extends RouteTest with ScalaFutures with Eventually with MockitoSupport {

  trait Test {
    lazy val callProbe = TestProbe("probe")

    lazy val ocHeaders = List(
      RawHeader("X-Yandex-Operator-Uid", "123"),
      model.headers.`Content-Type`(ContentType(MediaTypes.`application/json`))
    )

    lazy val route = seal(
      new CallHandler {
        override protected def callService: ActualCallService = new CallServiceProbeV2(callProbe.ref)

        override protected def blockedCallService: BlockedCallService =
          new BlockedCallServiceProbe(callProbe.ref)

        override protected def domain: TypedDomain = ???

        override protected def sourceLastCallService: SourceLastCallService = ???

        override protected def callHistoryHandler: CallHistoryHandler = ???
      }.route
    )

    implicit val requestContext: RequestContext = AutomatedContext(id = "id")

    def createHandler(actualCallService: ActualCallService) = seal(
      new CallHandler {
        override protected def callService: ActualCallService = actualCallService

        override protected def blockedCallService: BlockedCallService =
          new BlockedCallServiceProbe(callProbe.ref)

        override protected def domain: TypedDomain = ???

        override protected def sourceLastCallService: SourceLastCallService = ???

        override protected def callHistoryHandler: CallHistoryHandler = ???

      }.route
    )

    def createMock(filter: Filter) = {
      val callService = MockitoSupport.mock[ActualCallService]
      when(callService.callsByDay(MockitoSupport.eq(filter))(?)).thenReturn(Future.successful(Iterable(callsCount)))
      callService
    }

    lazy val historyRedirect = HistoryRedirectGen.next
    lazy val redirectId = historyRedirect.id

    lazy val call = CallV2(
      "xxx",
      DateTime.now(),
      DateTime.now(),
      "yyy",
      None,
      historyRedirect,
      DateTime.now(),
      10.seconds,
      5.seconds,
      hasRecord = false,
      CallResults.Unknown,
      fallbackCall = None,
      whitelistOwnerId = None
    )

    lazy val today: DateTime = DateTime.now()
    lazy val callsCount: CallsCountByDay = CallsCountByDay(today.toLocalDate, 1)

    lazy val blockedCall = BannedCall(
      "xxx",
      DateTime.now(),
      DateTime.now(),
      "yyy",
      None,
      historyRedirect,
      DateTime.now(),
      10.seconds
    )
  }

  val historyRedirect = HistoryRedirectGen.next
  val redirectId = historyRedirect.id

  val byRedirectId: ByRedirectId = ByRedirectId(
    redirectId = redirectId,
    start = DateTime.now().minusDays(1),
    end = DateTime.now().plusDays(1)
  )

  val byObjectId: ByObjectId = ByObjectId(
    objectId = historyRedirect.key.objectId,
    start = DateTime.now().minusDays(1),
    end = DateTime.now().plusDays(1)
  )

  val byTag: ByTag = ByTag(
    objectId = historyRedirect.key.objectId,
    tag = Tag.apply(Some("q")),
    start = DateTime.now().minusDays(1),
    end = DateTime.now().plusDays(1)
  )

  val byTagPart: ByTagPart = ByTagPart(
    objectId = historyRedirect.key.objectId,
    start = DateTime.now().minusDays(1),
    end = DateTime.now().plusDays(1),
    tagPart = TagPart("q")
  )

  "CallHandler" should {
    "list calls" in new Test {
      val page = Page(0, 10)
      val uri = Uri./.withQuery(Query("redirect-id" -> redirectId.value))
      callProbe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("list", f: Filter.ByRedirectId, p, _) if f.redirectId == redirectId && p == page =>
            sender ! SlicedResult(Iterable(call), 1, page)
            this
        }
      })
      Get(uri).withHeaders(ocHeaders) ~> route ~> check {
        import CallView.slicedResultViewUnmarshaller
        responseAs[SlicedResult[CallView]].head shouldEqual CallView.asView(call)
        status shouldEqual StatusCodes.OK
      }
    }
    Seq(
      Set(CallResults.Success),
      Set(CallResults.Success, CallResults.NoAnswer)
    ).foreach { callResultSet =>
      s"list calls by call-results: $callResultSet" in new Test {
        val page = Page(0, 10)
        val callResultsStr = callResultSet.mkString(",")
        val uri = Uri./.withQuery(Query("redirect-id" -> redirectId.value, "call-result" -> callResultsStr))
        callProbe.setAutoPilot(new AutoPilot {
          override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
            case ("list", f: ByRedirectId, p, _) if f.redirectId == redirectId && p == page =>
              f.callResultOpt shouldBe defined
              f.callResultOpt.get should ===(callResultSet)
              sender ! SlicedResult(Iterable(call), 1, page)
              this
          }
        })
        Get(uri).withHeaders(ocHeaders) ~> route ~> check {
          import CallView.slicedResultViewUnmarshaller
          responseAs[SlicedResult[CallView]].head shouldEqual CallView.asView(call)
          status shouldEqual StatusCodes.OK
        }
      }
    }
    Seq(
      Set(PhoneGen.next),
      Set(PhoneGen.next, PhoneGen.next)
    ).foreach { targetSet =>
      s"list calls by targets: $targetSet" in new Test {
        val page = Page(0, 10)
        val targetsStr = targetSet.map(_.value).mkString(",")
        val uri = Uri./.withQuery(Query("redirect-id" -> redirectId.value, "target" -> targetsStr))
        callProbe.setAutoPilot(new AutoPilot {
          override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
            case ("list", f: ByRedirectId, p, _) if f.redirectId == redirectId && p == page =>
              f.targetOpt shouldBe defined
              f.targetOpt.get should ===(targetSet)
              sender ! SlicedResult(Iterable(call), 1, page)
              this
          }
        })
        Get(uri).withHeaders(ocHeaders) ~> route ~> check {
          import CallView.slicedResultViewUnmarshaller
          responseAs[SlicedResult[CallView]].head shouldEqual CallView.asView(call)
          status shouldEqual StatusCodes.OK
        }
      }
    }
    "list calls by min-talk-duration" in new Test {
      val page = Page(0, 10)
      val talkDurationStr = call.talkDuration.toSeconds.toString
      val uri = Uri./.withQuery(Query("redirect-id" -> redirectId.value, "min-talk-duration" -> talkDurationStr))
      callProbe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("list", f: ByRedirectId, p, _) if f.redirectId == redirectId && p == page =>
            f.minTalkDurationOpt shouldBe defined
            f.minTalkDurationOpt.get should ===(call.talkDuration)
            sender ! SlicedResult(Iterable(call), 1, page)
            this
        }
      })
      Get(uri).withHeaders(ocHeaders) ~> route ~> check {
        import CallView.slicedResultViewUnmarshaller
        responseAs[SlicedResult[CallView]].head shouldEqual CallView.asView(call)
        status shouldEqual StatusCodes.OK
      }
    }
    "list calls by min-duration" in new Test {
      val page = Page(0, 10)
      val durationStr = call.duration.toSeconds.toString
      val uri = Uri./.withQuery(Query("redirect-id" -> redirectId.value, "min-duration" -> durationStr))
      callProbe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("list", f: ByRedirectId, p, _) if f.redirectId == redirectId && p == page =>
            f.minDurationOpt shouldBe defined
            f.minDurationOpt.get should ===(call.duration)
            sender ! SlicedResult(Iterable(call), 1, page)
            this
        }
      })
      Get(uri).withHeaders(ocHeaders) ~> route ~> check {
        import CallView.slicedResultViewUnmarshaller
        responseAs[SlicedResult[CallView]].head shouldEqual CallView.asView(call)
        status shouldEqual StatusCodes.OK
      }
    }
    "list limit calls" in new Test {
      val uri = Uri("/full").withQuery(Query("redirect-id" -> historyRedirect.id.value))
      callProbe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("list-limit", f: Filter.ByRedirectId, _, _) if f.redirectId == redirectId =>
            sender ! Iterable(call)
            this
        }
      })
      Get(uri).withHeaders(ocHeaders) ~> route ~> check {
        responseAs[Iterable[CallView]].head shouldEqual CallView.asView(call)
        status shouldEqual StatusCodes.OK
      }
    }
    "list calls fail for wrong input" in new Test {
      val uri = Uri("/").withQuery(Query("start" -> "1111"))
      Get(uri) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "list blocked calls" in new Test {
      val page = Page(0, 10)
      val uri = Uri("/blocked").withQuery(Query("redirect-id" -> redirectId.value))
      callProbe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("list", f: Filter.ByRedirectId, p, _) if f.redirectId == redirectId && p == page =>
            sender ! SlicedResult(Iterable(blockedCall), 1, page)
            this
        }
      })
      Get(uri).withHeaders(ocHeaders) ~> route ~> check {
        import BlockedCallView.slicedResultViewUnmarshaller
        responseAs[SlicedResult[BlockedCallView]].head shouldEqual BlockedCallView
          .asView(blockedCall)
        status shouldEqual StatusCodes.OK
      }
    }
    "list limit blocked calls" in new Test {
      val uri = Uri("/blocked/full").withQuery(Query("redirect-id" -> redirectId.value))
      callProbe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("list-limit", f: Filter.ByRedirectId, _, _) if f.redirectId == redirectId =>
            sender ! Iterable(blockedCall)
            this
          case _ =>
            fail("unexpected message")
            this
        }
      })
      Get(uri).withHeaders(ocHeaders) ~> route ~> check {
        responseAs[Iterable[BlockedCallView]].head shouldEqual BlockedCallView.asView(blockedCall)
        status shouldEqual StatusCodes.OK
      }
    }
    "get calls stats by object-id" in new Test {
      val now = DateTime.now()
      val stats = CallsStats(10, Some(now))
      val objectId = historyRedirect.key.objectId
      val uri = Uri(s"/stats").withQuery(Uri.Query("object-id" -> s"${objectId.value}"))
      callProbe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("stats", `objectId`, Tag.Empty, _) =>
            sender ! stats
            this
          case _ =>
            fail("unexpected message")
            this
        }
      })
      Get(uri).withHeaders(ocHeaders) ~> route ~> check {
        responseAs[CallsStatsView] shouldEqual CallsStatsView.asView(stats)
        status shouldEqual StatusCodes.OK
      }
    }
    "get calls stats by object-id and tag" in new Test {
      val now = DateTime.now()
      val stats = CallsStats(10, Some(now))
      val objectId = historyRedirect.key.objectId
      val tag = historyRedirect.key.tag
      val uri = Uri(s"/stats").withQuery(Uri.Query("object-id" -> objectId.value, "tag" -> tag.asOption.getOrElse("")))
      callProbe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("stats", `objectId`, `tag`, _) =>
            sender ! stats
            this
          case _ =>
            fail("unexpected message")
            this
        }
      })
      Get(uri).withHeaders(ocHeaders) ~> route ~> check {
        responseAs[CallsStatsView] shouldEqual CallsStatsView.asView(stats)
        status shouldEqual StatusCodes.OK
      }
    }
    "fail during counting calls for wrong input" in new Test {
      val uri = Uri(s"/countByDay").withQuery(Query("start" -> "1111"))
      val mock = createMock(Filter.Empty)
      val mockRoute = createHandler(mock)

      Get(uri) ~> mockRoute ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "fail during during counting calls without object-id and redirect-id" in new Test {
      val uri = Uri(s"/countByDay").withQuery(Query())
      val mock = createMock(Filter.Empty)
      val mockRoute = createHandler(mock)

      Get(uri) ~> mockRoute ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
    Seq(
      Query(
        "redirect-id" -> byRedirectId.redirectId.value,
        "start-time" -> byRedirectId.start.toString(),
        "end-time" -> byRedirectId.end.toString()
      ) -> byRedirectId,
      Query(
        "object-id" -> byObjectId.objectId.value,
        "start-time" -> byObjectId.start.toString(),
        "end-time" -> byObjectId.end.toString()
      ) -> byObjectId,
      Query(
        "tag" -> "q",
        "object-id" -> byTag.objectId.value,
        "start-time" -> byTag.start.toString,
        "end-time" -> byTag.end.toString
      ) -> byTag,
      Query(
        "tag-part" -> "q",
        "object-id" -> byTagPart.objectId.value,
        "start-time" -> byTagPart.start.toString,
        "end-time" -> byTagPart.end.toString
      ) -> byTagPart,
      Query(
        "redirect-id" -> byRedirectId.redirectId.value,
        "object-id" -> byObjectId.objectId.value,
        "start-time" -> byRedirectId.start.toString(),
        "end-time" -> byRedirectId.end.toString()
      ) -> byRedirectId
    ).foreach {
      case (q, f) =>
        s"test with query $q should produce filter $f" in new Test {
          val mock = createMock(f)
          val mockRoute = createHandler(mock)

          Get(Uri(s"/countByDay").withQuery(q)).withHeaders(ocHeaders) ~> mockRoute ~> check {
            responseAs[Iterable[CallsCountByDayView]].size shouldEqual 1
            responseAs[Iterable[CallsCountByDayView]].head shouldEqual
              CallsCountByDayView.asView(callsCount)
            status shouldEqual StatusCodes.OK
          }

          Mockito.verify(mock).callsByDay(MockitoSupport.eq(f))(?)
        }
    }
    "get call success" in new Test {
      val uri = Uri(s"/${call.id}")
      val mockService = mock[ActualCallService]
      val mockRoute = createHandler(mockService)

      when(mockService.get(MockitoSupport.eq(call.id))(?))
        .thenReturn(Future.successful(call))

      Get(uri) ~> mockRoute ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[CallView] shouldEqual CallView.asView(call)
      }
      Mockito.verify(mockService).get(MockitoSupport.eq(call.id))(?)
    }
    "get call fail no such id" in new Test {
      import DomainExceptionHandler.specificExceptionHandler
      val fakeId = "123"
      val uri = Uri(s"/$fakeId")
      val mockService = mock[ActualCallService]
      val mockRoute = createHandler(mockService)

      when(mockService.get(MockitoSupport.eq(fakeId))(?))
        .thenReturn(Future.failed(CallNotFoundException(fakeId)))

      Get(uri) ~> mockRoute ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockService).get(MockitoSupport.eq(fakeId))(?)
    }
  }
}
