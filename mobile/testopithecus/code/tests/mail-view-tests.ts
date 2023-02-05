import { LabelData, LabelType } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { DeviceType, MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import {
  ApplyLabelAddLabelAction,
  ApplyLabelTapOnCreateLabelAction,
} from '../mail/actions/base-actions/labeled-actions'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFilterImportantAction } from '../mail/actions/left-column/filter-navigator-actions'
import {
  CloseFolderListAction,
  GoToFolderAction,
  OpenFolderListAction,
} from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import {
  EnterNameForNewLabelAction,
  SetNewLabelColorAction,
  SubmitNewLabelAction,
} from '../mail/actions/left-column/manage-labels-actions'
import {
  MessageViewContextMenuApplyLabelsAction,
  MessageViewContextMenuArchiveAction,
  MessageViewContextMenuDeleteAction,
  MessageViewContextMenuMarkAsImportantAction,
  MessageViewContextMenuMarkAsNotSpamAction,
  MessageViewContextMenuMarkAsReadAction,
  MessageViewContextMenuMarkAsSpamAction,
  MessageViewContextMenuMarkAsUnimportantAction,
  MessageViewContextMenuMarkAsUnreadAction,
  MessageViewContextMenuMoveToFolderAction,
  MessageViewContextMenuOpenApplyLabelsAction,
  MessageViewContextMenuRemoveLabelsAction,
  MessageViewOpenContextMenuAction,
} from '../mail/actions/messages-list/context-menu-actions'
import { ExpandThreadAction } from '../mail/actions/messages-list/thread-markable-actions'
import { UndoArchiveAction, UndoDeleteAction, UndoSpamAction } from '../mail/actions/messages-list/undo-actions'
import {
  MessageViewBackToMailListAction,
  MessageViewDeleteMessageByIconAction,
  OpenMessageAction,
} from '../mail/actions/opened-message/message-actions'
import {
  ArchiveCurrentThreadAction,
  DeleteCurrentThreadAction,
} from '../mail/actions/opened-message/thread-view-navigator-actions'
import { CloseSearchAction, OpenSearchAction, SearchAllMessagesAction } from '../mail/actions/search/search-actions'
import {
  CloseAccountSettingsAction,
  OpenAccountSettingsAction,
  SwitchOffThreadingAction,
} from '../mail/actions/settings/account-settings-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  SetActionOnSwipe,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { SwitchContextToAction } from '../mail/actions/switch-context-in-2pane-actions'
import { MaillistComponent } from '../mail/components/maillist-component'
import { ActionOnSwipe } from '../mail/feature/settings/general-settings-feature'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class MarkAsUnreadFromMessageViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Пометка письма непрочитанным из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6283).androidCase(7381)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsUnreadAction())
      .then(new MessageViewBackToMailListAction())
  }
}

export class MarkAsReadFromMessageViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Пометка письма прочитанным из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6282).androidCase(7380)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsUnreadAction())
      .then(new MessageViewContextMenuMarkAsReadAction())
      .then(new MessageViewBackToMailListAction())
  }
}

export class DeleteSingleMessageFromMessageViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Удаление открытого на просмотр единичного письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6285).androidCase(7383)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenMessageAction(0)).then(new MessageViewContextMenuDeleteAction())
  }
}

