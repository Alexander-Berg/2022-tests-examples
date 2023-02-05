import { TestsRegistry } from '../../../testopithecus-common/code/mbt/test/tests-registry'
import { PaymentDataPreparer } from '../payment-sdk-data'
import {
  BindCardsAndCheckPaymentMethodsListTest,
  BindCardWithValidationFailedTest,
  BindCardWithValidationTest,
} from './card-binding-tests'
import { FamilyPayCheckUnbindButtonForOneCard, FamilyPayCheckUnbindButtonForSeveralCards } from './family-pay-tests'
import {
  LicenseAgreementBindCardTest,
  LicenseAgreementExistingPaymentMethodsTest,
  LicenseAgreementOpenTest,
  LicenseAgreementPayWithExistingMethodsNoAcquirerTest,
  LicenseAgreementPayWithNewCardNoAcquirerTest,
  LicenseAgreementPayWithNewCardTest,
  LicenseAgreementPreselectPayWithExistingCardCVVNoAcquirerTest,
  LicenseAgreementPreselectPayWithExistingCardCVVTest,
} from './license-agreement-tests'
import { NonAuthorizedPayWithNewCardTest } from './non-authorized-tests'
import {
  MinimizeKeyboardAfterTapOnCvvFieldTest,
  PaymentMethodCvvFieldNumericKeyboardTest,
  PaymentMethodEmptyCardListTest,
  PayWithExistingMethod3DSCVVTest,
  PayWithExistingMethod3DSTest,
  PayWithExistingMethodClose3DSPageTest,
  PayWithExistingMethodCVVTest,
  PayWithExistingMethodInvalid3dsTest,
  PayWithExistingMethodTest,
  PayWithSbpMethodTest,
  RotateAfterSelectPaymentMethodTest,
  TrySelectDifferentBanksForSbpMethodTest,
  ValidateBigPaymentMethodsListTest,
} from './pay-existing-method-test'
import {
  CheckCardNumberFieldTest,
  CheckCvvFieldTest,
  CheckExpirationDateFieldTest,
  CorrectCardNumberIncorrectDateAndCvvTest,
  DisablePayButtonTest,
  FillAllFieldsAndRotateTest,
  NewCardAutomaticallyFocusNumberFieldTest,
  NewCardFieldsNumericKeyboardTest,
  NewCardFieldsValidatorTest,
  NewCardTapBackTest,
  PasteValuesToNewCardFieldsTest,
  PayWithNewCardNotEnoughFundsTest,
  PayWithNewCardWithoutSavingTest,
  PayWithNewCardWithSavingTest,
  ValidatePaymentMethodsAndNewCardInLandscapeTest,
} from './pay-new-card-tests'
import {
  PreselectPayWithCardNotEnoughFundsTest,
  PreselectPayWithCashTest,
  PreselectPayWithExistingCardCVV3DSTest,
  PreselectPayWithExistingCardCVVTest,
  PreselectPayWithExistingCardTest,
  PreselectPayWithNewCardCVVTest,
  PreselectPayWithNewCardTest,
  PreselectUnbindCardTest,
} from './preselect-tests'

export class AllPaymentSdkTests {
  public static readonly get: TestsRegistry<PaymentDataPreparer> = new TestsRegistry<PaymentDataPreparer>()
    .regular(new PayWithNewCardWithoutSavingTest())
    .regular(new DisablePayButtonTest())
    .regular(new PayWithNewCardWithSavingTest())
    .regular(new NewCardFieldsValidatorTest())
    .regular(new NewCardFieldsNumericKeyboardTest())
    .regular(new PasteValuesToNewCardFieldsTest())
    .regular(new PayWithNewCardNotEnoughFundsTest())
    .regular(new ValidatePaymentMethodsAndNewCardInLandscapeTest())
    .regular(new PaymentMethodCvvFieldNumericKeyboardTest())
    .regular(new PayWithExistingMethodInvalid3dsTest())
    .regular(new PayWithExistingMethodCVVTest())
    .regular(new PayWithExistingMethod3DSCVVTest())
    .regular(new PaymentMethodEmptyCardListTest())
    .regular(new CheckCvvFieldTest())
    .regular(new CheckExpirationDateFieldTest())
    .regular(new FillAllFieldsAndRotateTest())
    .regular(new RotateAfterSelectPaymentMethodTest())
    .regular(new NonAuthorizedPayWithNewCardTest())
    .regular(new MinimizeKeyboardAfterTapOnCvvFieldTest())
    .regular(new CorrectCardNumberIncorrectDateAndCvvTest())
    .regular(new ValidateBigPaymentMethodsListTest())
    .regular(new PayWithExistingMethod3DSTest())
    .regular(new PayWithExistingMethodTest())
    .regular(new BindCardsAndCheckPaymentMethodsListTest())
    .regular(new BindCardWithValidationTest())
    .regular(new PreselectPayWithExistingCardTest())
    .regular(new PreselectPayWithExistingCardCVVTest())
    .regular(new PreselectPayWithExistingCardCVV3DSTest())
    .regular(new PreselectUnbindCardTest())
    .regular(new PreselectPayWithCardNotEnoughFundsTest())
    .regular(new PreselectPayWithCashTest())
    .regular(new PreselectPayWithNewCardCVVTest())
    .regular(new PreselectPayWithNewCardTest())
    .regular(new LicenseAgreementPayWithNewCardTest())
    .regular(new LicenseAgreementExistingPaymentMethodsTest())
    .regular(new LicenseAgreementPayWithNewCardNoAcquirerTest())
    .regular(new LicenseAgreementBindCardTest())
    .regular(new LicenseAgreementPayWithExistingMethodsNoAcquirerTest())
    .regular(new LicenseAgreementPreselectPayWithExistingCardCVVTest())
    .regular(new LicenseAgreementPreselectPayWithExistingCardCVVNoAcquirerTest())
    .regular(new LicenseAgreementOpenTest())
    .regular(new NewCardTapBackTest())
    .regular(new NewCardAutomaticallyFocusNumberFieldTest())
    .regular(new PayWithExistingMethodClose3DSPageTest())
    .regular(new BindCardWithValidationFailedTest())
    .regular(new CheckCardNumberFieldTest())
    .regular(new PayWithSbpMethodTest())
    .regular(new TrySelectDifferentBanksForSbpMethodTest())
    .regular(new FamilyPayCheckUnbindButtonForOneCard())
    .regular(new FamilyPayCheckUnbindButtonForSeveralCards())
}

export enum TestScenario {
  existingPaymentMethods = '[Existing payment methods]',
  newCard = '[New card]',
  nonAuthorized = '[Non authorized]',
  cardBinding = '[Card binding]',
  preselect = '[Preselect]',
  licenseAgreement = '[License agreement]',
}
