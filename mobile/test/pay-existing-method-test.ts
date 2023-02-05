import { Nullable } from '../../../../common/ys'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTPlatform, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RotateDeviceAction } from '../action/device-orientation-actions'
import { Close3dsAction, Enter3dsAction } from '../action/enter-3ds-action'
import { MinimizeKeyboardAction } from '../action/keyboard-actions'
import { SetPaymentButtonStatusAction, PressPaymentButtonAction } from '../action/payment-button-actions'
import {
  EnterCvvForSelectPaymentMethodAction,
  SelectPaymentMethodAction,
  SelectSbpMethodAction,
  TapOnCvvFieldOfSelectPaymentMethodAction,
} from '../action/payment-methods-list-actions'
import { OpenSampleAppAction, StartRegularPaymentProcessAction } from '../action/sample-app-actions'
import { SearchQueryActionBankAction, SelectAnotherBankAction } from '../action/sbp-banks-list-actions'
import { ApproveSbpPurchaseAction } from '../action/sbp-sample-bank-actions'
import { BoundCard } from '../card-generator'
import { DeviceOrientation } from '../feature/device-orientation-feature'
import { PaymentDataPreparer } from '../payment-sdk-data'
import { TestScenario } from './all-tests'

export class PayWithExistingMethod3DSCVVTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods}  Test pay with existing payment method (3ds + cvv)`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(39)
  }

  private card: BoundCard = BoundCard.generated()
  private code3ds: string = '200'
  private forceCvv: boolean = true

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).set3ds(this.code3ds).setForceCvv(this.forceCvv)
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
      .then(new SelectPaymentMethodAction(0))
      .then(new AssertAction())
      .then(new EnterCvvForSelectPaymentMethodAction(this.card.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new Enter3dsAction(this.code3ds))
  }
}

export class PayWithExistingMethod3DSTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods}  Test pay with existing payment method (3ds)`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(40)
  }

  private card: BoundCard = BoundCard.generated()
  private code3ds: string = '200'

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).set3ds(this.code3ds)
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
      .then(new SelectPaymentMethodAction(0))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new Enter3dsAction(this.code3ds))
  }
}

export class PayWithExistingMethodCVVTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Test pay with existing payment method (cvv)`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(41)
  }

  private card: BoundCard = BoundCard.generated()
  private forceCvv: boolean = true

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).setForceCvv(this.forceCvv)
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
      .then(new SelectPaymentMethodAction(0))
      .then(new AssertAction())
      .then(new EnterCvvForSelectPaymentMethodAction(this.card.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
  }
}

export class PayWithExistingMethodTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Test pay with existing payment method`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(42)
  }

  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card)
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
      .then(new SelectPaymentMethodAction(0))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
  }
}

export class PayWithSbpMethodTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Test pay with sbp payment method`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(103)
  }

  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card)
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
      .then(new SelectSbpMethodAction())
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new ApproveSbpPurchaseAction())
  }
}

export class TrySelectDifferentBanksForSbpMethodTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Test bank selection for sbp`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(103)
  }

  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card)
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
      .then(new SelectSbpMethodAction())
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new SelectAnotherBankAction())
      .then(new AssertAction())
      .then(new SearchQueryActionBankAction('OLOLO'))
      .then(new AssertAction())
      .then(new SearchQueryActionBankAction('asdasd'))
      .then(new AssertAction())
      .then(new SearchQueryActionBankAction(''))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new ApproveSbpPurchaseAction())
  }
}

// CVV doesn't matter to testing backend
export class PayWithExistingMethodInvalidCvvTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Test pay with existing payment method (invalid cvv)`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(44)
  }

  private card: BoundCard = BoundCard.generated()
  private code3ds: string = '200'
  private forceCvv: boolean = true
  private cvv: string = '567'

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).setForceCvv(this.forceCvv).set3ds(this.code3ds).setCvv(this.cvv)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new StartRegularPaymentProcessAction())
      .then(new SelectPaymentMethodAction(0))
      .then(new EnterCvvForSelectPaymentMethodAction(this.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new Enter3dsAction(this.code3ds))
  }
}

export class PayWithExistingMethodInvalid3dsTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Test pay with existing payment method (invalid 3ds)`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(62)
  }

  private card: BoundCard = BoundCard.generated()
  private code3ds: string = '400'
  private forceCvv: boolean = true

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).setForceCvv(this.forceCvv).set3ds(this.code3ds)
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
      .then(new SelectPaymentMethodAction(0))
      .then(new EnterCvvForSelectPaymentMethodAction(this.card.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new Enter3dsAction(this.code3ds))
  }
}

export class PaymentMethodCvvFieldNumericKeyboardTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Test opening of the numeric keyboard after tap on cvv field`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(51)
  }

  private card: BoundCard = BoundCard.generated()
  private forceCvv: boolean = true

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).setForceCvv(this.forceCvv)
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
      .then(new SelectPaymentMethodAction(0))
      .then(new TapOnCvvFieldOfSelectPaymentMethodAction())
  }
}

export class PaymentMethodEmptyCardListTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Test opening new card screen if payment method list is empty`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(1)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new OpenSampleAppAction()).then(new StartRegularPaymentProcessAction())
  }
}

export class RotateAfterSelectPaymentMethodTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Rotate after select payment method`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(46)
  }

  private masterCard1: BoundCard = BoundCard.generated()
  private masterCard2: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.masterCard1).addBoundCard(this.masterCard2)
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
      .then(new SelectPaymentMethodAction(1))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new RotateDeviceAction(DeviceOrientation.landscape))
  }
}

export class MinimizeKeyboardAfterTapOnCvvFieldTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Minimize keyboard after tap on cvv field`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(53).ignoreOn(MBTPlatform.IOS)
  }

  private card: BoundCard = BoundCard.generated()
  private forceCvv: boolean = true

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).setForceCvv(this.forceCvv)
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
      .then(new SelectPaymentMethodAction(0))
      .then(new AssertAction())
      .then(new TapOnCvvFieldOfSelectPaymentMethodAction())
      .then(new MinimizeKeyboardAction())
  }
}

export class ValidateBigPaymentMethodsListTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods} Validate big payment methods list`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(67)
  }

  private card1: BoundCard = BoundCard.generated()
  private card2: BoundCard = BoundCard.generated()
  private card3: BoundCard = BoundCard.generated()
  private card4: BoundCard = BoundCard.generated()
  private card5: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0]
      .addBoundCard(this.card1)
      .addBoundCard(this.card2)
      .addBoundCard(this.card3)
      .addBoundCard(this.card4)
      .addBoundCard(this.card5)
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
      .then(new SelectPaymentMethodAction(1))
      .then(new SetPaymentButtonStatusAction(true))
  }
}

export class PayWithExistingMethodClose3DSPageTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.existingPaymentMethods}  Close 3ds page`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(88).ignoreOn(MBTPlatform.Android)
  }

  private card: BoundCard = BoundCard.generated()
  private code3ds: string = '200'
  private forceCvv: boolean = true

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).set3ds(this.code3ds).setForceCvv(this.forceCvv)
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
      .then(new SelectPaymentMethodAction(0))
      .then(new AssertAction())
      .then(new EnterCvvForSelectPaymentMethodAction(this.card.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new Close3dsAction())
  }
}