export class DeleteSingleMessageFromThreadFromMessageViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Удаление одного письма из треда из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6284).androidCase(7382)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .thenChain([new MessageViewContextMenuDeleteAction(), new MessageViewBackToMailListAction()])
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class MarkAsSpamFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Пометка спамом из просмотра одного письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6292).androidCase(7390)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsSpamAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class MarkAsNotSpamFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Пометка не спамом из просмотра')
  }
  // todo Игнорируем тест на андроиде-7622, потому что на андроид письмо не закрывается на просмотр
  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7621).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam)
    mailbox.nextMessage('subj')
    mailbox.switchFolder(DefaultFolderName.inbox)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsNotSpamAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class MarkAsNotSpamFromMessageViewAndroid extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Пометка не спамом из просмотра')
  }
  // todo Игнорируем тест на ios-7621, потому что ios нет выхода из письма
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7622).ignoreOn(MBTPlatform.IOS)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam)
    mailbox.nextMessage('subj')
    mailbox.switchFolder(DefaultFolderName.inbox)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsNotSpamAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class ArchiveSingleMessageFromMessageView extends RegularYandexMailTestBase {
  // todo Добавить тест ios-1174, он аналогичный этому, но у нас в начале нет папки Archive
  public constructor() {
    super('MailView. Архивация единичного письма из просмотра через меню действий')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6289).androidCase(7387)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuArchiveAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class ArchiveMessageFromThreadFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Архивация письма из треда со страницы просмотра письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6288).androidCase(7386)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuArchiveAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class MoveMessageToUsersFolderFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Перемещение одного письма в пользовательскую папку из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6296).androidCase(7394)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj').createFolder('TestFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMoveToFolderAction('TestFolder'))
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('TestFolder'))
  }
}

export class MoveMessageToUsersFolderFromThreadFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Перемещение письма из треда в пользовательскую папку из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7623).androidCase(7624)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3).createFolder('TestFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMoveToFolderAction('TestFolder'))
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('TestFolder'))
  }
}

export class MarkImportantFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Пометка важным единичного письма из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6298).androidCase(7396)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsImportantAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class MarkUnimportantFromMessageView extends RegularYandexMailTestBase {
  // todo Крестик в метке
  public constructor() {
    super('MailView. Снятие метки важное из просмотра через меню действий')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7626).androidCase(7625)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsImportantAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsUnimportantAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class AddLabelsFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Добавление нескольких пользовательских меток в просмотре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7627).androidCase(7628)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj').createLabel(new LabelData('test1')).createLabel(new LabelData('test2'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuApplyLabelsAction(['test1', 'test2']))
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test1'))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test2'))
  }
}

export class DeleteLabelsFromMessageView extends RegularYandexMailTestBase {
  // todo Добавить удаление метки из шапки письма
  public constructor() {
    super('MailView. Снятие нескольких пользовательских меток в просмотре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6299).androidCase(7397)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder()
        .withDefaults()
        .addLabels([new LabelData('test1'), new LabelData('test2'), new LabelData('test3')])
        .withSubject('subj'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuRemoveLabelsAction(['test1', 'test2']))
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test1'))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test2'))
  }
}

export class DeleteMessageFromMessageViewThreadModeOff extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Удаление письма из просмотра при выключенной группировке')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(4395) // TODO: поменять id после появления кейса в новой пальме
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new SwitchOffThreadingAction())
      .then(new CloseAccountSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuDeleteAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class LablesViewFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Отображение меток в просмотре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6896).androidCase(7138)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([
            new LabelData('test1'),
            new LabelData('test2'),
            new LabelData('test3'),
            new LabelData('test4'),
            new LabelData('test5'),
            new LabelData('test6'),
            new LabelData('test7'),
            new LabelData('test8'),
            new LabelData('test9'),
            new LabelData('test10'),
            new LabelData('test11'),
            new LabelData('test12'),
          ])
          .withSubject('subj1'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([
            new LabelData('1'),
            new LabelData('12'),
            new LabelData('12345'),
            new LabelData('1234567890'),
            new LabelData('123456789012345'),
          ])
          .withSubject('subj2'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('qazWSX'), new LabelData('!@#$%^&*()_+')])
          .withSubject('subj3'),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewBackToMailListAction())
      .then(new OpenMessageAction(1))
      .then(new MessageViewBackToMailListAction())
      .then(new OpenMessageAction(2))
  }
}

export class MarkAsReadByOpeningMessageViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Автоматическая пометка письма прочитанным при открытии')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6898).androidCase(7140)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenMessageAction(0))
  }
}

