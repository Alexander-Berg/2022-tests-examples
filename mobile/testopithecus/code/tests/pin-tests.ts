import { Nullable } from '../../../../common/ys'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { GoToBackgroundState, GoToForegroundState } from '../mail/actions/general/application-state-actions'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { YandexLoginAction } from '../mail/actions/login/login-actions'
import {
  ChangePasswordAction,
  EnterPasswordAction,
  ResetPasswordAction,
  TurnOffLoginUsingPasswordAction,
  TurnOnLoginUsingPasswordAction,
  WaitForPinToTriggerAction,
} from '../mail/actions/pin-actions'
import { OpenGeneralSettingsAction } from '../mail/actions/settings/general-settings-actions'
import { OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class TurnOnPinTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Pin. Установка входа по паролю')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(87).androidCase(9861)
  }

  public prepareAccount(builder: MailboxBuilder): void {}

  private password: string = '1234'

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnLoginUsingPasswordAction(this.password))
      .then(new GoToBackgroundState())
      .then(new WaitForPinToTriggerAction())
      .then(new GoToForegroundState())
      .then(new EnterPasswordAction(this.password))
  }
}

export class TurnOffPinTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Pin. Отключение входа по паролю')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(119).androidCase(11163)
  }

  public prepareAccount(builder: MailboxBuilder): void {}

  private password: string = '1234'

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnLoginUsingPasswordAction(this.password))
      .then(new GoToBackgroundState())
      .then(new WaitForPinToTriggerAction())
      .then(new GoToForegroundState())
      .then(new EnterPasswordAction(this.password))
      .then(new TurnOffLoginUsingPasswordAction())
      .then(new GoToBackgroundState())
      .then(new WaitForPinToTriggerAction())
      .then(new GoToForegroundState())
  }
}

export class ChangePinTest extends RegularYandexMailTestBase {
  public constructor() {
    super('Pin. Изменение пароля')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(88).androidCase(11164)
  }

  public prepareAccount(builder: MailboxBuilder): void {}

  private password: string = '1234'
  private newPassword: string = '5678'

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnLoginUsingPasswordAction(this.password))
      .then(new ChangePasswordAction(this.newPassword))
      .then(new GoToBackgroundState())
      .then(new WaitForPinToTriggerAction())
      .then(new GoToForegroundState())
      .then(new EnterPasswordAction(this.newPassword))
      .then(new ChangePasswordAction(this.newPassword))
  }
}

export class ResetPinTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('Pin. Сброс пароля')
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex, AccountType2.Yandex]
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(103).androidCase(9771)
  }

  public prepareAccounts(preparers: MailboxBuilder[]): void {
    preparers[0].nextMessage('subj1')
    preparers[1].nextMessage('subj2')
  }

  private password: string = '1234'

  public scenario(accounts: UserAccount[], _model: Nullable<AppModel>, _supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
      .then(new TurnOnLoginUsingPasswordAction(this.password))
      .then(new GoToBackgroundState())
      .then(new WaitForPinToTriggerAction())
      .then(new GoToForegroundState())
      .then(new ResetPasswordAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenGeneralSettingsAction())
  }
}
