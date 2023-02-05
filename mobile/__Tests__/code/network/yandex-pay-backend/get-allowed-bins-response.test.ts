import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { GetAllowedBinsResponse } from '../../../../code/network/yandex-pay-backend/get-allowed-bins-response'

describe(GetAllowedBinsResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = GetAllowedBinsResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          bins: ['123', '456'],
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(new GetAllowedBinsResponse('success', 200, ['123', '456']))
  })
  it('should deserialize error if status is not success', () => {
    const response = GetAllowedBinsResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'GetAllowedBins'))
  })
  it('should skip incorrect bins', () => {
    const response = GetAllowedBinsResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          bins: [123, false, '456'],
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(new GetAllowedBinsResponse('success', 200, ['456']))
  })
})
