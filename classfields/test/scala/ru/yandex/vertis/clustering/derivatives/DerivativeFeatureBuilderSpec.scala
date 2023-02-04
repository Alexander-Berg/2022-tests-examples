package ru.yandex.vertis.clustering.derivatives

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.specs2.mock.Mockito
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.derivatives.impl.{ApiDerivativeFeaturesBuilder, BuilderDerivativeFeaturesBuilder}
import ru.yandex.vertis.clustering.geobase.GeobaseClient.IpInfo
import ru.yandex.vertis.clustering.geobase.impl.LibGeobaseClient
import ru.yandex.vertis.clustering.model.FeatureTypes._
import ru.yandex.vertis.clustering.model._
import ru.yandex.vertis.clustering.utils.features.FeatureHelpers

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class DerivativeFeatureBuilderSpec extends BaseSpec with Mockito {

  private case class TestCase(featureType: FeatureType,
                              value: String,
                              isExcluded: Option[Boolean] = None,
                              features: Iterable[Feature] = Iterable.empty)

  private val commonTestCases = Seq(
    TestCase(SuidType, "suid.40e59cacbb4b2d23bdaff6226f8e6afa", Some(true)),
    TestCase(FuidType, "fuid", Some(false)),
    TestCase(FingerprintType, "fp"),
    TestCase(YandexUidType, "4839277111555107652"),
    TestCase(PhoneType, "+79652861010", None, Iterable(FeatureHelpers.parsePhoneNet("79652861010"))),
    TestCase(PasswordHashType, "pA$$wd"),
    TestCase(UserAgentType, "ua"),
    TestCase(AutoruApiUuidType, "uuid"),
    TestCase(DeviceUidType, "device-uid"),
    TestCase(IpIspNameType, "defaultName"),
    TestCase(IpAsType, "defaultAsnList"),
    TestCase(IpOrgNameType, "defaultOrgName")
  )

  private val builderTestCases = Seq(
    TestCase(IpType, "8.8.8.8", None, Iterable(IpNet("8.8.8.255", None))),
    TestCase(IpType, "2001:db8:85a3:0:0:8a2e:370:7334")
  )

  private val apiTestCases = Seq(
    TestCase(IpType,
             "8.8.8.8",
             None,
             Iterable(IpAs("as1", None),
                      IpAs("as2", None),
                      IpOrgName("orgName", None),
                      IpIspName("ispName", None),
                      IpNet("8.8.8.255", None))),
    TestCase(IpType,
             "9.9.9.9",
             None,
             Iterable(IpAs("asn", None), IpIspName("ispName", None), IpNet("9.9.9.255", None))),
    TestCase(IpType, "8.9.9.9", None, Iterable(IpOrgName("orgName", None), IpNet("8.9.9.255", None)))
  )

  private val builderDerivativeFeaturesBuilder = new BuilderDerivativeFeaturesBuilder
  private val mockGeoClient = mock[LibGeobaseClient]
  mockGeoClient
    .ipInfo(Ip("8.8.8.8", None).toInetAddress)
    .returns(Try(Some(IpInfo(Iterable("as1", "as2"), Some("ispName"), Some("orgName")))))
  mockGeoClient
    .ipInfo(Ip("9.9.9.9", None).toInetAddress)
    .returns(Try(Some(IpInfo(Iterable("asn"), Some("ispName"), None))))
  mockGeoClient
    .ipInfo(Ip("8.9.9.9", None).toInetAddress)
    .returns(Try(Some(IpInfo(None, None, Some("orgName")))))
  private val apiDerivativeFeaturesBuilder = new ApiDerivativeFeaturesBuilder(mockGeoClient)

  Seq(
    ("builder", commonTestCases ++ builderTestCases, builderDerivativeFeaturesBuilder),
    ("api", commonTestCases ++ apiTestCases, apiDerivativeFeaturesBuilder)
  ).map {
    case (description, testCases, derivativeFeatures) =>
      testCases.foreach { testCase =>
        s"DerivativeFeatureBuilder $testCase for $description" in {
          derivativeFeatures
            .build(FeatureHelpers.parse(testCase.featureType, testCase.value, testCase.isExcluded).get)
            .toSet shouldBe testCase.features.toSet
        }
      }
  }
}
