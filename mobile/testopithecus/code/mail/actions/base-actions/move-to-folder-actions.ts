import { keysArray, requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { Int32, int64, Throwing } from '../../../../../../common/ys'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { CreateFolderComponent } from '../../components/folder-manager/create-folder-component'
import { MovableToFolderFeature } from '../../feature/base-action-features'
import { FolderName, FolderNavigatorFeature } from '../../feature/folder-list-features'
import { ContainerGetterFeature, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { MoveToFolderFeature } from '../../feature/move-to-folder-feature'

export class MoveToFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'MoveToFolder'

  public constructor(private order: Int32, private folderName: FolderName) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.included(modelFeatures) &&
      MovableToFolderFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ContainerGetterFeature.get.included(modelFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const folderNavigatorModel = FolderNavigatorFeature.get.forceCast(model)
    const folders = folderNavigatorModel.getFoldersList()
    const containerGetterModel = ContainerGetterFeature.get.forceCast(model)
    const currentContainer = containerGetterModel.getCurrentContainer()
    return (
      keysArray(folders).filter((folder) => folder === this.folderName).length > 0 &&
      currentContainer.type === MessageContainerType.folder &&
      currentContainer.name !== this.folderName
    )
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Eventus.messageListEvents.moveMessageToFolder(this.order, int64(-1)),
    ]
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    MovableToFolderFeature.get.forceCast(model).moveMessageToFolder(this.order, this.folderName)
    MovableToFolderFeature.get.forceCast(application).moveMessageToFolder(this.order, this.folderName)
    return history.currentComponent
  }

  public tostring(): string {
    return `MovableToFolderAction(${this.order} ${this.folderName})`
  }

  public getActionType(): MBTActionType {
    return MoveToFolderAction.type
  }
}

export class MoveToFolderTapOnFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'MoveToFolderTapOnFolderAction'

  public constructor(private readonly folderName: FolderName) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.included(modelFeatures) &&
      MoveToFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const folderList = FolderNavigatorFeature.get.forceCast(model).getFoldersList()
    return folderList.has(this.folderName)
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    MoveToFolderFeature.get.forceCast(model).tapOnFolder(this.folderName)
    MoveToFolderFeature.get.forceCast(application).tapOnFolder(this.folderName)
    return requireNonNull(history.previousDifferentComponent, 'There is no previous different component')
  }

  public tostring(): string {
    return `${MoveToFolderTapOnFolderAction.type}(${this.folderName})`
  }

  public getActionType(): MBTActionType {
    return MoveToFolderTapOnFolderAction.type
  }
}

export class MoveToFolderTapOnCreateFolderAction implements MBTAction {
  public static readonly type: MBTActionType = 'MoveToFolderTapOnCreateFolderAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MoveToFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    MoveToFolderFeature.get.forceCast(model).tapOnCreateFolder()
    MoveToFolderFeature.get.forceCast(application).tapOnCreateFolder()
    return new CreateFolderComponent()
  }

  public tostring(): string {
    return `${MoveToFolderTapOnCreateFolderAction.type}`
  }

  public getActionType(): MBTActionType {
    return MoveToFolderTapOnCreateFolderAction.type
  }
}
