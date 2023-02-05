import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { MarkAsRead, MarkAsUnread } from '../mail/actions/base-actions/markable-actions'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFilterUnreadAction } from '../mail/actions/left-column/filter-navigator-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { MessageViewContextMenuMoveToFolderAction } from '../mail/actions/messages-list/context-menu-actions'
import {
  ArchiveMessageByShortSwipeAction,
  DeleteMessageByShortSwipeAction,
  MarkAsReadFromShortSwipeAction,
} from '../mail/actions/messages-list/short-swipe-actions'
import { UndoArchiveAction, UndoDeleteAction } from '../mail/actions/messages-list/undo-actions'
import { MessageViewBackToMailListAction, OpenMessageAction } from '../mail/actions/opened-message/message-actions'
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
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { TestSettings, DeviceType } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class SwipeToReadMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка письма прочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6211).androidCase(6466)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder
      .nextMessage('inbox_msg1')
      .nextMessage('inbox_msg2')
      .createFolder('AutotestFolder')
      .switchFolder('AutotestFolder')
      .nextMessage('subj')
      .nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('AutotestFolder'))
      .then(new MarkAsRead(0))
      .then(new OpenFolderListAction())
      .then(new GoToFilterUnreadAction())
      .then(new MarkAsRead(2))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class SwipeToUnreadMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка письма непрочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6212).androidCase(6467)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.switchFolder(DefaultFolderName.archive).nextMessage('subj').nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new MarkAsRead(0))
      .then(new MarkAsUnread(0))
  }
}

export class SwipeToReadThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка треда прочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6214).androidCase(6469)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.nextThread('thread1', 2).nextThread('thread2', 5)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MarkAsRead(1))
  }
}

export class SwipeToUnreadThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка треда непрочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6213).androidCase(6468)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.nextThread('thread1', 4).nextThread('thread2', 1)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MarkAsRead(0)).then(new MarkAsUnread(0))
  }
}

export class ShortSwipeToDeleteThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление коротким свайпом треда с письмами в разных папках')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6223).androidCase(6477)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.nextThread('thread1', 4).nextThread('thread2', 1).createFolder('AutotestFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMoveToFolderAction('AutotestFolder'))
      .then(new MessageViewBackToMailListAction())
      .then(new DeleteMessageByShortSwipeAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('AutotestFolder'))
  }
}

export class ShortSwipeToDeleteMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление письма коротким свайпом из папки Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(25).androidCase(6192)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new DeleteMessageByShortSwipeAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ShortSwipeToArchiveMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Архивация письма коротким свайпом из пользовательской подпапки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6227).androidCase(6481)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('subfolder', ['folder'])
      .switchFolder('subfolder', ['folder'])
      .nextMessage('subj1')
      .nextMessage('subj2')
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
      .then(new ArchiveMessageByShortSwipeAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new DeleteMessageByShortSwipeAction(0))
  }
}

export class ShortSwipeToArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Архивация треда коротким свайпом из папки Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6228).androidCase(6482)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3).nextThread('thread2', 7).nextThread('thread3', 4)
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
      .then(new ArchiveMessageByShortSwipeAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class ShortSwipeToDeleteFromTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление письма коротким свайпом из папки Удаленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6563).androidCase(6628)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage('subj').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new DeleteMessageByShortSwipeAction(1))
  }
}

export class ShortSwipeToArchiveFromArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление письма из Архива при действии по свайпу Архивировать')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6564).androidCase(6629)
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
      .then(new DeleteMessageByShortSwipeAction(1))
  }
}

export class SwipeToReadMessageInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка письма прочитанным-непрочитанным в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(625).androidCase(10299)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.nextMessage('inbox_msg1').nextMessage('inbox_msg2').nextMessage('inbox_msg3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFilterUnreadAction())
      .then(new MarkAsRead(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new MarkAsUnread(1))
  }
}

export class ShortSwipeToDeleteInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление письма коротким свайпом в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(627).androidCase(10317)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.sent)
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
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new DeleteMessageByShortSwipeAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ShortSwipeToArchiveInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Архивация письма коротким свайпом в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(629).androidCase(10318)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.draft)
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
      .then(new GoToFolderAction(DefaultFolderName.draft))
      .then(new ArchiveMessageByShortSwipeAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class ShortSwipeUndoArchiveMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Отмена архивирования письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8631).androidCase(10301)
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
      .then(new ArchiveMessageByShortSwipeAction(0))
      .then(new UndoArchiveAction())
  }
}

export class ShortSwipeUndoArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Отмена архивирования треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8633).androidCase(10316)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextThread('subj1', 2)
      .nextThread('subj2', 3)
      .nextThread('subj3', 4)
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
      .then(new ArchiveMessageByShortSwipeAction(1))
      .then(new UndoArchiveAction())
  }
}

export class ShortSwipeUndoDeleteThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Отмена удаления треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8623).androidCase(10300)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 2).nextThread('subj2', 3).nextThread('subj3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new DeleteMessageByShortSwipeAction(1)).then(new UndoDeleteAction())
  }
}

export class ShortSwipeUndoDeleteMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Отмена удаления письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8622).androidCase(10315)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new DeleteMessageByShortSwipeAction(0))
      .then(new UndoDeleteAction())
  }
}

export class ShortSwipeMarkMessageAsReadAtSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка письма прочитанным в поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8784).androidCase(7354)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new MarkAsReadFromShortSwipeAction(1))
      .then(new AssertAction())
      .then(new CloseSearchAction())
  }
}

export class ShortSwipeDeleteMessageAtSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление письма в поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8786).androidCase(10983)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new DeleteMessageByShortSwipeAction(1))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ShortSwipeDeleteThreadLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление треда в лендскейпе')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8795).androidCase(10305)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 2)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new DeleteMessageByShortSwipeAction(0))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ShortSwipeDeleteMessageAtSearch2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление письма в поиске в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8792).androidCase(10302).setTags([DeviceType.Tab])
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
      .then(new DeleteMessageByShortSwipeAction(1))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ShortSwipeArchiveMessageAtSearch2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Архивация письма в поиске в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9124).androidCase(10307).setTags([DeviceType.Tab])
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
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new ArchiveMessageByShortSwipeAction(0))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class ShortSwipeMarkMessageAsReadAtSearch2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка письма прочитанным в поиске в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9125).androidCase(10957).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new MarkAsReadFromShortSwipeAction(1))
      .then(new AssertAction())
      .then(new CloseSearchAction())
  }
}

export class ShortSwipeDeleteMessage2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Удаление письма в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9153).androidCase(10306).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(2))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new DeleteMessageByShortSwipeAction(2))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ShortSwipeArchiveMessage2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Архивация письма в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9154).androidCase(10312).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('UserFolder').nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
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
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(2))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new ArchiveMessageByShortSwipeAction(2))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class ShortSwipeMarkMessageAsRead2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка письма прочитанным в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8793).androidCase(10958).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new MarkAsReadFromShortSwipeAction(2))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterUnreadAction())
      .then(new MarkAsReadFromShortSwipeAction(0))
  }
}

export class ShortSwipeUndoDeleteMessage2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Отмена удаления письма в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8794).androidCase(10304).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new DeleteMessageByShortSwipeAction(0))
      .then(new UndoDeleteAction())
  }
}

export class ShortSwipeUndoArchiveMessage2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Отмена архивации письма в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9155).androidCase(10310).setTags([DeviceType.Tab])
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
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new ArchiveMessageByShortSwipeAction(0))
      .then(new UndoArchiveAction())
  }
}

export class SwipeToReadMessageFromInboxTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe. Пометка свайпом письма прочитанным в папке Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6191).iosCase(23)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MarkAsRead(0))
  }
}

export class SwipeToDeleteMessageFromTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipe menu. Удаление из папки Удаленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7618)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.switchFolder(DefaultFolderName.trash).nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new DeleteMessageByShortSwipeAction(0))
  }
}
