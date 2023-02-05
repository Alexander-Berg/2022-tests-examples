import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { MobileBackendErrorResponse } from '../../../../../code/network/mobile-backend/entities/mobile-backend-error-response'
import { ArrayJSONItem } from '../../../../../../common/code/json/json-types'

describe(MobileBackendErrorResponse, () => {
  const sample = JSON.parse(`
  {
    "status": "success",
    "code": 100,
    "req_id": "request id",
    "message": "request message"
  }
  `)

  it('should parse MobileBackendErrorResponse', () => {
    const item = JSONItemFromJSON(sample)
    const response = MobileBackendErrorResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new MobileBackendErrorResponse('success', 100, 'request id', 'request message'),
    )
  })

  it('should fail to parse MobileBackendErrorResponse', () => {
    const response = MobileBackendErrorResponse.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
