package ru.yandex.realty2.extdataloader.loaders.village

import java.time.{LocalDate, ZoneId}

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.proto.village.ConnectivityType
import ru.yandex.realty2.extdataloader.loaders.TestExtDataLoaders

@RunWith(classOf[JUnitRunner])
class RawVillageBuilderTest extends WordSpec with MockFactory with Matchers with OneInstancePerTest {

  val target = new RawVillageBuilder(TestExtDataLoaders.createVerbaStorage("villages/verba_village.xml"))

  "Builder" should {
    "work correct" in {
      val values = target.build()
      values.size shouldEqual 1
      val village = values.head

      village.getId shouldEqual 1763942
      village.getName shouldEqual "Цернское"
      village.getLocation.getPolygon shouldNot be(null)
      village.getConnectivityList should contain(ConnectivityType.CONNECTIVITY_TYPE_INTERNET)
      (village.getConstructionPhotoCount should not).equal(0)
      village.getConstructionPhoto(0).getTimestamp.getSeconds shouldBe {
        LocalDate.of(2016, 9, 30).atStartOfDay(ZoneId.systemDefault()).toEpochSecond
      }
      (village.getConstructionPhoto(0).getPhotoCount should not).equal(0)
      (village.getPhotoCount should not).equal(0)
      (village.getPhaseCount should not).equal(0)
      village.getMainPhoto.getOrigUrl shouldBe "http://avatars.mds.yandex.net:80/get-verba/1672712/2a000001690b7db44803e8158d162c5d22f3/orig"
      village.getAliasList.size shouldEqual 6
    }
  }
}
