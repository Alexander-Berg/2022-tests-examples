import { LabelData } from '../../../../mapi/code/api/entities/label/label'
import { Throwing } from '../../../../../common/ys'
import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { BackendActionsFeature } from '../feature/backend-actions-feature'
import { FolderName } from '../feature/folder-list-features'

export class BackendCreateFolderAction implements MBTAction {
  public constructor(protected folder: FolderName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return 'BackendCreateFolderAction(${this.folder})'
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    BackendActionsFeature.get.forceCast(model).addFolder(this.folder)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return true
  }

  public tostring(): string {
    return `BackendCreateFolderAction(${this.folder})`
  }
}

export class BackendCreateLabelAction implements MBTAction {
  public constructor(protected label: LabelData) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return 'BackendCreateLabelAction'
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    BackendActionsFeature.get.forceCast(model).addLabel(this.label)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return true
  }

  public tostring(): string {
    return `BackendCreateLabelAction(${this.label})`
  }
}
