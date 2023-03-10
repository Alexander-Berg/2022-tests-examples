import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { TestSettings, DeviceType, MBTPlatform } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { MarkAsRead } from '../mail/actions/base-actions/markable-actions'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFilterImportantAction } from '../mail/actions/left-column/filter-navigator-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import { MessageViewContextMenuMoveToFolderAction } from '../mail/actions/messages-list/context-menu-actions'
import {
  GroupModeApplyLabelsAction,
  GroupModeArchiveAction,
  GroupModeDeleteAction,
  GroupModeInitialSelectAction,
  GroupModeMarkAsReadAction,
  GroupModeMarkAsUnreadAction,
  GroupModeMarkImportantAction,
  GroupModeMarkNotSpamAction,
  GroupModeMarkSpamAction,
  GroupModeMarkUnimportantAction,
  GroupModeMoveToFolderAction,
  GroupModeRemoveLabelsAction,
  GroupModeSelectAllAction,
  GroupModeSelectAction,
  GroupModeUnselectAllAction,
  GroupModeUnselectMessageAction,
} from '../mail/actions/messages-list/group-mode-actions'
import { MarkAsReadFromShortSwipeAction } from '../mail/actions/messages-list/short-swipe-actions'
import { UndoArchiveAction, UndoDeleteAction, UndoSpamAction } from '../mail/actions/messages-list/undo-actions'
import { MessageViewBackToMailListAction, OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import { CloseSearchAction, OpenSearchAction, SearchAllMessagesAction } from '../mail/actions/search/search-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  TurnOnCompactMode,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { SwitchContextToAction } from '../mail/actions/switch-context-in-2pane-actions'
import { MaillistComponent } from '../mail/components/maillist-component'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName, FolderBackendName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class GroupMarkAsReadDifferentMessagesTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ?????????????????????? ?? ?????????? ?????????????????????????? ???????????? ????????????????????????')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkAsReadAction())
  }
}

export class CanOpenMessageAfterGroupActionTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ???????????? ???? ???????????????? ?????????? ???????????? ???? ???????????????????? ????????????')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new MarkAsRead(1))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkAsUnreadAction())
      .then(new OpenMessageAction(0))
  }
}

export class GroupDeleteMessagesTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ?????????????????? ?????????? ???? ?????????? ??????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6220).androidCase(6475)
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
      .then(new GoToFolderAction(DefaultFolderName.draft))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeDeleteAction())
  }
}

export class GroupModeMarkImportantTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ?????????????????? ?????????? ??????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(27).androidCase(6491)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.archive).nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeMarkImportantAction())
  }
}

export class GroupModeUnmarkImportantTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ?????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6238).androidCase(6492)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeMarkImportantAction())
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeMarkUnimportantAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class GroupModeMarkLabelTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????????? ???????????????????????????????? ??????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6245).androidCase(6499)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createLabel(new LabelData('label1'))
      .createLabel(new LabelData('label2'))
      .nextMessage('subj1')
      .nextThread('thread2', 3)
      .nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeApplyLabelsAction(['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class GroupModeUnmarkLabelTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ???????????????????????????????? ??????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6248).androidCase(6502)
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
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('subj3'),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeRemoveLabelsAction(['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class GroupModeMarkReadMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ?????????? ?????????????????????? ?? ?????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6513).androidCase(6512)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkImportantAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkAsReadAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class GroupModeMarkUnreadMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ?????????? ?????????????????????????? ?? ???????????????????????????????? ??????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6514).androidCase(6515)
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
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('subj3'),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkAsReadAction())
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(0))
      .then(new GroupModeMarkAsUnreadAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class GroupModeMarkSpamTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????????????? ?????????????????? ?????????? ?? ????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6233).androidCase(6070)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(2))
      .then(new GroupModeMarkSpamAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class GroupModeArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????????????? ?????????????????? ???????????? ?? ??????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6230).androidCase(6484)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 3).nextThread('thread3', 2)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(2))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeArchiveAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class GroupModeArchiveMessagesTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????????????? ?????????????????? ?????????? ?? ?????????? ???? ???????????????????????????????? ??????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6229).androidCase(6483)
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
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('subj3'),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(0))
      .then(new GroupModeArchiveAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class GroupModeCancelSelectionTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????? ???? ???????????? ?????????????????? ???????????????? ?????????? ???? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6508).androidCase(6509)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject1').nextMessage('subject2').nextMessage('subject3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(2))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeUnselectAllAction())
  }
}

