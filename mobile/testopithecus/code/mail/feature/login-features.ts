import { Int32, Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { UserAccount } from '../../../../testopithecus-common/code/users/user-pool'

export type Login = string

export class MultiAccountFeature extends Feature<MultiAccount> {
  public static get: MultiAccountFeature = new MultiAccountFeature()

  private constructor() {
    super('MultiAccount', 'Фича переключения между аккаунтами, логина и разлогина')
  }
}

export interface MultiAccount {
  switchToAccount(login: Login): Throwing<void>

  logoutFromAccount(login: Login): Throwing<void>

  getCurrentAccount(): Throwing<Login>

  getNumberOfAccounts(): Throwing<Int32>

  addNewAccount(): Throwing<void>

  getLoggedInAccountsList(): Throwing<Login[]>
}

export class AccountsListFeature extends Feature<AccountsList> {
  public static get: AccountsListFeature = new AccountsListFeature()

  private constructor() {
    super('AccountsList', 'Список аккаунтов на экране с каруселью аккаунтов АМа')
  }
}

export interface AccountsList {
  choseAccountFromAccountsList(account: UserAccount): Throwing<void>

  getAccountsList(): Throwing<UserAccount[]>
}

export class ExpiringTokenFeature extends Feature<ExpiringToken> {
  public static get: ExpiringTokenFeature = new ExpiringTokenFeature()

  private constructor() {
    super('ExpiringToken', 'Фича, позволяющая инвалидировать токен yandex-аккаунта')
  }
}

export interface ExpiringToken {
  // TODO модель необходимо дописать. Не хватает методов: логина через форму релогина, переключение аккаунтов из формы релогина, удаление аккаунта из формы релогина
  revokeToken(account: UserAccount): Throwing<void>

  exitFromReloginWindow(): Throwing<void>
}

export class YandexLoginFeature extends Feature<YandexLogin> {
  public static get: YandexLoginFeature = new YandexLoginFeature()

  private constructor() {
    super('YandexLogin', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface YandexLogin {
  loginWithYandexAccount(account: UserAccount): Throwing<Promise<void>>
}

export class YandexTeamLoginFeature extends Feature<YandexTeamLogin> {
  public static get: YandexTeamLoginFeature = new YandexTeamLoginFeature()

  private constructor() {
    super('YandexTeamLogin', 'Залогин yandex-team аккаунтом. Ввод логина, пароля и переход к списку писем.')
  }
}

export interface YandexTeamLogin {
  loginWithYandexTeamAccount(account: UserAccount): Throwing<Promise<void>>
}

export class MailRuLoginFeature extends Feature<MailRuLogin> {
  public static get: MailRuLoginFeature = new MailRuLoginFeature()

  private constructor() {
    super('MailRuLogin', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface MailRuLogin {
  loginWithMailRuAccount(account: UserAccount): Throwing<Promise<void>>
}

export class GoogleLoginFeature extends Feature<GoogleLogin> {
  public static get: GoogleLoginFeature = new GoogleLoginFeature()

  private constructor() {
    super('GoogleLogin', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface GoogleLogin {
  loginWithGoogleAccount(account: UserAccount): Throwing<Promise<void>>
}

export class OutlookLoginFeature extends Feature<OutlookLogin> {
  public static get: OutlookLoginFeature = new OutlookLoginFeature()

  private constructor() {
    super('OutlookLogin', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface OutlookLogin {
  loginWithOutlookAccount(account: UserAccount): Throwing<Promise<void>>
}

export class HotmailLoginFeature extends Feature<HotmailLogin> {
  public static get: HotmailLoginFeature = new HotmailLoginFeature()

  private constructor() {
    super('HotmailLogin', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface HotmailLogin {
  loginWithHotmailAccount(account: UserAccount): Throwing<Promise<void>>
}

export class RamblerLoginFeature extends Feature<RamblerLogin> {
  public static get: RamblerLoginFeature = new RamblerLoginFeature()

  private constructor() {
    super('RamblerLogin', 'TODO: добрый человек, напиши тут описание этой фичи')
  }
}

export interface RamblerLogin {
  loginWithRamblerAccount(account: UserAccount): Throwing<Promise<void>>
}

export class YahooLoginFeature extends Feature<YahooLogin> {
  public static get: YahooLoginFeature = new YahooLoginFeature()

  private constructor() {
    super('YahooLogin', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface YahooLogin {
  loginWithYahooAccount(account: UserAccount): Throwing<Promise<void>>
}

export class CustomMailServiceLoginFeature extends Feature<CustomMailServiceLogin> {
  public static get: CustomMailServiceLoginFeature = new CustomMailServiceLoginFeature()

  private constructor() {
    super('CustomMailService', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface CustomMailServiceLogin {
  loginWithCustomMailServiceAccount(account: UserAccount): Throwing<Promise<void>>
}
