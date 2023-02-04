package ru.yandex.vos2.services.promocoder

import org.joda.time.DateTime
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.model.promocoder.{Constraints, Feature, Promocode}

import scala.concurrent.duration._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 18/09/2018.
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PromocoderClientTest extends AnyFunSuite with Matchers with MockitoSupport {

  implicit val trace = Traced.empty

  private val client =
    new HttpPromocoderClient("autoru-users", "promocoder-api-test-int.slb.vertis.yandex.net", 80, None, None)

  val now: Long = System.currentTimeMillis()
  val code: String = s"16958532_cc_photo_" + now

  test("create promocode") {
    pending
    val jsonPayload = Json.obj(
      "group_id" -> 0,
      "is_personal" -> true,
      "discount" -> Json.obj(
        "discountType" -> "percent",
        "value" -> 100
      )
    )

    val deadline = DateTime.now().plusDays(30)

    val constraints = Constraints(deadline, 1, 1)

    val feature = Feature("boost", 365.days, count = 1, jsonPayload)

    val promocode = Promocode(code, None, Seq(feature), constraints)
    client.createPromocode(promocode)
  }

  test("apply promocode") {
    pending
    client.applyPromocode(16958532, code)
  }
}
