import { AvailableMethods } from '../../../payment-sdk/code/models/available-methods'
import { Acquirer } from '../../../payment-sdk/code/network/mobile-backend/entities/init/acquirer'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { int64, Int64, Nullable } from '../../../../common/ys'
import { LicenseAgreementFeature } from '../feature/license-agreement-feature'
import { SbpBanksListFeature, SbpExtendedBanksListFeature } from '../feature/sbp-banks-list-feature'
import { SbpSampleBankFeature } from '../feature/sbp-sample-bank-feature'
import { UnbindCardFeature } from '../feature/unbind-card-feature'
import { PaymentErrorType } from '../payment-sdk-data'
import { PersonalInfoMode } from '../personal-info-mode'
import { App, FeatureID, FeatureRegistry } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { AppModel } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { OAuthUserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { copyArray } from '../../../testopithecus-common/code/utils/utils'
import { DeviceOrientationFeature } from '../feature/device-orientation-feature'
import { Fill3dsFeature } from '../feature/fill-3ds-feature'
import { FillNewCardFeature } from '../feature/fill-new-card-feature'
import { KeyboardFeature } from '../feature/keyboard-feature'
import { NewCardFieldsValidatorFeature } from '../feature/new-card-fields-validator-feature'
import { PaymentButtonFeature } from '../feature/payment-button-feature'
import { ReadPaymentDetailsFeature } from '../feature/payment-details-feature'
import {
  GooglePayFeature,
  ApplePayFeature,
  PaymentMethodsListFeature,
  SBPFeature,
  PreselectFeature,
  PreselectCvvFeature,
  MethodsListMode,
} from '../feature/payment-methods-list-feature'
import { PaymentScreenTitleFeature } from '../feature/payment-screen-title-feature'
import { PaymentResultFeature } from '../feature/payment-result-feature'
import { PersonalInformationFeature } from '../feature/personal-information-feature'
import { PersonalInformationFieldsValidatorFeature } from '../feature/personal-information-fields-validator-feature'
import { SampleAppFeature } from '../feature/sample-app-feature'
import { AuthorizationMode } from '../sample/sample-configuration'
import { DeviceOrientationModel } from './device-orientation-model'
import { Fill3dsModel } from './fill-3ds-model'
import { FillNewCardModel, NewCardField, NewCardMode } from './fill-new-card-model'
import { KeyboardModel } from './keyboard-model'
import { LicenseAgreementModel } from './license-agreement-model'
import { NewCardFieldsValidatorModel } from './new-card-fields-validator-model'
import { PaymentButtonModel } from './payment-button-model'
import { ReadPaymentDetailsModel } from './payment-details-model'
import {
  ApplePayModel,
  GooglePayModel,
  PaymentMethodsListModel,
  PreselectCvvModel,
  PreselectModel,
  SBPModel,
} from './payment-methods-list-model'
import { PaymentResultModel } from './payment-result-model'
import { PaymentScreenTitleModel } from './payment-screen-title-model'
import { PersonalInfoFieldsValidatorModel } from './personal-info-fields-validator-model'
import { PersonalInformationModel } from './personal-information-model'
import { SampleAppModel } from './sample-app-model'
import { SbpBanksListModel } from './sbp-banks-list-model'
import { SbpExtendedBanksListModel } from './sbp-extended-banks-list-model'
import { SbpSampleBankModel } from './sbp-sample-bank-model'
import { UnbindCardModel } from './unbind-card-model'

export class PaymentSdkModel implements AppModel {
  public readonly readPaymentDetailsModel: ReadPaymentDetailsModel
  public readonly sampleAppModel: SampleAppModel
  public readonly paymentMethodsListModel: PaymentMethodsListModel
  public readonly paymentResultModel: PaymentResultModel
  public readonly paymentButtonModel: PaymentButtonModel
  public readonly fillNewCardModel: FillNewCardModel
  public readonly fill3dsModel: Fill3dsModel
  public readonly newCardFieldsValidatorModel: NewCardFieldsValidatorModel
  public readonly keyboardModel: KeyboardModel
  public readonly deviceOrientationModel: DeviceOrientationModel
  public readonly paymentScreenTitleModel: PaymentScreenTitleModel
  public readonly personalInformationModel: PersonalInformationModel
  public readonly personalInfoFieldsValidatorModel: PersonalInfoFieldsValidatorModel
  public readonly applePayModel: ApplePayModel
  public readonly googlePayModel: GooglePayModel
  public readonly sbpModel: SBPModel
  public readonly preselectCvvModel: PreselectCvvModel
  public readonly preselectModel: PreselectModel
  public readonly unbindCardModel: UnbindCardModel
  public readonly licenseAgreementModel: LicenseAgreementModel
  public readonly sbpSampleBankModel: SbpSampleBankModel
  public readonly sbpBanksListModel: SbpBanksListModel
  public readonly sbpExtendedBanksListModel: SbpExtendedBanksListModel

  public constructor(
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
    this.paymentScreenTitleModel = new PaymentScreenTitleModel()
    this.readPaymentDetailsModel = new ReadPaymentDetailsModel(
      account,
      merchantId,
      paymentId,
      forceCvv,
      methodsFilter,
      isDarkModeEnabled,
      personalInfoShowingMode,
      authorizationMode,
      amount,
      currency,
      isBindingV2Enabled,
      isCashEnabled,
      expected3ds,
      acquirer,
    )
    this.paymentButtonModel = new PaymentButtonModel(amount, currency)
    this.keyboardModel = new KeyboardModel()
    this.applePayModel = new ApplePayModel(availableMethods)
    this.googlePayModel = new GooglePayModel(availableMethods)
    this.sbpModel = new SBPModel(availableMethods)
    this.paymentMethodsListModel = new PaymentMethodsListModel(
      availableMethods,
      this.paymentScreenTitleModel,
      this.keyboardModel,
      forceCvv,
      this.paymentButtonModel,
    )
    this.preselectCvvModel = new PreselectCvvModel(this.paymentMethodsListModel, forceCvv)
    this.fill3dsModel = new Fill3dsModel()
    this.fillNewCardModel = new FillNewCardModel(
      this.paymentScreenTitleModel,
      this.paymentMethodsListModel.getAllMethods().length > 0,
      this.keyboardModel,
    )
    this.licenseAgreementModel = new LicenseAgreementModel(
      this.readPaymentDetailsModel,
      this.fillNewCardModel,
      this.paymentMethodsListModel,
    )
    this.paymentResultModel = new PaymentResultModel(
      forcedErrorType,
      expected3ds,
      isCvvValid,
      this.fillNewCardModel,
      this.fill3dsModel,
    )
    this.preselectModel = new PreselectModel(
      this.paymentScreenTitleModel,
      this.keyboardModel,
      this.paymentMethodsListModel,
      this.paymentButtonModel,
      this.fillNewCardModel,
    )
    this.unbindCardModel = new UnbindCardModel(this.paymentMethodsListModel, this.paymentButtonModel)
    this.sampleAppModel = new SampleAppModel(
      this.paymentScreenTitleModel,
      this.readPaymentDetailsModel,
      this.paymentButtonModel,
      this.fillNewCardModel,
      this.paymentMethodsListModel,
      this.unbindCardModel,
      this.keyboardModel,
    )
    this.sbpSampleBankModel = new SbpSampleBankModel()
    this.sbpBanksListModel = new SbpBanksListModel()
    this.sbpExtendedBanksListModel = new SbpExtendedBanksListModel()
    this.newCardFieldsValidatorModel = new NewCardFieldsValidatorModel(this.fillNewCardModel)
    this.deviceOrientationModel = new DeviceOrientationModel()
    this.personalInformationModel = new PersonalInformationModel(this.readPaymentDetailsModel, this.keyboardModel)
    this.personalInfoFieldsValidatorModel = new PersonalInfoFieldsValidatorModel(this.personalInformationModel)
    this.paymentButtonModel.setButtonAction((enabled) => {
      const paymentMethods = this.paymentMethodsListModel.getMethods()
      const isSaveCardCheckboxEnabled = this.fillNewCardModel.isSaveCardCheckboxEnabled()
      if (
        paymentMethods.length < 5 &&
        this.fillNewCardModel.isAllFieldsFilled() &&
        (isSaveCardCheckboxEnabled || this.fillNewCardModel.getNewCardMode() === NewCardMode.bind) &&
        this.paymentResultModel.isSuccess()
      ) {
        if (this.paymentMethodsListModel.addCard(this.fillNewCardModel.getFieldValue(NewCardField.cardNumber))) {
          if (this.paymentMethodsListModel.getMethodsListMode() === MethodsListMode.preselect) {
            this.paymentMethodsListModel.selectMethod(this.paymentMethodsListModel.getCards().length - 1)
            this.unbindCardModel.setEditButtonShowingStatus(this.unbindCardModel.checkHasCardsToUnbind())
          }
        }
      }
      if (enabled) {
        this.paymentMethodsListModel.resetFields()
        this.fillNewCardModel.resetFields()
      }
    })
  }

  public static allSupportedFeatures: FeatureID[] = [
    ReadPaymentDetailsFeature.get.name,
    SampleAppFeature.get.name,
    PaymentScreenTitleFeature.get.name,
    PaymentMethodsListFeature.get.name,
    PaymentButtonFeature.get.name,
    PaymentResultFeature.get.name,
    FillNewCardFeature.get.name,
    Fill3dsFeature.get.name,
    NewCardFieldsValidatorFeature.get.name,
    KeyboardFeature.get.name,
    DeviceOrientationFeature.get.name,
    PersonalInformationFeature.get.name,
    PersonalInformationFieldsValidatorFeature.get.name,
    ApplePayFeature.get.name,
    GooglePayFeature.get.name,
    SBPFeature.get.name,
    PreselectFeature.get.name,
    PreselectCvvFeature.get.name,
    UnbindCardFeature.get.name,
    LicenseAgreementFeature.get.name,
    SbpSampleBankFeature.get.name,
    SbpBanksListFeature.get.name,
    SbpExtendedBanksListFeature.get.name,
  ]

  public supportedFeatures: FeatureID[] = copyArray(PaymentSdkModel.allSupportedFeatures)

  public copy(): AppModel {
    const account = this.readPaymentDetailsModel.getAccount()
    const merchantId = this.readPaymentDetailsModel.getMerchantId()
    const paymentId = this.readPaymentDetailsModel.getPaymentId()
    const availableMethods = this.paymentMethodsListModel.getAvailableMethods()
    const amount = this.readPaymentDetailsModel.getAmount()
    const currency = this.readPaymentDetailsModel.getCurrency()
    const expected3ds = this.readPaymentDetailsModel.getExpected3ds()
    const isCvvValid = this.paymentResultModel.isCvvValid()
    const forcedErrorType = this.paymentResultModel.forcedPaymentErrorType()
    const forceCvv = this.readPaymentDetailsModel.getForceCvv()
    const methodsFilter = this.readPaymentDetailsModel.getPaymentMethodsFilter()
    const isDarkModeEnabled = this.readPaymentDetailsModel.isDarkModeEnabled()
    const personalInfoShowingMode = this.readPaymentDetailsModel.getPersonalInfoShowingMode()
    const authorizationMode = this.readPaymentDetailsModel.getAuthorizationMode()
    const isBindingV2Enabled = this.readPaymentDetailsModel.isBindingV2Enabled()
    const isCashEnabled = this.readPaymentDetailsModel.isCashEnabled()
    const acquirer = this.readPaymentDetailsModel.getAcquirer()

    return new PaymentSdkModel(
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

  public getCurrentStateHash(): Int64 {
    return int64(0) // TODO: если вы хотите умный перебор всех состояний приложения, то можете реализовать
  }

  public async dump(model: App): Promise<string> {
    return ''
  }

  public getFeature(feature: FeatureID): any {
    return new FeatureRegistry()
      .register(ReadPaymentDetailsFeature.get, this.readPaymentDetailsModel)
      .register(SampleAppFeature.get, this.sampleAppModel)
      .register(PaymentScreenTitleFeature.get, this.paymentScreenTitleModel)
      .register(PaymentMethodsListFeature.get, this.paymentMethodsListModel)
      .register(PaymentButtonFeature.get, this.paymentButtonModel)
      .register(PaymentResultFeature.get, this.paymentResultModel)
      .register(FillNewCardFeature.get, this.fillNewCardModel)
      .register(Fill3dsFeature.get, this.fill3dsModel)
      .register(NewCardFieldsValidatorFeature.get, this.newCardFieldsValidatorModel)
      .register(KeyboardFeature.get, this.keyboardModel)
      .register(DeviceOrientationFeature.get, this.deviceOrientationModel)
      .register(PersonalInformationFeature.get, this.personalInformationModel)
      .register(PersonalInformationFieldsValidatorFeature.get, this.personalInfoFieldsValidatorModel)
      .register(ApplePayFeature.get, this.applePayModel)
      .register(GooglePayFeature.get, this.googlePayModel)
      .register(SBPFeature.get, this.sbpModel)
      .register(PreselectFeature.get, this.preselectModel)
      .register(PreselectCvvFeature.get, this.preselectCvvModel)
      .register(UnbindCardFeature.get, this.unbindCardModel)
      .register(LicenseAgreementFeature.get, this.licenseAgreementModel)
      .register(SbpSampleBankFeature.get, this.sbpSampleBankModel)
      .register(SbpBanksListFeature.get, this.sbpBanksListModel)
      .register(SbpExtendedBanksListFeature.get, this.sbpExtendedBanksListModel)
      .get(feature)
  }
}
