package ru.yandex.vertis.parsing.scheduler.workers.meta

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import ru.auto.api.CommonModel

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class CarPhotoMetaTest extends FunSuite {
  test("fromJson") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.licensePlates.length == 3)
    assert(carPhotoMeta.get.licensePlates.head == LicensePlate("M958KB159", 0.892))
    assert(carPhotoMeta.get.licensePlates(1) == LicensePlate("B548OY159", 0.721))
    assert(carPhotoMeta.get.licensePlates(2) == LicensePlate("K865OT159", 0.857))
    assert(
      carPhotoMeta.get.vector == Seq(
        0.15062, -0.04797, 0.10632, 0.02738, 0.23107, 0.01406, 0.17915, 0.03727, -0.03573, -0.10146, 0.04651, 0.18664,
        0.02057, 0.13659, 0.05135, -0.06258, -0.02842, -0.12399, -0.12312, 0.12371, 0.05785, -0.09607, -0.04543,
        -0.07804, -0.01630, -0.04332, 0.01024, -0.01068, 0.07637, 0.11733, -0.06010, -0.08776, 0.04335, -0.11755,
        0.08839, 0.02037, -0.05070, 0.03678, 0.07364, 0.01931, -0.01591, -0.24227, 0.18528, -0.05135, 0.04247, -0.15327,
        -0.10837, 0.04701, -0.07508, 0.05618, 0.09497, 0.11809, -0.06061, -0.08625, 0.11755, 0.02387, -0.17616, 0.07796,
        0.18876, -0.02043, 0.03980, -0.25451, -0.07635, -0.10332, 0.03840, 0.11578, -0.07247, -0.13223, -0.14882,
        -0.02952, -0.04533, -0.14534, 0.03524, -0.03825, 0.10767, 0.00343, -0.22363, 0.06419, -0.06566, -0.10502,
        0.18938, 0.04542, -0.12697, 0.05703, -0.06627, 0.00359, 0.01772, -0.08253, 0.02406, -0.02517, -0.08692,
        -0.12971, 0.11956, 0.12180, 0.01710, -0.16588
      )
    )
  }

  test("fromJson2") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta2.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.licensePlates.isEmpty)
    assert(
      carPhotoMeta.get.vector == Seq(
        0.1041, 0.07268, 0.02216, -0.04298, -0.05621, 0.09684, -0.08087, -0.0365, -0.007, -0.01232, 0.20802, 0.04825,
        0.18036, 0.01068, -0.00476, -0.17459, -0.04316, 0.13772, 0.07362, -0.02308, -0.01009, 0.15768, -0.12273,
        -0.32964, -0.20252, 0.08865, -0.16554, 0.07076, 0.08203, 0.04897, -0.02537, -0.02338, 0.06146, -0.06788,
        0.14594, 0.00244, -0.12727, 0.06194, 0.05916, -0.06638, 0.07258, 0.14, 0.03892, -0.03162, -0.16757, -0.05257,
        0.02513, 0.04283, 0.01161, -0.00523, 0.07382, 0.10519, -0.14737, 0.092, -0.03324, 0.06553, -0.02502, 0.03897,
        0.07639, 0.00181, 0.11555, -0.02434, -0.00297, 0.04709, -0.00218, 0.01294, 0.03368, -0.00721, -0.12933, 0.19067,
        -0.23189, -0.04521, -0.06873, 0.03484, 0.12603, -0.13388, 0.00572, 0.20412, -0.13952, -0.17291, 0.0785,
        -0.02158, -0.07141, 0.08967, -0.00777, -0.15108, -0.05186, 0.07704, 0.22811, -0.10087, 0.10193, 0.02559,
        0.07806, -0.01244, -0.12881, 0.04409
      )
    )
  }

  test("fromJson3") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta3.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.probableAngleClass == CommonModel.PhotoClass.AUTO_VIEW_FRONT_LEFT)
  }

  test("fromJson4") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta4.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.probableAngleClass == CommonModel.PhotoClass.PHOTO_CLASS_UNDEFINED)
  }

  test("fromJson5") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta5.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.probableAngleClass == CommonModel.PhotoClass.PHOTO_CLASS_UNDEFINED)
  }

  test("fromJson6") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta6.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.probableAngleClass == CommonModel.PhotoClass.PHOTO_CLASS_UNDEFINED)
  }

  test("fromJson7") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta7.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.probableAngleClass == CommonModel.PhotoClass.PHOTO_CLASS_UNDEFINED)
  }

  test("fromJson8") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta8.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.probableAngleClass == CommonModel.PhotoClass.PHOTO_CLASS_UNDEFINED)
  }

  test("fromJson9") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta9.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.probableAngleClass == CommonModel.PhotoClass.PHOTO_CLASS_UNDEFINED)
  }

  test("fromJson10") {
    val in = this.getClass.getResourceAsStream("/carPhotoMeta10.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.nonEmpty)
    assert(carPhotoMeta.get.probableAngleClass == CommonModel.PhotoClass.AUTO_VIEW_FRONT_RIGHT)
  }

  test("failed meta") {
    val in = this.getClass.getResourceAsStream("/carPhotoFailedMeta.json")
    val str = scala.io.Source.fromInputStream(in).mkString
    val json = Json.parse(str)
    val carPhotoMeta = CarPhotoMeta.fromJson(json)
    assert(carPhotoMeta.isEmpty)
  }
}
