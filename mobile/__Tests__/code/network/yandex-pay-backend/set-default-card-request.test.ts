import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { SetDefaultCardRequest } from '../../../../code/network/yandex-pay-backend/set-default-card-request'

describe(SetDefaultCardRequest, () => {
  it('should represent "user_cards/{card_id}/set_default" request', () => {
    const request = new SetDefaultCardRequest('card1')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('api/mobile/v1/user_cards/card1/set_default')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(JSONItemFromJSON({}))
  })
})
