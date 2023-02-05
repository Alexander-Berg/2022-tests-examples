import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { BindGooglePayTokenRequest } from '../../../../../../code/network/diehard-backend/entities/bind/bind-google-pay-token-request'
import { RegionIds } from '../../../../../../code/network/diehard-backend/entities/bind/region-ids'

describe(BindGooglePayTokenRequest, () => {
  it('should build BindGooglePayTokenRequest request', () => {
    const request = new BindGooglePayTokenRequest('token', 'serviceToken', 'orderTag', 'googleToken', RegionIds.russia)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          service_token: 'serviceToken',
          order_tag: 'orderTag',
          google_pay_token: 'googleToken',
          region_id: 225,
        },
      }),
    )
    expect(request.targetPath()).toBe('bind_google_pay_token')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
