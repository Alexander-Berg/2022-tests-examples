import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { EncryptedAppIdResponse } from '../../../../code/network/yandex-pay-backend/encrypted-app-id-response'

describe(EncryptedAppIdResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = EncryptedAppIdResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          encrypted_app_id: 'enc_app_id',
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(new EncryptedAppIdResponse('success', 200, 'enc_app_id'))
  })
  it('should deserialize error if status is not success', () => {
    const response = EncryptedAppIdResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'EncryptedAppID'))
  })
})
