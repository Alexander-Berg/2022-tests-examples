package ru.yandex.vertis.moderation.vin

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  instanceGen,
  AutoReviewPartGen,
  AutoReviewsEssentialsGen,
  AutoruEssentialsGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service

class SimpleVinFinderSpec extends SpecBase {

  val finder = new SimpleVinFinder

  "SimpleVinFinder" should {
    "find vin in auto.ru offer" in {
      val vin = "1KLBN52TWXM186109"
      val desc = s"some description $vin here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set(vin)
    }

    "find vin in review" in {
      val vin1 = "1KLBN52TWXM186109"
      val vin2 = "1KLBN52TWXM186111"
      val text = s"some text with $vin1 here"
      val contra = s"contra for review $vin2 here"
      val part = AutoReviewPartGen.next.copy(value = Seq(text))
      val essentials = AutoReviewsEssentialsGen.next.copy(content = Seq(part), contra = Seq(contra))
      val instance = instanceGen(Service.AUTO_REVIEWS).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set(vin1, vin2)
    }

    "find lowercase vin" in {
      val vin = "1klbn52twxm186109"
      val desc = s"some description $vin here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set(vin)
    }

    "find vin with cyrillic letters" in {
      val vin = "1КLВN52ТWХМ186109"
      val desc = s"some description $vin here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set(vin)
    }

    "return empty Set if vin has too many cyrillic letters" in {
      val vin = "1КLВN52ТWХМ186А09"
      val desc = s"some description $vin here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe empty
    }

    "find vin with lowercase cyrillic letters" in {
      val vin = "1кlвn52тwхм186109"
      val desc = s"some description $vin here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set(vin)
    }

    "return empty Set if vin with cyrillic has no digits" in {
      val vin = "AкlвnBCтwхмDEFGHJ"
      val desc = s"some description $vin here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe empty
    }

    "find vin with forbidden char: O" in {
      val vin = "1KLBN52TWXM18610O"
      val desc = s"some description $vin here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set(vin)
    }

    "return empty Set in case of wrong vin (forbidden chars: Q, I)" in {
      val vin1 = "1KLBN52TWXM18610I"
      val vin2 = "1KLBN52TWXM18610Q"
      val text = s"some text with $vin1 here"
      val pro = s"pro for review $vin2 here"
      val part = AutoReviewPartGen.next.copy(value = Seq(text))
      val essentials = AutoReviewsEssentialsGen.next.copy(content = Seq(part), pro = Seq(pro))
      val instance = instanceGen(Service.AUTO_REVIEWS).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set.empty
    }

    "return empty Set in case of wrong vin (too long)" in {
      val vin = "111KLBN55TWXM186119ER"
      val desc = s"some description $vin here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set.empty
    }

    "return empty Set in case of description without vin" in {
      val desc = s"some description here"
      val essentials = AutoruEssentialsGen.next.copy(description = Some(desc))
      val instance = instanceGen(Service.AUTORU).next.copy(essentials = essentials)

      finder.findVin(instance) shouldBe Set.empty
    }
  }
}
