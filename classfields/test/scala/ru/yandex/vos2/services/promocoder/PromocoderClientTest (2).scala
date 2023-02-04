package ru.yandex.vos2.services.promocoder

import org.scalatest.{FunSuite, Matchers}
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 18/09/2018.
  */
class PromocoderClientTest extends FunSuite with Matchers with MockitoSupport {

  private val client = new HttpPromocoderClient("autoru-users", "promocoder-api-test-int.slb.vertis.yandex.net", 80,
    None, None)
  test("create promocode") {
    client.createPromocode("reviews", "REVIEWS-TEST2")
  }
}
 