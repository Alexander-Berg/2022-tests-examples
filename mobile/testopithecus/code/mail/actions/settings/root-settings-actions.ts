import { Throwing } from '../../../../../../common/ys'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { FolderListComponent } from '../../components/folder-list-component'
import { RootSettingsComponent } from '../../components/settings/root-settings-component'
import { RootSettingsFeature } from '../../feature/settings/root-settings-feature'

export class OpenSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenRootSettings'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return RootSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    RootSettingsFeature.get.forceCast(model).openRootSettings()
    RootSettingsFeature.get.forceCast(application).openRootSettings()
    return new RootSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
  public tostring(): string {
    return 'OpenRootSettings'
  }

  public getActionType(): string {
    return OpenSettingsAction.type
  }
}

export class CloseRootSettings implements MBTAction {
  public static readonly type: MBTActionType = 'CloseRootSettings'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return RootSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    RootSettingsFeature.get.forceCast(model).closeRootSettings()
    RootSettingsFeature.get.forceCast(application).closeRootSettings()
    return new FolderListComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseRootSettings'
  }

  public getActionType(): string {
    return CloseRootSettings.type
  }
}
