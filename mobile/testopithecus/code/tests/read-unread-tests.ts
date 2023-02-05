import { MarkAsRead, MarkAsUnread } from '../mail/actions/base-actions/markable-actions'
import { ExpandThreadAction } from '../mail/actions/messages-list/thread-markable-actions'
import { MessageViewBackToMailListAction, OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class MarkUnreadAfterReadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should able to mark unread after read')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MarkAsRead(0)).then(new MarkAsUnread(0))
  }
}

export class ReadMessageAfterOpeningTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should read message after opening')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewBackToMailListAction())
      .then(new MarkAsUnread(0))
  }
}

export class MarkAllThreadMessagesReadByMarkingMainMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should mark all thread messages read by marking main message')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj').nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MarkAsRead(0)).then(new ExpandThreadAction(0))
  }
}
