import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RotateToLandscape } from '../mail/actions/general/rotatable-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import {
  QuickReplySetTextFieldAction,
  QuickReplyTapOnComposeButtonAction,
  QuickReplyTapOnSendButtonAction,
  QuickReplyTapOnTextFieldAction,
} from '../mail/actions/opened-message/quick-reply-actions'
import {
  CloseGeneralSettingsAction,
  OpenGeneralSettingsAction,
  TurnOffSmartReplyAction,
} from '../mail/actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class QuickReplyToMessageTests extends RegularYandexMailTestBase {
  public constructor() {
    super('QuickReply. Отправка ответа на одиночное письмо')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7667).androidCase(6536)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .sendMessageViaMobileApi()
      .nextCustomMessage(new MessageSpecBuilder().withDefaults().withTextBody('Как дела?').withSubject('Тема письма'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new QuickReplyTapOnTextFieldAction())
      .then(new QuickReplySetTextFieldAction('Хорошо'))
      .then(new QuickReplyTapOnSendButtonAction())
  }
}

export class QuickReplyIsTextFieldExpandedTests extends RegularYandexMailTestBase {
  public constructor() {
    super('QuickReply. Растягивание текстового поля')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7672).androidCase(6541)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .sendMessageViaMobileApi()
      .nextCustomMessage(new MessageSpecBuilder().withDefaults().withTextBody('Как дела?').withSubject('Тема письма'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new QuickReplyTapOnTextFieldAction())
      .then(new QuickReplySetTextFieldAction('Хорошо\n\n\n'))
  }
}

export class QuickReplyOpenFilledComposeTests extends RegularYandexMailTestBase {
  public constructor() {
    super('QuickReply. Переход в композ из заполненного Quick Reply')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7669).androidCase(6538)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .sendMessageViaMobileApi()
      .nextCustomMessage(new MessageSpecBuilder().withDefaults().withTextBody('Как дела?').withSubject('Тема письма'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new QuickReplyTapOnTextFieldAction())
      .then(new QuickReplySetTextFieldAction('Хорошо'))
      .then(new AssertAction())
      .then(new QuickReplyTapOnComposeButtonAction())
  }
}

export class QuickReplyRotateTests extends RegularYandexMailTestBase {
  public constructor() {
    super('QuickReply. Изменение ориентации устройства с введенным текстом')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7685).androidCase(6554)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .sendMessageViaMobileApi()
      .nextCustomMessage(new MessageSpecBuilder().withDefaults().withTextBody('Как дела?').withSubject('Тема письма'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new QuickReplyTapOnTextFieldAction())
      .then(new QuickReplySetTextFieldAction('Хорошо'))
      .then(new RotateToLandscape())
  }
}

export class SmartReplyMissingIfSettingsDisabledTest extends RegularYandexMailTestBase {
  public constructor() {
    super('SmartReply. Отсутствие умных ответов при выключенной настройке')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7683).androidCase(6552)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .sendMessageViaMobileApi()
      .nextCustomMessage(new MessageSpecBuilder().withDefaults().withTextBody('Как дела?').withSubject('Тема письма'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOffSmartReplyAction())
      .then(new CloseGeneralSettingsAction())
      .then(new CloseRootSettings())
      .then(new GoToFolderAction(DefaultFolderName.inbox))
      .then(new OpenMessageAction(0))
  }
}

export class SmartReplyShowIfSettingsEnabledTest extends RegularYandexMailTestBase {
  public constructor() {
    super('SmartReply. Показ умных ответов при включенной настройке')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7682).androidCase(6551).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .sendMessageViaMobileApi()
      .nextCustomMessage(new MessageSpecBuilder().withDefaults().withTextBody('Как дела?').withSubject('Тема письма'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new OpenMessageAction(0))
  }
}
