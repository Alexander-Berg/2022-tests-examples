import {
  PaymentMethodTypes,
  paymentMethodTypeFromString,
  paymentMethodTypeToString,
} from '../../../code/models/payment-method-types'

describe('PaymentMethodTypes', () => {
  const map: Readonly<{ readonly [key: string]: PaymentMethodTypes }> = {
    CARD: PaymentMethodTypes.card,
  }
  it.each(Object.entries(map))('should be deserializable from string %s to PaymentMethodTypes %s', (key, value) => {
    expect(paymentMethodTypeFromString(key)).toBe(value)
  })
  it('should deserialize into null for unknown string value', () => {
    expect(paymentMethodTypeFromString('unknown')).toBeNull()
  })
  it.each(Object.entries(map))('should be serializable from PaymentMethodTypes %s to string %s', (key, value) => {
    expect(paymentMethodTypeToString(value)).toBe(key)
  })
})
