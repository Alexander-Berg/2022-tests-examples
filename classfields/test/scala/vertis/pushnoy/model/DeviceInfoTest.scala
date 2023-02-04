package vertis.pushnoy.model

import vertis.pushnoy.PushnoySuiteBase
import vertis.pushnoy.gen.ModelGenerators._
import vertis.pushnoy.model.request.enums.ClientOS
import vertis.pushnoy.model.template.{OfferIncompleteFormTemplate, SeveralDaysInactivityTemplate}
import vertis.pushnoy.util.GeneratorUtils.RichGen

class DeviceInfoTest extends PushnoySuiteBase {
  private val androidDevice = deviceInfoGen(ClientOS.ANDROID).next
  private val iOSDevice = deviceInfoGen(ClientOS.IOS).next

  test("isDeeplinkSupported") {
    assert(androidDevice.copy(appVersion = Some("4.2.0.1")).isDeeplinkSupported)
    assert(androidDevice.copy(appVersion = Some("4.4.0.23")).isDeeplinkSupported)
    assert(!androidDevice.copy(appVersion = Some("4.4.0_APPS_666")).isDeeplinkSupported)

    assert(iOSDevice.copy(appVersion = Some("7.17.0.1")).isDeeplinkSupported)
    assert(iOSDevice.copy(appVersion = Some("7.19.1")).isDeeplinkSupported)
    assert(!iOSDevice.copy(appVersion = Some("7.19.1.4456")).isDeeplinkSupported)
  }

  test("checkPushSupport for SeveralDaysInactivityTemplate") {
    val template = SeveralDaysInactivityTemplate("mark", "model", None, None)
    assert(androidDevice.copy(appVersion = Some("5.2.0")).checkPushSupport(template))
    assert(!androidDevice.copy(appVersion = Some("5.1.0")).checkPushSupport(template))
    assert(iOSDevice.copy(appVersion = Some("7.18.0")).checkPushSupport(template))
  }

  test("checkPushSupport for OfferIncompleteFormTemplate") {
    val template = OfferIncompleteFormTemplate("test", Seq("test"))
    assert(!androidDevice.copy(appVersion = Some("5.1.0")).checkPushSupport(template))
    assert(androidDevice.copy(appVersion = Some("5.2.0.31337")).checkPushSupport(template))
    assert(!iOSDevice.copy(appVersion = Some("5.1.0")).checkPushSupport(template))
    assert(iOSDevice.copy(appVersion = Some("8.4.0.6853")).checkPushSupport(template))
  }
}
