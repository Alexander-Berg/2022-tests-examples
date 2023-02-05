import { Merchant } from '../../../code/models/merchant'

describe(Merchant, () => {
  it('build Merchant', () => {
    const merchant = new Merchant('token', 'name')
    expect(merchant.serviceToken).toStrictEqual('token')
    expect(merchant.localizedName).toStrictEqual('name')
  })
})