export class MarkAsReadByExpandThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Письмо помечается прочитанным при разворачивании его в треде')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7125).iosCase(6883)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3).nextThread('thread2', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ExpandThreadAction(0)).then(new OpenMessageAction(1))
  }
}

export class DeleteMessageByTapOnIconTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Удалить письмо тапом на иконку')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6081)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewDeleteMessageByIconAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class DeleteMessageByTapOnTopBarTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2Pane Удалить письмо тапом на иконку в топ баре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9127).androidCase(9904).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new DeleteCurrentThreadAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class DeleteMessageInSearchByTapOnTopBarTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Search 2Pane Архивация письма тапом на иконку в топ баре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9129).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
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
      .then(new CloseFolderListAction())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new ArchiveCurrentThreadAction())
  }
}

export class MoveMessageToTabFromMailViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Перемещение одиночного сообщения из папки в таб')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7585).androidCase(543).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().createFolder('custom_folder').switchFolder('custom_folder').nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('custom_folder'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMoveToFolderAction(DefaultFolderName.socialNetworks))
      .then(new MessageViewBackToMailListAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
  }
}

export class MoveMessageToSpamFromMailViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Отправка в спам единичного письма из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9134).androidCase(9920).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsSpamAction())
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class MoveMessageToSpamFromMailViewFromSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Search Отправка в спам единичного письма из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9135).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsSpamAction())
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class MoveMessageOfThreadToArchiveFromMailViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Архивация одного письма из треда из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9139).androidCase(9929).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuArchiveAction())
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class MoveMessageToTrashFromMailViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Удаление единичного письма из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9140).androidCase(10985).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuDeleteAction())
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class MoveMessageToTrashFromMailViewFromSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Search 2pane Удаление единичного письма из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9141).androidCase(9933).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuDeleteAction())
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class MoveMessageFromThreadToTrashFromMailViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Удаление одного письма из треда из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9142).androidCase(9928).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuDeleteAction())
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class MarkAsSpamMessageFromThreadFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Пометка спамом одного письма в треде из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9900).iosCase(9133)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('subfolder', ['folder']).switchFolder('subfolder', ['folder']).nextThread('thread1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('subfolder', ['folder']))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsSpamAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class UndoMessageDeleteFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Отмена удаления единичного письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9902).iosCase(9132)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.archive).nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuDeleteAction())
      .then(new UndoDeleteAction())
  }
}

export class MarkImportantMessageFromThreadFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Добавление метки Важное для одного письма треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9910).iosCase(10290)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsImportantAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class AddAndDeleteLabelsFromMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Снять и поставить несколько пользовательских меток в просмотре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9911).iosCase(10292)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([
            new LabelData('label1'),
            new LabelData('label2'),
            new LabelData('label3'),
            new LabelData('label4'),
            new LabelData('label5'),
          ])
          .withSubject('subj2'),
      )
      .createLabel(new LabelData('test1'))
      .createLabel(new LabelData('test2'))
      .createLabel(new LabelData('test3'))
      .createLabel(new LabelData('test4'))
      .createLabel(new LabelData('test5'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuApplyLabelsAction(['test1', 'test2', 'test3']))
      .then(new MessageViewContextMenuRemoveLabelsAction(['label1', 'label2']))
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test1'))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test2'))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test3'))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label2'))
  }
}

export class DeleteMessageByTapOnIcon2PaneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. landscape Удалить письмо тапом на иконку')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10019)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new MessageViewDeleteMessageByIconAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class MailViewMarkAsSpamMessageFromThread2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Пометка спамом одного письма в треде из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9921).iosCase(9136).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsSpamAction())
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class ViewOperationsInInboxFolderWithLabeledMsg extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Операции с письмом в метках в папке Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10030)
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
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test1'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewOpenContextMenuAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ViewOperationsInArchiveFolderWithLabeledMsg extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Операции с письмом в метках в папке Архив')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10030)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.archive).nextCustomMessage(
      new MessageSpecBuilder()
        .withDefaults()
        .addLabels([new LabelData('test1')])
        .withSubject('subj'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('test1'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewOpenContextMenuAction())
  }
}

