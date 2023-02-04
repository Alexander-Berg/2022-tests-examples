package ru.yandex.realty.model.offer

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.serialization.BuildingInfoProtoConverter

@RunWith(classOf[JUnitRunner])
class BuildingInfoSpec extends SpecBase {

  "buildingInfo" should {
    "default to a null value for buildYear" in {
      val buildingInfo = new BuildingInfo
      buildingInfo.getBuildYear should be(null)
    }

    "allow non-null value for buildYear" in {
      val buildingInfo = new BuildingInfo
      buildingInfo.setBuildYear(2016)
      buildingInfo.getBuildYear should be(2016)
    }

    "allow null value for buildYear" in {
      val buildingInfo = new BuildingInfo
      buildingInfo.setBuildYear(1970)
      buildingInfo.getBuildYear should be(1970)
      buildingInfo.setBuildYear(null)
      buildingInfo.getBuildYear should be(null)
    }

    "preserve null value for buildYear during ser/deser" in {
      val buildingInfo = new BuildingInfo

      buildingInfo.setBuildYear(1914)
      buildingInfo.setBuildYear(null)
      buildingInfo.getBuildYear should be(null)

      val msg = BuildingInfoProtoConverter.toMessage(buildingInfo)
      val deserialized = BuildingInfoProtoConverter.fromMessage(msg)

      deserialized.getBuildYear should be(null)
      BuildingInfoProtoConverter.toMessage(deserialized) should be(BuildingInfoProtoConverter.toMessage(buildingInfo))
    }

    "default to a null value for buildQuarter" in {
      val buildingInfo = new BuildingInfo
      buildingInfo.getBuildQuarter should be(null)
    }

    "allow non-null value for buildQuarter" in {
      val buildingInfo = new BuildingInfo
      buildingInfo.setBuildQuarter(2)
      buildingInfo.getBuildQuarter should be(2)
    }

    "allow null value for buildQuarter" in {
      val buildingInfo = new BuildingInfo
      buildingInfo.setBuildQuarter(3)
      buildingInfo.getBuildQuarter should be(3)
      buildingInfo.setBuildQuarter(null)
      buildingInfo.getBuildQuarter should be(null)
    }

    "preserve null value for buildQuarter during ser/deser" in {
      val buildingInfo = new BuildingInfo

      buildingInfo.setBuildQuarter(4)
      buildingInfo.setBuildQuarter(null)
      buildingInfo.getBuildQuarter should be(null)

      val msg = BuildingInfoProtoConverter.toMessage(buildingInfo)
      val deserialized = BuildingInfoProtoConverter.fromMessage(msg)

      deserialized.getBuildQuarter should be(null)
      BuildingInfoProtoConverter.toMessage(deserialized) should be(BuildingInfoProtoConverter.toMessage(buildingInfo))
    }
  }

}
