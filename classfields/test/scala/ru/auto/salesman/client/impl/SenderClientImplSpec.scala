package ru.auto.salesman.client.impl

import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.sttp.SttpClient

class SenderClientImplSpec extends BaseSpec {

  val mockedSttp: SttpClient = mock[SttpClient]

  val client = new SenderClientImpl(
    host = "sender",
    isStable = false,
    login = "sender-user",
    service = "salesman",
    backend = mockedSttp
  )

  "SenderClientImpl" should {
    "build correct json body" in {
      val args: Map[String, String] = Map(
        "cat" -> "meow",
        "dog" -> "woof"
      )

      val json = client.getJsonBody(args).success.value

      json shouldBe {
        """{"args":{"cat":"meow","dog":"woof"},"async":true}"""
      }
    }
  }

}
