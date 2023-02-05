import { MerchantInfo } from '../../../payment-sdk/code/network/mobile-backend/entities/init/merchant-info'
import { isStringNullOrEmpty } from '../../../common/code/utils/strings'
import { Nullable, Throwing } from '../../../../common/ys'
import { LicenseAgreement } from '../feature/license-agreement-feature'
import { MethodsListMode } from '../feature/payment-methods-list-feature'
import { FillNewCardModel, NewCardMode } from './fill-new-card-model'
import { ReadPaymentDetailsModel } from './payment-details-model'
import { PaymentMethodsListModel } from './payment-methods-list-model'

export class LicenseAgreementModel implements LicenseAgreement {
  public constructor(
    private readonly readPaymentDetailsModel: ReadPaymentDetailsModel,
    private readonly fillNewCardModel: FillNewCardModel,
    private readonly paymentMethodsListModel: PaymentMethodsListModel,
  ) {}

  private termsOfUse: string = 'By clicking "Pay", you consent to the terms and conditions.'
  private fullTermsOfUse: string =
    'By clicking pay, I consent to the Terms of Service and to the processing of my data by "YANDEX" LLC and the ' +
    'Recipient for the purposes specified in this document, as well as the Privacy Policy. "YANDEX" LLC is not ' +
    'the recipient of the payment.'

  private merchantInfoText(merchantInfo: MerchantInfo): string {
    return `Payment will be received by ${merchantInfo.name}`
  }

  private fullMerchantInfoText(merchantInfo: Nullable<MerchantInfo>): string {
    if (merchantInfo === null) {
      return ''
    }
    let text = ''
    if (!isStringNullOrEmpty(merchantInfo.name)) {
      text += `Recipient: ${merchantInfo.name}\n`
    }
    if (!isStringNullOrEmpty(merchantInfo.ogrn)) {
      text += `OGRN/OGRNIP: ${merchantInfo.ogrn}\n`
    }
    if (!isStringNullOrEmpty(merchantInfo.scheduleText)) {
      text += `Business hours: ${merchantInfo.scheduleText}\n`
    }
    const merchantAddress = merchantInfo.merchantAddress
    if (merchantAddress !== null) {
      text +=
        `Address: country ${merchantAddress!.country}, ` +
        `city ${merchantAddress!.city}, ` +
        `street ${merchantAddress!.street}, ` +
        `house ${merchantAddress!.home}, ` +
        `postal code ${merchantAddress!.zip}`
    }
    return text
  }

  public getLicenseAgreement(): Throwing<string> {
    if (!this.isLicenseAgreementShown()) {
      return ''
    }
    const merchantInfo = this.readPaymentDetailsModel.getMerchantInfo()
    return merchantInfo === null || isStringNullOrEmpty(merchantInfo!.name)
      ? this.termsOfUse
      : `${this.merchantInfoText(merchantInfo)}. ${this.termsOfUse}`
  }

  public isLicenseAgreementShown(): Throwing<boolean> {
    const isNoBindMode = this.fillNewCardModel.getNewCardMode() !== NewCardMode.bind
    const isNoPreselect = this.paymentMethodsListModel.getMethodsListMode() !== MethodsListMode.preselect
    return this.readPaymentDetailsModel.getAcquirer() !== null && isNoBindMode && isNoPreselect
  }

  public closeFullLicenseAgreement(): Throwing<void> {
    // do nothing
  }

  public getFullLicenseAgreement(): Throwing<string> {
    return this.isLicenseAgreementShown()
      ? `${this.fullMerchantInfoText(this.readPaymentDetailsModel.getMerchantInfo())}\n\n${this.fullTermsOfUse}`
      : ''
  }

  public openFullLicenseAgreement(): Throwing<void> {
    // do nothing
  }
}
