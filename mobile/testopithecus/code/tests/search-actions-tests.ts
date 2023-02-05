import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { MarkAsRead, MarkAsUnread } from '../mail/actions/base-actions/markable-actions'
import { GoToFilterImportantAction } from '../mail/actions/left-column/filter-navigator-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import {
  ShortSwipeContextMenuApplyLabelsAction,
  ShortSwipeContextMenuMarkAsImportantAction,
  ShortSwipeContextMenuMarkAsSpamAction,
} from '../mail/actions/messages-list/context-menu-actions'
import {
  GroupModeApplyLabelsAction,
  GroupModeDeleteAction,
  GroupModeInitialSelectAction,
  GroupModeMarkAsReadAction,
  GroupModeMarkAsUnreadAction,
} from '../mail/actions/messages-list/group-mode-actions'
import {
  ArchiveMessageByLongSwipeAction,
  DeleteMessageByLongSwipeAction,
} from '../mail/actions/messages-list/long-swipe-actions'
import { DeleteMessageByShortSwipeAction } from '../mail/actions/messages-list/short-swipe-actions'
import { AddFolderToSearchAction } from '../mail/actions/search/advanced-search-actions'
import { CloseSearchAction, OpenSearchAction, SearchAllMessagesAction } from '../mail/actions/search/search-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  SetActionOnSwipe,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { ActionOnSwipe } from '../mail/feature/settings/general-settings-feature'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName, FolderBackendName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class SearchAndMoveToSpamMessage extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Short swipe menu Пометить спамом')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7379)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new ShortSwipeContextMenuMarkAsSpamAction(0))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
  }
}

export class SearchAndDeleteMessageFromUserFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Swipe to delete из результатов поиска по всем папкам')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10157)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('UserFolder').switchFolder('UserFolder').nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new DeleteMessageByLongSwipeAction(0))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class SearchAndDeleteMessageShortSwipeFromTemplates extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Short swipe. Удаление по кнопке из папки Шаблоны')
  }

  public setupSettings(settings: TestSettings): void {
    // com.yandex.mail.xmail.PromiseException: Failure from Throwable: javax.mail.FolderNotFoundException: Templates not found
    // Underlying stack trace: javax.mail.FolderNotFoundException: Templates not found
    settings.androidCase(7361).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(FolderBackendName.templates).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.template))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new DeleteMessageByShortSwipeAction(0))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class SearchAndGroupDeleteMessageTestFromTemplates extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Group operation. Удаление письма из папки Шаблоны')
  }

  public setupSettings(settings: TestSettings): void {
    // com.yandex.mail.xmail.PromiseException: Failure from Throwable: javax.mail.FolderNotFoundException: Templates not found
    // Underlying stack trace: javax.mail.FolderNotFoundException: Templates not found
    settings.androidCase(7362).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.template).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.template))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeDeleteAction())
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class SearchAndGroupDeleteMessageTestFromDraft extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Group operation. Удаление письма из папки Черновики')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7362)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.draft).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.draft))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeDeleteAction())
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}

export class SearchAndArchiveMessageLongSwipe extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Swipe to archive из результатов поиска по всем папкам')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10568)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1')
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
      .then(new ArchiveMessageByLongSwipeAction(0))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
  }
}

export class SearchAndMarkImportantMessageShortSwipe extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Short swipe menu. Пометка письма Важным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7371).ignoreOn(MBTPlatform.IOS)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.draft).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.draft))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new ShortSwipeContextMenuMarkAsImportantAction(0))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFilterImportantAction())
  }
}

export class SearchAndMarkMessageRead extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Group operation. Пометка письма из папки Архив прочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7356)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.archive).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkAsReadAction())
  }
}

export class SearchAndMarkMessageReadFromUserFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Group operation. Пометка письма из user subfolder прочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7356)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('subfolder', ['folder']).switchFolder('subfolder', ['folder']).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('subfolder', ['folder']))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkAsReadAction())
  }
}

export class SearchAndMarkMessageUnreadFromUserFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Group operation. Пометка письма из user subfolder непрочитанным')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7357)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createFolder('subfolder', ['folder']).switchFolder('subfolder', ['folder']).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('subfolder', ['folder']))
      .then(new MarkAsRead(0))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkAsUnreadAction())
  }
}

export class SearchAndAddLabelMessageFromUserFolder extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Добавить user метку на письмо из user subfolder через меню действий')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7377)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('subfolder', ['folder'])
      .switchFolder('subfolder', ['folder'])
      .nextMessage('subj1')
      .createLabel(new LabelData('label1'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new ShortSwipeContextMenuApplyLabelsAction(0, ['label1']))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class SearchAndAddLabelMessageFromArchive extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Добавить user метку на письмо из Архива через меню действий письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7377)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.archive).nextMessage('subj1').createLabel(new LabelData('label1'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.archive))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new ShortSwipeContextMenuApplyLabelsAction(0, ['label1']))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class SearchAndAddLabelMessage extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Поставить пользовательскую метку на письмо по селекту')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6090)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj1').createLabel(new LabelData('label1'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeApplyLabelsAction(['label1']))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
  }
}

export class SearchAndMarkMessageUnreadBySwipeFromSent extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Пометить письмо непрочитанным по свайпу')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7355)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.sent).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
      .then(new MarkAsRead(0))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new MarkAsUnread(0))
  }
}

export class SearchAndMarkMessageUnreadFromSpam extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Group operation. Пометка непрочитанным письма из папки Spam')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9528)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.spam))
      .then(new MarkAsRead(0))
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new AddFolderToSearchAction(DefaultFolderName.spam))
      .then(new GroupModeInitialSelectAction(0))
      .then(new GroupModeMarkAsUnreadAction())
  }
}

export class SearchAndDeleteMessageFromSpam extends RegularYandexMailTestBase {
  public constructor() {
    super('Search. Short swipe menu. Удаление письма в поиске по папке Spam')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(9529)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.switchFolder(DefaultFolderName.spam).nextMessage('subj1')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchAllMessagesAction())
      .then(new AddFolderToSearchAction(DefaultFolderName.spam))
      .then(new DeleteMessageByShortSwipeAction(0))
      .then(new CloseSearchAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.trash))
  }
}
