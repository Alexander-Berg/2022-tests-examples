import { Nullable } from '../../../../common/ys'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTTest } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { YandexLoginAction } from '../mail/actions/login/login-actions'
import { AddNewAccountAction, LogoutFromAccountAction } from '../mail/actions/login/multi-account-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'

export class LogoutTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('should logout from yandex account after login')
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex, AccountType2.Yandex]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('firstAccountMsg')
    mailboxes[1].nextMessage('secondAccountMsg')
  }

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new OpenFolderListAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new OpenFolderListAction())
      .then(new LogoutFromAccountAction(accounts[0]))
  }
}
