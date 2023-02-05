import { int64 } from '../../../../../../common/ys'
import { Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { UserAccount } from '../../../../../testopithecus-common/code/users/user-pool'
import { FolderListComponent } from '../../components/folder-list-component'
import { LoginComponent } from '../../components/login-component'
import { MaillistComponent } from '../../components/maillist-component'
import { MultiAccountFeature } from '../../feature/login-features'

export abstract class MultiAccountAction implements MBTAction {
  protected constructor() {}

  public abstract events(): EventusEvent[]

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MultiAccountFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public abstract perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>>

  public abstract canBePerformed(model: App): Throwing<boolean>

  public abstract tostring(): string

  public abstract getActionType(): string
}

export class SwitchAccountAction extends MultiAccountAction {
  public static readonly type: MBTActionType = 'SwitchAccount'

  public constructor(private account: UserAccount) {
    super()
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return MultiAccountFeature.get.forceCast(model).getLoggedInAccountsList().includes(this.account.login)
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(model)
    this.performImpl(application)
    return new FolderListComponent()
  }

  public tostring(): string {
    return `SwitchAccountAction(login=${this.account.login})`
  }

  public getActionType(): string {
    return SwitchAccountAction.type
  }

  private performImpl(modelOrApplication: App): Throwing<void> {
    MultiAccountFeature.get.forceCast(modelOrApplication).switchToAccount(this.account.login)
  }

  public events(): EventusEvent[] {
    return [Eventus.multiAccountEvents.switchToAccount(int64(-1))]
  }
}

export class AddNewAccountAction extends MultiAccountAction {
  public static readonly type: MBTActionType = 'AddNewAccount'

  public constructor() {
    super()
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(model)
    this.performImpl(application)
    return new LoginComponent()
  }

  public tostring(): string {
    return 'AddNewAccountAction'
  }

  public getActionType(): string {
    return AddNewAccountAction.type
  }

  private performImpl(modelOrApplication: App): Throwing<void> {
    MultiAccountFeature.get.forceCast(modelOrApplication).addNewAccount()
  }

  public events(): EventusEvent[] {
    return [Eventus.multiAccountEvents.addNewAccount()]
  }
}

export class LogoutFromAccountAction extends MultiAccountAction {
  public static readonly type: MBTActionType = 'LogoutFromAccount'

  public constructor(private account: UserAccount) {
    super()
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(model)
    this.performImpl(application)
    return new MaillistComponent()
  }

  public tostring(): string {
    return `LogoutFromAccountAction(login=${this.account.login})`
  }

  public getActionType(): string {
    return LogoutFromAccountAction.type
  }

  private performImpl(modelOrApplication: App): Throwing<void> {
    MultiAccountFeature.get.forceCast(modelOrApplication).logoutFromAccount(this.account.login)
  }

  public events(): EventusEvent[] {
    return [Eventus.multiAccountEvents.logoutFromAccount()]
  }
}

// TODO Зачем нужен отдельный экшн, тем более с таким же конструктором как и у LogoutFromAccountAction?
export class LogoutFromLastAccountAction extends MultiAccountAction {
  public static readonly type: MBTActionType = 'LogoutFromLastAccount'

  public constructor(private account: UserAccount) {
    super()
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(model)
    this.performImpl(application)
    return new LoginComponent()
  }

  public tostring(): string {
    return `LogoutFromAccountAction(login=${this.account.login})`
  }

  public getActionType(): string {
    return LogoutFromAccountAction.type
  }

  private performImpl(modelOrApplication: App): Throwing<void> {
    MultiAccountFeature.get.forceCast(modelOrApplication).logoutFromAccount(this.account.login)
  }

  public events(): EventusEvent[] {
    return []
  }
}
