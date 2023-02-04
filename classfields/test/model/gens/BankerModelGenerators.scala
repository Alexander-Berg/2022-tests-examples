package ru.auto.salesman.test.model.gens

import org.scalacheck.Gen
import ru.auto.salesman.model.AutoruUser
import ru.yandex.vertis.banker.model.ApiModel.{Account, PaymentMethod, PaymentSystemId}
import ru.yandex.vertis.external.yandexkassa.ApiModel
import ru.yandex.vertis.generators.BasicGenerators

trait BankerModelGenerators extends BasicGenerators {

  val BankerUserGen: Gen[String] = for {
    id <- Gen.posNum[Long]
  } yield AutoruUser(id).toString

  val AccountGen: Gen[Account] = for {
    id <- readableString
    user <- BankerUserGen
  } yield Account.newBuilder().setId(id).setUser(user).build()

  val YandexKassaTiedCardGen: Gen[PaymentMethod] = for {
    cdd <- Gen.choose(100000, 999999)
    pan <- Gen.choose(1000, 9999)
    preferred <- Gen.option(bool)
  } yield {
    val mask = cdd + "|" + pan
    val b = PaymentMethod
      .newBuilder()
      .setId(s"AC#$mask")
      .setPsId(PaymentSystemId.YANDEXKASSA)
    b.getPropertiesBuilder.getCardBuilder.setCddPanMask(mask)
    preferred.foreach(b.getPreferredBuilder.setValue)
    b.build()
  }

  val YandexKassaV3TiedCardGen: Gen[PaymentMethod] = for {
    cdd <- Gen.choose(100000, 999999)
    pan <- Gen.choose(1000, 9999)
    preferred <- Gen.option(bool)
  } yield {
    val mask = cdd + "|" + pan
    val b = PaymentMethod
      .newBuilder()
      .setId(ApiModel.PaymentType.bank_card.toString)
      .setPsId(PaymentSystemId.YANDEXKASSA_V3)
    b.getPropertiesBuilder.getCardBuilder.setCddPanMask(mask)
    preferred.foreach(b.getPreferredBuilder.setValue)
    b.build()
  }

  val AnyTiedCardGen: Gen[PaymentMethod] =
    Gen.oneOf(YandexKassaTiedCardGen, YandexKassaV3TiedCardGen)

  val TrustTiedCardGen: Gen[PaymentMethod] =
    AnyTiedCardGen.map { card =>
      val b = card.toBuilder
      b.setPsId(PaymentSystemId.TRUST)
      b.build()
    }

  val PreferredTiedCardGen: Gen[PaymentMethod] =
    AnyTiedCardGen.map { card =>
      val b = card.toBuilder
      b.getPreferredBuilder.setValue(true)
      b.build()
    }

  val NotPreferredTiedCardGen: Gen[PaymentMethod] =
    AnyTiedCardGen.map(_.toBuilder.clearPreferred().build())

}
