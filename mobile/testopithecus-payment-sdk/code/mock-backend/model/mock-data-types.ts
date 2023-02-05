import { Acquirer } from '../../../../payment-sdk/code/network/mobile-backend/entities/init/acquirer'
import { MerchantInfo } from '../../../../payment-sdk/code/network/mobile-backend/entities/init/merchant-info'
import { Nullable, YSError } from '../../../../../common/ys'
import {
  EnabledPaymentMethod,
  PaymentMethod,
} from '../../../../payment-sdk/code/network/mobile-backend/entities/methods/payment-method'

export class MockCard {
  public constructor(
    public readonly cardNumber: string,
    public readonly expirationMonth: string,
    public readonly expirationYear: string,
    public readonly cvn: string,
    public readonly id: string,
  ) {}
}

export enum Verification3dsState {
  not_required = 'not_required',
  required = 'required',
  provided = 'provided',
}

export class MockSuppliedMethod {
  public constructor(
    public readonly methodId: string,
    public verification3ds: Verification3dsState = Verification3dsState.not_required,
    public code3ds: string = '',
    public sbpConfirmed: boolean = false,
  ) {}
}

export class MockOrder {
  public constructor(
    public readonly id: string,
    public readonly service: Nullable<string>,
    public readonly isBinding: boolean = false,
    public amount: string = '',
    public token: string = '',
    public acquirer: Nullable<Acquirer> = null,
    public checkCvn: boolean = false,
    public forced3ds: boolean = false,
    public supplied: Nullable<MockSuppliedMethod> = null,
    public finished: boolean = false,
  ) {}
}

export class MockBindingV2 {
  public constructor(
    public readonly oAuth: string,
    public readonly service: string,
    public readonly card: MockCard,
    public purchaseToken: string,
  ) {}
}

export class PaymentMethodsData {
  public constructor(
    public readonly methods: PaymentMethod[],
    public readonly enabledMethods: EnabledPaymentMethod[],
  ) {}
}

export class InitPaymentData {
  public constructor(
    public readonly purchaseToken: string,
    public readonly amount: string,
    public readonly methods: PaymentMethodsData,
    public readonly acquirer: Nullable<Acquirer> = null,
    public readonly merchantInfo: Nullable<MerchantInfo> = null,
  ) {}
}

export class CheckPaymentData {
  public constructor(
    public readonly status: string,
    public readonly statusDesc: string,
    public readonly purchaseToken: string,
    public readonly amount: string,
    public readonly isBinding: boolean,
    public readonly timestamp: string,
    public readonly redirect3ds: Nullable<string>,
    public readonly sbpPaymentForm: Nullable<string>,
  ) {}
}

export class MobPaymentError extends YSError {
  public constructor(public readonly status: string, message: string) {
    super(message)
  }
}

export enum FamilyInfoMode {
  disabled = 'disabled',
  enabled_low_allowance = 'enabled_low_allowance',
  enabled_high_allowance = 'enabled_high_allowance',
  enabled_unbound_limit = 'enabled_unbound_limit',
}

export function stringToFamilyInfoMode(value: string): Nullable<FamilyInfoMode> {
  switch (value) {
    case FamilyInfoMode.disabled.toString():
      return FamilyInfoMode.disabled
    case FamilyInfoMode.enabled_low_allowance.toString():
      return FamilyInfoMode.enabled_low_allowance
    case FamilyInfoMode.enabled_high_allowance.toString():
      return FamilyInfoMode.enabled_high_allowance
    case FamilyInfoMode.enabled_unbound_limit.toString():
      return FamilyInfoMode.enabled_unbound_limit
    default:
      return null
  }
}
