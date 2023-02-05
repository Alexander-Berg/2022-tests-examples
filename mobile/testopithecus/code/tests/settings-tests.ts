import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { ComposeOpenAction } from '../mail/actions/compose/compose-actions'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import {
  ArchiveMessageByLongSwipeAction,
  DeleteMessageByLongSwipeAction,
} from '../mail/actions/messages-list/long-swipe-actions'
import { SwipeDownMessageListAction } from '../mail/actions/messages-list/message-list-actions'
import { OpenAboutSettingsAction } from '../mail/actions/settings/about-settings-actions'
import {
  ChangeSignatureAction,
  CloseAccountSettingsAction,
  OpenAccountSettingsAction,
} from '../mail/actions/settings/account-settings-actions'
import {
  ClearCacheAction,
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  SetActionOnSwipe,
  TurnOffCompactMode,
  TurnOnCompactMode,
  TapToClearCacheAndCancelAction,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { ActionOnSwipe } from '../mail/feature/settings/general-settings-feature'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { DeviceType, MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class OpenAboutSettingsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. Открытие экрана About в настройках и проверка версии приложения')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(502).androidCase(6060)
  }

  public prepareAccount(builder: MailboxBuilder): void {
    builder.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAboutSettingsAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ClearCacheTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General setting. Удаление кэша')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(8318).androidCase(7456)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('subj')
      .nextMessage('subj2')
      .nextMessage('subj3')
      .nextMessage('subj4')
      .nextMessage('subj5')
      .nextMessage('subj6')
      .nextMessage('subj7')
      .nextMessage('subj8')
      .nextMessage('subj9')
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextMessage('usr_subj')
      .nextMessage('usr_subj2')
      .nextMessage('usr_subj3')
      .nextMessage('usr_subj4')
      .nextMessage('usr_subj5')
      .nextMessage('usr_subj6')
      .nextMessage('usr_subj7')
      .nextMessage('usr_subj8')
      .nextMessage('usr_subj9')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new ClearCacheAction())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction('UserFolder'))
      .then(new AssertAction())
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new ClearCacheAction())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction('UserFolder'))
  }
}

export class OpenAccountSettingsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings: Открытие настроек первого аккаунта в списке')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(6060).ignoreOn(MBTPlatform.IOS)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
  }
}

export class ValidateGeneralSettingsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Отображение общих настроек.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(403).androidCase(6096)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ValidateRootSettingsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Отображение главного экрана настроек.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(499).androidCase(6096)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ValidateAccountSettingsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. Account settings. Отображение настроек аккаунта.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(409).androidCase(7475)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new AssertSnapshotAction(this.description))
  }
}

export class TurningOnCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Включение компактного режима.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(21).androidCase(7442)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('Inbox subj1').nextMessage('Inbox subj2')
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

export class TurningOffCompactModeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Выключение компактного режима.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10890).androidCase(7443)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder('UserFolder')
      .switchFolder('UserFolder')
      .nextMessage('UserFolder subj1')
      .nextMessage('UserFolder subj2')
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
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOffCompactMode())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction('UserFolder'))
  }
}

export class ChangingActionOnSwipeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Изменение действия по свайпу.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(406).androidCase(7449)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('Inbox subj1')
      .nextMessage('Inbox subj2')
      .nextMessage('Inbox subj3')
      .nextMessage('Inbox subj4')
      .nextMessage('Inbox subj5')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new DeleteMessageByLongSwipeAction(1))
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SetActionOnSwipe(ActionOnSwipe.archive))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ArchiveMessageByLongSwipeAction(2))
  }
}

// TODO: register after fix compose
export class ChangingSignatureTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Изменение подписи.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(412).androidCase(7466)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('Inbox subj1')
      .nextMessage('Inbox subj2')
      .nextMessage('Inbox subj3')
      .nextMessage('Inbox subj4')
      .nextMessage('Inbox subj5')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new ChangeSignatureAction('--\nNew signature'))
      .then(new CloseAccountSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new ComposeOpenAction())
  }
}

export class DefaultActionOnSwipeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Действие по свайпу по-умолчанию.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7448)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextMessage('Inbox subj1')
      .nextMessage('Inbox subj2')
      .nextMessage('Inbox subj3')
      .nextMessage('Inbox subj4')
      .nextMessage('Inbox subj5')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new AssertAction())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new DeleteMessageByLongSwipeAction(2))
  }
}

export class ViewAccountSettingsInLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. Account settings. Отображение настроек аккаунта в landscape.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7476)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
  }
}

export class ViewSettingsInLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Отображение настроек в landscape.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7461)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new RotateToLandscape())
      .then(new AssertAction())
      .then(new OpenGeneralSettingsAction())
  }
}

export class ViewSettingsTestTab extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. General settings. Отображение настроек в landscape 2pane.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(7462).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new RotateToLandscape())
      .then(new AssertAction())
      .then(new OpenGeneralSettingsAction())
  }
}

export class UndoClearCacheTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Settings. Main settings. Отказаться от очиски кэша.')
  }

  public setupSettings(settings: TestSettings): void {
    settings.androidCase(10830)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextManyMessage(42)
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new SwipeDownMessageListAction())
      .then(new SwipeDownMessageListAction())
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TapToClearCacheAndCancelAction())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
  }
}
