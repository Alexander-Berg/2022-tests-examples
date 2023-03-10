// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/settings/root-settings-actions.ts >>>

import Foundation

open class OpenSettingsAction: MBTAction {
  public static let type: MBTActionType = "OpenRootSettings"
  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return RootSettingsFeature.`get`.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    return true
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) throws -> MBTComponent {
    (try RootSettingsFeature.`get`.forceCast(model).openRootSettings())
    (try RootSettingsFeature.`get`.forceCast(application).openRootSettings())
    return RootSettingsComponent()
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func tostring() -> String {
    return "OpenRootSettings"
  }

  @discardableResult
  open func getActionType() -> String {
    return OpenSettingsAction.type
  }

}

open class CloseRootSettings: MBTAction {
  public static let type: MBTActionType = "CloseRootSettings"
  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return RootSettingsFeature.`get`.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    return true
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) throws -> MBTComponent {
    (try RootSettingsFeature.`get`.forceCast(model).closeRootSettings())
    (try RootSettingsFeature.`get`.forceCast(application).closeRootSettings())
    return FolderListComponent()
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func tostring() -> String {
    return "CloseRootSettings"
  }

  @discardableResult
  open func getActionType() -> String {
    return CloseRootSettings.type
  }

}

