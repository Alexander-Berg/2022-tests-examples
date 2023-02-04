package ru.yandex.vertis.telepony.api.v2.shared.blacklistusername

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import org.mockito.Mockito
import ru.yandex.vertis.generators.NetGenerators.asProducer
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.api.v2.view.proto.ApiProtoConversions
import ru.yandex.vertis.telepony.generator.Generator.{BlacklistUsernameRequestGen, BlockUsernameInfoGen, VoxUsernameGen}
import ru.yandex.vertis.telepony.service.BlacklistUsernameService
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.Future

class BlacklistUsernameHandlerSpec extends RouteTest with MockitoSupport with ProtobufSupport with ApiProtoConversions {
  val RequestContext = RawHeader("X-Yandex-Operator-Uid", "123")

  private def createHandler(blacklist: BlacklistUsernameService): Route = seal(
    new BlacklistUsernameHandler {
      override protected def blacklistUsernameService: BlacklistUsernameService = blacklist
    }.route
  )

  "BlacklistUsernameHandler" should {
    Seq(Some("any comment"), None).foreach { comment =>
      s"route add with comment $comment" in {
        val request = BlacklistUsernameRequestGen.next
        val bl = mock[BlacklistUsernameService]
        val route = createHandler(bl)
        when(bl.addRequest(?)).thenReturn(Future.unit)
        Put(Uri./, request) ~> route ~> check {
          response.status shouldEqual StatusCodes.OK
        }
        Mockito.verify(bl).addRequest(?)
      }
    }
    "route delete" in {
      val voxUsername = VoxUsernameGen.next
      val uri = Uri("").withQuery(Query("vox_username" -> voxUsername))
      val bl = mock[BlacklistUsernameService]
      val route = createHandler(bl)
      when(bl.delete(eq(voxUsername))).thenReturn(Future.successful(true))
      Delete(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(bl).delete(eq(voxUsername))
    }
    "route exists true" in {
      val voxUsername = VoxUsernameGen.next
      val info = BlockUsernameInfoGen.next
      val uri = Uri("").withQuery(Query("vox_username" -> voxUsername))
      val bl = mock[BlacklistUsernameService]
      val route = createHandler(bl)
      when(bl.get(eq(voxUsername))).thenReturn(Future.successful(Some(info)))
      Get(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(bl).get(eq(voxUsername))
    }
    "route exists false" in {
      val voxUsername = VoxUsernameGen.next
      val uri = Uri("").withQuery(Query("vox_username" -> voxUsername))
      val bl = mock[BlacklistUsernameService]
      val route = createHandler(bl)
      when(bl.get(eq(voxUsername))).thenReturn(Future.successful(None))
      Get(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(bl).get(eq(voxUsername))
    }
  }

}
