import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { UserProfileResponse } from '../../../../code/network/yandex-pay-backend/user-profile-response'

describe(UserProfileResponse, () => {
  it('should be deserializable from JSON response', () => {
    const value = UserProfileResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          name: 'NAME',
          uid: 'UID',
          avatar: {
            lodpiUrl: 'LODPIURL',
            hidpiUrl: 'HIDPIURL',
          },
        },
      }),
    )
    expect(value.isError()).toBe(false)
    expect(value.getValue()).toStrictEqual(
      new UserProfileResponse('success', 200, 'NAME', 'UID', 'HIDPIURL', 'LODPIURL'),
    )
  })
  it('should have nulls in profile picture urls if no avatar field in JSON response', () => {
    const value = UserProfileResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          name: 'NAME',
          uid: 'UID',
        },
      }),
    )
    expect(value.isError()).toBe(false)
    expect(value.getValue()).toStrictEqual(new UserProfileResponse('success', 200, 'NAME', 'UID', null, null))
  })
  it('should deserialize error with desc, if any, if status is not success', () => {
    const value = UserProfileResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 401,
        data: {
          message: 'NOT_AUTHORIZED',
        },
      }),
    )
    expect(value.isError()).toBe(true)
    expect(value.getError()).toStrictEqual(new APIError('fail', 401, null, 'UserProfile'))
  })
})
