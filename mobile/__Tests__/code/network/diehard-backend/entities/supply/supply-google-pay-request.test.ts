import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { SupplyGooglePayRequest } from '../../../../../../code/network/diehard-backend/entities/supply/supply-google-pay-request'

describe(SupplyGooglePayRequest, () => {
  it('should build SupplyGooglePayRequest request', () => {
    const request = new SupplyGooglePayRequest('token', 'purchaseToken', 'email@ya.ru', 'googlePayToken', null)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          purchase_token: 'purchaseToken',
          email: 'email@ya.ru',
          google_pay_token: 'googlePayToken',
        },
      }),
    )
    expect(request.targetPath()).toBe('supply_payment_data')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
