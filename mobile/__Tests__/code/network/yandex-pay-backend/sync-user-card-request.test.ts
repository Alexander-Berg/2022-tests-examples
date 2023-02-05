import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { SyncUserCardRequest } from '../../../../code/network/yandex-pay-backend/sync-user-card-request'

describe(SyncUserCardRequest, () => {
  it('should represent "sync_user_card" request', () => {
    const request = new SyncUserCardRequest('card_id')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('api/mobile/v1/sync_user_card')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(JSONItemFromJSON({ card_id: 'card_id' }))
  })
})
