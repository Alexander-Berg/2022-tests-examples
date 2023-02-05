import { Nullable } from '../../../../common/ys'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { PersonalInfoMode } from '../personal-info-mode'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTPlatform, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { FillNewCardDataAction } from '../action/new-card-actions'
import { OpenSampleAppAction, StartRegularPaymentProcessAction } from '../action/sample-app-actions'
import { BoundCard } from '../card-generator'
import { PaymentDataPreparer } from '../payment-sdk-data'
import { AuthorizationMode } from '../sample/sample-configuration'
import { TestScenario } from './all-tests'

export class NonAuthorizedPayWithNewCardTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.nonAuthorized} Pay with new card`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(69).ignoreOn(MBTPlatform.IOS)
  }

  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0]
      .setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
      .setPersonalInfoShowingMode(PersonalInfoMode.AUTOMATIC)
      .setAuthorizationMode(AuthorizationMode.nonauthorized)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new AssertAction())
      .then(new StartRegularPaymentProcessAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(this.card, true))
  }
}
