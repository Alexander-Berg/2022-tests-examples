import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { Throwing } from '../../../../../../common/ys'
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
import { AccountType2 } from '../../../../../testopithecus-common/code/mbt/test/mbt-test'
import { UserAccount } from '../../../../../testopithecus-common/code/users/user-pool'
import { ReloginComponent } from '../../components/login-component'
import { MaillistComponent } from '../../components/maillist-component'
import {
  AccountsListFeature,
  CustomMailServiceLogin,
  CustomMailServiceLoginFeature,
  ExpiringTokenFeature,
  GoogleLogin,
  GoogleLoginFeature,
  HotmailLogin,
  HotmailLoginFeature,
  MailRuLogin,
  MailRuLoginFeature,
  OutlookLogin,
  OutlookLoginFeature,
  RamblerLogin,
  RamblerLoginFeature,
  YahooLogin,
  YahooLoginFeature,
  YandexLogin,
  YandexLoginFeature,
  YandexTeamLogin,
  YandexTeamLoginFeature,
} from '../../feature/login-features'

export abstract class LoginAction<T> implements MBTAction {
  protected constructor(protected account: UserAccount, protected feature: Feature<T>) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return this.feature.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public events(): EventusEvent[] {
    return [Eventus.startEvents.startWithMessageListShow()]
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    await this.performImpl(this.feature.forceCast(model))
    await this.performImpl(this.feature.forceCast(application))
    return new MaillistComponent()
  }

  public tostring(): string {
    return `${this.getActionType()}(login=${this.account.login}, password=${this.account.password})`
  }

  public abstract getActionType(): string

  public abstract async performImpl(modelOrApplication: T): Throwing<Promise<void>>
}

export class YandexLoginAction extends LoginAction<YandexLogin> {
  public static readonly type: string = 'YandexLogin'

  public constructor(account: UserAccount) {
    super(account, YandexLoginFeature.get)
  }

  public async performImpl(modelOrApplication: YandexLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithYandexAccount(this.account)
  }

  public getActionType(): string {
    return YandexLoginAction.type
  }
}

export class YandexTeamLoginAction extends LoginAction<YandexTeamLogin> {
  public static readonly type: string = 'YandexTeamLogin'

  public constructor(account: UserAccount) {
    super(account, YandexTeamLoginFeature.get)
  }

  public async performImpl(modelOrApplication: YandexTeamLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithYandexTeamAccount(this.account)
  }

  public getActionType(): string {
    return YandexTeamLoginAction.type
  }
}

export class MailRuLoginAction extends LoginAction<MailRuLogin> {
  public static readonly type: string = 'MailRuLogin'

  public constructor(account: UserAccount) {
    super(account, MailRuLoginFeature.get)
  }

  public async performImpl(modelOrApplication: MailRuLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithMailRuAccount(this.account)
  }

  public getActionType(): string {
    return MailRuLoginAction.type
  }
}

export class GoogleLoginAction extends LoginAction<GoogleLogin> {
  public static readonly type: string = 'GoogleLogin'

  public constructor(account: UserAccount) {
    super(account, GoogleLoginFeature.get)
  }

  public async performImpl(modelOrApplication: GoogleLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithGoogleAccount(this.account)
  }

  public getActionType(): string {
    return GoogleLoginAction.type
  }
}

export class OutlookLoginAction extends LoginAction<OutlookLogin> {
  public static readonly type: string = 'OutlookLogin'

  public constructor(account: UserAccount) {
    super(account, OutlookLoginFeature.get)
  }

  public async performImpl(modelOrApplication: OutlookLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithOutlookAccount(this.account)
  }

  public getActionType(): string {
    return OutlookLoginAction.type
  }
}

export class HotmailLoginAction extends LoginAction<HotmailLogin> {
  public static readonly type: string = 'HotmailLogin'

  public constructor(account: UserAccount) {
    super(account, HotmailLoginFeature.get)
  }

  public async performImpl(modelOrApplication: HotmailLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithHotmailAccount(this.account)
  }

  public getActionType(): string {
    return HotmailLoginAction.type
  }
}

