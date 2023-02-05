import { NetworkMethod, UrlRequestEncoding } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { UserCardsRequest } from '../../../../code/network/yandex-pay-backend/user-cards-request'

describe(UserCardsRequest, () => {
  it('should represent "user_cards" request', () => {
    const request = new UserCardsRequest()
    expect(request.encoding()).toBeInstanceOf(UrlRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.targetPath()).toBe('api/mobile/v1/user_cards')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(JSONItemFromJSON({}))
  })
})
