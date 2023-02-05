import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { IsAuthorizedResponse } from '../../../../code/network/yandex-pay-backend/is-authorized-response'

describe(IsAuthorizedResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = IsAuthorizedResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          allowed: true,
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(new IsAuthorizedResponse('success', 200, true))
  })
  it('should deserialize error if status is not success', () => {
    const response = IsAuthorizedResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'IsAuthorized'))
  })
})
