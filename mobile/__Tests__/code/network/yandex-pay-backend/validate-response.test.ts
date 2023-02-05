import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { ErrorCodes } from '../../../../code/models/error-codes'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { ValidateResponse } from '../../../../code/network/yandex-pay-backend/validate-response'

describe(ValidateResponse, () => {
  it('should be deserializable from JSON response', () => {
    const value = ValidateResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
      }),
    )
    expect(value.isError()).toBe(false)
    expect(value.getValue()).toStrictEqual(new ValidateResponse('success', 200))
  })
  it('should deserialize error with desc, if any, if status is not success', () => {
    const value = ValidateResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 401,
        data: {
          message: 'CARD_NOT_FOUND',
        },
      }),
    )
    expect(value.isError()).toBe(true)
    expect(value.getError()).toStrictEqual(new APIError('fail', 401, ErrorCodes.cardNotFound, 'Validate'))
  })
  it('should deserialize error without desc, if unknown, if status is not success', () => {
    const value = ValidateResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 401,
        data: {
          message: 'SOME_OTHER_CODE',
        },
      }),
    )
    expect(value.isError()).toBe(true)
    expect(value.getError()).toStrictEqual(new APIError('fail', 401, null, 'Validate'))
  })
})
