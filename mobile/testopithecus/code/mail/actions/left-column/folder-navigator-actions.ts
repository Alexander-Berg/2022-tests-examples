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
import { keysArray, requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { formatFolderName } from '../../../utils/mail-utils'
import { FolderListComponent } from '../../components/folder-list-component'
import { MaillistComponent } from '../../components/maillist-component'
import { FolderNavigatorFeature } from '../../feature/folder-list-features'

export class GoToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'GoToFolder'

  public constructor(private folderName: string, private parentFolders: string[] = []) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    const folders = folderNavigatorModel.getFoldersList()
    return (
      keysArray(folders).filter((folder) => folder === formatFolderName(this.folderName, this.parentFolders)).length > 0
    )
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    FolderNavigatorFeature.get.forceCast(model).goToFolder(this.folderName, this.parentFolders)
    FolderNavigatorFeature.get.forceCast(application).goToFolder(this.folderName, this.parentFolders)
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public getActionType(): MBTActionType {
    return GoToFolderAction.type
  }

  public tostring(): string {
    return `GoToFolder(${this.folderName})`
  }
}

export class OpenFolderListAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenFolderList'

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    FolderNavigatorFeature.get.forceCast(model).openFolderList()
    FolderNavigatorFeature.get.forceCast(application).openFolderList()
    return new FolderListComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public getActionType(): MBTActionType {
    return OpenFolderListAction.type
  }

  public tostring(): string {
    return 'OpenFolderList'
  }
}

export class CloseFolderListAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseFolderList'

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    FolderNavigatorFeature.get.forceCast(model).closeFolderList()
    FolderNavigatorFeature.get.forceCast(application).closeFolderList()
    return requireNonNull(history.previousDifferentComponent, 'There is no previous screen')
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public getActionType(): MBTActionType {
    return CloseFolderListAction.type
  }

  public tostring(): string {
    return 'CloseFolderList'
  }
}

export class PtrFolderListAction implements MBTAction {
  public static readonly type: MBTActionType = 'PtrFolderList'

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    FolderNavigatorFeature.get.forceCast(model).ptrFoldersList()
    FolderNavigatorFeature.get.forceCast(application).ptrFoldersList()
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FolderNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public getActionType(): MBTActionType {
    return PtrFolderListAction.type
  }

  public tostring(): string {
    return 'PtrFolderList'
  }
}