export class GroupModeSelectAllTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ?????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6510).androidCase(6511)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject1').nextMessage('subject2').nextMessage('subject3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new GroupModeInitialSelectAction(1)).then(new GroupModeSelectAllAction())
  }
}

export class GroupModeMarkReadThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ?????????????????? ?????????? ????????????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6215).androidCase(6470)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject1').nextMessage('subject2').nextMessage('subject3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeMarkAsReadAction())
  }
}

export class GroupMarkAsReadMessagesTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ?????????????????? ?????????? ????????????????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6216).androidCase(6471)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('folder').switchFolder('folder').nextMessage('subj1').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('folder'))
      .then(new MarkAsRead(0))
      .then(new MarkAsRead(1))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkAsUnreadAction())
  }
}

export class GroupModeMarkUnreadThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ???????? ?????????? ?????????????????????? ?????????? ????????????????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6516).androidCase(6517)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 5).nextThread('thread2', 7).nextThread('thread3', 2)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkAsUnreadAction())
  }
}

export class GroupModeDeleteThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ?????????? ?????? ???????????? ???????????????? ?????????? ?? ?????????? ????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6518).androidCase(6519)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 7).nextThread('thread3', 1)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeDeleteAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class GroupModeMarkSpamThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????????????? ?? ???????? ?????????? ?? ???????????????? ???? ???????????? ??????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6234).androidCase(6488)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 2).nextThread('thread2', 7).nextThread('thread3', 1).createFolder('AutotestFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(1))
      .then(new MessageViewContextMenuMoveToFolderAction('AutotestFolder'))
      .then(new MessageViewBackToMailListAction())
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeMarkSpamAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class GroupModeUnmarkSpamTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ???????????? ?????? ???? ????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6520).androidCase(6535)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage('subject1').nextMessage('subject2').nextMessage('subject3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeMarkNotSpamAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class GroupModeMoveThreadsToInboxTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????????????? ???????????? ???? ???????????????????????????????? ?????????? ???? ????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6505).androidCase(6506)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('AutoTestFolder')
      .switchFolder('AutoTestFolder')
      .nextThread('thread1', 2)
      .nextThread('thread2', 3)
      .nextThread('thread3', 4)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('AutoTestFolder'))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeMoveToFolderAction(DefaultFolderName.inbox))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}

export class GroupModeMoveMessageToUserFolderFromInboxTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????????????? ?????????? ?? ???????????? ??????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(6243).androidCase(6497)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject1').nextMessage('subject2').nextMessage('subject3').createFolder('AutotestFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMoveToFolderAction('AutotestFolder'))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('AutotestFolder'))
  }
}

export class GroupModeInitialSelectTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ?? ?????????? ?????????????????? ????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(3).androidCase(6068)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject1').nextMessage('subject2').nextMessage('subject3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new GroupModeInitialSelectAction(1)).then(new GroupModeSelectAction(2))
  }
}

export class GroupModeExitByTapOnSelectedMessagesTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????? ???? ???????????? ?????????????????? ???????????????? ?????????? ???? ???????????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(4).androidCase(6069)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject1').nextMessage('subject2').nextMessage('subject3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeSelectAction(0))
      .then(new GroupModeUnselectMessageAction(2))
      .then(new GroupModeUnselectMessageAction(0))
      .then(new GroupModeUnselectMessageAction(1))
  }
}

export class GroupModeDeleteFromTrashTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ???????????? ???? ?????????? ??????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8979).androidCase(10142) // ?????? ?????? ???????? ?????????? ?????????????? ??????????, ?????????? ???????????????? ???????????????? ?? ?????????????????????? ??????????????. ?????????? ???? ???????????????? ??????????????, ?????? ?????? ???????? ?????????? ???? ?????????????????? ???????? ??????????
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('Trash').nextMessage('subject1').nextMessage('subject2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('Trash'))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeDeleteAction())
  }
}

export class GroupModeInitialSelectInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ?? group mode ?? ?????????? ???? ???????? ?? ???????????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(643).androidCase(10759)
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
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeUnselectAllAction())
  }
}

export class GroupModeDeleteMessageInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ???????????? ?? ???????????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(642).androidCase(10760)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserSubfolder', ['UserFolder'])
      .switchFolder('UserSubfolder', ['UserFolder'])
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction('UserSubfolder', ['UserFolder']))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeDeleteAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class GroupModeMarkAsSpamNotSpamMessageInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ???????????? ???????????? - ???? ???????????? ?? ???????????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(644).androidCase(10761)
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
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeMarkSpamAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkNotSpamAction())
  }
}

export class GroupModeArchiveMessageFromSearchInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????????????????? ???????????? ?? ???????????? ?? ???????????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(645).androidCase(10762)
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
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeArchiveAction())
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class GroupModeSelectAllMessagesInSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Group mode. ?????????? ???????? ?????????? ?? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8982).androidCase(10145)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .switchFolder('UserFolder')
      .nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAllAction())
  }
}

export class GroupModeMarkImportantUnimportantMessageInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ???????????? ????????????-???????????????? ?? ???????????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(646).androidCase(10763).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('inbox_subj1')
      .nextMessage('inbox_subj2')
      .nextMessage('inbox_subj3')
      .switchFolder(DefaultFolderName.sent)
      .nextMessage('sent_subj1')
      .nextMessage('sent_subj2')
      .nextMessage('sent_subj3')
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
      .then(new GroupModeMarkImportantAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkImportantAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkUnimportantAction())
  }
}

export class GroupModeAddRemoveLabelInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ????????????????????-???????????? ???????????????????????????????? ?????????? ?? ???????????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(648).androidCase(10764)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createLabel(new LabelData('label1'))
      .nextMessage('inbox_subj1')
      .nextMessage('inbox_subj2')
      .nextMessage('inbox_subj3')
      .switchFolder('UserFolder')
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1'), new LabelData('label2')])
          .withSubject('user_subj1'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1'), new LabelData('label2')])
          .withSubject('user_subj2'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1'), new LabelData('label2')])
          .withSubject('user_subj3'),
      )
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
      .then(new GroupModeMarkImportantAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeApplyLabelsAction(['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeRemoveLabelsAction(['label2']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label2'))
  }
}

export class GroupModeUndoDeleteThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ???????????????? ?????????? ?? ?????????? ????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8608).androidCase(10765)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('thread1', 3).nextMessage('subj2').nextMessage('subj3').nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeDeleteAction())
      .then(new UndoDeleteAction())
  }
}

export class GroupModeUndoArchiveThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ?????????????????????????? ?????????? ?? ?????????? ????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8612).androidCase(10766)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj2').nextThread('thread1', 3).nextMessage('subj3').nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeArchiveAction())
      .then(new UndoArchiveAction())
  }
}

export class GroupModeUndoSpamThreadTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ?????????????? ?????????? ???????????? ?? ?????????? ????????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8618).androidCase(10767)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextThread('subj1', 3).nextThread('subj2', 4).nextThread('subj3', 3)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeMarkSpamAction())
      .then(new UndoSpamAction())
  }
}

export class GroupModeUndoArchiveMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ?????????????????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8613).androidCase(10768)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.trash)
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeArchiveAction())
      .then(new UndoArchiveAction())
  }
}

export class GroupModeUndoArchiveMessagesAndThreadsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ?????????????????????????? ???????????????????? ?????????? ?? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8614).androidCase(10769)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj2').nextThread('thread1', 3).nextMessage('subj3').nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(0))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeArchiveAction())
      .then(new UndoArchiveAction())
  }
}

export class GroupModeUndoSpamMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ?????????????? ???????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8616).androidCase(10770)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .switchFolder(DefaultFolderName.trash)
      .nextMessage('subj1')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeMarkSpamAction())
      .then(new UndoSpamAction())
  }
}

export class GroupModeUndoDeleteMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ???????????????? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8617).androidCase(10771)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createLabel(new LabelData('label1'))
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('subj1'),
      )
      .nextMessage('subj2')
      .nextMessage('subj3')
      .nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeDeleteAction())
      .then(new UndoDeleteAction())
  }
}

export class GroupModeUndoSpamMessagesAndThreadsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ?????????????? ???????????? ???????????????????? ?????????? ?? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8602).androidCase(10772)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj2').nextThread('thread1', 3).nextMessage('subj3').nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeMarkSpamAction())
      .then(new UndoSpamAction())
  }
}

export class GroupModeUndoDeleteMessagesAndThreadsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????? ???????????????? ???????????????????? ?????????? ?? ????????????')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8607).androidCase(10773)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextThread('thread1', 3)
      .nextMessage('subj2')
      .nextMessage('subj3')
      .nextMessage('subj4')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeDeleteAction())
      .then(new UndoDeleteAction())
  }
}

export class GroupModeMoveMessageFromTabInboxToUserFolderTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????????????? ???????????????????? ?????????????????? ???? ???????? ?? ??????????')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7584).androidCase(542).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .turnOnTab()
      .switchFolder(FolderBackendName.mailingLists)
      .nextMessage('msg1')
      .nextMessage('msg2')
      .nextMessage('msg3')
      .createFolder('UserFolder')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeMoveToFolderAction('UserFolder'))
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
  }
}

export class GroupModeMoveMessageToSpamFromSocialTabTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ???????????? ?? ???????? ???? ????????')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7592).androidCase(550).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .turnOnTab()
      .switchFolder(FolderBackendName.socialNetworks)
      .nextMessage('msg1')
      .nextMessage('msg2')
      .nextMessage('msg3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.socialNetworks))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkSpamAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class GroupModeApplyLabelToMessageInMailingListTabTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ???????????? ???????????????????????????????? ???????????? ?? ????????')
  }

  public setupSettings(settings: TestSettings): void {
    // TODO: delete ignore after fix crash " Abort message: 'ubsan: add-overflow'"
    settings.iosCase(7595).androidCase(553).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .turnOnTab()
      .createLabel(new LabelData('label1'))
      .createLabel(new LabelData('label2'))
      .switchFolder(FolderBackendName.mailingLists)
      .nextMessage('msg1')
      .nextMessage('msg2')
      .nextMessage('msg3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.mailingLists))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeApplyLabelsAction(['label1']))
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class GroupModeDeleteMessage2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ???????????????? ?????????? ?? 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8995).androidCase(10774).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenMessageAction(0))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new GroupModeInitialSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeDeleteAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class GroupModeMardMessagesAsRead2paneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('GroupMode. ?????????????? ?????????? ???????????????????????? ?? 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8997).androidCase(10775).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('UserFolder').nextMessage('subj1').nextMessage('subj2').nextMessage('subj3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new MarkAsReadFromShortSwipeAction(1))
      .then(new OpenMessageAction(0))
      .then(new SwitchContextToAction(new MaillistComponent()))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeSelectAction(1))
      .then(new GroupModeSelectAction(2))
      .then(new GroupModeMarkAsReadAction())
  }
}
