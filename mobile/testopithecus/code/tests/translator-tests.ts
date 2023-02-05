import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { DeviceType, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { MessageViewContextMenuShowTranslatorAction } from '../mail/actions/messages-list/context-menu-actions'
import { MessageViewBackToMailListAction, OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import {
  SettingsCloseIgnoredTranslationLanguageListAction,
  SettingsDeleteLanguageFromIgnoredAction,
  SettingsOpenDefaultTranslationLanguageListAction,
  SettingsOpenIgnoredTranslationLanguageListAction,
  SettingsSetDefaultTranslationLanguageAction,
  TranslatorBarTapOnCloseButtonAction,
  TranslatorBarTapOnRevertButtonAction,
  TranslatorBarTapOnSourceLanguageAction,
  TranslatorBarTapOnTargetLanguageAction,
  TranslatorBarTapOnTranslateButtonAction,
  TranslatorSetSourceLanguageAction,
} from '../mail/actions/translator-actions'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { TranslatorLanguageName } from '../mail/model/translator-models'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class HideTranslatorBarTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. Скрытие плашки переводчика')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5932).androidCase(9874)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Тело письма').withSubject('Тема письма'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new TranslatorBarTapOnSourceLanguageAction())
      .then(new AssertSnapshotAction(this.description))
      .then(new TranslatorSetSourceLanguageAction(TranslatorLanguageName.russian))
      .then(new TranslatorBarTapOnCloseButtonAction(true))
  }
}

export class AbsentOfTranslatorBarIfSourceLanguageIsEqualToTargetLanguageTests extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. Плашка отсутствует при совпадении языков ОС и приложения')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5926).androidCase(9889)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Message body').withSubject('Message subject'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new AssertAction())
      .then(new MessageViewContextMenuShowTranslatorAction())
  }
}

export class ResetTranslateAfterReopenMessageTests extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. Отсутствие переведенного текста после переоткрытия письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5927).androidCase(9878)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Тело письма').withSubject('Тема письма'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new TranslatorBarTapOnTranslateButtonAction())
      .then(new AssertAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenMessageAction(0))
  }
}

export class RevertToOriginalMessageLanguageTests extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. Вернуть оригинальный текст письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5937).androidCase(9872)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Тело письма').withSubject('Тема письма'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new TranslatorBarTapOnTranslateButtonAction())
      .then(new AssertAction())
      .then(new TranslatorBarTapOnRevertButtonAction())
  }
}

export class AddLanguageToRecentLanguagesListTests extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. Добавление языка перевода в список недавних')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5941).androidCase(9877)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Тело письма').withSubject('Тема письма'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new TranslatorBarTapOnSourceLanguageAction())
      .then(new TranslatorSetSourceLanguageAction(TranslatorLanguageName.afrikaans))
      .then(new TranslatorBarTapOnSourceLanguageAction())
      .then(new TranslatorSetSourceLanguageAction(TranslatorLanguageName.albanian))
      .then(new TranslatorBarTapOnSourceLanguageAction())
      .then(new TranslatorSetSourceLanguageAction(TranslatorLanguageName.amharic))
      .then(new TranslatorBarTapOnSourceLanguageAction())
      .then(new TranslatorSetSourceLanguageAction(TranslatorLanguageName.arabic))
      .then(new TranslatorBarTapOnSourceLanguageAction())
  }
}

export class AddLanguageToIgnoredLanguagesListAndDeleteTests extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. Добавление языка в список Отключенных и удаление из него')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5948).androidCase(9883)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Тело письма').withSubject('Тема письма'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new TranslatorBarTapOnSourceLanguageAction())
      .then(new TranslatorSetSourceLanguageAction(TranslatorLanguageName.russian))
      .then(new TranslatorBarTapOnCloseButtonAction(true))
      .then(new MessageViewBackToMailListAction())
      .then(new OpenMessageAction(0))
      .then(new AssertAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SettingsOpenIgnoredTranslationLanguageListAction())
      .then(new SettingsDeleteLanguageFromIgnoredAction(TranslatorLanguageName.russian))
      .then(new SettingsCloseIgnoredTranslationLanguageListAction())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new OpenMessageAction(0))
  }
}

export class HideTranslatorBarForAutoLanguageTests extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. Скрытие плашки переводчика для авто')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(10026)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Тело письма').withSubject('Тема письма'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new TranslatorBarTapOnCloseButtonAction(true))
      .then(new AssertAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenMessageAction(0))
      .then(new AssertAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
  }
}

export class TranslateMessage2paneTests extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. 2pane. Перевод письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5924).androidCase(9897).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Тело письма').withSubject('Тема письма'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new TranslatorBarTapOnTranslateButtonAction())
      .then(new RotateToLandscape())
  }
}

export class ChangeDefaultTranslateLanguageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Translator. Замена языка по умолчанию в настройках')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5939).androidCase(9885)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextCustomMessage(
      new MessageSpecBuilder().withDefaults().withTextBody('Тело письма').withSubject('Тема письма'),
    )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new AssertAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new SettingsOpenDefaultTranslationLanguageListAction())
      .then(new SettingsSetDefaultTranslationLanguageAction(TranslatorLanguageName.afrikaans))
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new OpenMessageAction(0))
      .then(new AssertAction())
      .then(new TranslatorBarTapOnTargetLanguageAction())
      .then(new AssertSnapshotAction(this.description))
  }
}
