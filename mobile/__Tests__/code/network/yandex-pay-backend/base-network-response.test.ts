import { JSONItemKind, JSONParsingError } from '../../../../../common/code/json/json-types'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { ErrorCodes } from '../../../../code/models/error-codes'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { BaseNetworkResponse } from '../../../../code/network/yandex-pay-backend/base-network-response'

describe(BaseNetworkResponse, () => {
  it('should deserialize itself from response JSON', () => {
    expect(
      BaseNetworkResponse.fromJSON(
        JSONItemFromJSON({
          status: 'success',
          code: 200,
          data: {},
        }),
        'Sample',
      ).getValue(),
    ).toStrictEqual(new BaseNetworkResponse('success', 200))
  })
  it('should be no error if JSON response is successful', () => {
    expect(
      BaseNetworkResponse.fromJSON(
        JSONItemFromJSON({
          status: 'success',
          code: 200,
          data: {},
        }),
        'Sample',
      ).isError(),
    ).toBe(false)
  })
  it('should be error, with error desc if any, if JSON response is not successful', () => {
    const response = BaseNetworkResponse.fromJSON(
      JSONItemFromJSON({
        status: 'fail',
        code: 400,
        data: {
          message: 'AMOUNT_MISMATCH',
        },
      }),
      'Sample',
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toBeInstanceOf(APIError)
    expect(response.getError() as APIError).toStrictEqual(
      new APIError('fail', 400, ErrorCodes.amountMismatch, 'Sample'),
    )
  })
  it('should be error, with null error if none provided, if JSON response is not successful', () => {
    const response = BaseNetworkResponse.fromJSON(
      JSONItemFromJSON({
        status: 'fail',
        code: 400,
        data: {},
      }),
      'Sample',
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toBeInstanceOf(APIError)
    expect(response.getError() as APIError).toStrictEqual(new APIError('fail', 400, null, 'Sample'))
  })
  it('should return JSON deserialization error if response JSON is malformed', () => {
    const json = JSONItemFromJSON({
      some: 1000,
      other: false,
    })
    expect(BaseNetworkResponse.fromJSON(json, 'Sample').getError()).toStrictEqual(
      JSONParsingError.deserializationFailed(
        json,
        JSONParsingError.mapTryGetFailed(json, 'status', JSONItemKind.string),
      ),
    )
  })
})
