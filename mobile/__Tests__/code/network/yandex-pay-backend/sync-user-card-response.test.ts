import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { AuthMethods } from '../../../../code/models/auth-methods'
import { CardArt } from '../../../../code/models/card-art'
import { CardNetworks } from '../../../../code/models/card-networks'
import { UserCard } from '../../../../code/models/user-card'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { SyncUserCardResponse } from '../../../../code/network/yandex-pay-backend/sync-user-card-response'

describe(SyncUserCardResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = SyncUserCardResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          id: 'id-1',
          trust_card_id: 'trust_card_1',
          uid: 1,
          last4: '1111',
          card_network: 'AMEX',
          card_art: {
            pictures: {
              original: {
                uri: 'https://card.1.com',
              },
            },
          },
          issuer_bank: 'issuer1',
          allowed_auth_methods: ['CLOUD_TOKEN', 'PAN_ONLY'],
          bin: '123456',
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(
      new SyncUserCardResponse(
        'success',
        200,
        new UserCard(
          'id-1',
          'trust_card_1',
          [AuthMethods.cloudToken, AuthMethods.panOnly],
          'issuer1',
          1,
          CardNetworks.amex,
          '1111',
          new CardArt(new Map().set('original', 'https://card.1.com')),
          '123456',
        ),
      ),
    )
    expect(response.getValue().card.cardArt.original()).toBe('https://card.1.com')
  })
  it('should deserialize error if status is not success', () => {
    const response = SyncUserCardResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'SyncUserCard'))
  })
})
