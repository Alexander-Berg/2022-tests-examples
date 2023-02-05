import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { IsReadyToPayResponse } from '../../../../code/network/yandex-pay-backend/is-ready-to-pay-response'

describe(IsReadyToPayResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = IsReadyToPayResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          is_ready_to_pay: true,
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(new IsReadyToPayResponse('success', 200, true))
  })
  it('should deserialize error if status is not success', () => {
    const response = IsReadyToPayResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'IsReadyToPay'))
  })
})
