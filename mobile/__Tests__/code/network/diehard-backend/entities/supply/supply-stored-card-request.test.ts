import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { SupplyStoredCardRequest } from '../../../../../../code/network/diehard-backend/entities/supply/supply-stored-card-request'

describe(SupplyStoredCardRequest, () => {
  it('should build SupplyStoredCardRequest request', () => {
    const request = new SupplyStoredCardRequest('token', 'purchaseToken', 'email@ya.ru', 'card-1234', '123')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          purchase_token: 'purchaseToken',
          email: 'email@ya.ru',
          payment_method: 'card-1234',
          cvn: '123',
        },
      }),
    )
    expect(request.targetPath()).toBe('supply_payment_data')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
