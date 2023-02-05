import { YSError } from '../../../../../../common/ys'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { EncryptedCardResponse } from '../../../../code/network/diehard/encrypted-card-response'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'

describe(EncryptedCardResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = EncryptedCardResponse.fromJSONItem(
      JSONItemFromJSON({
        id: '12345',
        result: {
          encrypted_card: 'encrypted_card',
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(new EncryptedCardResponse('success', 200, '12345', 'encrypted_card'))
  })
  it('should deserialize error if status is not success', () => {
    const response = EncryptedCardResponse.fromJSONItem(
      JSONItemFromJSON({
        id: '12345',
        error: {
          code: 501,
          message: 'FAIL',
        },
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('FAIL', 501, null, 'EncryptedCard'))
  })
  it('should return error if no map in payload', () => {
    const response = EncryptedCardResponse.fromJSONItem(JSONItemFromJSON([]))
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new YSError('Incorrect format of message: not a map'))
  })
  it('should return error if no result field in payload', () => {
    const response = EncryptedCardResponse.fromJSONItem(
      JSONItemFromJSON({
        id: '12345',
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new YSError('Incorrect format of successful message: no result'))
  })
  it('should return error if no encrypted_card field in result', () => {
    const response = EncryptedCardResponse.fromJSONItem(
      JSONItemFromJSON({
        id: '12345',
        result: {
          hello: 'world',
        },
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new YSError('Incorrect format of successful message: no encrypted_card'))
  })
})
