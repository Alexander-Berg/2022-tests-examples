import { AvailableMethods } from '../../../payment-sdk/code/models/available-methods'
import { Acquirer } from '../../../payment-sdk/code/network/mobile-backend/entities/init/acquirer'
import { Nullable } from '../../../../common/ys'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AccountType2 } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModelProvider } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { OAuthUserAccount, UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { NewCardField } from '../../code/model/fill-new-card-model'
import { NewCardFieldsValidationError } from '../../code/model/new-card-fields-validator-model'
import { PaymentSdkModel } from '../../code/model/payment-sdk-model'
import { PersonalInformationValidationError } from '../../code/model/personal-info-fields-validator-model'
import { PersonalInformationField } from '../../code/model/personal-information-model'
import { PaymentErrorType } from '../../code/payment-sdk-data'
import { PersonalInfoMode } from '../../code/personal-info-mode'
import { AuthorizationMode } from '../../code/sample/sample-configuration'

export class MockPaymentDataProvider implements AppModelProvider {
  public readonly model: PaymentSdkModel

  private constructor(
    account: OAuthUserAccount,
    merchantId: string,
    paymentId: string,
    availableMethods: AvailableMethods,
    amount: string,
    currency: string,
    expected3ds: Nullable<string>,
    isCvvValid: boolean,
    forcedErrorType: Nullable<PaymentErrorType>,
    forceCvv: boolean,
    methodsFilter: PaymentMethodsFilter,
    isDarkModeEnabled: boolean,
    personalInfoShowingMode: PersonalInfoMode,
    authorizationMode: AuthorizationMode,
    isBindingV2Enabled: boolean,
    isCashEnabled: boolean,
    acquirer: Nullable<Acquirer>,
  ) {
    this.model = new PaymentSdkModel(
      account,
      merchantId,
      paymentId,
      availableMethods,
      amount,
      currency,
      expected3ds,
      isCvvValid,
      forcedErrorType,
      forceCvv,
      methodsFilter,
      isDarkModeEnabled,
      personalInfoShowingMode,
      authorizationMode,
      isBindingV2Enabled,
      isCashEnabled,
      acquirer,
    )
  }

  public static authorizedAccWithHidePersonalInfoMode(): MockPaymentDataProvider {
    return new MockPaymentDataProvider(
      new OAuthUserAccount(new UserAccount('test', 'qwe'), '', AccountType2.Yandex),
      'yandex_games_b9b09e9dcc29ef3063e10482b942aa59',
      '3136546516583745313514561',
      AvailableMethods.EMPTY,
      '100',
      'RUB',
      null,
      true,
      null,
      false,
      new PaymentMethodsFilter(),
      false,
      PersonalInfoMode.HIDE,
      AuthorizationMode.authorized,
      false,
      false,
      null,
    )
  }

  public static nonauthorizedAccWithAutomaticPersonalInfoMode(): MockPaymentDataProvider {
    return new MockPaymentDataProvider(
      new OAuthUserAccount(new UserAccount('test', 'qwe'), '', AccountType2.Yandex),
      'yandex_games_b9b09e9dcc29ef3063e10482b942aa59',
      '3136546516583745313514561',
      AvailableMethods.EMPTY,
      '100',
      'RUB',
      null,
      true,
      null,
      false,
      new PaymentMethodsFilter(),
      false,
      PersonalInfoMode.AUTOMATIC,
      AuthorizationMode.nonauthorized,
      false,
      false,
      null,
    )
  }

  public static authorizedAccWithAcquirer(acquirer: Nullable<Acquirer>): MockPaymentDataProvider {
    return new MockPaymentDataProvider(
      new OAuthUserAccount(new UserAccount('test', 'qwe'), '', AccountType2.Yandex),
      'yandex_games_b9b09e9dcc29ef3063e10482b942aa59',
      '3136546516583745313514561',
      AvailableMethods.EMPTY,
      '100',
      'RUB',
      null,
      true,
      null,
      false,
      new PaymentMethodsFilter(),
      false,
      PersonalInfoMode.HIDE,
      AuthorizationMode.authorized,
      false,
      false,
      acquirer,
    )
  }

  public async takeAppModel(): Promise<PaymentSdkModel> {
    return this.model
  }
}

describe('License agreement unit tests', () => {
  const modelKassa = MockPaymentDataProvider.authorizedAccWithAcquirer(Acquirer.kassa).model
  const modelNoAcquirer = MockPaymentDataProvider.authorizedAccWithAcquirer(null).model
  const licenseAgreementText =
    'Payment will be received by Индивидуальный предприниматель Soft Kitty Little ball of fur. By clicking "Pay", you consent to the terms and conditions.'
  const fillLicenseAgreementText =
    'Recipient: Индивидуальный предприниматель Soft Kitty Little ball of fur\n' +
    'OGRN/OGRNIP: 310287914385811\n' +
    'Business hours: c 00:00 до 05:00\n' +
    'Address: country RUS, city Москва, street Вознесенский переулок, house 7, postal code 195027\n\nBy clicking pay, I consent to the Terms of Service and to the processing of my data by "YANDEX" LLC and the Recipient for the purposes specified in this document, as well as the Privacy Policy. "YANDEX" LLC is not the recipient of the payment.'

  it('should show License agreement', () => {
    expect(modelKassa.licenseAgreementModel.isLicenseAgreementShown()).toBe(true)
    expect(modelKassa.licenseAgreementModel.getLicenseAgreement()).toBe(licenseAgreementText)
  })

  it('should show full License agreement', () => {
    expect(modelKassa.licenseAgreementModel.isLicenseAgreementShown()).toBe(true)
    expect(modelKassa.licenseAgreementModel.getFullLicenseAgreement()).toBe(fillLicenseAgreementText)
  })

  it('should not show License agreement', () => {
    expect(modelNoAcquirer.licenseAgreementModel.isLicenseAgreementShown()).toBe(false)
    expect(modelNoAcquirer.licenseAgreementModel.getLicenseAgreement()).toBe('')
  })

  it('should not show full License agreement', () => {
    expect(modelNoAcquirer.licenseAgreementModel.getFullLicenseAgreement()).toBe('')
  })
})

describe('New card fields validator unit tests', () => {
  const model = MockPaymentDataProvider.authorizedAccWithHidePersonalInfoMode().model

  beforeEach(() => {
    model.newCardFieldsValidatorModel.resetFields()
  })

  it('should be shown card number error message', () => {
    model.fillNewCardModel.tapOnField(NewCardField.cardNumber)
    model.fillNewCardModel.setFieldValue(NewCardField.cardNumber, '123')
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe(NewCardFieldsValidationError.cardNumber)
    model.fillNewCardModel.tapOnField(NewCardField.cardNumber)
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe(NewCardFieldsValidationError.cardNumber)
    model.fillNewCardModel.setFieldValue(NewCardField.cardNumber, '13')
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe(NewCardFieldsValidationError.cardNumber)
    model.fillNewCardModel.tapOnField(NewCardField.cardNumber)
    model.fillNewCardModel.setFieldValue(NewCardField.cardNumber, '1234123412341234')
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe(NewCardFieldsValidationError.cardNumber)
    model.fillNewCardModel.tapOnField(NewCardField.cardNumber)
    model.fillNewCardModel.setFieldValue(NewCardField.cardNumber, '4200006115699289')
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
  })

  it('should not be shown card number error message', () => {
    model.fillNewCardModel.tapOnField(NewCardField.cardNumber)
    model.fillNewCardModel.setFieldValue(NewCardField.cardNumber, '4200006115699289')
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
  })

  it('should not be shown card number error message (empty)', () => {
    model.fillNewCardModel.tapOnField(NewCardField.cardNumber)
    model.fillNewCardModel.setFieldValue(NewCardField.cardNumber, '')
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getCardNumberErrorText()).toBe('')
  })

  it('should be shown cvv error message', () => {
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    model.fillNewCardModel.setFieldValue(NewCardField.cvv, '12')
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe(NewCardFieldsValidationError.cvv)
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe(NewCardFieldsValidationError.cvv)
    model.fillNewCardModel.setFieldValue(NewCardField.cvv, '3')
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cardNumber)
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe(NewCardFieldsValidationError.cvv)
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    model.fillNewCardModel.setFieldValue(NewCardField.cvv, '123')
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe('')
  })

  it('should not be shown cvv error message', () => {
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    model.fillNewCardModel.setFieldValue(NewCardField.cvv, '123')
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe('')
  })

  it('should not be shown cvv error message (empty)', () => {
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    model.fillNewCardModel.setFieldValue(NewCardField.cvv, '')
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    expect(model.newCardFieldsValidatorModel.getCvvErrorText()).toBe('')
  })

  it('should not be shown expiration date error message', () => {
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    model.fillNewCardModel.setFieldValue(NewCardField.expirationDate, '1234')
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
  })

  it('should not be shown expiration date error message (empty)', () => {
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    model.fillNewCardModel.setFieldValue(NewCardField.expirationDate, '')
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
  })

  it('should be shown expiration date error message', () => {
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    model.fillNewCardModel.setFieldValue(NewCardField.expirationDate, '120')
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe(
      NewCardFieldsValidationError.expirationDate,
    )
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe(
      NewCardFieldsValidationError.expirationDate,
    )
    model.fillNewCardModel.setFieldValue(NewCardField.expirationDate, '10')
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cardNumber)
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe(
      NewCardFieldsValidationError.expirationDate,
    )
  })

  it('should be shown expiration date error message (month gt 12)', () => {
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    model.fillNewCardModel.setFieldValue(NewCardField.expirationDate, '1320')
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe(
      NewCardFieldsValidationError.expirationDate,
    )
  })

  it('should be shown expiration date error message (date length lt 5)', () => {
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    model.fillNewCardModel.setFieldValue(NewCardField.expirationDate, '120')
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe(
      NewCardFieldsValidationError.expirationDate,
    )
  })

  it('should be shown expiration date error message (month eq 0)', () => {
    model.fillNewCardModel.tapOnField(NewCardField.expirationDate)
    model.fillNewCardModel.setFieldValue(NewCardField.expirationDate, '0020')
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe('')
    model.fillNewCardModel.tapOnField(NewCardField.cvv)
    expect(model.newCardFieldsValidatorModel.getExpirationDateErrorText()).toBe(
      NewCardFieldsValidationError.expirationDate,
    )
  })
})

