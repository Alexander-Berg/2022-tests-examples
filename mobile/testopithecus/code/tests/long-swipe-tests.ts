import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { TestSettings, DeviceType, MBTPlatform } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFilterImportantAction } from '../mail/actions/left-column/filter-navigator-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import { ShortSwipeContextMenuMarkAsImportantAction } from '../mail/actions/messages-list/context-menu-actions'
import {
  ArchiveMessageByLongSwipeAction,
  DeleteMessageByLongSwipeAction,
} from '../mail/actions/messages-list/long-swipe-actions'
import { UndoArchiveAction, UndoDeleteAction } from '../mail/actions/messages-list/undo-actions'
import { OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import { CloseSearchAction, OpenSearchAction, SearchAllMessagesAction } from '../mail/actions/search/search-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  SetActionOnSwipe,
  TurnOnCompactMode,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { SwitchContextToAction } from '../mail/actions/switch-context-in-2pane-actions'
import { MaillistComponent } from '../mail/components/maillist-component'
import { ActionOnSwipe } from '../mail/feature/settings/general-settings-feature'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName, FolderBackendName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class LongSwipeToDeleteMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление письма из папки Отправленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(24).androidCase(6194)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new DeleteMessageByLongSwipeAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeToDeleteThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление треда из папки Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6219).androidCase(6474)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 3).nextThread('thread3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new DeleteMessageByLongSwipeAction(2))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeToArchiveMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Архивация письма из пользовательской папки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6225).androidCase(6479)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('UserFolder').switchFolder('UserFolder').nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction('UserFolder'))
      .then(new ArchiveMessageByLongSwipeAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class LongSwipeToArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Архивация треда из папки Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6226).androidCase(6480)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 3).nextThread('thread3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ArchiveMessageByLongSwipeAction(2))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class LongSwipeToArchiveFromArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление письма из Архива при действии по свайпу Архивировать')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6613).androidCase(6623)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.archive).nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new DeleteMessageByLongSwipeAction(1))
  }
}

export class LongSwipeToDeleteFromTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление письма длинным свайпом из папки Удаленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6612).androidCase(6624)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new DeleteMessageByLongSwipeAction(1))
  }
}

export class LongSwipeToArchiveInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Архивация письма длинным свайпом в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(628).androidCase(10164)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.spam)
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new ArchiveMessageByLongSwipeAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class LongSwipeLongSwipeToDeleteFromSearchInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление письма длинным свайпом из поиска в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(626).androidCase(10163)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new DeleteMessageByLongSwipeAction(1))
  }
}

export class LongSwipeUndoDeleteMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Отмена удаления письма из папки Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6195).androidCase(6066)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new DeleteMessageByLongSwipeAction(1)).then(new UndoDeleteAction())
  }
}

export class LongSwipeUndoDeleteThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Отмена удаления треда из Пользовательской папки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8628).androidCase(9843)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('UserFolder').switchFolder('UserFolder').nextThread('subj1', 3).nextThread('subj2', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new DeleteMessageByLongSwipeAction(0))
      .then(new UndoDeleteAction())
  }
}

export class LongSwipeUndoArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Отмена архивирования треда из папки Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10591)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 3).nextThread('subj2', 2).nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ArchiveMessageByLongSwipeAction(1))
      .then(new UndoArchiveAction())
  }
}

export class LongSwipeUndoArchiveMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Отмена архивирования письма из папки Спам')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8624).androidCase(9851)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new ArchiveMessageByLongSwipeAction(1))
      .then(new UndoArchiveAction())
  }
}

export class LongSwipeUndoDeleteMessageAtSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Отмена удаления письма в поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8856).androidCase(10159)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new DeleteMessageByLongSwipeAction(1))
      .then(new UndoDeleteAction())
  }
}

export class LongSwipeDeleteMessageAtSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление письма в поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8852).androidCase(10157)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new DeleteMessageByLongSwipeAction(1))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeArchiveMessageAtSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Архивация письма в поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8853).androidCase(10568)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new ArchiveMessageByLongSwipeAction(2))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class LongSwipeDeleteMessageAtTabMailingListsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление письма из таба Рассылки')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7590).androidCase(9835).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .turnOnTab()
      .switchFolder(FolderBackendName.mailingLists)
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new DeleteMessageByLongSwipeAction(1))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeDeleteThreadLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление треда в лендскейпе')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8864).androidCase(10343)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new DeleteMessageByLongSwipeAction(0))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeDeleteMessageAtSearch2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление письма в поиске в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8858).androidCase(10341).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(1))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new DeleteMessageByLongSwipeAction(1))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeDeleteThread2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление треда в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8861).androidCase(10342).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new DeleteMessageByLongSwipeAction(0))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeToDeleteFromImportantTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление письма длинным свайпом из метки Важные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10358).androidCase(9836)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMarkAsImportantAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new DeleteMessageByLongSwipeAction(0))
  }
}

export class LongSwipeToDeleteFromTrashUndoTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Отмена удаления письма длинным свайпом из папки Удаленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10364).androidCase(9838)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new DeleteMessageByLongSwipeAction(0, false))
  }
}

export class LongSwipeToDeleteDraftInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление черновика длинным свайпом в compact режиме в Черновиках')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9844).iosCase(10375)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.draft).nextMessage('draft1').nextMessage('draft2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.draft))
      .then(new DeleteMessageByLongSwipeAction(0))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeToArchiveMsgFromLabelInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Архивация письма длинным свайпом в пользовательской метке')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9848)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('subj1'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('subj2'),
      )
      .createLabel(new LabelData('label1'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToLabelAction('label1'))
      .then(new ArchiveMessageByLongSwipeAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class LongSwipeToDeleteThreadInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Удаление треда длинным свайпом в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10350).iosCase(8865)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new DeleteMessageByLongSwipeAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LongSwipeToArchiveMessageInLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Архивация письма в пользовательской папке 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10348).iosCase(8862).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('UserFolder').switchFolder('UserFolder').nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new RotateToLandscape())
      .then(new GoToFolderAction('UserFolder'))
      .then(new ArchiveMessageByLongSwipeAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class LongSwipeToUndoDeleteMessageLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Отмена удаления письма из папки Отправленные 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10349).iosCase(8863).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new DeleteMessageByLongSwipeAction(0))
      .then(new UndoDeleteAction())
  }
}

export class LongSwipeToUndoArchiveMessageLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Отмена архивирования письма из пользовательской папки 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10352).iosCase(9163).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('UserFolder').switchFolder('UserFolder').nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction('UserFolder'))
      .then(new ArchiveMessageByLongSwipeAction(0))
      .then(new UndoArchiveAction())
  }
}

export class LongSwipeToArchiveTemplateTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Архивирование шаблона в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9853)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.template).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.template))
      .then(new ArchiveMessageByLongSwipeAction(0))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class LongSwipeToArchiveThreadWithUndoTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LongSwipe. Архивация треда с Отменой (письма в разных папках)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9852).iosCase(8626)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('thread1')
      .switchFolder(DefaultFolderName.sent)
      .nextMessage('thread1')
      .createFolder('subfolder', ['folder'])
      .switchFolder('subfolder', ['folder'])
      .nextMessage('thread1')
      .createFolder('folder2')
      .switchFolder('folder2')
      .nextMessage('thread1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction('subfolder', ['folder']))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ArchiveMessageByLongSwipeAction(0))
      .then(new UndoArchiveAction())
  }
}
