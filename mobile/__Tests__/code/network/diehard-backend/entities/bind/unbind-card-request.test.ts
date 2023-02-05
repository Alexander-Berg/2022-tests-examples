import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { UnbindCardRequest } from '../../../../../../code/network/diehard-backend/entities/bind/unbind-card-request'

describe(UnbindCardRequest, () => {
  it('should build UnbindCardRequest request', () => {
    const request = new UnbindCardRequest('token', '1234')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          card: '1234',
        },
      }),
    )
    expect(request.targetPath()).toBe('unbind_card')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
