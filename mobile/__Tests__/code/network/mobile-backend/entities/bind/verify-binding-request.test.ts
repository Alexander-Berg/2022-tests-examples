import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { VerifyBindingRequest } from '../../../../../../code/network/mobile-backend/entities/bind/verify-binding-request'

describe(VerifyBindingRequest, () => {
  it('should build VerifyBindingRequest', () => {
    const request = new VerifyBindingRequest('card-123')

    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.targetPath()).toBe('verify_binding')
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        binding_id: 'card-123',
      }),
    )
  })
})
