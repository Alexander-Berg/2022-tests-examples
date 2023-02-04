package ru.yandex.vertis.telepony.api.v2.shared.whitelist

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito
import org.scalacheck.Gen
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.json.AntiFraudValues
import ru.yandex.vertis.telepony.model.WhitelistEntry
import ru.yandex.vertis.telepony.service.{WhitelistHistoryService, WhitelistService}

import scala.concurrent.Future

/**
  * @author neron
  */
class WhitelistHandlerSpec extends RouteTest with MockitoSupport {

  val RequestContext = RawHeader("X-Yandex-Operator-Uid", "123")

  private def createHandler(whitelist: WhitelistService, wlHistoryService: WhitelistHistoryService): Route =
    seal(
      new WhitelistHandler {
        override protected def whitelistService: WhitelistService = whitelist

        override protected def whitelistHistoryService: WhitelistHistoryService = wlHistoryService
      }.route
    )

  "WhitelistHandler" should {
    "route add" in {
      val source = RefinedSourceGen.next
      val antiFraud = AntiFraudOptionSetGen.next
      val updateTime = DateTimeGen.next
      val comment = Gen.option(ShortStr).next
      val ttl = TtlGen.next
      val entry = WhitelistEntry.withTtl(source, updateTime, comment, ttl)
      val queryParameters =
        (Seq("source" -> source.callerId.value) ++
          AntiFraudValues.fromAntiFraudSet(antiFraud).map(v => "antifraud" -> v) ++
          comment.map(v => "comment" -> v) ++
          ttl.map(v => "ttl" -> v.toSeconds.toInt.toString)).toMap
      val uri = Uri("").withQuery(Query(queryParameters))
      val wls = mock[WhitelistService]
      val wlhs = mock[WhitelistHistoryService]
      val route = createHandler(wls, wlhs)
      def matcher = argThat { e: WhitelistEntry =>
        (e.deadline, entry.deadline) match {
          case (None, None) => e == entry.copy(updateTime = e.updateTime)
          case (Some(_), Some(_)) =>
            e == entry.copy(deadline = e.deadline, updateTime = e.updateTime)
          case _ => false
        }
      }
      when(wls.add(matcher)).thenReturn(Future.unit)
      Put(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(wls).add(?)
    }

    "route delete" in {
      val source = RefinedSourceGen.next
      val uri = Uri("").withQuery(Query("source" -> source.callerId.value))
      val wls = mock[WhitelistService]
      val wlhs = mock[WhitelistHistoryService]
      val route = createHandler(wls, wlhs)
      when(wls.delete(eq(source))).thenReturn(Future.successful(true))
      Delete(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(wls).delete(eq(source))
    }

    "route get Some" in {
      val entry = WhitelistEntryGen.next
      val uri = Uri("").withQuery(Query("source" -> entry.source.callerId.value))
      val wls = mock[WhitelistService]
      val wlhs = mock[WhitelistHistoryService]
      val route = createHandler(wls, wlhs)
      when(wls.get(eq(entry.source))).thenReturn(Future.successful(Some(entry)))
      Get(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(wls).get(eq(entry.source))
    }

    "route get None" in {
      val source = RefinedSourceGen.next
      val uri = Uri("").withQuery(Query("source" -> source.callerId.value))
      val wls = mock[WhitelistService]
      val wlhs = mock[WhitelistHistoryService]
      val route = createHandler(wls, wlhs)
      when(wls.get(eq(source))).thenReturn(Future.successful(None))
      Get(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(wls).get(eq(source))
    }
  }

}
