import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { EncryptedAppIdRequest } from '../../../../code/network/yandex-pay-backend/encrypted-app-id-request'

describe(EncryptedAppIdRequest, () => {
  it('should represent "encrypted_id" request', () => {
    const request = new EncryptedAppIdRequest('app_id')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('api/mobile/v1/wallet/app/encrypted_id')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(JSONItemFromJSON({ raw_app_id: 'app_id' }))
  })
})
