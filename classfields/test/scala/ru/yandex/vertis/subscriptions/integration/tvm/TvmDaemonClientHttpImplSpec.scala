package ru.yandex.vertis.subscriptions.integration.tvm

import sttp.client.impl.zio.TaskMonadAsyncError
import sttp.client.testing.SttpBackendStub
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.subscriptions.SpecBase
import zio._

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class TvmDaemonClientHttpImplSpec extends SpecBase with ProducerProvider {

  private val runtime = Runtime.default

  def ioTest(action: => Task[Any]): Unit = {
    runtime.unsafeRun(action)
  }

  "TvmDaemonClientHttpImpl.tickets" should {
    val secret = "some"
    val src = "subs"
    val dest = "bb"
    val ticket = "abc"
    "work" in ioTest {
      implicit val backend =
        SttpBackendStub[Task, Nothing](TaskMonadAsyncError)
          .whenRequestMatches { r =>
            r.uri.path == Seq("tvm", "tickets") &&
            r.uri.paramsMap("src") == src &&
            r.uri.paramsMap("dsts") == dest &&
            r.headers.find(_.name == "Authorization").exists(_.value == secret)
          }
          .thenRespond(s""" {"$dest": {"ticket": "$ticket"}} """)

      val client = new TvmDaemonClientHttpImpl(TvmConfig("http://localhost", src, secret))
      client.tickets(Set(dest)).map { result =>
        result.size shouldBe 1
        result.get(dest) shouldBe Some(ticket)
      }
    }
  }

}
