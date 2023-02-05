import { AccountType2, DeviceType, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { RegularYandexTestBase } from '../../../testopithecus-common/code/mbt/test/regular-yandex-test-base'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { loginAction } from '../mail/actions/login/login-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { DeviceTypeModel } from '../mail/model/mail-model'

export abstract class RegularYandexMailTestBase extends RegularYandexTestBase<MailboxBuilder> {
  protected constructor(description: string) {
    super(description)
  }

  public regularScenario(account: UserAccount): TestPlan {
    this.setDeviceTypeInModel()
    return this.testScenario(account)
  }

  abstract testScenario(account: UserAccount): TestPlan

  abstract prepareAccount(preparer: MailboxBuilder): void

  private setDeviceTypeInModel(): void {
    const settings = new TestSettings()
    this.setupSettings(settings)
    DeviceTypeModel.instance.setDeviceType(settings.hasTag(DeviceType.Tab) ? DeviceType.Tab : DeviceType.Phone)
  }

  /**
   * Позволяет не писать первый действием LoginAction
   */
  protected yandexLogin(account: UserAccount): TestPlan {
    return this.login(account, AccountType2.Yandex)
  }

  protected login(account: UserAccount, accountType: AccountType2): TestPlan {
    return TestPlan.empty().then(loginAction(account, accountType))
  }
}
