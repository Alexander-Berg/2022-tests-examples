import { Logger } from '../../../common/code/logging/logger'
import { Int32, range, TypeSupport } from '../../../../common/ys'
import { ClearCacheAction } from '../mail/actions/settings/general-settings-actions'
import { OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { LoginComponent } from '../mail/components/login-component'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { allActionsBehaviour } from '../mail/walk/mail-full-user-behaviour'
import { BaseUserBehaviourTest } from '../../../testopithecus-common/code/mbt/test/base-user-behaviour-test'
import { AccountType2 } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { UserBehaviour } from '../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'

export class RandomWalkTest extends BaseUserBehaviourTest<MailboxBuilder> {
  public constructor(pathLength: Int32, logger: Logger, seed: Int32) {
    super(`random walk for ${pathLength} steps with seed ${seed}`, new LoginComponent(), pathLength, logger, seed)
  }

  public static generate(count: Int32, logger: Logger): RandomWalkTest[] {
    const tests: RandomWalkTest[] = []
    for (const i of range(0, count)) {
      tests.push(new RandomWalkTest(5, logger, TypeSupport.asInt32(i)!))
    }
    return tests
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('subj')
  }

  public getUserBehaviour(accounts: UserAccount[]): UserBehaviour {
    return allActionsBehaviour(accounts).blacklist(ClearCacheAction.type).blacklist(OpenSettingsAction.type)
  }
}
