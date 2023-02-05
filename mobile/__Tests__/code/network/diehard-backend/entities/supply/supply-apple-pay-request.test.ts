import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { SupplyApplePayRequest } from '../../../../../../code/network/diehard-backend/entities/supply/supply-apple-pay-request'

describe(SupplyApplePayRequest, () => {
  it('should build SupplyApplePayRequest request', () => {
    const request = new SupplyApplePayRequest('token', 'purchaseToken', 'email@ya.ru', 'appleToken', null)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          purchase_token: 'purchaseToken',
          email: 'email@ya.ru',
          apple_token: 'appleToken',
        },
      }),
    )
    expect(request.targetPath()).toBe('supply_payment_data')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
