import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import { MarkAsRead } from '../mail/actions/base-actions/markable-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { MessageViewBackToMailListAction, OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import { CloseSearchAction, OpenSearchAction, SearchAllMessagesAction } from '../mail/actions/search/search-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  TurnOnCompactMode,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { MBTPlatform, TestSettings, DeviceType } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class MarkAsReadInSearch extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Пометка свайпом письма прочитанным в поисковой выдаче')
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').nextMessage('subj2')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new MarkAsRead(0))
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(3462).iosCase(1207)
  }
}

export class SearchMessageListViewInCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Отображение писем в поиске в компактном режиме')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(3462).iosCase(5807).ignoreOn(MBTPlatform.Android) // TODO: need to implement compact mode on android
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
      .then(new AssertSnapshotAction(this.description))
  }
}

export class SearchAndOpenMessage extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Открытие письма по запросу')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(612).androidCase(7403)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new MessageViewBackToMailListAction())
  }
}

export class SearchAndOpenMessageIn2Pane extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. 2Pane. Поиск письма по запросу и открытие на просмотр')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(609).androidCase(7400).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new OpenMessageAction(0))
      .then(new CloseSearchAction())
  }
}
