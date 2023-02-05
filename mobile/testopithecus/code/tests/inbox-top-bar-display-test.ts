import { loginAction } from '../mail/actions/login/login-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { AccountType2, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { RegularTestBase } from '../../../testopithecus-common/code/mbt/test/regular-test-base'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'

export class InboxTopBarDisplayTest extends RegularTestBase<MailboxBuilder> {
  public constructor(type: AccountType2) {
    super(`Отображение топ бара в списке писем Инбокса для аккаунта ${type}`, type)
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6061).iosCase(2)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.nextMessage('subj')
  }

  public regularScenario(account: UserAccount): TestPlan {
    return TestPlan.empty().then(loginAction(account, this.accountType))
  }
}
