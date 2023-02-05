import { BankName } from '../../../code/busilogics/bank-name'
import { AvailableMethods } from '../../../code/models/available-methods'
import { PaymentMethod } from '../../../code/network/mobile-backend/entities/methods/payment-method'

describe(AvailableMethods, () => {
  const payments = [new PaymentMethod('123', 'test', 'visa', false, BankName.UnknownBank, null, null)]
  it('should return false for isEmpty() check', () => {
    const mustHaveNewCard = [
      new AvailableMethods(payments, false, false, false, false),
      new AvailableMethods([], true, false, false, false),
      new AvailableMethods([], false, true, false, false),
      new AvailableMethods([], false, false, true, false),
      new AvailableMethods([], false, false, false, true),
    ]

    for (const methods of mustHaveNewCard) {
      expect(methods.isEmpty()).toBe(false)
    }
  })
  it('should return true for hasNewCardOption', () => {
    const methods = new AvailableMethods([], false, false, false, false)
    expect(methods.isEmpty()).toBe(true)
  })
  it('should build the same object', () => {
    const methods = new AvailableMethods(payments, true, true, true, true)
    expect(methods).toStrictEqual(methods.builder().build())
  })
})
