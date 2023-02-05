import { range } from '../../../../common/ys'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { RegularYandexMailTestBase } from '../tests/abstract-mail-tests'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'

export class LogGeneratedTest extends RegularYandexMailTestBase {
  public constructor(public plan: TestPlan) {
    super('Test was generated from logs')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    for (const i of range(0, 15)) {
      mailbox.nextMessage(`subj${i}`)
    }
  }

  public testScenario(_account: UserAccount): TestPlan {
    return this.plan
  }
}
