import { Int32, Throwing } from '../../../../../../common/ys'
import { Login, MultiAccount } from '../../feature/login-features'
import { MailAppModelHandler } from '../mail-model'

export class MultiAccountModel implements MultiAccount {
  public constructor(private accountDataHandler: MailAppModelHandler) {}

  public switchToAccount(login: Login): Throwing<void> {
    this.accountDataHandler.switchToAccountByLogin(login)
  }

  public getLoggedInAccountsList(): Throwing<Login[]> {
    return this.accountDataHandler.getLoggedInAccounts().map((acc) => acc.login)
  }

  public logoutFromAccount(login: Login): Throwing<void> {
    return this.accountDataHandler.logoutAccount(login)
  }

  public getCurrentAccount(): Throwing<Login> {
    return this.accountDataHandler.getCurrentAccount().client.oauthAccount.account.login
  }

  public getNumberOfAccounts(): Throwing<Int32> {
    return this.getLoggedInAccountsList().length
  }

  public addNewAccount(): Throwing<void> {
    // В модели ничего не делаем
  }
}
