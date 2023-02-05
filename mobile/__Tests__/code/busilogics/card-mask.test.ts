import { BankName } from '../../../code/busilogics/bank-name'
import { cardToMethod, maskCardNumber, maskCardNumberWithBin } from '../../../code/busilogics/card-mask'
import { NewCard } from '../../../code/models/new-card'
import { PaymentMethod } from '../../../code/network/mobile-backend/entities/methods/payment-method'

it('should mask card number with bin correctlly', () => {
  expect(maskCardNumberWithBin('1234123412341234')).toBe('123412******1234')
  expect(maskCardNumberWithBin('1234123412341')).toBe('123412*****41')
})

it('should mask card number correctlly', () => {
  expect(maskCardNumber('1234123412341234')).toBe('************1234')
  expect(maskCardNumber('1234123412341')).toBe('*********2341')
})

it('should build payment method correctlly', () => {
  const card = new NewCard('5555444433331111', '01', '22', '', false)
  const id = 'card-x-1234567'
  expect(cardToMethod(id, card)).toStrictEqual(
    new PaymentMethod(id, '************1111', 'MasterCard', true, BankName.UnknownBank, null, null),
  )
})

it('should build payment method with unknown system correctlly', () => {
  const card = new NewCard('1111444433331111', '01', '22', '', false)
  const id = 'card-x-1234567'
  expect(cardToMethod(id, card)).toStrictEqual(
    new PaymentMethod(id, '************1111', '', true, BankName.UnknownBank, null, null),
  )
})
