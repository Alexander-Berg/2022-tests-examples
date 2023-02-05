import { Int32, Nullable, Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class PaymentMethodsListFeature extends Feature<PaymentMethodsList> {
  public static get: PaymentMethodsListFeature = new PaymentMethodsListFeature()

  private constructor() {
    super('PaymentMethodsListFeature', 'Represent payment methods list')
  }
}

export interface PaymentMethodsList {
  waitForPaymentMethods(mSec: Int32): Throwing<boolean>

  getMethods(): string[]

  selectMethod(index: Int32): Throwing<void>

  getSelected(): Throwing<Int32>

  tapOnCvvField(): Throwing<void>

  setCvvFieldValue(cvv: string): Throwing<void>

  getCvvFieldValue(): Throwing<Nullable<string>>

  clickNewCard(): Throwing<void>

  selectSbpMethod(): Throwing<void>

  getMethodsListMode(): Throwing<MethodsListMode>
}

export class PreselectFeature extends Feature<Preselect> {
  public static get: PreselectFeature = new PreselectFeature()

  private constructor() {
    super('PreselectFeature', 'Specific for Preselect mode actions')
  }
}

export interface Preselect {
  selectCash(): Throwing<void>

  isCashSelected(): Throwing<boolean>

  tapOnSelectButton(): Throwing<void>

  tapOnOtherCard(): Throwing<void>

  tapOnAddCard(): Throwing<void>
}

export interface PreselectCvv {
  waitForPreselectCvv(mSec: Int32): Throwing<boolean>

  getCardName(): Throwing<string>

  getCvvFieldValue(): Throwing<Nullable<string>>
}

export class PreselectCvvFeature extends Feature<PreselectCvv> {
  public static get: PreselectCvvFeature = new PreselectCvvFeature()

  private constructor() {
    super('PreselectCvvFeature', 'Preselect Cvv screen')
  }
}

export class ApplePayFeature extends Feature<ApplePay> {
  public static get: ApplePayFeature = new ApplePayFeature()

  private constructor() {
    super('ApplePayFeature', 'Check is Apple pay available')
  }
}

export interface ApplePay {
  isAvailable(): boolean
}

export class GooglePayFeature extends Feature<GooglePay> {
  public static get: GooglePayFeature = new GooglePayFeature()

  private constructor() {
    super('GooglePayFeature', 'Check is Google pay available')
  }
}

export interface GooglePay {
  isAvailable(): boolean
}

export class SBPFeature extends Feature<SBP> {
  public static get: SBPFeature = new SBPFeature()

  private constructor() {
    super('SBPFeature', 'Check is SBP available')
  }
}

export interface SBP {
  isAvailable(): boolean
}

export enum MethodsListMode {
  regular,
  preselect,
}
