import { CardPaymentSystem } from '../../../payment-sdk/code/busilogics/card-payment-system'
import { CardGenerator } from '../../code/card-generator'
import { normalizeCardNumber, formatCvv, normalizeExpirationDate } from '../../code/utils/payment-utils'
import { checkLuhn } from '../../../payment-sdk/code/busilogics/check-luhn'

describe('Payment utils unit tests', () => {
  it('should be correct formatted cvv', () => {
    expect(formatCvv('1')).toBe('•')
    expect(formatCvv('12')).toBe('••')
    expect(formatCvv('123')).toBe('•••')
    expect(formatCvv('1234')).toBe('•••')
    expect(formatCvv('zxc')).toBe('')
    expect(formatCvv('!sπˆ2#$%^')).toBe('•')
  })

  it('should be correct formatted expiration date', () => {
    expect(normalizeExpirationDate('0')).toBe('0')
    expect(normalizeExpirationDate('01')).toBe('01')
    expect(normalizeExpirationDate('01/0')).toBe('010')
    expect(normalizeExpirationDate('01/20')).toBe('0120')
    expect(normalizeExpirationDate('01/201')).toBe('01201')
    expect(normalizeExpirationDate('asdqwe')).toBe('')
    expect(normalizeExpirationDate('!/sπˆ2#$%^')).toBe('2')
  })

  it('should be correct formatted card number', () => {
    expect(normalizeCardNumber('0')).toBe('0')
    expect(normalizeCardNumber('01234')).toBe('01234')
    expect(normalizeCardNumber('01234 56789')).toBe('0123456789')
    expect(normalizeCardNumber('0123456789012345')).toBe('0123456789012345')
    expect(normalizeCardNumber('01234567890123455678')).toBe('01234567890123455678')
    expect(normalizeCardNumber('as01234ˆπd!@#567890123asd45')).toBe('0123456789012345')
    expect(normalizeCardNumber('!/sπˆ#$%^')).toBe('')
  })

  it.each([
    [CardPaymentSystem.MIR, '2'],
    [CardPaymentSystem.VISA, '4'],
    [CardPaymentSystem.AmericanExpress, '34'],
    [CardPaymentSystem.Maestro, '50'],
    [CardPaymentSystem.MasterCard, '51'],
    [CardPaymentSystem.UNKNOWN, '00'],
  ])('should be correct generated card number', (cardType, prefix) => {
    const cardNumber = CardGenerator.generateCardNumber(cardType)
    expect(cardNumber.slice(0, prefix.length)).toBe(prefix)
    expect(checkLuhn(cardNumber)).toBe(true)
  })
})
