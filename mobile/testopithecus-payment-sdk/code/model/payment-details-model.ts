import { Acquirer } from '../../../payment-sdk/code/network/mobile-backend/entities/init/acquirer'
import { MerchantInfo } from '../../../payment-sdk/code/network/mobile-backend/entities/init/merchant-info'
import { Nullable } from '../../../../common/ys'
import { getMerchantInfoByAcquirer } from '../payment-sdk-data'
import { PersonalInfoMode } from '../personal-info-mode'
import { OAuthUserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { ReadPaymentDetails } from '../feature/payment-details-feature'
import { AuthorizationMode } from '../sample/sample-configuration'

export class ReadPaymentDetailsModel implements ReadPaymentDetails {
  public constructor(
    private readonly account: OAuthUserAccount,
    private readonly merchantId: string,
    private readonly paymentId: string,
    private readonly forceCvv: boolean,
    private readonly methodsFilter: PaymentMethodsFilter,
    private readonly darkModeEnabled: boolean,
    private readonly personalInfoShowingMode: PersonalInfoMode,
    private readonly authorizationMode: AuthorizationMode,
    private readonly amount: string,
    private readonly currency: string,
    private readonly bindingV2Enabled: boolean,
    private readonly cashEnabled: boolean,
    private readonly expected3ds: Nullable<string>,
    private readonly acquirer: Nullable<Acquirer>,
  ) {}

  public getAccount(): OAuthUserAccount {
    return this.account
  }

  public getAmount(): string {
    return this.amount
  }

  public getCurrency(): string {
    return this.currency
  }

  public getMerchantId(): string {
    return this.merchantId
  }

  public getPaymentId(): string {
    return this.paymentId
  }

  public getForceCvv(): boolean {
    return this.forceCvv
  }

  public getPaymentMethodsFilter(): PaymentMethodsFilter {
    return this.methodsFilter
  }

  public isDarkModeEnabled(): boolean {
    return this.darkModeEnabled
  }

  public isBindingV2Enabled(): boolean {
    return this.bindingV2Enabled
  }

  public isCashEnabled(): boolean {
    return this.cashEnabled
  }

  public getPersonalInfoShowingMode(): PersonalInfoMode {
    return this.personalInfoShowingMode
  }

  public getAuthorizationMode(): AuthorizationMode {
    return this.authorizationMode
  }

  public getExpected3ds(): Nullable<string> {
    return this.expected3ds
  }

  public isPersonalInfoShown(): boolean {
    const personalInfoMode = this.getPersonalInfoShowingMode()
    const authMode = this.getAuthorizationMode()
    return (
      personalInfoMode === PersonalInfoMode.SHOW ||
      (personalInfoMode === PersonalInfoMode.AUTOMATIC && authMode === AuthorizationMode.nonauthorized)
    )
  }

  public getMerchantInfo(): Nullable<MerchantInfo> {
    return this.acquirer === null ? null : getMerchantInfoByAcquirer(this.acquirer)
  }

  public getAcquirer(): Nullable<Acquirer> {
    return this.acquirer
  }
}
