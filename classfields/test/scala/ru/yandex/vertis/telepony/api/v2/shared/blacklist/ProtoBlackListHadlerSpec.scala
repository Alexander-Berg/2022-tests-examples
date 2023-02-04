package ru.yandex.vertis.telepony.api.v2.shared.blacklist

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.service.BlacklistService

import scala.concurrent.Future

class ProtoBlackListHadlerSpec extends RouteTest with MockitoSupport {
  val RequestContext = RawHeader("X-Yandex-Operator-Uid", "123")

  private def createHandler(blacklist: BlacklistService): Route = seal(
    new ProtoBlacklistHandler(blacklist).route
  )

  "ProtoBlacklistHandler" should {

    "route exists true" in {
      val source = RefinedSourceGen.next
      val reason = BlockInfoGen.next
      val uri = Uri("").withQuery(Query("source" -> source.callerId.value))
      val bl = mock[BlacklistService]
      val route = createHandler(bl)
      when(bl.getList(eq(source))).thenReturn(Future.successful(Seq(reason)))
      Get(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(bl).getList(eq(source))
    }
    "route exists true v2" in {
      val info = BlockInfoGen.next
      val uri = Uri("").withQuery(Query("source" -> info.source.callerId.value, "ver" -> "v2"))
      val bl = mock[BlacklistService]
      val route = createHandler(bl)
      when(bl.getList(eq(info.source))).thenReturn(Future.successful(Seq(info)))
      Get(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(bl).getList(eq(info.source))
    }
    "route exists false" in {
      val source = RefinedSourceGen.next
      val uri = Uri("").withQuery(Query("source" -> source.callerId.value))
      val bl = mock[BlacklistService]
      val route = createHandler(bl)
      when(bl.getList(eq(source))).thenReturn(Future.successful(Seq.empty))
      Get(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(bl).getList(eq(source))
    }
  }

}
