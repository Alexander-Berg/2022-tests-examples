// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/left-column/folder-navigator-actions.ts >>>

import Foundation

open class GoToFolderAction: MBTAction {
  public static let type: MBTActionType = "GoToFolder"
  private var folderName: String
  private var parentFolders: YSArray<String>
  public init(_ folderName: String, _ parentFolders: YSArray<String> = YSArray()) {
    self.folderName = folderName
    self.parentFolders = parentFolders
  }

  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    let folderNavigatorModel = FolderNavigatorFeature.`get`.forceCast(model)
    let folders = (try folderNavigatorModel.getFoldersList())
    return (keysArray(folders).filter({
      (folder) in
      folder == formatFolderName(self.folderName, self.parentFolders)
    }).length > 0)
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ _history: MBTHistory) throws -> MBTComponent {
    (try FolderNavigatorFeature.`get`.forceCast(model).goToFolder(self.folderName, self.parentFolders))
    (try FolderNavigatorFeature.`get`.forceCast(application).goToFolder(self.folderName, self.parentFolders))
    return MaillistComponent()
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return FolderNavigatorFeature.`get`.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return GoToFolderAction.type
  }

  @discardableResult
  open func tostring() -> String {
    return "GoToFolder(\(self.folderName))"
  }

}

open class OpenFolderListAction: MBTAction {
  public static let type: MBTActionType = "OpenFolderList"
  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    return true
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ _history: MBTHistory) throws -> MBTComponent {
    (try FolderNavigatorFeature.`get`.forceCast(model).openFolderList())
    (try FolderNavigatorFeature.`get`.forceCast(application).openFolderList())
    return FolderListComponent()
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return FolderNavigatorFeature.`get`.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return OpenFolderListAction.type
  }

  @discardableResult
  open func tostring() -> String {
    return "OpenFolderList"
  }

}

open class CloseFolderListAction: MBTAction {
  public static let type: MBTActionType = "CloseFolderList"
  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    return true
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) throws -> MBTComponent {
    (try FolderNavigatorFeature.`get`.forceCast(model).closeFolderList())
    (try FolderNavigatorFeature.`get`.forceCast(application).closeFolderList())
    return requireNonNull(history.previousDifferentComponent, "There is no previous screen")
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return FolderNavigatorFeature.`get`.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return CloseFolderListAction.type
  }

  @discardableResult
  open func tostring() -> String {
    return "CloseFolderList"
  }

}

open class PtrFolderListAction: MBTAction {
  public static let type: MBTActionType = "PtrFolderList"
  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    return true
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) throws -> MBTComponent {
    (try FolderNavigatorFeature.`get`.forceCast(model).ptrFoldersList())
    (try FolderNavigatorFeature.`get`.forceCast(application).ptrFoldersList())
    return history.currentComponent
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return FolderNavigatorFeature.`get`.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return PtrFolderListAction.type
  }

  @discardableResult
  open func tostring() -> String {
    return "PtrFolderList"
  }

}

