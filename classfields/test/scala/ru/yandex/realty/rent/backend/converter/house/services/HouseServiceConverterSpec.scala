package ru.yandex.realty.rent.backend.converter.house.services

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.rent.backend.converter.ImageConverter
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.HouseServiceType
import ru.yandex.realty.rent.proto.api.house.service.MeterTypeNamespace.{MeterType => ApiMeterType}
import ru.yandex.realty.rent.proto.model.house.service.MeterTypeNamespace.MeterType
import ru.yandex.realty.rent.proto.model.house.service.{HouseServiceData, Meter}
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class HouseServiceConverterSpec extends SpecBase with RentModelsGen {
  val mdsUrlBuilder = new MdsUrlBuilder("//hostname")
  val imageConverter = new ImageConverter(mdsUrlBuilder)
  val converter: HouseServiceConverter = new HouseServiceConverter(imageConverter)

  "HouseServiceConverter" should {
    "fill in API model attributes" in {
      val meter = Meter.newBuilder().setType(MeterType.WATER_COLD).setNumber("123").build()
      val houseService = houseServiceGen(readableString.next).next.copy(
        `type` = HouseServiceType.Meter,
        data = HouseServiceData.newBuilder().setMeter(meter).build(),
        createTime = DateTimeUtil.now().minusDays(10),
        updateTime = DateTimeUtil.now().minusDays(5)
      )

      val result = converter.convertHouseService(houseService)
      result.getHouseServiceId shouldBe houseService.houseServiceId
      result.getMeter.getType shouldBe ApiMeterType.WATER_COLD
      result.getMeter.getNumber shouldBe meter.getNumber
      result.getCreateTime.getSeconds shouldBe houseService.createTime.getMillis / 1000
      result.getUpdateTime.getSeconds shouldBe houseService.updateTime.getMillis / 1000
    }
  }
}
