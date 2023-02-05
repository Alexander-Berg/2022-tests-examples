import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { Throwing } from '../../../../common/ys'
import { PersonalInfoMode } from '../personal-info-mode'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { OAuthUserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AuthorizationMode } from '../sample/sample-configuration'

export class SampleAppFeature extends Feature<SampleApp> {
  public static get: SampleAppFeature = new SampleAppFeature()

  private constructor() {
    super('SampleAppFeature', 'Feature for interacting with the SampleApp')
  }
}

export interface SampleApp {
  startSampleApp(
    user: OAuthUserAccount,
    merchantId: string,
    paymentId: string,
    additionalSettings: PaymentAdditionalSettings,
  ): Throwing<void>

  startRegularPayment(): Throwing<void>

  startPreselectPayment(): Throwing<void>

  bindCard(): Throwing<void>

  unbindCard(): Throwing<void>

  waitForAppReady(): Throwing<boolean>
}

export class PaymentAdditionalSettings {
  public constructor(
    public readonly forceCvv: boolean,
    public readonly paymentMethodsFilter: PaymentMethodsFilter,
    public readonly isDarkModeEnabled: boolean,
    public readonly personalInfoShowingMode: PersonalInfoMode,
    public readonly authorizationMode: AuthorizationMode,
    public readonly useBindingV2: boolean,
    public readonly cashEnabled: boolean,
  ) {}
}
