import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { NewCardBindingRequest } from '../../../../../../code/network/diehard-backend/entities/bind/new-card-binding-request'
import { RegionIds } from '../../../../../../code/network/diehard-backend/entities/bind/region-ids'

describe(NewCardBindingRequest, () => {
  it('should build NewCardBindingRequest', () => {
    const request = new NewCardBindingRequest('token', 'serviceToken', 'SHA512', 'xxx', RegionIds.russia)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        card_data_encrypted: 'xxx',
        hash_algo: 'SHA512',
        service_token: 'serviceToken',
        region_id: 225,
      }),
    )
    expect(request.targetPath()).toBe('bindings/v2.0/bindings')
    expect(request.urlExtra().asMap().size).toBe(0)
    const headers = request.headersExtra()
    expect(headers.getString('X-Oauth-Token')).toStrictEqual('token')
    expect(headers.getString('X-Service-Token')).toStrictEqual('serviceToken')
  })
})
