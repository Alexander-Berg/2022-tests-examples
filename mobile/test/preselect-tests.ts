import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { Nullable } from '../../../../common/ys'
import { Enter3dsAction, PreselectEnter3dsAction } from '../action/enter-3ds-action'
import { FillNewCardDataAction } from '../action/new-card-actions'
import { SetPaymentButtonStatusAction, PressPaymentButtonAction } from '../action/payment-button-actions'
import {
  EnterCvvForSelectPaymentMethodAction,
  PreselectTapOnAddCardAction,
  PressSelectButtonAction,
  SelectPaymentMethodAction,
  TapOnCashPaymentMethodAction,
  TapOnCvvFieldOfSelectPaymentMethodAction,
} from '../action/payment-methods-list-actions'
import { OpenSampleAppAction, StartPreselectPaymentProcessAction } from '../action/sample-app-actions'
import { TapOnDoneButtonAction, TapOnEditButtonAction, UnbindCardAction } from '../action/unbind-card-actions'
import { BoundCard } from '../card-generator'
import { PaymentDataPreparer, PaymentErrorType } from '../payment-sdk-data'
import { TestScenario } from './all-tests'

export class PreselectPayWithExistingCardTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.preselect} Pay with existing card`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(74)
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
      .then(new StartPreselectPaymentProcessAction())
      .then(new AssertAction())
      .then(new SelectPaymentMethodAction(0))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressSelectButtonAction())
  }
}

export class PreselectPayWithExistingCardCVVTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.preselect} Pay with existing card (cvv)`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(75)
  }

  private forceCvv: boolean = true
  private card: BoundCard = BoundCard.generated()

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
      .then(new StartPreselectPaymentProcessAction())
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new SelectPaymentMethodAction(0))
      .then(new PressSelectButtonAction())
      .then(new SetPaymentButtonStatusAction(false))
      .then(new AssertAction())
      .then(new TapOnCvvFieldOfSelectPaymentMethodAction())
      .then(new EnterCvvForSelectPaymentMethodAction(this.card.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
  }
}

export class PreselectPayWithExistingCardCVV3DSTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.preselect} Pay with existing card (cvv + 3ds)`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(77)
  }

  private code3ds: string = '200'
  private forceCvv: boolean = true
  private card: BoundCard = BoundCard.generated()

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
      .then(new StartPreselectPaymentProcessAction())
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new SelectPaymentMethodAction(0))
      .then(new PressSelectButtonAction())
      .then(new SetPaymentButtonStatusAction(false))
      .then(new AssertAction())
      .then(new TapOnCvvFieldOfSelectPaymentMethodAction())
      .then(new EnterCvvForSelectPaymentMethodAction(this.card.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
      .then(new AssertAction())
      .then(new Enter3dsAction(this.code3ds))
  }
}

export class PreselectPayWithCashTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.preselect} Pay with cash`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(82)
  }

  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].enableCash().addBoundCard(this.card)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new AssertAction())
      .then(new StartPreselectPaymentProcessAction())
      .then(new AssertAction())
      .then(new TapOnCashPaymentMethodAction())
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressSelectButtonAction())
  }
}

export class PreselectPayWithCardNotEnoughFundsTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.preselect} Pay with card with not enough funds`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(78)
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
      .then(new StartPreselectPaymentProcessAction())
      .then(new AssertAction())
      .then(new SelectPaymentMethodAction(0))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressSelectButtonAction())
  }
}

export class PreselectUnbindCardTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.preselect} Unbind card`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(84)
  }

  private card1: BoundCard = BoundCard.generated()
  private card2: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card1).addBoundCard(this.card2)
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
      .then(new AssertAction())
      .then(new TapOnDoneButtonAction())
  }
}

export class PreselectPayWithNewCardCVVTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.preselect} Pay with new card (cvv)`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(86)
  }

  private card: BoundCard = BoundCard.generated()
  private forceCvv: boolean = true

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setForceCvv(this.forceCvv).setPaymentMethodsFilter(new PaymentMethodsFilter(true, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new AssertAction())
      .then(new StartPreselectPaymentProcessAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(this.card, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PreselectTapOnAddCardAction())
      .then(new AssertAction())
      .then(new PreselectEnter3dsAction())
      .then(new AssertAction())
      .then(new PressSelectButtonAction())
      .then(new SetPaymentButtonStatusAction(false))
      .then(new AssertAction())
      .then(new TapOnCvvFieldOfSelectPaymentMethodAction())
      .then(new EnterCvvForSelectPaymentMethodAction(this.card.cvv))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new PressPaymentButtonAction())
  }
}

export class PreselectPayWithNewCardTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.preselect} Pay with new card`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(87)
  }

  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setPaymentMethodsFilter(new PaymentMethodsFilter(true, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty()
      .then(new OpenSampleAppAction())
      .then(new AssertAction())
      .then(new StartPreselectPaymentProcessAction())
      .then(new AssertAction())
      .then(new FillNewCardDataAction(this.card, false))
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PreselectTapOnAddCardAction())
      .then(new PreselectEnter3dsAction())
      .then(new AssertAction())
      .then(new SetPaymentButtonStatusAction(true))
      .then(new AssertAction())
      .then(new PressSelectButtonAction())
  }
}
