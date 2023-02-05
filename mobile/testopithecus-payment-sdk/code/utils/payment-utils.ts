import { Int32, Nullable } from '../../../../common/ys'
import { CardPaymentSystemChecker, CardPaymentSystem } from '../../../payment-sdk/code/busilogics/card-payment-system'
import { PaymentMethod } from '../../../payment-sdk/code/network/mobile-backend/entities/methods/payment-method'
import { PaymentMethodName } from '../payment-sdk-data'

export function normalizeCardNumber(cardNumber: string): string {
  return cardNumber
    .split('')
    .filter((item) => isCharDigit(item))
    .join('')
}

export function formatCvv(cvv: Nullable<string>): string {
  if (cvv === null) {
    return ''
  }
  let formattedCvv = ''
  const cvvList = cvv
    .split('')
    .filter((item) => isCharDigit(item) || item === '•')
    .slice(0, 3)
  for (const _ of cvvList) {
    formattedCvv += '•'
  }
  return formattedCvv
}

export function isCharDigit(str: string): boolean {
  return ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'].includes(str)
}

export function normalizeExpirationDate(expirationDate: string): string {
  return expirationDate
    .split('')
    .filter((item) => isCharDigit(item))
    .join('')
}

export function buildPaymentMethodNameByCardNumber(cardNumber: string): string {
  const system = CardPaymentSystemChecker.instance.lookup(cardNumber)
  return `${system !== CardPaymentSystem.UNKNOWN ? system.toString() : ''}  •••• ${cardNumber.slice(-4)}`
}

export function buildPaymentMethodNameByPaymentMethod(paymentMethod: PaymentMethod): string {
  const prefix = paymentMethod.familyInfo !== null ? PaymentMethodName.familyPayPrefix : paymentMethod.system
  return `${prefix}  •••• ${paymentMethod.account.slice(-4)}`
}

export class PaymentSdkConstants {
  public static readonly SCREEN_APPEARANCE_TIMEOUT: Int32 = 20_000
}
