import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { BindAppleTokenRequest } from '../../../../../../code/network/diehard-backend/entities/bind/bind-apple-token-request'
import { RegionIds } from '../../../../../../code/network/diehard-backend/entities/bind/region-ids'

describe(BindAppleTokenRequest, () => {
  it('should build BindAppleTokenRequest request', () => {
    const request = new BindAppleTokenRequest(
      'token',
      'serviceToken',
      'purchaseToken',
      'orderTag',
      'appleToken',
      RegionIds.russia,
    )
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          service_token: 'serviceToken',
          purchase_token: 'purchaseToken',
          order_tag: 'orderTag',
          apple_token: 'appleToken',
          region_id: 225,
        },
      }),
    )
    expect(request.targetPath()).toBe('bind_apple_token')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
