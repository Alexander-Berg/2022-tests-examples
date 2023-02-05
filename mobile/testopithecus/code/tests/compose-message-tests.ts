import { Contact } from '../../../mapi/code/api/entities/contact/contact'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import {
  ComposeClearBodyAction,
  ComposeDeleteLastRecipientByTapOnBackspaceAction,
  ComposeDeleteRecipientByTapOnCrossAction,
  ComposeExpandExtendedRecipientFormAction,
  ComposeOpenAction,
  ComposeSendAction,
  ComposeSetBodyAction,
  ComposeSetRecipientFieldAction,
  ComposeSetSubjectAction,
  ComposeTapOnBodyFieldAction,
  ComposeTapOnRecipientFieldAction,
  ComposeTapOnRecipientSuggestByIndexAction,
  ComposeTapOnSenderFieldAction,
  ComposeTapOnSenderSuggestByIndexAction,
  ComposeTapOnSubjectFieldAction,
} from '../mail/actions/compose/compose-actions'
import { RotateToLandscape, RotateToPortrait } from '../mail/actions/general/rotatable-actions'
import { GoToFolderAction, OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import {
  MessageViewContextMenuOpenForwardComposeAction,
  MessageViewContextMenuOpenReplyComposeAction,
  ShortSwipeContextMenuOpenForwardComposeAction,
  ShortSwipeContextMenuOpenReplyComposeAction,
} from '../mail/actions/messages-list/context-menu-actions'
import { MessageViewBackToMailListAction, OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import { ComposeEmailProvider, ComposeRecipientFieldType } from '../mail/feature/compose/compose-features'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { DefaultFolderName } from '../mail/model/folder-data-model'
import { TextGenerator } from '../utils/mail-utils'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

// Reply/Forward
export class ComposeReplyViaShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Reply/Forward] Ответ на письмо через shortSwipeMenu')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11729)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuOpenReplyComposeAction(0))
      .then(new AssertAction())
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeReplyFromMailViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Reply/Forward] Ответ на письмо из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11717)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuOpenReplyComposeAction())
      .then(new AssertAction())
      .then(new ComposeSendAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeForwardViaShortSwipeMenuTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Reply/Forward] Пересылка письма через shortSwipeMenu')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11731)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ShortSwipeContextMenuOpenForwardComposeAction(0))
      .then(
        new ComposeSetRecipientFieldAction(
          ComposeRecipientFieldType.to,
          ComposeEmailProvider.instance.emailToReceiveFwdMessage,
        ),
      )
      .then(new AssertAction())
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeForwardFromMailViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Reply/Forward] Пересылка письма из просмотра')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11725)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenMessageAction(0))
      .then(new MessageViewContextMenuOpenForwardComposeAction())
      .then(
        new ComposeSetRecipientFieldAction(
          ComposeRecipientFieldType.to,
          ComposeEmailProvider.instance.emailToReceiveFwdMessage,
        ),
      )
      .then(new ComposeTapOnBodyFieldAction())
      .then(new ComposeSetBodyAction('Тело письма'))
      .then(new AssertAction())
      .then(new ComposeSendAction())
      .then(new MessageViewBackToMailListAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

// From
export class ComposeSelectSenderFromSuggestTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [From] Выбор адреса отправителя')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11640)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnSenderFieldAction())
      .then(new ComposeTapOnSenderSuggestByIndexAction(2))
  }
}

export class ComposeCloseSenderSuggestTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [From] Закрытие списка адресов отправителя тапом вне списка')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11635)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnSenderFieldAction())
      .then(new AssertAction())
      .then(new ComposeTapOnSenderFieldAction())
  }
}

// Suggest
export class ComposeEmptyRecipientsSuggestTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Саджест популярных контактов (контактов нет)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11609)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
  }
}

export class ComposeMinimizeRecipientsSuggestAfterSomeActionsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Скрытие саджеста после расфокусировки или повторного тапа в поле')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11639)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createContact(new Contact('name1', 'email1@example.com'))
      .createContact(new Contact('name2', 'email2@example.com'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new AssertAction())
      .then(new ComposeExpandExtendedRecipientFormAction())
  }
}

export class ComposeSuggestOfManyContactTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Саджест популярных контактов (больше 10ти)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11642)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createContact(new Contact('name1', 'email1@example.com'))
      .createContact(new Contact('name2', 'email2@example.com'))
      .createContact(new Contact('name3', 'email3@example.com'))
      .createContact(new Contact('name4', 'email4@example.com'))
      .createContact(new Contact('name5', 'email5@example.com'))
      .createContact(new Contact('name1', 'email6@example.com'))
      .createContact(new Contact('name2', 'email7@example.com'))
      .createContact(new Contact('name3', 'email8@example.com'))
      .createContact(new Contact('name4', 'email9@example.com'))
      .createContact(new Contact('name5', 'email10@example.com'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new AssertAction())
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.cc))
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.cc))
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.bcc))
  }
}

export class ComposeSuggestOfSomeContactTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Саджест популярных контактов (меньше 5ти)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11669)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createContact(new Contact('name1', 'email1@example.com'))
      .createContact(new Contact('name2', 'email2@example.com'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
  }
}

export class ComposeLongEmailInSuggestTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Длинное имя адрес в саджесте')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11659)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createContact(new Contact('thisisaverylongaccountloginyes', 'thisisaverylongaccountloginyes@yandex.ru'))
      .createContact(new Contact('longaccountname', 'yndx-very-very-longaccountname@yandex.ru'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientSuggestByIndexAction(0))
      .then(new AssertAction())
      .then(new ComposeSendAction())
  }
}

export class ComposeSuggestMissingAddedRecipientsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Отсутствие в саджесте уже добавленных получателей')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11614)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createContact(new Contact('name1', 'email1@example.com'))
      .createContact(new Contact('name2', 'email2@example.com'))
      .createContact(new Contact('name3', 'email3@example.com'))
      .createContact(new Contact('name4', 'email4@example.com'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientSuggestByIndexAction(0))
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new AssertAction())
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.cc))
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.cc, 'email2@example.com'))
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.bcc))
  }
}

export class ComposeSuggestDomainTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Саджест алиаса (доменный)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11617)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, 'example@', false))
      .then(new AssertAction())
      .then(new ComposeDeleteLastRecipientByTapOnBackspaceAction(ComposeRecipientFieldType.to))
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, 'example@y', false))
      .then(new AssertAction())
      .then(new ComposeDeleteLastRecipientByTapOnBackspaceAction(ComposeRecipientFieldType.to))
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, 'example@g', false))
      .then(new AssertAction())
      .then(new ComposeDeleteLastRecipientByTapOnBackspaceAction(ComposeRecipientFieldType.to))
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, 'example@m', false))
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientSuggestByIndexAction(0))
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeSuggestRotateTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Саджест контактов после изменения ориентации устройства')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11627)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createContact(new Contact('name1', 'email1@example.com'))
      .createContact(new Contact('name2', 'email2@example.com'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new RotateToLandscape())
      .then(new AssertAction())
      .then(new RotateToPortrait())
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new AssertAction())
      .then(new RotateToLandscape())
  }
}

export class ComposeSuggestBehaviorWhileEnterEmailTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Suggest] Ввод адреса и поведение саджеста')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11631)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createContact(new Contact('name1', 'email@example.com'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, 'email', false))
      .then(new AssertAction())
      .then(new ComposeDeleteLastRecipientByTapOnBackspaceAction(ComposeRecipientFieldType.to))
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, 'email1', false))
      .then(new AssertAction())
      .then(new ComposeDeleteLastRecipientByTapOnBackspaceAction(ComposeRecipientFieldType.to))
  }
}

// Yabbles
export class ComposeCreateYabbleWithLongEmailTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Yabbles] Формирование яббла с длинным адресом')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11641)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(
        new ComposeSetRecipientFieldAction(
          ComposeRecipientFieldType.to,
          'loginloginloginlogin@domaindomaindomaindomain.rurururururururururu',
        ),
      )
  }
}

export class ComposeCreateYabbleWithNumericEmailTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Yabbles] Формирование яббла с email состоящим из цифр')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11656)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, '123456@111.11'))
      .then(new AssertAction())
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.cc))
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.cc, '123456'))
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.bcc))
      .then(new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.bcc, '123456@test.domain'))
  }
}

