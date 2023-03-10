// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/backend-actions.ts >>>

import Foundation

open class BackendCreateFolderAction: MBTAction {
  public var folder: FolderName
  public init(_ folder: FolderName) {
    self.folder = folder
  }

  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    return true
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return "BackendCreateFolderAction(${this.folder})"
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) throws -> MBTComponent {
    (try BackendActionsFeature.`get`.forceCast(model).addFolder(self.folder))
    return history.currentComponent
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return true
  }

  @discardableResult
  open func tostring() -> String {
    return "BackendCreateFolderAction(\(self.folder))"
  }

}

open class BackendCreateLabelAction: MBTAction {
  public var label: LabelData
  public init(_ label: LabelData) {
    self.label = label
  }

  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    return true
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return "BackendCreateLabelAction"
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) throws -> MBTComponent {
    (try BackendActionsFeature.`get`.forceCast(model).addLabel(self.label))
    return history.currentComponent
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return true
  }

  @discardableResult
  open func tostring() -> String {
    return "BackendCreateLabelAction(\(self.label))"
  }

}

