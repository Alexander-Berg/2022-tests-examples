import { AccountType } from '../../../mapi/code/api/entities/account/account-type'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import {
  ShortSwipeContextMenuArchiveAction,
  ShortSwipeContextMenuDeleteAction,
  ShortSwipeContextMenuMarkAsSpamAction,
  ShortSwipeContextMenuMoveToFolderAction,
} from '../mail/actions/messages-list/context-menu-actions'
import {
  GroupModeDeleteAction,
  GroupModeInitialSelectAction,
  GroupModeMoveToFolderAction,
  GroupModeSelectAction,
} from '../mail/actions/messages-list/group-mode-actions'
import { UndoArchiveAction, UndoDeleteAction, UndoSpamAction } from '../mail/actions/messages-list/undo-actions'
import { OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import { DeleteCurrentThreadAction } from '../mail/actions/opened-message/thread-view-navigator-actions'
import {
  CloseAccountSettingsAction,
  OpenAccountSettingsAction,
  SwitchOffTabsAction,
  SwitchOnTabsAction,
} from '../mail/actions/settings/account-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class MoveThreadFromMailingListToSocialNetworksTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Перемещение треда из таба рассылка в таб социальные сети.')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(544).iosCase(7586).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextThread('subj1', 3).nextThread('subj2', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new ShortSwipeContextMenuMoveToFolderAction(1, DefaultFolderName.socialNetworks))
  }
}

export class MoveThreadFromSocialNetworksTabToUserFolderTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Перемещение треда из таба социальные сети в пользовательскую папку.')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(545).iosCase(7587).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextThread('subj1', 3).nextThread('subj2', 4).createFolder('user_folder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeMoveToFolderAction('user_folder'))
  }
}

export class MoveThreadFromUserFolderToInboxTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Перемещение треда из пользовательской папки в таб входящие.')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(546).iosCase(7588).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextThread('subj1', 3).nextThread('subj2', 4).createFolder('user_folder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, 'user_folder'))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, 'user_folder'))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('user_folder'))
      .then(new ShortSwipeContextMenuMoveToFolderAction(1, DefaultFolderName.inbox))
  }
}

export class UndoDeleteMessageFromTabTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Отмена удаления сообщения из таба Социальные сети.')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(549).iosCase(7591).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuDeleteAction(0))
      .then(new UndoDeleteAction())
  }
}

export class TurnOnTabsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Включение сортировки писем (ТАБов).')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(11524).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new SwitchOnTabsAction(AccountType.login))
      .then(new CloseAccountSettingsAction())
      .then(new CloseRootSettings())
  }
}

export class TurnOffTabsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Выключение сортировки писем (ТАБов).')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(11526).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab()
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new SwitchOffTabsAction(AccountType.login))
      .then(new CloseAccountSettingsAction())
      .then(new CloseRootSettings())
  }
}

export class DeleteMessageByTapOnTopBarInTabTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Удаление сообщения из таба социальные сети.')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(11531).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new OpenMessageAction(1))
      .then(new DeleteCurrentThreadAction())
  }
}

export class DeleteThreadInTabTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Удаление треда из таба социальные сети.')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(11604).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextThread('subj2', 3).nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(0))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeDeleteAction())
  }
}

export class UndoMoveMessageToSpamTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Отмена отправки в спам сообщения из таба.')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(11605).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextThread('subj3', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new ShortSwipeContextMenuMarkAsSpamAction(0))
      .then(new UndoSpamAction())
  }
}

export class UndoMoveMessageToArchiveFromInboxTabTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Tabs. Отмена отправки в архив сообщения из таба входящие.')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(11606).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ShortSwipeContextMenuArchiveAction(1)).then(new UndoArchiveAction())
  }
}
