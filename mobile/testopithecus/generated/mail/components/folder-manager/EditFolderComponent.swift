// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/folder-manager/edit-folder-component.ts >>>

import Foundation

open class EditFolderComponent: MBTComponent {
  public static let type: String = "EditFolderComponent"
  @discardableResult
  open func getComponentType() -> String {
    return EditFolderComponent.type
  }

  @discardableResult
  open func assertMatches(_ model: App, _ application: App) throws -> Void {
    let manageableFolderModel: ManageableFolder! = ManageableFolderFeature.`get`.castIfSupported(model)
    let manageableFolderApplication: ManageableFolder! = ManageableFolderFeature.`get`.castIfSupported(application)
    if manageableFolderModel != nil && manageableFolderApplication != nil {
      let currentFolderNameModel = (try manageableFolderModel.getCurrentEditedFolderName())
      let currentFolderNameApplication = (try manageableFolderApplication.getCurrentEditedFolderName())
      (try assertStringEquals(currentFolderNameModel, currentFolderNameApplication, "Folder name is incorrect"))
      let currentParentFolderModel = (try manageableFolderModel.getCurrentParentFolderForEditedFolder())
      let currentParentFolderApplication = (try manageableFolderApplication.getCurrentParentFolderForEditedFolder())
      (try assertStringEquals(currentParentFolderModel, currentParentFolderApplication, "Parent folder name is incorrect"))
    }
  }

  @discardableResult
  open func tostring() -> String {
    return self.getComponentType()
  }

}

open class EditFolderActions: MBTComponentActions {
  @discardableResult
  open func getActions(_ _model: App) -> YSArray<MBTAction> {
    let actions: YSArray<MBTAction> = YSArray()
    return actions
  }

}

