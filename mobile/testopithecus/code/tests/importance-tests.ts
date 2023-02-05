import { MarkAsImportant, MarkAsUnimportant } from '../mail/actions/base-actions/labeled-actions'
import { ExpandThreadAction } from '../mail/actions/messages-list/thread-markable-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class MarkAsImportantTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should label as unimportant after labelling as important')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MarkAsImportant(0)).then(new MarkAsUnimportant(0))
  }
}

export class LabelAllThreadMessagesImportantByLabellingMainMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should label all thread messages important by labelling main message')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj').nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MarkAsImportant(0)).then(new ExpandThreadAction(0))
  }
}
