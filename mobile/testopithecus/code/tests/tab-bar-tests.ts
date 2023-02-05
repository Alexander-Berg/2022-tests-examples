import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { ComposeOpenAction } from '../mail/actions/compose/compose-actions'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFilterImportantAction } from '../mail/actions/left-column/filter-navigator-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import { GroupModeInitialSelectAction } from '../mail/actions/messages-list/group-mode-actions'
import { OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import { OpenSearchAction, SearchAllMessagesAction } from '../mail/actions/search/search-actions'
import { OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import {
  ShtorkaCloseBySwipeAction,
  ShtorkaCloseByTapOverAction,
  ShtorkaTapOnItemAction,
  TabBarTapOnItemAction,
} from '../mail/actions/tab-bar-actions'
import { TabBarItem } from '../mail/feature/tab-bar-feature'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class TabBarOpenCalendarTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Открытие календаря')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10447)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new AssertAction()).then(new TabBarTapOnItemAction(TabBarItem.calendar))
  }
}

export class TabBarOpenTelemostTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Открытие телемоста')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10452)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new AssertAction()).then(new TabBarTapOnItemAction(TabBarItem.telemost))
  }
}

export class TabBarOpenDocumentsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Открытие документов')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(12232)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new AssertAction()).then(new TabBarTapOnItemAction(TabBarItem.documents))
  }
}

export class TabBarOpenMoreTabTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Открытие вкладки Еще')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10457)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new AssertAction()).then(new TabBarTapOnItemAction(TabBarItem.more))
  }
}

export class TabBarItemsYandexLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Элементы таб бара у Яндекс аккаунта в лендскейп')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10432)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new AssertAction()).then(new RotateToLandscape())
  }
}

// TODO: научиться переводить время на девайсе
export class TabBarCalendarDateLabelTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Текущая дата на иконке Календаря')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10437)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
  }
}

export class TabBarShownInMailListTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Наличие таб бара в списке писем')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10438)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject').createFolder('UserFolder').createLabel(new LabelData('UserLabel'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('UserLabel'))
  }
}

export class TabBarNotShownInMailViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Отсутствие таб бара в Просмотре письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10439)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenMessageAction(0))
  }
}

export class TabBarNotShownInSearchTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Отсутствие таб бара в Поиске')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10440)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new AssertAction())
      .then(new SearchAllMessagesAction())
  }
}

export class TabBarNotShownInComposeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Отсутствие таб бара в Компоузе')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10441)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new ComposeOpenAction())
  }
}

export class TabBarNotShownInSettingsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Отсутствие таб бара в Настройках')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10442)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenFolderListAction()).then(new OpenSettingsAction())
  }
}

export class TabBarNotShownInFolderListTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Отсутствие таб бара в Списке папок')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10443)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenFolderListAction())
  }
}

export class TabBarHideInGroupModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('TabBar. Перекрытие таб бара тулбаром при переходе в режим групповых операций')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10445)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new GroupModeInitialSelectAction(0))
      .then(new AssertAction())
      .then(new RotateToLandscape())
  }
}

export class ShtorkaOpenNotesTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Shtorka. Открытие заметок, если приложение Яндекс.Диск не установлено')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10492)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('UserFolder').nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new TabBarTapOnItemAction(TabBarItem.more))
      .then(new AssertAction())
      .then(new ShtorkaTapOnItemAction(TabBarItem.notes))
  }
}

export class ShtorkaOpenDiskTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Shtorka. Открытие Диска, если приложение Диск не установлено')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10572)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder('UserFolder').nextMessage('subject')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new TabBarTapOnItemAction(TabBarItem.more))
      .then(new AssertAction())
      .then(new ShtorkaTapOnItemAction(TabBarItem.disk))
  }
}

export class ShtorkaCloseBySwipeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Shtorka. Закрытие шторки свайпом вниз')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10498)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new TabBarTapOnItemAction(TabBarItem.more))
      .then(new AssertAction())
      .then(new ShtorkaCloseBySwipeAction())
  }
}

export class ShtorkaCloseByTapOverTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Shtorka. Закрытие шторки при тапе на область над шторкой')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10497)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
      .then(new TabBarTapOnItemAction(TabBarItem.more))
      .then(new AssertAction())
      .then(new ShtorkaCloseByTapOverAction())
  }
}
