import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { Nullable, Throwing } from '../../../../common/ys'
import { PersonalInfoMode } from '../personal-info-mode'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { OAuthUserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AuthorizationMode } from '../sample/sample-configuration'

export class ReadPaymentDetailsFeature extends Feature<ReadPaymentDetails> {
  public static get: ReadPaymentDetailsFeature = new ReadPaymentDetailsFeature()

  private constructor() {
    super('ReadPaymentDetails', 'Payments details stores in model and use to launch first PaymentSDK screen')
  }
}

export interface ReadPaymentDetails {
  getAccount(): OAuthUserAccount

  getMerchantId(): Throwing<string>

  getPaymentId(): Throwing<string>

  getForceCvv(): boolean

  getPaymentMethodsFilter(): PaymentMethodsFilter

  isDarkModeEnabled(): boolean

  getPersonalInfoShowingMode(): PersonalInfoMode

  getAuthorizationMode(): AuthorizationMode

  isPersonalInfoShown(): boolean

  isBindingV2Enabled(): boolean

  isCashEnabled(): boolean

  getExpected3ds(): Nullable<string>
}
