import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { GetInstallRewardResponse } from '../../../../code/network/yandex-pay-backend/get-install-reward-response'

describe(GetInstallRewardResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = GetInstallRewardResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          reward: {
            amount: '200.0',
          },
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(new GetInstallRewardResponse('success', 200, '200.0'))
  })
  it('should deserialize error if status is not success', () => {
    const response = GetInstallRewardResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'GetInstallReward'))
  })
})
