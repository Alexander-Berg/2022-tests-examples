import { Throwing } from '../../../../../../common/ys'
import { UserAccount } from '../../../../../testopithecus-common/code/users/user-pool'
import { AccountsList } from '../../feature/login-features'
import { MailAppModelHandler } from '../mail-model'

export class AccountManagerModel implements AccountsList {
  public constructor(private accountDataHandler: MailAppModelHandler) {}

  public choseAccountFromAccountsList(account: UserAccount): Throwing<void> {
    this.accountDataHandler.choseAccountFromAccountsManager(account)
  }

  public getAccountsList(): Throwing<UserAccount[]> {
    return this.accountDataHandler.getLoggedInAccounts()
  }
}
