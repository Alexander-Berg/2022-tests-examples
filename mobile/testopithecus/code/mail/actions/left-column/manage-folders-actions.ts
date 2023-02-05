import { Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { formatFolderName } from '../../../utils/mail-utils'
import { FolderListComponent } from '../../components/folder-list-component'
import { CreateFolderComponent } from '../../components/folder-manager/create-folder-component'
import { EditFolderComponent } from '../../components/folder-manager/edit-folder-component'
import { ManageFoldersComponent } from '../../components/folder-manager/manage-folders-component'
import { SelectParentFolderComponent } from '../../components/folder-manager/select-parent-folder-component'
import { FolderName, FolderNavigatorFeature } from '../../feature/folder-list-features'
import { ContainerDeletionMethod, ManageableFolderFeature } from '../../feature/manageable-container-features'

export class DeleteFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'DeleteFolder'

  public constructor(
    private folderName: FolderName,
    private parentFolders: FolderName[] = [],
    private deletionMethod: ContainerDeletionMethod = ContainerDeletionMethod.tap,
  ) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    const folders = folderNavigatorModel.getFoldersList()
    return folders.has(this.folderName)
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).deleteFolder(this.folderName, this.parentFolders, this.deletionMethod)
    ManageableFolderFeature.get
      .forceCast(application)
      .deleteFolder(this.folderName, this.parentFolders, this.deletionMethod)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return DeleteFolderAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `Delete folder ${this.folderName} by ${this.deletionMethod}`
  }
}

export class SelectParentFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'SelectParentFolder'

  public constructor(private parentFolder: FolderName[]) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).selectParentFolder(this.parentFolder)
    ManageableFolderFeature.get.forceCast(application).selectParentFolder(this.parentFolder)
    return requireNonNull(history.previousDifferentComponent, 'There is no previous screen')
  }

  public getActionType(): MBTActionType {
    return SelectParentFolderAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SelectParentFolder'
  }
}

export class CloseFolderLocationScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseFolderLocationScreen'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).closeFolderLocationScreen()
    ManageableFolderFeature.get.forceCast(application).closeFolderLocationScreen()
    return requireNonNull(history.previousDifferentComponent, 'There is no previous screen')
  }

  public getActionType(): MBTActionType {
    return CloseFolderLocationScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseFolderLocationScreen'
  }
}

export class OpenFolderLocationScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenFolderLocationScreen'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).openFolderLocationScreen()
    ManageableFolderFeature.get.forceCast(application).openFolderLocationScreen()
    return new SelectParentFolderComponent()
  }

  public getActionType(): MBTActionType {
    return OpenFolderLocationScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenFolderLocationScreen'
  }
}

export class SubmitEditedFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'SubmitEditedFolder'

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).submitEditedFolder()
    ManageableFolderFeature.get.forceCast(application).submitEditedFolder()
    return new ManageFoldersComponent()
  }

  public getActionType(): MBTActionType {
    return SubmitEditedFolderAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SubmitEditedFolder'
  }
}

export class EnterNameForEditedFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'EnterNameForEditedFolder'

  public constructor(private folderName: FolderName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).enterNameForEditedFolder(this.folderName)
    ManageableFolderFeature.get.forceCast(application).enterNameForEditedFolder(this.folderName)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return EnterNameForEditedFolderAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'EnterNameForEditedFolder'
  }
}

export class CloseEditFolderScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseEditFolderScreen'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).closeEditFolderScreen()
    ManageableFolderFeature.get.forceCast(application).closeEditFolderScreen()
    return new ManageFoldersComponent()
  }

  public getActionType(): MBTActionType {
    return CloseEditFolderScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseEditFolderScreen'
  }
}

export class OpenEditFolderScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenEditFolderScreen'

  public constructor(private folderName: FolderName, private parentFolders: FolderName[] = []) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    const folders = folderNavigatorModel.getFoldersList()
    return folders.has(formatFolderName(this.folderName, this.parentFolders))
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).openEditFolderScreen(this.folderName, this.parentFolders)
    ManageableFolderFeature.get.forceCast(application).openEditFolderScreen(this.folderName, this.parentFolders)
    return new EditFolderComponent()
  }

  public getActionType(): MBTActionType {
    return OpenEditFolderScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenEditFolderScreen'
  }
}

export class SubmitNewFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'SubmitNewFolder'

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).submitNewFolder()
    ManageableFolderFeature.get.forceCast(application).submitNewFolder()
    return new ManageFoldersComponent()
  }

  public getActionType(): MBTActionType {
    return SubmitNewFolderAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SubmitNewFolder'
  }
}

export class EnterNameForNewFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'EnterNameForNewFolder'

  public constructor(private folderName: FolderName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).enterNameForNewFolder(this.folderName)
    ManageableFolderFeature.get.forceCast(application).enterNameForNewFolder(this.folderName)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return EnterNameForNewFolderAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'EnterNameForNewFolder'
  }
}

export class CloseCreateFolderScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseCreateFolderScreen'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).closeCreateFolderScreen()
    ManageableFolderFeature.get.forceCast(application).closeCreateFolderScreen()
    return new ManageFoldersComponent()
  }

  public getActionType(): MBTActionType {
    return CloseCreateFolderScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseCreateFolderScreen'
  }
}

export class OpenCreateFolderScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenCreateFolderScreen'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).openCreateFolderScreen()
    ManageableFolderFeature.get.forceCast(application).openCreateFolderScreen()
    return new CreateFolderComponent()
  }

  public getActionType(): MBTActionType {
    return OpenCreateFolderScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenCreateFolderScreen'
  }
}

export class OpenFolderManagerAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenFolderManager'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).openFolderManager()
    ManageableFolderFeature.get.forceCast(application).openFolderManager()
    return new ManageFoldersComponent()
  }

  public getActionType(): MBTActionType {
    return OpenFolderManagerAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenFolderManager'
  }
}

export class CloseFolderManagerAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseFolderManager'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableFolderFeature.get.forceCast(model).closeFolderManager()
    ManageableFolderFeature.get.forceCast(application).closeFolderManager()
    return new FolderListComponent()
  }

  public getActionType(): MBTActionType {
    return CloseFolderManagerAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseFolderManager'
  }
}
