package ru.auto.api.managers.features

import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.ClientFeature
import ru.auto.api.model.bunker.forceupdate.{AppVersions, ForceUpdateVersions}
import ru.auto.api.model.{ModelGenerators, Version}

import scala.jdk.CollectionConverters.ListHasAsScala

class FeaturesConverterSpec extends BaseSpec {

  "FeatureConverter" should {
    "build supported features string with force update feature on android" in {
      import ru.auto.api.model.Version.AppVersionOrdering.mkOrderingOps

      val device = ModelGenerators.androidDevice.next
      val startAndroidVersion = Version("1.0.0_0")
      val endAndroidVersion = Version("6.0.0_0")
      val iosStartVersion = Version("6.0.0_0")
      val iosEndVersion = Version("10.0.0_0")

      val helloRequest = ModelGenerators
        .helloRequestGen(device)
        .filter { request =>
          val version = Version(request.getAppVersion)
          version > startAndroidVersion && version < endAndroidVersion
        }
        .next

      val forceUpdateVersions =
        ForceUpdateVersions(
          enabled = true,
          AppVersions(startAndroidVersion.toString, endAndroidVersion.toString),
          AppVersions(iosStartVersion.toString, iosEndVersion.toString)
        )

      val result = FeaturesConverter.build(helloRequest, forceUpdateVersions)
      val expectedFeatures = (helloRequest.getSupportedFeaturesList.asScala ++ List(ClientFeature.FORCE_UPDATE))
        .map(_.name())
        .toSet
      result shouldBe expectedFeatures
        .collect { case s if s.nonEmpty => s.trim.toLowerCase }
        .mkString(",")
    }

    "build supported features string with force update feature on ios" in {
      import ru.auto.api.model.Version.AppVersionOrdering.mkOrderingOps

      val device = ModelGenerators.iosDevice.next
      val startAndroidVersion = Version("1.0.0_0")
      val endAndroidVersion = Version("6.0.0_0")
      val iosStartVersion = Version("7.0.0_0")
      val iosEndVersion = Version("10.0.0_0")

      val helloRequest = ModelGenerators
        .helloRequestGen(device)
        .filter { request =>
          val version = Version(request.getAppVersion)
          version > iosStartVersion && version < iosEndVersion
        }
        .next

      val forceUpdateVersions =
        ForceUpdateVersions(
          enabled = true,
          AppVersions(startAndroidVersion.toString, endAndroidVersion.toString),
          AppVersions(iosStartVersion.toString, iosEndVersion.toString)
        )

      val result = FeaturesConverter.build(helloRequest, forceUpdateVersions)
      val expectedFeatures = (helloRequest.getSupportedFeaturesList.asScala ++ List(ClientFeature.FORCE_UPDATE))
        .map(_.name())
        .toSet
      result shouldBe expectedFeatures
        .collect { case s if s.nonEmpty => s.trim.toLowerCase }
        .mkString(",")
    }

    "build supported features string without force update feature" in {

      val device = ModelGenerators.DeviceGen.next

      val helloRequest = ModelGenerators
        .helloRequestGen(device)
        .next

      val forceUpdateVersions =
        ForceUpdateVersions(
          enabled = false,
          AppVersions("", ""),
          AppVersions("", "")
        )

      val result = FeaturesConverter.build(helloRequest, forceUpdateVersions)
      val expectedFeatures = helloRequest.getSupportedFeaturesList.asScala
        .map(_.name())
        .toSet
      result shouldBe expectedFeatures
        .collect { case s if s.nonEmpty => s.trim.toLowerCase }
        .mkString(",")
    }

    "build supported features string without force update feature if AppVersions not parsable" in {

      val device = ModelGenerators.DeviceGen.next

      val helloRequest = ModelGenerators
        .helloRequestGen(device)
        .next

      val forceUpdateVersions =
        ForceUpdateVersions(
          enabled = true,
          AppVersions("", ""),
          AppVersions("", "")
        )

      val result = FeaturesConverter.build(helloRequest, forceUpdateVersions)
      val expectedFeatures = helloRequest.getSupportedFeaturesList.asScala
        .map(_.name())
        .toSet
      result shouldBe expectedFeatures
        .collect { case s if s.nonEmpty => s.trim.toLowerCase }
        .mkString(",")
    }

    "build supported features string with version not in range" in {
      import ru.auto.api.model.Version.AppVersionOrdering.mkOrderingOps

      val device = ModelGenerators.DeviceGen.next
      val startVersion = Version("1.0.0_0")
      val endVersion = Version("6.0.0_0")

      val helloRequest = ModelGenerators
        .helloRequestGen(device)
        .filter { request =>
          val version = Version(request.getAppVersion)
          version < startVersion || version > endVersion
        }
        .next

      val forceUpdateVersions =
        ForceUpdateVersions(
          enabled = true,
          AppVersions(startVersion.toString, endVersion.toString),
          AppVersions(startVersion.toString, endVersion.toString)
        )

      val result = FeaturesConverter.build(helloRequest, forceUpdateVersions)
      val expectedFeatures = helloRequest.getSupportedFeaturesList.asScala
        .map(_.name())
        .toSet
      result shouldBe expectedFeatures
        .collect { case s if s.nonEmpty => s.trim.toLowerCase }
        .mkString(",")
    }

  }

}
