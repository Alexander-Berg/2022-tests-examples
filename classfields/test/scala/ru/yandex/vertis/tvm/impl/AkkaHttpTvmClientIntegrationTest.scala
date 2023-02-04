package ru.yandex.vertis.tvm.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.Ignore
import ru.yandex.vertis.billing.banker.util.AkkaHttpUtil.DefaultHttpResponder
import ru.yandex.vertis.tvm.TvmClientIntegrationTest

/**
  * @author alex-kovalenko
  */
@Ignore
class AkkaHttpTvmClientIntegrationTest
  extends TestKit(ActorSystem("AkkaHttpTvmClientIntegrationSpec"))
  with TvmClientIntegrationTest {

  val tvmUrl = "https://tvm-api.yandex.net"
  val responder = new DefaultHttpResponder()
  val client = new AkkaHttpTvmClient(tvmUrl, responder)
}
