import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import { Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { GeneralSettingsComponent } from '../../components/settings/general-settings-component'
import { RootSettingsComponent } from '../../components/settings/root-settings-component'
import {
  ActionOnSwipe,
  AndroidGeneralSettingsFeature,
  GeneralSettings,
  GeneralSettingsFeature,
} from '../../feature/settings/general-settings-feature'

export class ClearCacheAction implements MBTAction {
  public static readonly type: MBTActionType = 'ClearCache'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GeneralSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GeneralSettingsFeature.get.forceCast(model).clearCache()
    GeneralSettingsFeature.get.forceCast(application).clearCache()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return [Eventus.settingsEvents.clearCache()]
  }

  public tostring(): string {
    return 'ClearCache'
  }

  public getActionType(): string {
    return ClearCacheAction.type
  }
}

export class OpenGeneralSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenGeneralSettings'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GeneralSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GeneralSettingsFeature.get.forceCast(model).openGeneralSettings()
    GeneralSettingsFeature.get.forceCast(application).openGeneralSettings()
    return new GeneralSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenGeneralSettings'
  }

  public getActionType(): string {
    return OpenGeneralSettingsAction.type
  }
}

export class CloseGeneralSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseGeneralSettings'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GeneralSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GeneralSettingsFeature.get.forceCast(model).closeGeneralSettings()
    GeneralSettingsFeature.get.forceCast(application).closeGeneralSettings()
    return new RootSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return CloseGeneralSettingsAction.type
  }

  public tostring(): string {
    return 'CloseGeneralSettings'
  }
}

export class SetActionOnSwipe implements MBTAction {
  public static readonly type: MBTActionType = 'SetActionOnSwipe'

  public constructor(private action: ActionOnSwipe) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GeneralSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GeneralSettingsFeature.get.forceCast(model).setActionOnSwipe(this.action)
    GeneralSettingsFeature.get.forceCast(application).setActionOnSwipe(this.action)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `Set ${this.action} action on swipe`
  }

  public getActionType(): string {
    return SetActionOnSwipe.type
  }
}

export class TurnOnCompactMode implements MBTAction {
  public static readonly type: MBTActionType = 'TurnOnCompactMode'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GeneralSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isCompactModeEnabled = GeneralSettingsFeature.get.forceCast(model).isCompactModeEnabled()
    return !isCompactModeEnabled
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GeneralSettingsFeature.get.forceCast(model).switchCompactMode()
    GeneralSettingsFeature.get.forceCast(application).switchCompactMode()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return [Eventus.settingsEvents.toggleCompactMode(true)]
  }

  public tostring(): string {
    return `Turn on compact mode`
  }

  public getActionType(): string {
    return TurnOnCompactMode.type
  }
}

export class TurnOffCompactMode implements MBTAction {
  public static readonly type: MBTActionType = 'TurnOffCompactMode'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GeneralSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return GeneralSettingsFeature.get.forceCast(model).isCompactModeEnabled()
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    GeneralSettingsFeature.get.forceCast(model).switchCompactMode()
    GeneralSettingsFeature.get.forceCast(application).switchCompactMode()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return [Eventus.settingsEvents.toggleCompactMode(false)]
  }

  public tostring(): string {
    return `Turn off compact mode`
  }

  public getActionType(): string {
    return TurnOffCompactMode.type
  }
}

export class TurnOnSmartReplyAction extends BaseSimpleAction<GeneralSettings, MBTComponent> {
  public static readonly type: MBTActionType = 'TurnOnSmartReplyAction'

  public constructor() {
    super(TurnOnSmartReplyAction.type)
  }

  public requiredFeature(): Feature<GeneralSettings> {
    return GeneralSettingsFeature.get
  }

  public canBePerformedImpl(model: GeneralSettings): Throwing<boolean> {
    const isSmartRepliesEnabled = model.isSmartRepliesEnabled()
    return !isSmartRepliesEnabled
  }

  public performImpl(modelOrApplication: GeneralSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.switchSmartReplies()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TurnOnSmartReplyAction'
  }
}

export class TurnOffSmartReplyAction extends BaseSimpleAction<GeneralSettings, MBTComponent> {
  public static readonly type: MBTActionType = 'TurnOffSmartReplyAction'

  public constructor() {
    super(TurnOffSmartReplyAction.type)
  }

  public requiredFeature(): Feature<GeneralSettings> {
    return GeneralSettingsFeature.get
  }

  public canBePerformedImpl(model: GeneralSettings): Throwing<boolean> {
    return model.isSmartRepliesEnabled()
  }

  public performImpl(modelOrApplication: GeneralSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.switchSmartReplies()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TurnOffSmartReplyAction'
  }
}

export class TapToClearCacheAndCancelAction implements MBTAction {
  public static readonly type: MBTActionType = 'tapToClearCacheAndCancel'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return GeneralSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AndroidGeneralSettingsFeature.get.forceCast(model).tapToClearCacheAndCancel()
    AndroidGeneralSettingsFeature.get.forceCast(application).tapToClearCacheAndCancel()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'tapToClearCacheAndCancel'
  }

  public getActionType(): string {
    return TapToClearCacheAndCancelAction.type
  }
}
