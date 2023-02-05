import { Int32, Throwing } from '../../../../../common/ys'
import { MailboxClientHandler } from '../../client/mailbox-client'
import { Login, MultiAccount } from '../feature/login-features'

export class MultiAccountBackend implements MultiAccount {
  public constructor(private clientsHandler: MailboxClientHandler) {}

  public switchToAccount(login: Login): Throwing<void> {
    this.clientsHandler.switchToClientForAccountWithLogin(login)
  }

  public logoutFromAccount(login: Login): Throwing<void> {
    this.clientsHandler.getLoggedInAccounts().filter((account) => account.login !== login)
  }

  public getCurrentAccount(): Throwing<Login> {
    return this.clientsHandler.getCurrentClient().oauthAccount.account.login
  }

  public getNumberOfAccounts(): Throwing<Int32> {
    return this.getLoggedInAccountsList().length
  }

  public getLoggedInAccountsList(): Throwing<Login[]> {
    return this.clientsHandler.getLoggedInAccounts().map((acc) => acc.login)
  }

  public addNewAccount(): Throwing<void> {
    // на бекенде ничего не делаем
  }
}
