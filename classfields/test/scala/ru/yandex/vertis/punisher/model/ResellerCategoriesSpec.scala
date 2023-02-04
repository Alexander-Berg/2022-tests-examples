package ru.yandex.vertis.punisher.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru._

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class ResellerCategoriesSpec extends BaseSpec {

  "ResellerCategoriesGroup" should {
    "grouped" in {

      ResellerCategoriesGroup.categoryGroup(CARS) shouldBe Set(CARS, LCV)

      ResellerCategoriesGroup.categoryGroup(SCOOTERS) shouldBe Set(MOTORCYCLE, SCOOTERS, SNOWMOBILE, ATV)

      ResellerCategoriesGroup.categoryGroup(TRAILER) shouldBe Set(TRUCK, BUS, ARTIC, TRAILER, SPECIAL)

    }
  }

}
