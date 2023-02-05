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
import { AboutSettingsComponent } from '../../components/settings/about-settings-component'
import { RootSettingsComponent } from '../../components/settings/root-settings-component'
import { AboutSettingsFeature } from '../../feature/settings/about-settings-feature'

export class OpenAboutSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenAboutSettings'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AboutSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AboutSettingsFeature.get.forceCast(model).openAboutSettings()
    AboutSettingsFeature.get.forceCast(application).openAboutSettings()
    return new AboutSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenAboutSettings'
  }

  public getActionType(): string {
    return OpenAboutSettingsAction.type
  }
}

export class CloseAboutSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseAboutSettings'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AboutSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AboutSettingsFeature.get.forceCast(model).closeAboutSettings()
    AboutSettingsFeature.get.forceCast(application).closeAboutSettings()
    return new RootSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseAboutSettings'
  }

  public getActionType(): string {
    return CloseAboutSettingsAction.type
  }
}
