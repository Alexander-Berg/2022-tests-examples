package ru.yandex.vos2.reviews.client

import org.scalatest.{FunSuite, Matchers}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.reviews.env.ReviewsEnv

import scala.util.Random

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 25/07/2018.
  */
class MdsClientTest extends FunSuite with Matchers with MockitoSupport {

  private val env = mock[ReviewsEnv]
  when(env.isEnvironmentStable).thenReturn(false)

  test("put image") {
    val client = new MdsClient("avatars-int.mdst.yandex.net", 13000, "//avatars.mdst.yandex.net/get-", env)
    val imageName = Some(Random.alphanumeric.take(10).mkString)
    val res = client.putImage("autoru-reviews",
      imageName,
      "http://avatars.mdst.yandex.net/get-autoru/3302/77352784337c3b769d64d9cf691e2797/orig")

    res.imageName shouldBe imageName
  }

}
