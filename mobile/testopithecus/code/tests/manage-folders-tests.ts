import { TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import {
  CloseFolderManagerAction,
  DeleteFolderAction,
  EnterNameForEditedFolderAction,
  EnterNameForNewFolderAction,
  OpenCreateFolderScreenAction,
  OpenEditFolderScreenAction,
  OpenFolderLocationScreenAction,
  OpenFolderManagerAction,
  SelectParentFolderAction,
  SubmitEditedFolderAction,
  SubmitNewFolderAction,
} from '../mail/actions/left-column/manage-folders-actions'
import { ContainerDeletionMethod } from '../mail/feature/manageable-container-features'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class ManageFoldersAddNewFolderInRootTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FoldersManager. Добавление новой папки в корневую папку')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5844).androidCase(10236)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenFolderManagerAction())
      .then(new OpenCreateFolderScreenAction())
      .then(new EnterNameForNewFolderAction('NewFolder'))
      .then(new SubmitNewFolderAction())
      .then(new CloseFolderManagerAction())
  }
}

export class ManageFoldersDeleteFolderWithSubfoldersByLongSwipeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FoldersManager. Удаление папки с подпапкой длинным свайпом')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5858)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('SubFolder', ['Folder'])
      .switchFolder('SubFolder', ['Folder'])
      .nextMessage('subj21')
      .nextMessage('subj22')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenFolderManagerAction())
      .then(new DeleteFolderAction('Folder', [], ContainerDeletionMethod.longSwipe))
      .then(new CloseFolderManagerAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ManageFoldersEditFolderTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FoldersManager. Изменение папки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5853).androidCase(10237)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('Folder1')
      .switchFolder('Folder1')
      .nextMessage('subj11')
      .nextMessage('subj12')
      .createFolder('Folder2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenFolderManagerAction())
      .then(new OpenEditFolderScreenAction('Folder1'))
      .then(new EnterNameForEditedFolderAction('EditedFolder1'))
      .then(new OpenFolderLocationScreenAction())
      .then(new SelectParentFolderAction(['Folder2']))
      .then(new SubmitEditedFolderAction())
      .then(new CloseFolderManagerAction())
      .then(new GoToFolderAction('EditedFolder1', ['Folder2']))
  }
}

export class ManageFoldersValidateViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FoldersManager. Внешний вид экрана Управление папками')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5838).androidCase(10267)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('Folder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenFolderManagerAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ManageFoldersValidateEditFolderViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FoldersManager. Внешний вид экрана Изменить папку')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5840).androidCase(10271)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('Folder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenFolderManagerAction())
      .then(new OpenEditFolderScreenAction('Folder'))
  }
}

export class ManageFoldersValidateAddFolderViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FoldersManager. Внешний вид экрана Новая папка')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5839).androidCase(10268)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('Folder1')
      .createFolder('Folder2')
      .createFolder('SubFolder1', ['Folder'])
      .createFolder('SubFolder2', ['Folder'])
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenFolderManagerAction())
      .then(new OpenCreateFolderScreenAction())
  }
}

export class ManageFoldersValidateFolderLocationViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FoldersManager. Внешний вид экрана Расположение папки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5841).androidCase(10272)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('Folder1')
      .createFolder('Folder2')
      .createFolder('SubFolder1', ['Folder'])
      .createFolder('SubFolder2', ['Folder'])
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenFolderManagerAction())
      .then(new OpenCreateFolderScreenAction())
      .then(new OpenFolderLocationScreenAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ManageFoldersDeleteFolderTest extends RegularYandexMailTestBase {
  public constructor() {
    super('FoldersManager. Удаление папки в которой есть подпапки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5859).androidCase(10238)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('SubFolder', ['Folder'])
      .switchFolder('SubFolder', ['Folder'])
      .nextMessage('subj21')
      .nextMessage('subj22')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenFolderManagerAction())
      .then(new DeleteFolderAction('Folder', [], ContainerDeletionMethod.tap))
      .then(new CloseFolderManagerAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}
