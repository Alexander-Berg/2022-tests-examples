import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { SetDefaultCardResponse } from '../../../../code/network/yandex-pay-backend/set-default-card-response'

describe(SetDefaultCardResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = SetDefaultCardResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {},
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(new SetDefaultCardResponse('success', 200))
  })
  it('should deserialize error if status is not success', () => {
    const response = SetDefaultCardResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'SetDefaultCard'))
  })
})
