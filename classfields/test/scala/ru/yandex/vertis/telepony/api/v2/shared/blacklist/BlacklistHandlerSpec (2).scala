package ru.yandex.vertis.telepony.api.v2.shared.blacklist

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import org.mockito.ArgumentMatchers.argThat
import org.mockito.{ArgumentMatchers, Mockito}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{AntiFraudOptions, BlockInfo}
import ru.yandex.vertis.telepony.model.BlockInfo.Origins
import ru.yandex.vertis.telepony.service.BlacklistService

import scala.concurrent.Future

/**
  * @author @logab
  */
class BlacklistHandlerSpec extends RouteTest with MockitoSupport {
  val RequestContext = RawHeader("X-Yandex-Operator-Uid", "123")

  private def createHandler(blacklist: BlacklistService): Route = seal(
    new BlacklistHandler {
      override protected def blacklistService: BlacklistService = blacklist
      override protected def protoBlacklistHandler: ProtoBlacklistHandler = new ProtoBlacklistHandler(blacklist)
    }.route
  )

  "BlacklistHandler" should {
    Seq(Some("any comment"), None).foreach { comment =>
      s"route add with comment $comment" in {
        val source = RefinedSourceGen.next
        val reason = BlockReasonGen.next
        val info = BlockInfo(source, reason, comment, Origins.BlacklistApi, AntiFraudOptions.Blacklist)
        val qp = Map("source" -> source.callerId.value, "reason" -> reason.toString)
        val queryParameters = comment.map(v => qp.updated("comment", v)).getOrElse(qp)
        val uri = Uri("").withQuery(Query(queryParameters))
        val bl = mock[BlacklistService]
        val route = createHandler(bl)
        when(bl.add(?)).thenReturn(Future.unit)
        Put(uri).withHeaders(RequestContext) ~> route ~> check {
          response.status shouldEqual StatusCodes.OK
        }
        Mockito.verify(bl).add(sameBan(info))
      }
    }
    "route delete" in {
      val source = RefinedSourceGen.next
      val uri = Uri("").withQuery(Query("source" -> source.callerId.value))
      val bl = mock[BlacklistService]
      val route = createHandler(bl)
      when(bl.delete(eq(source))).thenReturn(Future.successful(true))
      Delete(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(bl).delete(eq(source))
    }
    "route add with ttl" in {
      val source = RefinedSourceGen.next
      val reason = BlockReasonGen.next
      val comment = Some(ShortStr.next)
      val ttl = TtlGen.next
      val blockInfo = BlockInfo.withTtl(source, reason, comment, ttl, Origins.BlacklistApi, AntiFraudOptions.Blacklist)
      val queryParameters =
        (Seq("source" -> source.callerId.value, "reason" -> reason.toString) ++
          comment.map(v => "comment" -> v) ++
          ttl.map(v => "ttl" -> v.toSeconds.toInt.toString)).toMap
      val uri = Uri("").withQuery(Query(queryParameters))
      val bl = mock[BlacklistService]
      val route = createHandler(bl)
      def blockInfoMatcher = argThat { info: BlockInfo =>
        val expected = blockInfo.copy(deadline = info.deadline, updateTime = info.updateTime)
        (info.deadline, blockInfo.deadline) match {
          case (None, None) => info == expected
          case (Some(d1), Some(d2)) => math.abs(d1.getMillis - d2.getMillis) < 1000 && info == expected
          case _ => false
        }
      }
      when(bl.add(blockInfoMatcher)).thenReturn(Future.unit)
      Put(uri).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(bl).add(?)
    }
  }

  private def sameBan(info: BlockInfo) = {
    ArgumentMatchers.argThat((v: BlockInfo) => v.copy(updateTime = info.updateTime) == info)
  }
}
