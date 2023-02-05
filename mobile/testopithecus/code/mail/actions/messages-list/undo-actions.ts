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
import { UndoFeature, UndoState } from '../../feature/message-list/undo-feature'

export class UndoDeleteAction implements MBTAction {
  public static readonly type: MBTActionType = 'UndoDeleteAction'
  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return UndoFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const undoModel = UndoFeature.get.forceCast(model)
    return undoModel.isUndoDeleteToastShown() !== UndoState.notShown
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    UndoFeature.get.forceCast(model).undoDelete()
    UndoFeature.get.forceCast(application).undoDelete()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return UndoDeleteAction.type
  }

  public tostring(): string {
    return 'UndoDeleteMessage'
  }
}

export class UndoArchiveAction implements MBTAction {
  public static readonly type: MBTActionType = 'UndoArchiveAction'
  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return UndoFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const undoModel = UndoFeature.get.forceCast(model)
    return undoModel.isUndoArchiveToastShown() !== UndoState.notShown
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    UndoFeature.get.forceCast(model).undoArchive()
    UndoFeature.get.forceCast(application).undoArchive()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return UndoArchiveAction.type
  }

  public tostring(): string {
    return 'UndoArchiveMessage'
  }
}

export class UndoSpamAction implements MBTAction {
  public static readonly type: MBTActionType = 'UndoSpamAction'
  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return UndoFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const undoModel = UndoFeature.get.forceCast(model)
    return undoModel.isUndoSpamToastShown() !== UndoState.notShown
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    UndoFeature.get.forceCast(model).undoSpam()
    UndoFeature.get.forceCast(application).undoSpam()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return UndoSpamAction.type
  }

  public tostring(): string {
    return 'UndoSpamMessage'
  }
}
