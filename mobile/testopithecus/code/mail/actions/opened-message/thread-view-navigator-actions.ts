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
import { ThreadViewNavigatorFeature } from '../../feature/mail-view-features'
import { ContainerGetterFeature } from '../../feature/message-list/container-getter-feature'
import { ActionOnSwipe, GeneralSettingsFeature } from '../../feature/settings/general-settings-feature'
import { DefaultFolderName } from '../../model/folder-data-model'

export class DeleteCurrentThreadAction implements MBTAction {
  private type: MBTActionType = 'DeleteCurrentThreadAction'

  public canBePerformed(model: App): Throwing<boolean> {
    const actionOnSwipe = GeneralSettingsFeature.get.forceCast(model).getActionOnSwipe()
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return actionOnSwipe === ActionOnSwipe.delete || currentContainer.name === DefaultFolderName.archive
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ThreadViewNavigatorFeature.get.forceCast(model).deleteCurrentThread()
    ThreadViewNavigatorFeature.get.forceCast(application).deleteCurrentThread()
    return history.previousDifferentComponent!
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ThreadViewNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return this.type
  }
}

export class ArchiveCurrentThreadAction implements MBTAction {
  private type: MBTActionType = 'ArchiveCurrentThreadAction'

  public canBePerformed(model: App): Throwing<boolean> {
    const actionOnSwipe = GeneralSettingsFeature.get.forceCast(model).getActionOnSwipe()
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return actionOnSwipe === ActionOnSwipe.archive && currentContainer.name !== DefaultFolderName.archive
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ThreadViewNavigatorFeature.get.forceCast(model).archiveCurrentThread()
    ThreadViewNavigatorFeature.get.forceCast(application).archiveCurrentThread()
    return history.previousDifferentComponent!
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ThreadViewNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return this.type
  }
}
