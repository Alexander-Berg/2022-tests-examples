import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { ClearSpamFolderAction, ClearTrashFolderAction } from '../mail/actions/left-column/clear-folder-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class ClearSpamTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Очистка папки Спам')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10391).androidCase(10189)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new ClearSpamFolderAction())
      .then(new AssertAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class CancelClearSpamTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Отмена очистки папки Спам')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10392).androidCase(10190)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new ClearSpamFolderAction(false))
      .then(new AssertAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class ClearTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Очистка папки Удаленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10397).androidCase(10195)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new ClearTrashFolderAction())
      .then(new AssertAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class CancelClearTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Отмена очистки папки Удаленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10398).androidCase(10196)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new ClearTrashFolderAction(false))
      .then(new AssertAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}
