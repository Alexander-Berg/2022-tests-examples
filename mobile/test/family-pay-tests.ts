import { Nullable } from '../../../../common/ys'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { SetPaymentButtonStatusAction } from '../action/payment-button-actions'
import { OpenSampleAppAction, StartPreselectPaymentProcessAction } from '../action/sample-app-actions'
import { TapOnEditButtonAction, UnbindCardAction } from '../action/unbind-card-actions'
import { BoundCard } from '../card-generator'
import { FamilyInfoMode } from '../mock-backend/model/mock-data-types'
import { PaymentDataPreparer } from '../payment-sdk-data'
import { TestScenario } from './all-tests'

export class FamilyPayCheckUnbindButtonForOneCard extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.cardBinding} Unbind button is disabled for method list with one family pay card `)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(150)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0]
      .setPaymentMethodsFilter(new PaymentMethodsFilter(true, false, false, false))
      .addBoundCard(BoundCard.generated())
      .setFamilyInfoMode(FamilyInfoMode.enabled_high_allowance)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new AssertAction())
      .then(new StartPreselectPaymentProcessAction())
      .then(new SetPaymentButtonStatusAction(true))
  }
}

export class FamilyPayCheckUnbindButtonForSeveralCards extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(
      `${TestScenario.cardBinding} Unbind button gets disabled for method list when only family pay card is left after unbind `,
    )
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(151)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0]
      .setPaymentMethodsFilter(new PaymentMethodsFilter(true, false, false, false))
      .addBoundCard(BoundCard.generated())
      .addBoundCard(BoundCard.generated())
      .setFamilyInfoMode(FamilyInfoMode.enabled_high_allowance)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new StartPreselectPaymentProcessAction())
      .then(new SetPaymentButtonStatusAction(true))
      .then(new TapOnEditButtonAction())
      .then(new AssertAction())
      .then(new UnbindCardAction(0))
  }
}
