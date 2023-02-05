import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import { AccountType } from '../../../../../mapi/code/api/entities/account/account-type'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { Int32, Throwing } from '../../../../../../common/ys'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTComponentType,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountSettingsComponent } from '../../components/settings/account-settings-component'
import { FilterListComponent } from '../../components/settings/filter-list-component'
import { RootSettingsComponent } from '../../components/settings/root-settings-component'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { AccountSettings, AccountSettingsFeature } from '../../feature/settings/account-settings-feature'
import { TabsFeature } from '../../feature/tabs-feature'

export class OpenAccountSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenAccountSettings'

  public constructor(public accountIndex: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AccountSettingsFeature.get.forceCast(model).openAccountSettings(this.accountIndex)
    AccountSettingsFeature.get.forceCast(application).openAccountSettings(this.accountIndex)
    return new AccountSettingsComponent()
  }

  public events(): EventusEvent[] {
    return [Eventus.accountSettingsEvents.openAccountSettings()]
  }

  public getActionType(): string {
    return OpenAccountSettingsAction.type
  }

  public tostring(): string {
    return 'OpenAccountSettings'
  }
}

export class CloseAccountSettingsAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseAccountSettings'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AccountSettingsFeature.get.forceCast(model).closeAccountSettings()
    AccountSettingsFeature.get.forceCast(application).closeAccountSettings()
    return new RootSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return CloseAccountSettingsAction.type
  }

  public tostring(): string {
    return 'CloseAccountSettings'
  }
}

export class ChangeSignatureAction implements MBTAction {
  public static readonly type: MBTActionType = 'ChangeSignature'

  public constructor(private newSignature: string) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AccountSettingsFeature.get.forceCast(model).changeSignature(this.newSignature)
    AccountSettingsFeature.get.forceCast(application).changeSignature(this.newSignature)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return ChangeSignatureAction.type
  }

  public tostring(): string {
    return this.getActionType()
  }
}

export abstract class BaseGroupBySubjectAction implements MBTAction {
  protected constructor(private type: MBTActionType) {}

  public abstract canBePerformed(model: App): Throwing<boolean>

  public abstract events(): EventusEvent[]

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(model)
    this.performImpl(application)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountSettingsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public abstract performImpl(modelOrApplication: App): Throwing<void>

  public getActionType(): MBTComponentType {
    return this.type
  }

  public tostring(): string {
    return this.getActionType()
  }
}

export class SwitchOnThreadingAction extends BaseGroupBySubjectAction {
  public static readonly type: MBTActionType = 'SwitchOnThreading'

  public constructor() {
    super(SwitchOnThreadingAction.type)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isGroupBySubjectEnabled = AccountSettingsFeature.get.forceCast(model).isGroupBySubjectEnabled()
    return !isGroupBySubjectEnabled
  }

  public performImpl(modelOrApplication: App): Throwing<void> {
    const modelOrAppImpl = AccountSettingsFeature.get.forceCast(modelOrApplication)
    modelOrAppImpl.switchGroupBySubject()
  }

  public events(): EventusEvent[] {
    return [Eventus.accountSettingsEvents.toggleThreading(true)]
  }
}

export class SwitchOffThreadingAction extends BaseGroupBySubjectAction {
  public static readonly type: MBTActionType = 'SwitchOffThreading'

  public constructor() {
    super(SwitchOffThreadingAction.type)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isGroupBySubjectEnabled = AccountSettingsFeature.get.forceCast(model).isGroupBySubjectEnabled()
    return isGroupBySubjectEnabled
  }

  public performImpl(modelOrApplication: App): Throwing<void> {
    const modelOrAppImpl = AccountSettingsFeature.get.forceCast(modelOrApplication)
    modelOrAppImpl.switchGroupBySubject()
  }

  public events(): EventusEvent[] {
    return [Eventus.accountSettingsEvents.toggleThreading(false)]
  }
}

export abstract class BaseTabsActions implements MBTAction {
  protected constructor(protected accountType: AccountType) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      MessageListDisplayFeature.get.included(modelFeatures) &&
      TabsFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    let isAccountTypeYandex = false
    if (this.accountType === AccountType.login) {
      isAccountTypeYandex = true
    }
    const canPerform = this.canBePerformedImpl(model)
    return isAccountTypeYandex && canPerform
  }
  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(model)
    this.performImpl(application)
    return history.currentComponent
  }

  public abstract events(): EventusEvent[]

  public abstract canBePerformedImpl(model: App): Throwing<boolean>

  public abstract performImpl(modelOrApplication: App): Throwing<void>

  public abstract tostring(): string

  public abstract getActionType(): MBTActionType
}

export class SwitchOnTabsAction extends BaseTabsActions {
  public static readonly type: MBTActionType = 'SwitchOnTabsAction'

  public constructor(accountType: AccountType) {
    super(accountType)
  }

  public canBePerformedImpl(model: App): Throwing<boolean> {
    const isSortingEmailsByCategoryEnabled = AccountSettingsFeature.get
      .forceCast(model)
      .isSortingEmailsByCategoryEnabled()
    return !isSortingEmailsByCategoryEnabled
  }

  public performImpl(modelOrApplication: App): Throwing<void> {
    AccountSettingsFeature.get.forceCast(modelOrApplication).switchSortingEmailsByCategory()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `SwitchOnTabsAction`
  }

  public getActionType(): string {
    return SwitchOnTabsAction.type
  }
}

export class SwitchOffTabsAction extends BaseTabsActions {
  public static readonly type: MBTActionType = 'SwitchOffTabsAction'

  public constructor(accountType: AccountType) {
    super(accountType)
  }

  public canBePerformedImpl(model: App): Throwing<boolean> {
    const isSortingEmailsByCategoryEnabled = AccountSettingsFeature.get
      .forceCast(model)
      .isSortingEmailsByCategoryEnabled()
    return isSortingEmailsByCategoryEnabled
  }

  public performImpl(modelOrApplication: App): Throwing<void> {
    AccountSettingsFeature.get.forceCast(modelOrApplication).switchSortingEmailsByCategory()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `SwitchOffTabsAction`
  }

  public getActionType(): string {
    return SwitchOffTabsAction.type
  }
}

export class GetThreadingSetting {
  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public performImpl(modelOrApplication: App): Throwing<void> {
    const modelOrAppImpl = AccountSettingsFeature.get.forceCast(modelOrApplication)
    modelOrAppImpl.isGroupBySubjectEnabled()
  }

  public tostring(): string {
    return 'GetThreadingSetting'
  }
}

export class AccountSettingsOpenFiltersAction extends BaseSimpleAction<AccountSettings, MBTComponent> {
  public static readonly type: MBTActionType = 'AccountSettingsOpenFiltersAction'

  public constructor() {
    super(AccountSettingsOpenFiltersAction.type)
  }

  public requiredFeature(): Feature<AccountSettings> {
    return AccountSettingsFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: AccountSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.openFilters()
    return new FilterListComponent()
  }
}