export class ViewOperationsInSentFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Операции с письмом в папке Отправленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10031)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new OpenMessageAction(0))
      .then(new MessageViewOpenContextMenuAction())
  }
}

export class ViewOperationsInInboxFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Операции с письмом в папке Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10036)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenMessageAction(0)).then(new MessageViewOpenContextMenuAction())
  }
}

export class ViewOperationsInUserFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Операции с письмом в пользовательской папке')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10036)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('TestFolder').nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('TestFolder'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewOpenContextMenuAction())
  }
}

export class UndoMessageDeleteFromMessage2paneView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane. Отмена удаления единичного письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9927).iosCase(9143).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1'), new LabelData('label2')])
          .withSubject('subj1'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label2')])
          .withSubject('subj2'),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label2'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuDeleteAction())
      .then(new UndoDeleteAction())
  }
}

export class MarkLabelInMessageView extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Добавление пользовательской метки в просмотре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10291)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
      .createLabel(new LabelData('test1'))
      .createLabel(new LabelData('test2'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuApplyLabelsAction(['test1']))
  }
}

export class MessageViewCreateAndMarkLabelTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. Создание и пометка новой пользовательской меткой в просмотре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10288)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuOpenApplyLabelsAction())
      .then(new ApplyLabelTapOnCreateLabelAction())
      .then(new EnterNameForNewLabelAction('new label'))
      .then(new SetNewLabelColorAction(2))
      .then(new SubmitNewLabelAction())
      .then(new ApplyLabelAddLabelAction(['new label']))
  }
}

export class MailViewArchiveMessageByTapOnTopBarTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2Pane Архивация письма тапом на иконку в топ баре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6504).androidCase(9901).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
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
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new OpenMessageAction(0))
      .then(new ArchiveCurrentThreadAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class MailViewArchiveThreadByTapOnTopBarTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2Pane Архивация треда тапом на иконку в топ баре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9126).androidCase(7399).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('UserFolder').nextThread('subj', 3)
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
      .then(new OpenMessageAction(0))
      .then(new ArchiveCurrentThreadAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class MailViewDeleteThreadByTapOnTopBarTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2Pane Удаление треда тапом на иконку в топ баре')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9128).androidCase(7398).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new DeleteCurrentThreadAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class MailViewMarkAsSpamOneMessageInThreadLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. landscape Отправка в спам одного письма из треда из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10296).androidCase(10021)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserSubfolder', ['UserFolder'])
      .switchFolder('UserSubfolder', ['UserFolder'])
      .nextThread('subj', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserSubfolder', ['UserFolder']))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsSpamAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class MailView2paneUndoMessageDeleteInSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Search Отмена удаления единичного письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9144).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuDeleteAction())
      .then(new UndoDeleteAction())
  }
}

export class MailView2paneUndoDeleteOneMessageInThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Отмена удаления одного письма из треда из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9145).androidCase(9925).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuDeleteAction())
      .then(new UndoDeleteAction())
  }
}

export class MailView2paneUndoArchiveOneMessageInThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Отмена архивации одного письма из треда из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9148).androidCase(9924).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('UserFolder').nextThread('subj', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuArchiveAction())
      .then(new UndoArchiveAction())
  }
}

export class MailView2paneUndoSpamMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView. 2pane Отмена отправки в спам единичного письма из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9150).androidCase(9922).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withSystemLabel(LabelType.important).withSubject('subj'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsSpamAction())
      .then(new UndoSpamAction())
  }
}

export class MailView2paneUndoSpamMessageInSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MailView.Search.2pane Отмена отправки в спам единичного письма из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(9152).androidCase(9930).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuMarkAsSpamAction())
      .then(new UndoSpamAction())
  }
}
