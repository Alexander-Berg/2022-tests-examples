// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/settings/about-settings-feature.ts >>>

import Foundation

public protocol AboutSettings {
  @discardableResult
  func openAboutSettings() throws -> Void
  @discardableResult
  func closeAboutSettings() throws -> Void
  @discardableResult
  func isAppVersionValid() throws -> Bool
  @discardableResult
  func isCopyrightValid() throws -> Bool
}

open class AboutSettingsFeature: Feature<AboutSettings> {
  public static var `get`: AboutSettingsFeature = AboutSettingsFeature()
  private init() {
    super.init("AboutSettings", "Экран с информацией о приложении. В iOS и Android открывается из Root Settings")
  }

}

