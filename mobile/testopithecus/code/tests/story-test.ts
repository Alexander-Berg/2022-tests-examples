import { Nullable } from '../../../../common/ys'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTPlatform, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RotateToLandscape, RotateToPortrait } from '../mail/actions/general/rotatable-actions'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { YandexLoginAction } from '../mail/actions/login/login-actions'
import { AddNewAccountAction } from '../mail/actions/login/multi-account-actions'
import { HideStoriesBlockAction } from '../mail/actions/stories/stories-block-actions'
import { MailboxBuilder } from '../mail/mailbox-preparer'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class StoriesDifferentAccountTest extends MBTTest<MailboxBuilder> {
  public constructor() {
    super('should displays in different accounts and count block display')
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.Yandex, AccountType2.Yandex, AccountType2.Yandex]
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.IOS)
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new YandexLoginAction(accounts[0]))
      .then(new OpenFolderListAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[1]))
      .then(new OpenFolderListAction())
      .then(new AddNewAccountAction())
      .then(new YandexLoginAction(accounts[2]))
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
  }

  public prepareAccounts(mailboxes: MailboxBuilder[]): void {
    mailboxes[0].nextMessage('firstAccountMsg')
    mailboxes[1].nextMessage('secondAccountMsg')
    mailboxes[2].nextMessage('thirdAccountMsg')
  }
}

export class StoriesHideTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should hide click should hide')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.IOS)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('firstAccountMsg')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account).then(new HideStoriesBlockAction()).then(new RotateToLandscape())
  }
}

export class StoriesRotationTest extends RegularYandexMailTestBase {
  public constructor() {
    super('should hide after 9 rotation')
  }

  public setupSettings(settings: TestSettings): void {
    settings.ignoreOn(MBTPlatform.IOS)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('firstAccountMsg')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
      .then(new RotateToLandscape())
      .then(new RotateToPortrait())
  }
}
