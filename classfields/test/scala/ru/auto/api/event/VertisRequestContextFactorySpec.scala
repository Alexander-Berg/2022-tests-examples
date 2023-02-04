package ru.auto.api.event

import java.time.YearMonth
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.model.{ModelGenerators, RequestParams}
import ru.auto.api.model.pushnoy.{ClientOS, DeviceInfo}
import ru.auto.api.testkit.TestEnvironment
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.Domain
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._

class VertisRequestContextFactorySpec extends BaseSpec with ScalaCheckPropertyChecks {

  import VertisRequestContextFactory._

  private def requestForApplication(application: Application): Request = {
    val r = new RequestImpl
    r.setTrace(Traced.empty)
    r.setRequestParams(RequestParams.construct(ip = "1.1.1.1", experiments = Set("EXPERIMENT-1")))
    r.setApplication(application)
    r
  }

  private val deviceInfo: DeviceInfo = DeviceInfo(
    fingerprint = "",
    manufacturer = "",
    brand = "",
    model = "",
    device = "",
    product = "",
    clientOS = ClientOS.IOS,
    name = "",
    appVersion = "",
    gaid = Some("GAID"),
    idfa = Some("IDFA"),
    appmetricaDeviceId = Some("METRICA_DEVICE_ID"),
    timezone = None,
    osVersion = None,
    androidId = Some("ANDROID_ID"),
    iosDeviceCheckBits = Some(
      DeviceInfo.IosDeviceCheckBits(
        bit0 = false,
        bit1 = true,
        lastUpdateTime = Some(YearMonth.parse("2019-10"))
      )
    )
  )

  "VertisRequestContextFactory" should {

    "requestId" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        actual.getRequestId shouldBe request.requestId
      }
    }

    "sourceIp" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        actual.getSourceIp shouldBe request.requestParams.ip
      }
    }

    "platform" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        actual.getPlatform shouldBe request.application.platform
      }
    }

    "domain" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        actual.getDomain shouldBe Domain.DOMAIN_AUTO
      }
    }

    "application" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        actual.getApplication shouldBe request.application.name
      }
    }

    "environment" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        actual.getEnvironment shouldBe TestEnvironment.toProto
      }
    }

    "userAgent" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        val expected = request.requestParams.userAgent.getOrElse("")
        actual.getUserAgent shouldBe expected
      }
    }

    "devideId" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        val expected = request.requestParams.deviceUid.getOrElse("")
        actual.getDeviceId shouldBe expected
      }
    }

    "userId" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        val expected = request.user.optUid.map(_.toString).getOrElse("")
        actual.getUserId shouldBe expected
      }
    }

    "yandexUid" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        val expected = request.requestParams.yandexUid.getOrElse("")
        actual.getYandexUid shouldBe expected
      }
    }

    "fingerprint" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        val expected = request.requestParams.fingerprint.getOrElse("")
        actual.getFingerprint shouldBe expected
      }
    }

    "suid" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        val expected = request.requestParams.suid.getOrElse("")
        actual.getSuid shouldBe expected
      }
    }

    "androidId" in {
      val request = requestForApplication(Application.androidApp)
      val actual = VertisRequestContextFactory(request, TestEnvironment, Some(deviceInfo))
      actual.getAndroidId shouldBe "ANDROID_ID"
    }

    "userLocation" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, None)
        actual.hasGeoPoint shouldBe request.requestParams.userLocation.isDefined
      }
    }

    "metricaDeviceId" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val actual = VertisRequestContextFactory(request, TestEnvironment, Some(deviceInfo))
        actual.getMetricaDeviceId shouldBe "METRICA_DEVICE_ID"
      }
    }

    "mobileAdvertisingId Gaid" in {
      val request = requestForApplication(Application.androidApp)
      val actual = VertisRequestContextFactory(request, TestEnvironment, Some(deviceInfo))
      actual.getMobileAdvertisingId shouldBe "GAID"
    }

    "mobileAdvertisingId Idfa" in {
      val request = requestForApplication(Application.iosApp)
      val actual = VertisRequestContextFactory(request, TestEnvironment, Some(deviceInfo))
      actual.getMobileAdvertisingId shouldBe "IDFA"
    }

    "experiments" in {
      val request = requestForApplication(Application.desktop)
      val actual = VertisRequestContextFactory(request, TestEnvironment, None)
      actual.getExperimentsList.asScala.toSet shouldBe Set("EXPERIMENT-1")
    }

    "iosDeviceCheckBits" in {
      val expected = (false, true, "2019-10")
      val request = requestForApplication(Application.iosApp)
      val actual = VertisRequestContextFactory(request, TestEnvironment, Some(deviceInfo))
      val bits = actual.getAppleDeviceCheckBits
      (bits.getBit0, bits.getBit1, bits.getLastUpdateTime) shouldBe expected
    }

    "externalAuth" in {
      forAll(ModelGenerators.RequestGen) { request =>
        val result = VertisRequestContextFactory(request, TestEnvironment, Some(deviceInfo))
        val actual = result.getExternalAuthList.asScala.map(item => (item.getSocialProvider, item.getId))
        val expected = request.user.session.get.getUser.getSocialProfilesList.asScala.map { item =>
          (item.getProvider, item.getSocialUserId)
        }
        actual shouldBe expected
      }
    }
  }
}
