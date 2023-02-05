import { Nullable } from '../../../../common/ys'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import {
  CustomMailServiceLoginAction,
  MailRuLoginAction,
  RevokeTokenForAccount,
  YandexLoginAction,
} from '../mail/actions/login/login-actions'
import { AddNewAccountAction, SwitchAccountAction } from '../mail/actions/login/multi-account-actions'
import { RefreshMessageListAction } from '../mail/actions/messages-list/message-list-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { AccountType2, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'

// TODO: сделать так, чтобы тест работал
export class YandexLoginTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('should login 3 yandex accounts')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(455).androidCase(472)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex, AccountType2.Yandex, AccountType2.Yandex]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('firstAccountMsg')
    mailboxes[1].nextMessage('secondAccountMsg')
    mailboxes[2].nextMessage('thirdAccountMsg')
  }

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new OpenFolderListAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new OpenFolderListAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[2]))
  }
}

export class SwitchAccountTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('should switch between 2 yandex accounts')
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex, AccountType2.Yandex]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('firstAccountMsg')
    mailboxes[1].nextMessage('secondAccountMsg')
  }

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new OpenFolderListAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new OpenFolderListAction())
      .then(new SwitchAccountAction(accounts[0]))
  }
}

export class OuterMailLoginTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('Пробуем залогиниться через mail.ru аккаунт')
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Mail]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('pizza')
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new MailRuLoginAction(accounts[0]))
  }
}

export class GenericIMAPOtherLoginTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('Account manager. Залогиниться через простую форму GenericIMAP')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(7).androidCase(6094) // Не работает так как не получаем токены других аккаунтов
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Other]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('subj0')
  }

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new CustomMailServiceLoginAction(accounts[0]))
  }
}

// TODO: сделать так, чтобы тест работал
export class GenericIMAPYandexLoginTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('Account manager. Переход к странице авторизации Yandex из формы GenericIMAP')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(433).androidCase(11081)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('subj0')
  }

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new CustomMailServiceLoginAction(accounts[0]))
  }
}

export class ChoseAccountFromAccountsListTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('Account manager. Выбор аккаунта из карусели аккаунтов')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(469).androidCase(473)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex, AccountType2.Yandex]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('firstAccountMsg')
    mailboxes[1].nextMessage('secondAccountMsg')
  }

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new OpenFolderListAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new OpenFolderListAction())
      .then(new SwitchAccountAction(accounts[0]))
  }
}

export class LogoutWorkingTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('Account manager. Проверка работы приложения после логаута')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(462).androidCase(484)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex, AccountType2.Yandex]
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('firstAccountMsg')
    mailboxes[1].nextMessage('secondAccountMsg')
  }

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new OpenFolderListAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new RevokeTokenForAccount(accounts[1]))
      .then(new RefreshMessageListAction())
  }
}
