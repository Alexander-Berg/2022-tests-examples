import { Nullable } from '../../../../common/ys'
import { CardPaymentSystem } from '../../../payment-sdk/code/busilogics/card-payment-system'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { RotateDeviceAction } from '../action/device-orientation-actions'
import { Enter3dsAction } from '../action/enter-3ds-action'
import { MinimizeKeyboardAction } from '../action/keyboard-actions'
import {
  FillNewCardDataAction,
  FillNewCardFieldAction,
  PasteNewCardFieldAction,
  TapAndFillNewCardFieldAction,
  TapAndPasteNewCardFieldAction,
  TapOnNewCardBackButtonAction,
  TapOnNewCardFieldAction,
} from '../action/new-card-actions'
import { SetPaymentButtonStatusAction, PressPaymentButtonAction } from '../action/payment-button-actions'
import { ClickNewCardPaymentMethodAction } from '../action/payment-methods-list-actions'
import { ClosePaymentResultScreenAction } from '../action/payment-result-actions'
import { OpenSampleAppAction, StartRegularPaymentProcessAction } from '../action/sample-app-actions'
import { BoundCard, CardGenerator, ExpirationDateType, SpecificCards } from '../card-generator'
import { DeviceOrientation } from '../feature/device-orientation-feature'
import { NewCardField } from '../model/fill-new-card-model'
import { PaymentDataPreparer, PaymentErrorType } from '../payment-sdk-data'
import { TestScenario } from './all-tests'

export class PayWithNewCardWithoutSavingTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Pay with new card without saving`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(2)
  }

  private code3ds: string = '200'
  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false)).set3ds(this.code3ds)
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
      .then(new FillNewCardDataAction(this.card, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new Enter3dsAction(this.code3ds))
  }
}

export class DisablePayButtonTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Pay button disabling if not all required fields are filled`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(4)
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
      .then(new StartRegularPaymentProcessAction())
      .then(new AssertAction())
      .then(new ClickNewCardPaymentMethodAction())
      .then(new AssertAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.cardNumber, this.card.cardNumber))
      .then(new AssertAction())
      .then(
        new TapAndFillNewCardFieldAction(
          NewCardField.expirationDate,
          `${this.card.expirationMonth}${this.card.expirationYear}`,
        ),
      )
      .then(new AssertAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.expirationDate, ''))
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, this.card.cvv))
  }
}

export class PayWithNewCardWithSavingTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Pay with new card with saving`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(15)
  }

  private code3ds: string = '200'
  private card: BoundCard = BoundCard.generated()
  private newCard: BoundCard = BoundCard.generated()

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
      .then(new ClickNewCardPaymentMethodAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(this.newCard, true))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new Enter3dsAction(this.code3ds))
      .then(new AssertAction())
      .then(new ClosePaymentResultScreenAction())
      .then(new AssertAction())
      .then(new StartRegularPaymentProcessAction())
  }
}

export class PasteValuesToNewCardFieldsTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Paste values into new card fields`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(66)
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
      .then(new ClickNewCardPaymentMethodAction())
      .then(new AssertAction())
      .then(new TapAndPasteNewCardFieldAction(NewCardField.cardNumber, this.card.cardNumber))
      .then(
        new TapAndPasteNewCardFieldAction(
          NewCardField.expirationDate,
          `${this.card.expirationMonth}${this.card.expirationYear}`,
        ),
      )
      .then(new TapAndPasteNewCardFieldAction(NewCardField.cvv, this.card.cvv))
      .then(new SetPaymentButtonStatusAction(true))
  }
}

export class NewCardFieldsValidatorTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Validate new card fields`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(8)
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
      .then(new ClickNewCardPaymentMethodAction())
      .then(new AssertAction())
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new FillNewCardFieldAction(NewCardField.cardNumber, '123'))
      .then(new TapOnNewCardFieldAction(NewCardField.expirationDate))
      .then(new AssertAction())
      .then(new FillNewCardFieldAction(NewCardField.expirationDate, '123'))
      .then(new TapOnNewCardFieldAction(NewCardField.cvv))
      .then(new AssertAction())
      .then(new FillNewCardFieldAction(NewCardField.cvv, '1'))
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(new FillNewCardFieldAction(NewCardField.cardNumber, this.card.cardNumber))
  }
}

export class NewCardFieldsNumericKeyboardTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Test opening of the numeric keyboard after tap on the field`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(58)
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
      .then(new ClickNewCardPaymentMethodAction())
      .then(new AssertAction())
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(new TapOnNewCardFieldAction(NewCardField.expirationDate))
      .then(new AssertAction())
      .then(new TapOnNewCardFieldAction(NewCardField.cvv))
  }
}

export class PayWithNewCardNotEnoughFundsTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Pay with new card with not enough funds`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(61)
  }

  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).forcePaymentError(PaymentErrorType.notEnoughFunds)
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
      .then(new ClickNewCardPaymentMethodAction())
      .then(new FillNewCardDataAction(this.card, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
  }
}

export class ValidatePaymentMethodsAndNewCardInLandscapeTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Validate payment methods view and new card view in landscape`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(65)
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
      .then(new RotateDeviceAction(DeviceOrientation.landscape))
      .then(new ClickNewCardPaymentMethodAction())
      .then(new MinimizeKeyboardAction())
      .then(new AssertAction())
      .then(new TapOnNewCardBackButtonAction())
  }
}

