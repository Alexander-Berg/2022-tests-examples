package vertis.pushnoy

import vertis.pushnoy.services.TestDeviceChecker
import vertis.pushnoy.services.apple.{AppleDeviceCheckClient, TestAppleDeviceCheckClient}
import vertis.pushnoy.services.xiva.{DeviceChecker, TestXivaClient, XivaClient}

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 12/07/2017.
  */
trait TestClients {
  lazy val xivaClient: XivaClient = new TestXivaClient
  lazy val appleDeviceCheckClient: AppleDeviceCheckClient = new TestAppleDeviceCheckClient
  lazy val deviceChecker: DeviceChecker = new TestDeviceChecker
}
