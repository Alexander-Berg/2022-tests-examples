import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { AuthMethods } from '../../../../code/models/auth-methods'
import { CardArt } from '../../../../code/models/card-art'
import { CardNetworks } from '../../../../code/models/card-networks'
import { UserCard } from '../../../../code/models/user-card'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { UserCardsResponse } from '../../../../code/network/yandex-pay-backend/user-cards-response'

describe(UserCardsResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = UserCardsResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          cards: [
            {
              id: 'id-1',
              trust_card_id: 'trust_id_1',
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
            {
              id: 'id-2',
              trust_card_id: 'trust_id_2',
              uid: 2,
              last4: '2222',
              card_network: 'DISCOVER',
              card_art: {},
              issuer_bank: 'issuer2',
              allowed_auth_methods: ['CLOUD_TOKEN'],
              bin: '654321',
            },
            {
              id: 'id-3',
              uid: 3,
              last4: '3333',
              card_network: 'MASTERCARD',
              card_art: {
                pictures: {
                  original: {
                    uri: 'https://original.com',
                  },
                  extra: {
                    uri: 'https://extra.com',
                  },
                },
              },
              issuer_bank: 'issuer3',
              allowed_auth_methods: [],
              bin: '333444',
            },
          ],
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(
      new UserCardsResponse('success', 200, [
        new UserCard(
          'id-1',
          'trust_id_1',
          [AuthMethods.cloudToken, AuthMethods.panOnly],
          'issuer1',
          1,
          CardNetworks.amex,
          '1111',
          new CardArt(new Map().set('original', 'https://card.1.com')),
          '123456',
        ),
        new UserCard(
          'id-2',
          'trust_id_2',
          [AuthMethods.cloudToken],
          'issuer2',
          2,
          CardNetworks.discover,
          '2222',
          new CardArt(new Map()),
          '654321',
        ),
        new UserCard(
          'id-3',
          null,
          [],
          'issuer3',
          3,
          CardNetworks.masterCard,
          '3333',
          new CardArt(
            new Map([
              ['original', 'https://original.com'],
              ['extra', 'https://extra.com'],
            ]),
          ),
          '333444',
        ),
      ]),
    )
    expect(response.getValue().cards[0].cardArt.original()).toBe('https://card.1.com')
  })
  it('should deserialize error if status is not success', () => {
    const response = UserCardsResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'UserCards'))
  })
})
