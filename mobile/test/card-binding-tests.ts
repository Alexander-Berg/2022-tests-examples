import { Nullable } from '../../../../common/ys'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { Enter3dsAction } from '../action/enter-3ds-action'
import { FillNewCardDataAction } from '../action/new-card-actions'
import { SetPaymentButtonStatusAction, PressPaymentButtonAction } from '../action/payment-button-actions'
import { ClosePaymentResultScreenAction } from '../action/payment-result-actions'
import {
  OpenSampleAppAction,
  StartCardBindingProcessAction,
  StartRegularPaymentProcessAction,
} from '../action/sample-app-actions'
import { BoundCard, SpecificCards } from '../card-generator'
import { PaymentDataPreparer } from '../payment-sdk-data'
import { TestScenario } from './all-tests'

export class BindCardsAndCheckPaymentMethodsListTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.cardBinding} Bind cards and check payment methods list`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(3)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(true, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new StartCardBindingProcessAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(SpecificCards.masterCard, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new ClosePaymentResultScreenAction())
      .then(new StartCardBindingProcessAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(SpecificCards.visa, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new ClosePaymentResultScreenAction())
      .then(new StartCardBindingProcessAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(SpecificCards.mir, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new ClosePaymentResultScreenAction())
      .then(new StartRegularPaymentProcessAction())
  }
}

export class BindCardWithValidationTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.cardBinding} Bind card with validation`)
  }

  private code3ds: string = '200'
  private card: BoundCard = BoundCard.generated()

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(73)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0]
      .setBindingV2(true)
      .set3ds(this.code3ds)
      .setPaymentMethodsFilter(new PaymentMethodsFilter(true, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new StartCardBindingProcessAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(this.card, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new Enter3dsAction(this.code3ds))
      .then(new ClosePaymentResultScreenAction())
      .then(new StartRegularPaymentProcessAction())
  }
}

export class BindCardWithValidationFailedTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.cardBinding} Bind card with validation failed`)
  }

  private code3ds: string = '400'
  private card: BoundCard = BoundCard.generated()

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(90)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0]
      .setBindingV2(true)
      .set3ds(this.code3ds)
      .setPaymentMethodsFilter(new PaymentMethodsFilter(true, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new StartCardBindingProcessAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(this.card, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new Enter3dsAction(this.code3ds))
      .then(new SetPaymentButtonStatusAction(false))
      .then(new ClosePaymentResultScreenAction())
      .then(new AssertAction())
      .then(new StartRegularPaymentProcessAction())
  }
}
