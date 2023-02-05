import { MoveToSpamAction } from '../mail/actions/base-actions/spamable-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class MoveToSpamFirstMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('first message should be deleted from inbox if move to spam')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MoveToSpamAction(0)).then(new MoveToSpamAction(0))
  }
}
