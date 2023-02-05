import { PseudoRandomProvider } from '../../../testopithecus-common/code/utils/pseudo-random'
import { Fuzzer } from '../../../testopithecus-common/code/fuzzing/fuzzer'
import { ComposeOpenAction } from '../mail/actions/compose/compose-actions'
import { AppendToBody, SetItalic, SetStrong } from '../mail/actions/compose/wysiwyg-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class FormatTextTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should format text in wysiwyg correct')
  }

  public prepareAccount(_mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new AppendToBody(0, new Fuzzer().fuzzyBody(PseudoRandomProvider.INSTANCE, 10)))
      .then(new SetItalic(1, 5))
      .then(new SetStrong(3, 8))
  }
}
