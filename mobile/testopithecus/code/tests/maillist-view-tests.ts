import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { DeviceType, MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import { MarkAsImportant } from '../mail/actions/base-actions/labeled-actions'
import { MarkAsRead } from '../mail/actions/base-actions/markable-actions'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFilterImportantAction, GoToFilterUnreadAction } from '../mail/actions/left-column/filter-navigator-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import { ShortSwipeContextMenuMoveToFolderAction } from '../mail/actions/messages-list/context-menu-actions'
import {
  GroupModeApplyLabelsAction,
  GroupModeArchiveAction,
  GroupModeInitialSelectAction,
  GroupModeMarkImportantAction,
  GroupModeMarkSpamAction,
  GroupModeSelectAction,
} from '../mail/actions/messages-list/group-mode-actions'
import { OpenTabByTabNotificationAction } from '../mail/actions/messages-list/message-list-actions'
import {
  ArrowDownClickAction,
  ArrowUpClickAction,
  MessageViewBackToMailListAction,
  OpenMessageAction,
} from '../mail/actions/opened-message/message-actions'
import {
  CloseAccountSettingsAction,
  OpenAccountSettingsAction,
  SwitchOffThreadingAction,
  SwitchOnThreadingAction,
} from '../mail/actions/settings/account-settings-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  TurnOnCompactMode,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class ThreadModeTurnOffAndThenOnTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Изменение настройки тредного режима')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6839).iosCase(6793)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj2')
      .nextMessage('subj2')
      .nextMessage('subj1')
      .nextMessage('subj3')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new SwitchOffThreadingAction())
      .then(new CloseAccountSettingsAction())
      .then(new CloseRootSettings())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new SwitchOnThreadingAction())
      .then(new CloseAccountSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class MailListViewInCompactMode extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение списка писем папки Входящие в compact режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(641).androidCase(10776)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').nextMessage('subj4').nextMessage('subj5')
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
  }
}

export class MarkAsImportantMessageInTabSubscriptionTest extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отметить письмо важным в табе Рассылки')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(551).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new MarkAsImportant(0))
  }
}

export class TabNotificationInTheMiddleMailList extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Плашка таба находится в середине списка писем')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8825).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').nextMessage('subj4')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(1, DefaultFolderName.socialNetworks))
      .then(new OpenTabByTabNotificationAction(DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}
export class TabNotificationInTheFirstInMailList extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Плашка таба находится в начале списка писем')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8821).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').nextMessage('subj4')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new OpenTabByTabNotificationAction(DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}
export class TabNotificationInEmptyMessageList extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Плашка таба находится в пустом списке писем')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8828).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new OpenTabByTabNotificationAction(DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class TabNotificationInCompactMode extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Вид плашки табов в компактном режиме')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8833).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').nextMessage('subj4')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new ShortSwipeContextMenuMoveToFolderAction(2, DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class GoToTabByTabsNotification extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Переход в папку таба через плашку нотификации в таб')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8836).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').nextMessage('subj4')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new MarkAsRead(0))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new ShortSwipeContextMenuMoveToFolderAction(2, DefaultFolderName.socialNetworks))
      .then(new OpenTabByTabNotificationAction(DefaultFolderName.socialNetworks))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class TabsNotificationOnlyInInbox extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Плашки только во Входящих')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8835).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').nextMessage('subj4')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new ShortSwipeContextMenuMoveToFolderAction(2, DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class TabsNotificationInLandscape extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение плашек в Landscape')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8831).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new RotateToLandscape())
  }
}

export class DisplayTabsNotificationAfterMoveMessage extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Появление плашки при перемещении письма в пустой ТАБ')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8843).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new ShortSwipeContextMenuMoveToFolderAction(1, DefaultFolderName.socialNetworks))
  }
}

