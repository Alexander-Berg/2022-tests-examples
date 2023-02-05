import { Throwing } from '../../../../../../common/ys'
import { UserAccount } from '../../../../../testopithecus-common/code/users/user-pool'
import { ExpiringToken } from '../../feature/login-features'
import { MailAppModelHandler } from '../mail-model'

export class ExpiringTokenModel implements ExpiringToken {
  public constructor(private accountDataHandler: MailAppModelHandler) {}

  public revokeToken(account: UserAccount): Throwing<void> {
    this.accountDataHandler.revokeToken(account)
  }

  public exitFromReloginWindow(): Throwing<void> {
    this.accountDataHandler.exitFromReloginWindow()
  }
}
