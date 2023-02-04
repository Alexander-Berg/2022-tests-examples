package ru.auto.api.util

import java.time.{ZoneId, ZonedDateTime}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Phone
import ru.auto.api.BaseSpec
import ru.auto.api.model.gen.SalesmanModelGenerators
import ru.auto.api.model.{ModelGenerators, OfferID}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 16.02.17
  */
class ModelUtilsSpec extends BaseSpec with ScalaCheckPropertyChecks with SalesmanModelGenerators {

  import ru.auto.api.model.ModelUtils._

  val tz: ZoneId = ZoneId.systemDefault()

  "ModelUtils" should {
    "getOfferID" in {
      forAll(ModelGenerators.OfferGen)(offer => offer.id shouldBe OfferID.parse(offer.getId))
    }

    "privateUserRef getUserRef" in {
      forAll(ModelGenerators.PrivateOfferGen)(offer => offer.privateUserRef.toPlain shouldBe offer.getUserRef)
    }

    "privateUserRef fail getUserRef for dealer" in {
      forAll(ModelGenerators.DealerOfferGen) { offer =>
        intercept[IllegalArgumentException] {
          offer.privateUserRef
        }
      }
    }

    "call time from phone" in {
      val now = ZonedDateTime.now(tz)
      forAll(ModelGenerators.PhoneObjectGen) { phone =>
        val (from, to) = phone.todayCallTime(now)
        from.getHour shouldBe phone.getCallHourStart
        to.getHour shouldBe phone.getCallHourEnd
      }
      val phoneWithoutTime = Phone.newBuilder().setPhone(ModelGenerators.PhoneGen.next).build()
      val (from, to) = phoneWithoutTime.todayCallTime(now)
      from.plusHours(24) shouldBe to
    }

    "call time from phone if end hour is less than the start hour" in {
      val now = ZonedDateTime.parse("2022-01-01T12:30:00.000000+03:00[Europe/Moscow]")
      val phone = ModelGenerators.PhoneObjectGen.next.toBuilder.setCallHourStart(6).setCallHourEnd(1).build
      val (from, to) = phone.todayCallTime(now)
      from shouldBe ZonedDateTime.parse("2022-01-01T06:00:00.000000+03:00[Europe/Moscow]")
      to shouldBe ZonedDateTime.parse("2022-01-02T01:00:00.000000+03:00[Europe/Moscow]")
    }

    "call time from phone if round-the-clock" in {
      val now = ZonedDateTime.parse("2022-01-01T12:30:00.000000+03:00[Europe/Moscow]")
      val phone = ModelGenerators.PhoneObjectGen.next.toBuilder.setCallHourStart(0).setCallHourEnd(0).build
      val (from, to) = phone.todayCallTime(now)
      from shouldBe ZonedDateTime.parse("2022-01-01T00:00:00.000000+03:00[Europe/Moscow]")
      to shouldBe ZonedDateTime.parse("2022-01-02T00:00:00.000000+03:00[Europe/Moscow]")
    }
  }

  "ModelUtils.RichProductPrice" should {
    "getProlongPrice in cents" in {
      forAll(productPriceGen(price = priceGen(prolongPrice = Gen.const(99900)), prolongationAllowed = true)) { price =>
        price.getOptBaseProlongPriceCent shouldBe Some(99900)
        price.getBaseProlongPriceCents shouldBe 99900
        price.getOptBaseProlongPriceRubles shouldBe Some(999)
        price.getBaseProlongPriceRubles shouldBe 999
      }
    }

    "getProlongPrice in cents when prolongPrice is empty" in {
      forAll(
        productPriceGen(
          price = priceGen(basePrice = Gen.const(99900), prolongPrice = Gen.const(0)),
          prolongationAllowed = true
        )
      ) { price =>
        price.getOptBaseProlongPriceCent shouldBe Some(99900)
        price.getBaseProlongPriceCents shouldBe 99900
        price.getOptBaseProlongPriceRubles shouldBe Some(999)
        price.getBaseProlongPriceRubles shouldBe 999
      }
    }

    "not getProlongPrice when prolongationAllowed = false" in {
      forAll(
        productPriceGen(
          price = priceGen(basePrice = Gen.const(77700), prolongPrice = Gen.const(99900)),
          prolongationAllowed = false
        )
      ) { price =>
        price.getOptBaseProlongPriceCent shouldBe None
        price.getBaseProlongPriceCents shouldBe 0
        price.getOptBaseProlongPriceRubles shouldBe None
        price.getBaseProlongPriceRubles shouldBe 0
      }
    }

  }
}
