import { Throwing } from '../../../../../../common/ys'
import { UserAccount } from '../../../../../testopithecus-common/code/users/user-pool'
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
} from '../../feature/login-features'
import { MessageContainer, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'

export class LoginModel
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
  public constructor(
    public accountDataHandler: MailAppModelHandler,
    private messageListDisplay: MessageListDisplayModel,
  ) {}

  public async loginWithYandexAccount(account: UserAccount): Throwing<Promise<void>> {
    this.login(account)
  }

  public async loginWithYandexTeamAccount(account: UserAccount): Throwing<Promise<void>> {
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

  public async loginWithMailRuAccount(account: UserAccount): Throwing<Promise<void>> {
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
    this.accountDataHandler.logInToAccount(account)
    this.messageListDisplay.setCurrentContainer(
      new MessageContainer(DefaultFolderName.inbox, MessageContainerType.folder),
    )
  }
}
