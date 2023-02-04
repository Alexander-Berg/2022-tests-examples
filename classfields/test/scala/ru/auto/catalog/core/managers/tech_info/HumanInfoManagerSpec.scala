package ru.auto.catalog.core.managers.tech_info

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.managers.tech_info.HumanInfoManager.AvailableVariantsParams
import ru.auto.catalog.core.testkit.syntax.LiteralIds
import ru.yandex.vertis.baker.util.extdata.geo.RegionTree

import scala.jdk.CollectionConverters._

class HumanInfoManagerSpec extends BaseSpec {

  import ru.auto.catalog.core.testkit._

  val humanInfoManager = new HumanInfoManager(TestCardCatalogWrapper, verbaCars, regionTree = mock[RegionTree])

  "HumanInfoManager" should {

    "process mark and model giving available complectation" in {
      val res = humanInfoManager
        .availableVariants(AvailableVariantsParams(mark = mark"BMW", model = Some(model"X1")))
      val res2 = res.getOrElse(sys.error("right expected"))
      res2.getBodyTypeCount shouldEqual 1
      res2.getBodyTypeList.asScala.toSet shouldEqual Set("ALLROAD_5_DOORS")
      res2.getEngineTypeCount shouldEqual 5
      res2.getEngineTypeList.asScala.toSet shouldEqual Set("ATMO", "TURBO", "HYBRID", "GASOLINE", "DIESEL")
      res2.getGearTypeCount shouldEqual 3
      res2.getGearTypeList.asScala.toSet shouldEqual Set("ALL_WHEEL_DRIVE", "REAR_DRIVE", "FORWARD_CONTROL")
      res2.getTransmissionCount shouldEqual 3
      res2.getTransmissionList.asScala.toSet shouldEqual Set("AUTOMATIC", "MECHANICAL", "ROBOT")
    }

    "process all models if no model provided" in {
      val res = humanInfoManager
        .availableVariants(AvailableVariantsParams(mark = mark"BMW"))
      val res2 = res.getOrElse(sys.error("right expected"))
      res2.getBodyTypeCount shouldEqual 13
      res2.getBodyTypeList.asScala.toSet shouldEqual Set(
        "LIFTBACK",
        "COUPE_HARDTOP",
        "HATCHBACK_5_DOORS",
        "ALLROAD_5_DOORS",
        "LIMOUSINE",
        "COUPE",
        "SEDAN",
        "CABRIO",
        "ROADSTER",
        "SEDAN_2_DOORS",
        "WAGON_5_DOORS",
        "COMPACTVAN",
        "HATCHBACK_3_DOORS"
      )
      res2.getEngineTypeCount shouldEqual 6
      res2.getEngineTypeList.asScala.toSet shouldEqual Set("ATMO", "TURBO", "HYBRID", "GASOLINE", "DIESEL", "ELECTRO")
      res2.getGearTypeCount shouldEqual 3
      res2.getGearTypeList.asScala.toSet shouldEqual Set("ALL_WHEEL_DRIVE", "REAR_DRIVE", "FORWARD_CONTROL")
      res2.getTransmissionCount shouldEqual 3
      res2.getTransmissionList.asScala.toSet shouldEqual Set("AUTOMATIC", "MECHANICAL", "ROBOT")
    }

    "filter out combinations by certain complectation type (body)" in {
      val res = humanInfoManager
        .availableVariants(AvailableVariantsParams(mark = mark"BMW", bodyType = Some("ALLROAD_5_DOORS")))
      val res2 = res.getOrElse(sys.error("right expected"))
      res2.getBodyTypeCount shouldEqual 1
      res2.getBodyType(0) shouldEqual "ALLROAD_5_DOORS"
    }

    "filter out combinations by certain complectation type (engine)" in {
      val res = humanInfoManager
        .availableVariants(AvailableVariantsParams(mark = mark"BMW", engineType = Some("DIESEL")))
      val res2 = res.getOrElse(sys.error("right expected"))
      res2.getEngineTypeCount shouldEqual 3
      res2.getEngineTypeList.asScala.toSet shouldEqual Set("DIESEL", "ATMO", "TURBO")
    }

    "filter out combinations by certain complectation type (gear)" in {
      val res = humanInfoManager
        .availableVariants(AvailableVariantsParams(mark = mark"BMW", gearType = Some("ALL_WHEEL_DRIVE")))
      val res2 = res.getOrElse(sys.error("right expected"))
      res2.getGearTypeCount shouldEqual 1
      res2.getGearType(0) shouldEqual "ALL_WHEEL_DRIVE"
    }
    "filter out combinations by certain complectation type (transmission)" in {
      val res = humanInfoManager
        .availableVariants(AvailableVariantsParams(mark = mark"BMW", transmission = Some("AUTOMATIC")))
      val res2 = res.getOrElse(sys.error("right expected"))
      res2.getTransmissionCount shouldEqual 1
      res2.getTransmission(0) shouldEqual "AUTOMATIC"

    }

    "return empty answer if nothing found" in {
      val res = humanInfoManager
        .availableVariants(AvailableVariantsParams(mark = mark"BMW", engineType = Some("ELECTRIC")))
      val res2 = res.getOrElse(sys.error("right expected"))
      res2.getBodyTypeCount shouldEqual 0
      res2.getEngineTypeCount shouldEqual 0
      res2.getGearTypeCount shouldEqual 0
      res2.getTransmissionCount shouldEqual 0
    }

  }
}
