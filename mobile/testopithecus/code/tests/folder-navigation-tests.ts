import { LabelColor, LabelData } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import { BackendCreateFolderAction, BackendCreateLabelAction } from '../mail/actions/backend-actions'
import {
  GoToFolderAction,
  OpenFolderListAction,
  PtrFolderListAction,
} from '../mail/actions/left-column/folder-navigator-actions'
import { RefreshMessageListAction } from '../mail/actions/messages-list/message-list-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class ChangeFoldersInboxCustomTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Переход в папки с письмами (Инбокс и кастомная папка)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10393).androidCase(10191)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj').createFolder('custom').switchFolder('custom').nextMessage('custom folder subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('custom'))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new AssertAction())
  }
}

export class ChangeFoldersSentDraftTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Переход в папки с письмами (Отправленные и черновики)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10393).androidCase(10191)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.sent)
      .nextMessage('sent subj')
      .switchFolder(DefaultFolderName.draft)
      .nextMessage('draft subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new AssertAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new AssertAction())
      .then(new GoToFolderAction(DefaultFolderName.draft))
  }
}

export class ChangeFoldersArchiveSpamTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Переход в папки с письмами (Архив, Спам, Удаленные)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10393).androidCase(10191)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.spam)
      .nextMessage('spam subj')
      .switchFolder(DefaultFolderName.archive)
      .nextMessage('archive subj')
      .switchFolder(DefaultFolderName.trash)
      .nextMessage('trash subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new AssertAction())
  }
}

export class ValidateFolderListTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Внешний вид списка папок')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6350).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('subsubsubfolder1_longname', ['folder1', 'subfolder1', 'subsubfolder1'])
      .createFolder('subfolder2', ['folder2'])
      .createFolder('subfolder22', ['folder2'])
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new AssertAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ValidateLabelListTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Внешний вид списка меток')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6350).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createLabel(new LabelData('label1', LabelColor.red1.toString()))
      .createLabel(new LabelData('label2', LabelColor.green2.toString()))
      .createLabel(new LabelData('label3', LabelColor.blue3.toString()))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenFolderListAction()).then(new AssertSnapshotAction(this.description))
  }
}

export class NewFolderFromBackTestIos extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Отображение новой папки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10338).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .thenChain([new BackendCreateFolderAction('custom'), new PtrFolderListAction()])
  }
}

export class NewFolderFromBackTestAndroid extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Отображение новой папки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10121).ignoreOn(MBTPlatform.IOS)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new BackendCreateFolderAction('custom'))
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new RefreshMessageListAction())
      .then(new OpenFolderListAction())
  }
}

export class NewLabelFromBackTestIos extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Отображение новой метки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10339).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .thenChain([new BackendCreateLabelAction(new LabelData('custom')), new PtrFolderListAction()])
  }
}

export class NewLabelFromBackTestAndroid extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Отображение новой метки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10122).ignoreOn(MBTPlatform.IOS)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new BackendCreateLabelAction(new LabelData('custom')))
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new RefreshMessageListAction())
      .then(new OpenFolderListAction())
      .then(new OpenFolderListAction())
  }
}

export class LongFolderNameViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FolderList. Отображение длинного имени папки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10359).androidCase(10146)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('0123456789012345678901234567890')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenFolderListAction())
  }
}
