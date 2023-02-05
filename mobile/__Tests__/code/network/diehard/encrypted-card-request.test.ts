import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { EncryptedCardRequest } from '../../../../code/network/diehard/encrypted-card-request'

describe(EncryptedCardRequest, () => {
  it('should represent "encrypted_card" request', () => {
    const request = new EncryptedCardRequest('req-id', 'card_id', '000')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('cp/inbound/api/v1/yandex-pay/wallet/thales/encrypted-card')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().getString('x-request-id')).toBe('req-id')
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          card_id: 'card_id',
          cvn: '000',
        },
      }),
    )
  })
})