describe('Personal info fields validator unit tests', () => {
  const model = MockPaymentDataProvider.nonauthorizedAccWithAutomaticPersonalInfoMode().model

  beforeEach(() => {
    model.personalInfoFieldsValidatorModel.resetFields()
  })

  it('should not be shown email error message', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'fanem@yandex.ru')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.lastName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
  })

  it('should not be shown email error message (empty)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, '')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
  })

  it('should not be shown email error message (short)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, '1@a.2')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
  })

  it('should not be shown email error message (long)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'longlong@longlonglong.longlonglong')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
  })

  it('should not be shown email error message (many .)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'a.2.3@sa.aa')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
  })

  it('should be shown email error message(two @)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'fanem@yan@dex.ru')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.lastName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
  })

  it('should be shown email error message (no @)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'fanemyandex.ru')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
  })

  it('should be shown email error message (only @ and .)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, '@.')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
  })

  it('should be shown email error message (no .)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'fanemyandex@ru')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
  })

  it('should be shown email error message after return focus', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'fanemyandex@ru')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
  })

  it('should be shown email error message after return focus and not shown after enter value', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'fanemyandex@ru')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'fanemx.ru')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe(PersonalInformationValidationError.email)
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    model.personalInformationModel.setFieldValue(PersonalInformationField.email, 'fanem@yandex.ru')
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getEmailErrorText()).toBe('')
  })

  it('should not be shown phone number error message (7)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '71231231212')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.lastName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
  })

  it('should not be shown phone number error message (+7)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '+71231231212')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
  })

  it('should not be shown phone number error message (8)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '81231231212')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
  })

  it('should not be shown phone number error message (empty)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.email)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
  })

  it('should be shown phone number error message (starts with 1)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '11231231212')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.lastName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe(
      PersonalInformationValidationError.phoneNumber,
    )
  })

  it('should be shown phone number error message (starts with +1)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '+11231231212')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.lastName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe(
      PersonalInformationValidationError.phoneNumber,
    )
  })

  it('should be shown phone number error message (no digit symbols)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '7qw#ˆ`¥we!@#$')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.lastName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe(
      PersonalInformationValidationError.phoneNumber,
    )
  })

  it('should be shown phone number error message (short)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '7')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.lastName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe(
      PersonalInformationValidationError.phoneNumber,
    )
  })

  it('should be shown phone number error message (long)', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '712312312120')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.lastName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe(
      PersonalInformationValidationError.phoneNumber,
    )
  })

  it('should be shown phone number error message after return focus and not shown after enter value', () => {
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '712312312120')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe(
      PersonalInformationValidationError.phoneNumber,
    )
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe(
      PersonalInformationValidationError.phoneNumber,
    )
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '712120')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe(
      PersonalInformationValidationError.phoneNumber,
    )
    model.personalInformationModel.tapOnField(PersonalInformationField.phoneNumber)
    model.personalInformationModel.setFieldValue(PersonalInformationField.phoneNumber, '71231231212')
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
    model.personalInformationModel.tapOnField(PersonalInformationField.firstName)
    expect(model.personalInfoFieldsValidatorModel.getPhoneNumberErrorText()).toBe('')
  })
})