export class ComposeDeleteYabbleByTapOnCrossTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Yabbles] Удаление яббла тапом на крестик')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11691)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createContact(new Contact('name1', 'email1@example.com'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.to))
      .then(new ComposeTapOnRecipientSuggestByIndexAction(0))
      .then(new AssertAction())
      .then(new ComposeDeleteRecipientByTapOnCrossAction(ComposeRecipientFieldType.to, 0))
      .then(new AssertAction())
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.cc))
      .then(
        new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.cc, ComposeEmailProvider.instance.validEmails[0]),
      )
      .then(new AssertAction())
      .then(new ComposeDeleteRecipientByTapOnCrossAction(ComposeRecipientFieldType.cc, 0))
      .then(new AssertAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.bcc))
      .then(
        new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.bcc, ComposeEmailProvider.instance.validEmails[1]),
      )
      .then(new AssertAction())
      .then(new ComposeDeleteRecipientByTapOnCrossAction(ComposeRecipientFieldType.bcc, 0))
  }
}

// Body
export class ComposeEnterAndDeleteTextLandscapeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Body] [landscape] Ввод и удаление текста при смене ориентации устройства')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11633)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeTapOnBodyFieldAction())
      .then(new ComposeSetBodyAction('Тело письма'))
      .then(new AssertAction())
      .then(new ComposeClearBodyAction())
      .then(new AssertAction())
      .then(new ComposeSetBodyAction('1 строка\n2 строка\n3 строка\n4 строка'))
      .then(new RotateToLandscape())
  }
}

// Sending
export class ComposeSendMessageWithLongSubjectTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Sending message] Отправка письма с длинной темой')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11600)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(
        new ComposeSetRecipientFieldAction(
          ComposeRecipientFieldType.to,
          ComposeEmailProvider.instance.getRandomValidEmail(),
        ),
      )
      .then(new ComposeTapOnSubjectFieldAction())
      .then(new ComposeSetSubjectAction(new TextGenerator().generateRandomString(TextGenerator.lowerCaseLatin, 800)))
      .then(new AssertAction())
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeSendMessageWithNotGeneratedValidYabbleTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Sending message] Отправка письма на несформированный валидный яббл')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11533)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(
        new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, ComposeEmailProvider.instance.validEmails[0]),
      )
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.cc))
      .then(
        new ComposeSetRecipientFieldAction(
          ComposeRecipientFieldType.cc,
          ComposeEmailProvider.instance.validEmails[1],
          false,
        ),
      )
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeSendMessageToRecipientWithLatinAndCyrillicLettersInEmailTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Sending message] Отправка письма получателю, email которого содержит кириллицу и латиницу')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11537)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(
        new ComposeSetRecipientFieldAction(
          ComposeRecipientFieldType.to,
          ComposeEmailProvider.instance.emailWithLatinAndCyrillicLetters,
        ),
      )
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeSendEmptyMessageTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Sending message] Отправка пустого письма')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11545)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(
        new ComposeSetRecipientFieldAction(
          ComposeRecipientFieldType.to,
          ComposeEmailProvider.instance.getRandomValidEmail(),
        ),
      )
      .then(new ComposeTapOnBodyFieldAction())
      .then(new ComposeClearBodyAction())
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeSendMessageToRecipientInCCFieldTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Sending message] Отправка письма получателю в копии')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11552)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.cc))
      .then(
        new ComposeSetRecipientFieldAction(
          ComposeRecipientFieldType.cc,
          ComposeEmailProvider.instance.getRandomValidEmail(),
        ),
      )
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}

export class ComposeSendMessageWithAllFilledFieldsTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Compose. [Sending message] Отправка письма с заполненными полями To/Cc/Bcc/Subj/Body')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.Android).iosCase(11556)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new ComposeOpenAction())
      .then(
        new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.to, ComposeEmailProvider.instance.validEmails[0]),
      )
      .then(new ComposeExpandExtendedRecipientFormAction())
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.cc))
      .then(
        new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.cc, ComposeEmailProvider.instance.validEmails[1]),
      )
      .then(new ComposeTapOnRecipientFieldAction(ComposeRecipientFieldType.bcc))
      .then(
        new ComposeSetRecipientFieldAction(ComposeRecipientFieldType.bcc, ComposeEmailProvider.instance.validEmails[2]),
      )
      .then(new ComposeTapOnSubjectFieldAction())
      .then(new ComposeSetSubjectAction('subj'))
      .then(new ComposeTapOnBodyFieldAction())
      .then(new ComposeSetBodyAction('body'))
      .then(new ComposeSendAction())
      .then(new OpenFolderListAction())
      .then(new GoToFolderAction(DefaultFolderName.sent))
  }
}
