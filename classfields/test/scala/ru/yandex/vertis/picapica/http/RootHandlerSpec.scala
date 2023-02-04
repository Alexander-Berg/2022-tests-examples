package ru.yandex.vertis.picapica.http


import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.vertis.picapica.client.PicaPicaSchemaVersion
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Request
import spray.http.{ContentTypes, HttpEntity, Uri}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * @author @logab
 */
@RunWith(classOf[JUnitRunner])
class RootHandlerSpec
    extends TestKit(ActorSystem("RootHandlerSpec"))
    with WordSpecLike
    with Matchers {

  import spray.client.pipelining._

  def pipeline: SendReceive = sendReceive(system, system.dispatcher, 1.minute)

  //scalastyle:off
  val log = LoggerFactory.getLogger(classOf[RootHandlerSpec])
  "spec" should {
    "do request" ignore {
      val post = Post(uri = "http://dev02f.vs.os.yandex.net:35020/realty/async/0")
      val msg = Request.newBuilder()
      msg.setVersion(PicaPicaSchemaVersion.REQUEST_FORMAT_VERSION)
      val elemBuilder = Request.Element.newBuilder()
      elemBuilder.setKey("-91")
      elemBuilder.addValue(Request.Value.newBuilder.setKey("-86")
          .setSrcUrl(
            "https://avatars.mds.yandex.net/get-autoru-all/139507/99d7ac79601225e76e266e798f4876ee/1200x900"))
          .addValue(Request.Value.newBuilder().setKey("-2")
          .setSrcUrl("//badurl/badurl.jpg"))
      elemBuilder.addValue(Request.Value.newBuilder.setKey("-3")
          .setSrcUrl("/badurl/badurl.jpg"))
          .addValue(Request.Value.newBuilder().setKey("-4")
          .setSrcUrl("оложолошж!!Ж@#*(=-~`"))
      msg.addElement(elemBuilder)

      Await.result(pipeline(post
          .withEntity(HttpEntity(ContentTypes.`application/octet-stream`, msg.build().toByteArray))
      ), 1.minute)
    }
    "fail uri" in {
      try {
        Uri(
          "http://avatars-int.mdst.yandex.net:13000/put-realty/offer.4285379592714870392.619198219695785542?url=http://export.incom.ru/img/?i=img/2ae9e532-00a6-4654-b53d-eb5c8e2ecaf5.jpg",
          Uri.ParsingMode.Strict)
        fail()
      } catch {
        case ignored:Exception=>
      }

    }
    "not fail uri" in {
      val query = URLEncoder.encode("http://export.incom.ru/img/?i=img/2ae9e532-00a6-4654-b53d-eb5c8e2ecaf5.jpg","UTF8")
      val uri: Uri = Uri(
        s"http://avatars-int.mdst.yandex.net:13000/put-realty/offer.4285379592714870392.619198219695785542?url=$query",
        Uri.ParsingMode.RelaxedWithRawQuery)
      println(uri)

    }
  }
  //scalastyle:on
}
