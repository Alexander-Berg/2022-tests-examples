import { Throwing } from '../../../../../common/ys'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { UserAccount } from '../../../../testopithecus-common/code/users/user-pool'
import {
  CustomMailServiceLogin,
  GoogleLogin,
  HotmailLogin,
  MailRuLogin,
  OutlookLogin,
  RamblerLogin,
  YahooLogin,
  YandexLogin,
  YandexTeamLogin,
} from '../feature/login-features'

export class LoginBackend
  implements
    YandexLogin,
    YandexTeamLogin,
    MailRuLogin,
    GoogleLogin,
    OutlookLogin,
    HotmailLogin,
    RamblerLogin,
    YahooLogin,
    CustomMailServiceLogin {
  public constructor(private clientsHandler: MailboxClientHandler) {}

  public async loginWithYandexAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithYandexTeamAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithMailRuAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithCustomMailServiceAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithGoogleAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithHotmailAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithOutlookAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithRamblerAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithYahooAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  private login(account: UserAccount): void {
    this.clientsHandler.loginToAccount(account)
  }
}