export class RamblerlLoginAction extends LoginAction<RamblerLogin> {
  public static readonly type: string = 'RamblerLogin'

  public constructor(account: UserAccount) {
    super(account, RamblerLoginFeature.get)
  }

  public async performImpl(modelOrApplication: RamblerLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithRamblerAccount(this.account)
  }

  public getActionType(): string {
    return RamblerlLoginAction.type
  }
}

export class YahooLoginAction extends LoginAction<YahooLogin> {
  public static readonly type: string = 'YahooLogin'

  public constructor(account: UserAccount) {
    super(account, YahooLoginFeature.get)
  }

  public async performImpl(modelOrApplication: YahooLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithYahooAccount(this.account)
  }

  public getActionType(): string {
    return YandexLoginAction.type
  }
}

export class CustomMailServiceLoginAction extends LoginAction<CustomMailServiceLogin> {
  public static readonly type: string = 'CustomMailServiceLogin'

  public constructor(account: UserAccount) {
    super(account, CustomMailServiceLoginFeature.get)
  }

  public async performImpl(modelOrApplication: CustomMailServiceLogin): Throwing<Promise<void>> {
    await modelOrApplication.loginWithCustomMailServiceAccount(this.account)
  }

  public getActionType(): string {
    return CustomMailServiceLoginAction.type
  }
}

export function loginAction(account: UserAccount, accountType: AccountType2): MBTAction {
  switch (accountType) {
    case AccountType2.Yandex:
      return new YandexLoginAction(account)
    case AccountType2.YandexTeam:
      return new YandexTeamLoginAction(account)
    case AccountType2.Google:
      return new GoogleLoginAction(account)
    case AccountType2.Hotmail:
      return new HotmailLoginAction(account)
    case AccountType2.Mail:
      return new MailRuLoginAction(account)
    case AccountType2.Outlook:
      return new OutlookLoginAction(account)
    case AccountType2.Rambler:
      return new RamblerlLoginAction(account)
    case AccountType2.Yahoo:
      return new YahooLoginAction(account)
    case AccountType2.Other:
      return new CustomMailServiceLoginAction(account)
    default:
      throw new Error('Unsupported account type: ' + accountType.toString())
  }
}

export class ChoseAccountFromAccountsListAction implements MBTAction {
  public static readonly type: string = 'ChoseAccountFromAccountsList'

  public constructor(private account: UserAccount) {}

  public events(): EventusEvent[] {
    return []
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountsListFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AccountsListFeature.get.forceCast(model).choseAccountFromAccountsList(this.account)
    AccountsListFeature.get.forceCast(application).choseAccountFromAccountsList(this.account)
    return new MaillistComponent()
  }

  public tostring(): string {
    return 'Chose account from accounts list'
  }

  public getActionType(): MBTActionType {
    return ChoseAccountFromAccountsListAction.type
  }
}

export class RevokeTokenForAccount implements MBTAction {
  public static readonly type: string = 'RevokeTokenForAccount'

  public constructor(private account: UserAccount) {}

  public events(): EventusEvent[] {
    return []
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountsListFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ExpiringTokenFeature.get.forceCast(model).revokeToken(this.account)
    ExpiringTokenFeature.get.forceCast(application).revokeToken(this.account)
    return new ReloginComponent()
  }

  public tostring(): string {
    return 'RevokeTokenForAccount'
  }

  public getActionType(): MBTActionType {
    return RevokeTokenForAccount.type
  }
}

export class ExitReloginWindowAction implements MBTAction {
  public static readonly type: string = 'ExitReloginWindow'

  public constructor() {}

  public events(): EventusEvent[] {
    return []
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AccountsListFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(_model: App): boolean {
    // TODO Исправить при дополнении модели
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ExpiringTokenFeature.get.forceCast(model).exitFromReloginWindow()
    ExpiringTokenFeature.get.forceCast(application).exitFromReloginWindow()
    return new MaillistComponent()
  }

  public tostring(): string {
    return 'ExitReloginWindow'
  }

  public getActionType(): MBTActionType {
    return ExitReloginWindowAction.type
  }
}
