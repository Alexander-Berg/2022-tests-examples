import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class LabelViewLoadingTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Загрузка и отображение списка писем по метке')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10394).androidCase(10192)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder()
        .withDefaults()
        .addLabels([new LabelData('test1')])
        .withSubject('subj'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenFolderListAction()).then(new GoToLabelAction('test1'))
  }
}

export class LongLabelNameViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Отображение длинного имени метки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10384).androidCase(10185)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createLabel(new LabelData('0123456789012345678901234567890'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenFolderListAction())
  }
}
