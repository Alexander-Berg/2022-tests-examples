import { PaymentToken } from '../../../code/models/payment-token'

describe(PaymentToken, () => {
  it('build PaymentToken', () => {
    const token = new PaymentToken('token')
    expect(token.token).toStrictEqual('token')
  })
})
