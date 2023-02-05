import { Logger } from '../../../common/code/logging/logger'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFolderAction } from '../mail/actions/left-column/folder-navigator-actions'
import { ShortSwipeContextMenuMoveToFolderAction } from '../mail/actions/messages-list/context-menu-actions'
import { LoginComponent } from '../mail/components/login-component'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { allActionsBehaviour } from '../mail/walk/mail-full-user-behaviour'
import { FullCoverageBaseTest } from '../../../testopithecus-common/code/mbt/test/base-user-behaviour-test'
import { AccountType2 } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { UserBehaviour } from '../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { ActionLimitsStrategy } from '../../../testopithecus-common/code/mbt/walk/limits/action-limits-strategy'
import { PersonalActionLimits } from '../../../testopithecus-common/code/mbt/walk/limits/personal-action-limits'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'

export class NoComposeFullCoverageTest extends FullCoverageBaseTest<MailboxBuilder> {
  public constructor(logger: Logger) {
    super('should cover all application without compose', new LoginComponent(), logger)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('subj')
  }

  public getUserBehaviour(userAccounts: UserAccount[]): UserBehaviour {
    return allActionsBehaviour(userAccounts).blacklist(RotateToLandscape.type)
  }

  public getActionLimits(): ActionLimitsStrategy {
    return new PersonalActionLimits(15)
      .setLimit(GoToFolderAction.type, 1)
      .setLimit(ShortSwipeContextMenuMoveToFolderAction.type, 1)
  }
}