export class GroupModeWithTabNotification extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Тап по плашке в групповом режиме')
  }
  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.androidCase(8850).ignoreOn(MBTPlatform.Android)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.turnOnTab().nextMessage('subj1').nextMessage('subj2')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ShortSwipeContextMenuMoveToFolderAction(0, DefaultFolderName.mailingLists))
      .then(new MarkAsRead(0))
      .then(new GroupModeInitialSelectAction(0))
      .then(new OpenTabByTabNotificationAction(DefaultFolderName.mailingLists))
  }
}

export class MailListViewInThreadFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в тредовальной папке')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6820).iosCase(6774)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.sent)
      .nextMessage('subj1')
      .nextThread('subj2', 3)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new AssertSnapshotAction(this.description))
  }
}

export class MailListViewInThreadLessFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в не тредовальной папке')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6821).iosCase(6775)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextThread('subj1', 2)
      .nextThread('subj2', 2)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
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
      .then(new MarkAsImportant(0))
      .then(new GroupModeInitialSelectAction(2))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeSelectAction(0))
      .then(new GroupModeArchiveAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class MailListViewInThreadFolderLandscape extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в тредовальной папке в портретном режиме')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6822).iosCase(6776)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextMessage('subj1')
      .nextThread('subj2', 3)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
  }
}

export class MailListViewInThreadLessFolderLandscape extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в нетредовальной папке в режиме портрета')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6823).iosCase(6777)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.trash)
      .nextMessage('subj1')
      .nextThread('subj2', 3)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class MailListViewInUnreadLabel extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в метке Непрочитанные')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6824).iosCase(11119)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextThread('subj2', 3)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenFolderListAction()).then(new GoToFilterUnreadAction())
  }
}

export class MailListViewInUnreadLabelLandscape extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в метке Непрочитанные в режиме портрета')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6825).iosCase(11120)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextThread('subj2', 3)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFilterUnreadAction())
      .then(new RotateToLandscape())
  }
}

export class MailListViewInImportantLabel extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в метке Важные')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6826).iosCase(6780)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextThread('subj2', 3)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeMarkImportantAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class MailListViewInUserLabel extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в Пользовательской метке')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9659).iosCase(10778)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextThread('subj3', 3).createLabel(new LabelData('label1'))
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new MarkAsImportant(0))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeApplyLabelsAction(['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class MailListViewInThreadFolderTab extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение списка писем в тредовальной папке в планшетах')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6831).setTags([DeviceType.Tab])
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextThread('subj2', 3)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new AssertAction())
      .then(new OpenMessageAction(0))
  }
}

export class MailListViewInThreadLessFolderTab extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Вид списка писем в нетредовальной папке в планшетах')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6832).setTags([DeviceType.Tab])
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextThread('subj1', 2)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkSpamAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class MailListViewInImportantLabelTab extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение писем в метке Важные в планшетах')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10868).iosCase(6781).setTags([DeviceType.Tab])
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextThread('subj2', 3)
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeMarkImportantAction())
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new AssertAction())
      .then(new OpenMessageAction(0))
  }
}

export class MailListViewNavigateByArrowsTab extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Переключение между письмами в просмотре письма по стрелкам')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6857).iosCase(6811).ignoreOn(MBTPlatform.IOS).setTags([DeviceType.Tab])
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new ArrowDownClickAction(0))
      .then(new AssertAction())
      .then(new ArrowUpClickAction())
  }
}

export class MailListViewInUnreadLabel2pane extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView.2pane. Вид писем в метке Непрочитанные с открытым письмом')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10867).iosCase(6779).setTags([DeviceType.Tab])
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3').nextMessage('subj4').nextMessage('subj5')
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToFilterUnreadAction())
      .then(new AssertAction())
      .then(new OpenMessageAction(0))
      .then(new AssertAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenMessageAction(1))
  }
}

export class MailListViewInbox extends RegularYandexMailTestBase {
  public constructor() {
    super('MessageListView. Отображение списка писем папки Входящие.')
  }
  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10777).iosCase(8)
  }
  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('user_subj3'),
      )
  }
  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new AssertAction())
      .then(new MarkAsRead(1))
      .then(new AssertAction())
  }
}
