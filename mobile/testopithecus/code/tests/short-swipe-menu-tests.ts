import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { MarkAsRead } from '../mail/actions/base-actions/markable-actions'
import { GoToFilterImportantAction, GoToFilterUnreadAction } from '../mail/actions/left-column/filter-navigator-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import {
  ShortSwipeContextMenuApplyLabelsAction,
  ShortSwipeContextMenuArchiveAction,
  ShortSwipeContextMenuDeleteAction,
  ShortSwipeContextMenuMarkAsImportantAction,
  ShortSwipeContextMenuMarkAsNotSpamAction,
  ShortSwipeContextMenuMarkAsReadAction,
  ShortSwipeContextMenuMarkAsSpamAction,
  ShortSwipeContextMenuMarkAsUnimportantAction,
  ShortSwipeContextMenuMarkAsUnreadAction,
  ShortSwipeContextMenuMoveToFolderAction,
  ShortSwipeContextMenuOpenReplyComposeAction,
  ShortSwipeContextMenuRemoveLabelsAction,
} from '../mail/actions/messages-list/context-menu-actions'
import {
  GroupModeInitialSelectAction,
  GroupModeMarkImportantAction,
  GroupModeSelectAction,
} from '../mail/actions/messages-list/group-mode-actions'
import { UndoArchiveAction, UndoDeleteAction, UndoSpamAction } from '../mail/actions/messages-list/undo-actions'
import { CloseSearchAction, OpenSearchAction, SearchAllMessagesAction } from '../mail/actions/search/search-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  TurnOnCompactMode,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName, FolderBackendName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class ShortSwipeMenuMarkAsReadMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма прочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6217).androidCase(6472)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('message1').nextMessage('message2').nextMessage('message3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ShortSwipeContextMenuMarkAsReadAction(1))
  }
}

export class ShortSwipeMenuMarkAsReadThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка треда прочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6618).androidCase(6631)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 1).nextThread('thread2', 2).nextThread('thread3', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ShortSwipeContextMenuMarkAsReadAction(1))
  }
}

export class ShortSwipeMenuMarkAsUnreadMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма непрочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6218).androidCase(6473)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextMessage('message1')
      .nextMessage('message2')
      .nextMessage('message3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new MarkAsRead(0))
      .then(new ShortSwipeContextMenuMarkAsUnreadAction(0))
  }
}

export class ShortSwipeMenuMarkAsUnreadThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка треда непрочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6619).androidCase(6632)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3).nextThread('thread2', 2).nextThread('thread3', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new MarkAsRead(0)).then(new ShortSwipeContextMenuMarkAsUnreadAction(0))
  }
}

export class ShortSwipeMenuDeleteMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Удаление письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6221).androidCase(6476)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserSubfolder', ['UserFolder'])
      .switchFolder('UserSubfolder', ['UserFolder'])
      .nextMessage('message1')
      .nextMessage('message2')
      .nextMessage('message3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserSubfolder', ['UserFolder']))
      .then(new ShortSwipeContextMenuDeleteAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ShortSwipeMenuDeleteThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Удаление треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6224).androidCase(6478)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 3).nextThread('thread3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuDeleteAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class ShortSwipeMenuMarkAsSpamThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка треда спамом')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6236).androidCase(6490)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserSubfolder', ['UserFolder'])
      .switchFolder('UserSubfolder', ['UserFolder'])
      .nextThread('thread1', 2)
      .nextThread('thread2', 3)
      .nextThread('thread3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserSubfolder', ['UserFolder']))
      .then(new ShortSwipeContextMenuMarkAsSpamAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class ShortSwipeMenuMarkAsSpamMessageTest extends RegularYandexMailTestBase {
  // TODO: нужно разобраться с моделью при перемещении письма в другую папку, находясь в метке
  public constructor() {
    super('ShortSwipeMenu. Пометка письма спамом')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6235).androidCase(6489)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('message1').nextMessage('message2').nextMessage('message3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFilterUnreadAction())
      .then(new ShortSwipeContextMenuMarkAsSpamAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class ShortSwipeMenuMarkAsNotSpamMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма не спамом')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6621).androidCase(6633)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage('message1').nextMessage('message2').nextMessage('message3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new ShortSwipeContextMenuMarkAsNotSpamAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class ShortSwipeMenuImportantMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма важным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6239).androidCase(6493)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.archive)
      .nextMessage('message1')
      .nextMessage('message2')
      .nextMessage('message3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new ShortSwipeContextMenuMarkAsImportantAction(2))
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class ShortSwipeMenuImportantThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка треда важным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6240).androidCase(6494)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 3).nextThread('thread3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMarkAsImportantAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class ShortSwipeMenuUnImportantMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Снятие метки Важное с письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6241).androidCase(6495)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('message1', 2).nextThread('message2', 2).nextThread('message3', 2)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMarkAsImportantAction(1))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new ShortSwipeContextMenuMarkAsUnimportantAction(1))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class ShortSwipeMenuUnImportantThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Снятие метки Важное с треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6242).androidCase(6496)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 3).nextThread('thread3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMarkAsImportantAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ShortSwipeContextMenuMarkAsUnimportantAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class ShortSwipeMenuMoveToFolderMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Перемещение письма в другую папку')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6244).androidCase(6498)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('message1').nextMessage('message2').nextMessage('message3').createFolder('UserFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(1, 'UserFolder'))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
  }
}

export class ShortSwipeMenuMoveToFolderThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Перемещение треда в другую папку')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6615).androidCase(6635)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 3).nextThread('thread3', 4).createFolder('UserFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(1, 'UserFolder'))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
  }
}

export class ShortSwipeMenuMoveToInboxFromTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Перемещение письма из папки Удаленные в папку Входящие')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(26).androidCase(6193)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.switchFolder(DefaultFolderName.trash).nextMessage('AutoTestSubj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.inbox))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class ShortSwipeMenuDeleteFromTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Удаление письма из папки Удаленные')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6620).androidCase(7618).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('Trash').nextMessage('subject1').nextMessage('subject2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('Trash'))
      .then(new ShortSwipeContextMenuDeleteAction(1))
  }
}

export class ShortSwipeMenuApplyLabelToThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Добавление пользовательской метки на тред')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6616).androidCase(6636)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextThread('thread1', 2)
      .nextThread('thread2', 3)
      .nextThread('thread3', 4)
      .createLabel(new LabelData('label1'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuApplyLabelsAction(1, ['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class ShortSwipeMenuApplyLabelToMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Добавление пользовательской метки на письмо')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6246).androidCase(6500)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextMessage('message1')
      .nextMessage('message2')
      .nextMessage('message3')
      .createLabel(new LabelData('label1'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new ShortSwipeContextMenuApplyLabelsAction(1, ['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class ShortSwipeMenuRemoveLabelFromMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Снятие пользовательской метки с письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6247).androidCase(6501)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.sent)
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
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new ShortSwipeContextMenuRemoveLabelsAction(1, ['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class ShortSwipeMenuRemoveLabelFromThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Снятие пользовательской метки с треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6617).androidCase(6637)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextThread('thread1', 2)
      .nextThread('thread2', 3)
      .nextThread('thread3', 4)
      .createLabel(new LabelData('label1'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuApplyLabelsAction(1, ['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ShortSwipeContextMenuRemoveLabelsAction(1, ['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class ShortSwipeMenuReplyOnMessageTest extends RegularYandexMailTestBase {
  // TODO: нужно реализовать метод getDraft()
  public constructor() {
    super('ShortSwipeMenu. Ответ на письмо')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6196).androidCase(6072)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('message1').nextMessage('message2').nextMessage('message3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ShortSwipeContextMenuOpenReplyComposeAction(1))
  }
}

export class ShortSwipeMenuArchiveMessageFromSpamTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Архивация одиночного письма из папки Спам')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6231).androidCase(6485)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.switchFolder(DefaultFolderName.spam).nextMessage('AutoTestSubj1').nextMessage('AutoTestSubj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new ShortSwipeContextMenuArchiveAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class ShortSwipeMenuArchiveThreadFromUserFolderTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Архивация треда письма из Пользовательской папки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6232).androidCase(6486)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('UserFolder').switchFolder('UserFolder').nextThread('AutoTestSubj1', 2)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new ShortSwipeContextMenuArchiveAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class ShortSwipeMenuMarkReadUnreadInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма прочитанным-непрочитанным в compact режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(632).androidCase(10535)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextMessage('message1')
      .nextMessage('message2')
      .nextMessage('message3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction('UserFolder'))
      .then(new MarkAsRead(0))
      .then(new ShortSwipeContextMenuMarkAsReadAction(1))
      .then(new ShortSwipeContextMenuMarkAsUnreadAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFilterUnreadAction())
  }
}

export class ShortSwipeMenuAddLabelInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма важным-неважным в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(635).androidCase(10537)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1'), new LabelData('label2')])
          .withSubject('inbox_subj1'),
      )
      .switchFolder(DefaultFolderName.sent)
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .createLabel(new LabelData('label1'))
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
      .then(new ShortSwipeContextMenuApplyLabelsAction(1, ['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label2'))
      .then(new ShortSwipeContextMenuRemoveLabelsAction(0, ['label2']))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class ShortSwipeMenuMarkUnmarkAsSpamInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма спамом-не спамом в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(636).androidCase(10538)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
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
      .then(new ShortSwipeContextMenuMarkAsSpamAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new ShortSwipeContextMenuMarkAsNotSpamAction(0))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class ShortSwipeMenuDeleteMessageFromTrashInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Удаление письма из папки Удаленные в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(637).androidCase(10539)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new ShortSwipeContextMenuDeleteAction(1))
  }
}

export class ShortSwipeMenuArchiveMessageInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Архивация письма в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(638).androidCase(10540)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
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
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkImportantAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new ShortSwipeContextMenuArchiveAction(1))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class ShortSwipeMenuUndoSpamMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Отмена отправки в спам письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8625).androidCase(10545)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.trash).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new ShortSwipeContextMenuMarkAsSpamAction(1))
      .then(new UndoSpamAction())
  }
}

export class ShortSwipeMenuUndoSpamThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Отмена отправки в спам треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8632).androidCase(10548)
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
      .then(new GoToFolderAction('UserFolder'))
      .then(new ShortSwipeContextMenuMarkAsSpamAction(1))
      .then(new UndoSpamAction())
  }
}

export class ShortSwipeMenuUndoDeleteThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Отмена удаления треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8627).androidCase(10514)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 2).nextThread('subj2', 3).nextThread('subj3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ShortSwipeContextMenuDeleteAction(1)).then(new UndoDeleteAction())
  }
}

export class ShortSwipeMenuUndoDeleteMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Отмена удаления письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8630).androidCase(10547)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFilterUnreadAction())
      .then(new ShortSwipeContextMenuDeleteAction(1))
      .then(new UndoDeleteAction())
  }
}

export class ShortSwipeMenuUndoArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Отмена архивирования треда')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8629).androidCase(10546)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 2).nextThread('subj2', 3).nextThread('subj3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ShortSwipeContextMenuArchiveAction(0)).then(new UndoArchiveAction())
  }
}

export class ShortSwipeMenuUndoArchiveMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Отмена архивирования письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8634).androidCase(10515)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new ShortSwipeContextMenuArchiveAction(0))
      .then(new UndoArchiveAction())
  }
}

export class SearchAndMarkMessageReadByShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма прочитанным в Поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8867).androidCase(10200)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new ShortSwipeContextMenuMarkAsReadAction(0))
      .then(new AssertAction())
      .then(new CloseSearchAction())
  }
}

export class SearchAndDeleteMessageByShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Удаление письма в Поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8869).androidCase(7363)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new ShortSwipeContextMenuDeleteAction(1))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class SearchAndMoveMessageToAnotherFolderByShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Перемещение письма в другую папку в Поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8876).androidCase(7374)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').createFolder('UserFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new ShortSwipeContextMenuMoveToFolderAction(1, 'UserFolder'))
      .then(new AssertAction())
      .then(new CloseSearchAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
  }
}

export class MoveMessageFromInboxTabToMailingListTabByShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Перемещение одиночного сообщения из таба в таб')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7583).androidCase(540).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(1, DefaultFolderName.mailingLists))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
  }
}

export class MarkMessageImportantInMailingListTabByShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Пометка письма Важным в табе')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7593).androidCase(551).ignoreOn(MBTPlatform.Android)
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
      .then(new ShortSwipeContextMenuMarkAsImportantAction(1))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class MarkMessageUnimportantInSocialNetworksTabByShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Снятие метки Важное в табе')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7594).androidCase(552).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .turnOnTab()
      .switchFolder(FolderBackendName.socialNetworks)
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuMarkAsImportantAction(1))
      .then(new ShortSwipeContextMenuMarkAsUnimportantAction(1))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class UnmarkLabelInInboxTabByShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ShortSwipeMenu. Снятие пользовательской метки с письма в табе')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7596).androidCase(554).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .turnOnTab()
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1'), new LabelData('label2')])
          .withSubject('subj1'),
      )
      .nextMessage('subj2')
      .nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuRemoveLabelsAction(0, ['label1']))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}
