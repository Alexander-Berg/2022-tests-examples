// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { LabelType } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import {
  GoToFilterImportantAction,
  GoToFilterUnreadAction,
  GoToFilterWithAttachmentsAction,
} from '../mail/actions/left-column/filter-navigator-actions'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { AttachmentSpec, MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class ChangeFilterTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Загрузка и отображение списка писем по фильтрам')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10394).androidCase(10192)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder()
        .withDefaults()
        .withSystemLabel(LabelType.important)
        .addAttachments([AttachmentSpec.withName('attach')]),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new AssertAction())
      .then(new GoToFilterImportantAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new AssertAction())
      .then(new GoToFilterUnreadAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new AssertAction())
      .then(new GoToFilterWithAttachmentsAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
  }
}
