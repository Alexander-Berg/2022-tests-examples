import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { BindNewCardRequest } from '../../../../../../code/network/diehard-backend/entities/bind/bind-new-card-request'
import { RegionIds } from '../../../../../../code/network/diehard-backend/entities/bind/region-ids'

describe(BindNewCardRequest, () => {
  it('should build BindNewCardRequest request', () => {
    const request = new BindNewCardRequest(
      'token',
      'serviceToken',
      '1234567812345678',
      '12',
      '21',
      '123',
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
          card_number: '1234567812345678',
          expiration_month: '12',
          expiration_year: '21',
          cvn: '123',
          region_id: 225,
        },
      }),
    )
    expect(request.targetPath()).toBe('bind_card')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
