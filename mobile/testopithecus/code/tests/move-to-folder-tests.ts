import { ShortSwipeContextMenuMoveToFolderAction } from '../mail/actions/messages-list/context-menu-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class MoveToFolderTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should move messages to folders')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.spam))
  }
}
