import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { Acquirer } from '../../../payment-sdk/code/network/mobile-backend/entities/init/acquirer'
import { Nullable } from '../../../../common/ys'
import { FeatureID } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AccountType2, MBTTest, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { CloseFullLicenseAgreementAction, OpenFullLicenseAgreementAction } from '../action/license-agreement-actions'
import { SetPaymentButtonStatusAction } from '../action/payment-button-actions'
import { PressSelectButtonAction, SelectPaymentMethodAction } from '../action/payment-methods-list-actions'
import {
  OpenSampleAppAction,
  StartCardBindingProcessAction,
  StartPreselectPaymentProcessAction,
  StartRegularPaymentProcessAction,
} from '../action/sample-app-actions'
import { BoundCard } from '../card-generator'
import { PaymentDataPreparer } from '../payment-sdk-data'
import { TestScenario } from './all-tests'

export class LicenseAgreementPayWithNewCardTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.licenseAgreement} License agreement is shown on the Pay with new card screen`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(91)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0]
      .setUseYaOplata(true)
      .setAcquirer(Acquirer.kassa)
      .setPaymentMethodsFilter(new PaymentMethodsFilter(false, false, false, false))
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new OpenSampleAppAction()).then(new StartRegularPaymentProcessAction())
  }
}

export class LicenseAgreementExistingPaymentMethodsTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.licenseAgreement} License agreement is shown on the Payment methods screen`)
  }

  private card: BoundCard = BoundCard.generated()

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(92)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa).addBoundCard(this.card)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new OpenSampleAppAction()).then(new StartRegularPaymentProcessAction())
  }
}

export class LicenseAgreementPayWithNewCardNoAcquirerTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.licenseAgreement} License agreement is not shown on the Pay with new card screen`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(94)
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

export class LicenseAgreementBindCardTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.licenseAgreement} License agreement is not shown on the Bind screen`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(95)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new OpenSampleAppAction()).then(new StartCardBindingProcessAction())
  }
}

export class LicenseAgreementPayWithExistingMethodsNoAcquirerTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.licenseAgreement} License agreement is not shown on the Payment methods screen`)
  }

  private card: BoundCard = BoundCard.generated()

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(97)
  }

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].addBoundCard(this.card)
  }

  public requiredAccounts(): AccountType2[] {
    return [AccountType2.YandexTest]
  }

  public scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan {
    return TestPlan.empty().then(new OpenSampleAppAction()).then(new StartRegularPaymentProcessAction())
  }
}

export class LicenseAgreementPreselectPayWithExistingCardCVVTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.licenseAgreement} License agreement is shown on the Preselect screen`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(98)
  }

  private forceCvv: boolean = true
  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa).addBoundCard(this.card).setForceCvv(this.forceCvv)
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
  }
}

export class LicenseAgreementPreselectPayWithExistingCardCVVNoAcquirerTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.licenseAgreement} License agreement is not shown on the Preselect screen`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(100)
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
  }
}

export class LicenseAgreementOpenTest extends MBTTest<PaymentDataPreparer> {
  public constructor() {
    super(`${TestScenario.licenseAgreement} Open license agreement screen`)
  }

  public setupSettings(settings: TestSettings): void {
    settings.commonCase(101)
  }

  private card: BoundCard = BoundCard.generated()

  public prepareAccounts(preparers: PaymentDataPreparer[]): void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa).addBoundCard(this.card)
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
      .then(new OpenFullLicenseAgreementAction())
      .then(new AssertAction())
      .then(new CloseFullLicenseAgreementAction())
  }
}