// CVV doesn't matter to testing backend
export class NewCardIncorrectCvvTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Enter incorrect cvv`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(59)
  }

  private card: BoundCard = BoundCard.generated()
  private code3ds: string = '200'
  private cvv: string = '456'

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card).set3ds(this.code3ds).setCvv(this.cvv)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new StartRegularPaymentProcessAction())
      .then(new ClickNewCardPaymentMethodAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.cardNumber, this.card.cardNumber))
      .then(
        new TapAndFillNewCardFieldAction(
          NewCardField.expirationDate,
          `${this.card.expirationMonth}${this.card.expirationYear}`,
        ),
      )
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, this.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
      .then(new Enter3dsAction(this.code3ds))
  }
}

export class CheckExpirationDateFieldTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Check expiration date field`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(60)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
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
      .then(
        new TapAndFillNewCardFieldAction(
          NewCardField.expirationDate,
          `${CardGenerator.generateExpirationDate(ExpirationDateType.previousMonth)}`,
        ),
      )
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(
        new TapAndFillNewCardFieldAction(
          NewCardField.expirationDate,
          `${CardGenerator.generateExpirationDate(ExpirationDateType.currentMonthAndYear)}`,
        ),
      )
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(
        new TapAndFillNewCardFieldAction(
          NewCardField.expirationDate,
          `${CardGenerator.generateExpirationDate(ExpirationDateType.date50YearsInFuture)}`,
        ),
      )
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(
        new TapAndFillNewCardFieldAction(
          NewCardField.expirationDate,
          `${CardGenerator.generateExpirationDate(ExpirationDateType.dateMore50YearsInFuture)}`,
        ),
      )
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(
        new TapAndFillNewCardFieldAction(
          NewCardField.expirationDate,
          `${CardGenerator.generateExpirationDate(ExpirationDateType.nonExistentMonth)}`,
        ),
      )
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
  }
}

export class CheckCvvFieldTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Check cvv field`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(64)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
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
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, '1'))
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, '12'))
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, '123'))
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, '1234'))
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, ''))
      .then(new PasteNewCardFieldAction(NewCardField.cvv, '!qπ'))
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new AssertAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, ''))
      .then(new PasteNewCardFieldAction(NewCardField.cvv, 'https://yandex.ru'))
      .then(new AssertAction())
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, ''))
      .then(new PasteNewCardFieldAction(NewCardField.cvv, '3456'))
  }
}

export class CheckCardNumberFieldTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.cardBinding} Check card number field`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(5)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new StartRegularPaymentProcessAction())
      .then(new AssertAction())
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new FillNewCardFieldAction(NewCardField.cardNumber, '!@#$'))
      .then(new AssertAction())
      .then(new FillNewCardFieldAction(NewCardField.cardNumber, 'sadfdsf'))
      .then(new AssertAction())
      .then(new FillNewCardFieldAction(NewCardField.cardNumber, 's#1234'))
      .then(new TapOnNewCardFieldAction(NewCardField.cvv))
      .then(new AssertAction())
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new FillNewCardFieldAction(NewCardField.cardNumber, '1234123412341234'))
      .then(new TapOnNewCardFieldAction(NewCardField.cvv))
      .then(new AssertAction())
      .then(new TapOnNewCardFieldAction(NewCardField.cardNumber))
      .then(new FillNewCardFieldAction(NewCardField.cardNumber, SpecificCards.visa.cardNumber))
      .then(new TapOnNewCardFieldAction(NewCardField.cvv))
  }
}

export class FillAllFieldsAndRotateTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Fill all fields and rotate`)
  }

  private card: BoundCard = BoundCard.generated()

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(16)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
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
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new RotateDeviceAction(DeviceOrientation.landscape))
  }
}

// TODO: register after fix TRUSTDUTY-1896
export class PaymentSystemTypeTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Check payment system type icon`)
  }

  private masterCard: BoundCard = BoundCard.generated(CardPaymentSystem.MasterCard)
  private maestro: BoundCard = BoundCard.generated(CardPaymentSystem.Maestro)
  private mir: BoundCard = BoundCard.generated(CardPaymentSystem.MIR)
  private visa: BoundCard = BoundCard.generated(CardPaymentSystem.VISA)

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(43)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0]
      .addBoundCard(this.masterCard)
      .addBoundCard(this.maestro)
      .addBoundCard(this.mir)
      .addBoundCard(this.visa)
      .setDarkMode(true)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new OpenSampleAppAction()).then(new StartRegularPaymentProcessAction())
  }
}

export class CorrectCardNumberIncorrectDateAndCvvTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Correct card number, incorrect expiration date and cvv`)
  }

  private card: BoundCard = BoundCard.generated()

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(9)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
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
      .then(new TapAndFillNewCardFieldAction(NewCardField.cardNumber, this.card.cardNumber))
      .then(new TapAndFillNewCardFieldAction(NewCardField.cvv, this.card.cvv))
      .then(
        new TapAndFillNewCardFieldAction(
          NewCardField.expirationDate,
          CardGenerator.generateExpirationDate(ExpirationDateType.nonExistentMonth),
        ),
      )
  }
}

export class NewCardTapBackTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Returning from the NewCard screen`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(89)
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
      .then(new ClickNewCardPaymentMethodAction())
      .then(new AssertAction())
      .then(new TapOnNewCardBackButtonAction())
  }
}

export class NewCardAutomaticallyFocusNumberFieldTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.newCard} Automatically focus card number field`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(109)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new AssertAction())
      .then(new StartRegularPaymentProcessAction())
  }
}
