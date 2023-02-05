import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import { UnbindCardResponse } from '../../../../../../code/network/diehard-backend/entities/bind/unbind-card-response'

export const sample = JSON.parse(`
{
  "status": "success",
  "status_code": "200",
  "status_desc": "success"
}
`)

describe(UnbindCardResponse, () => {
  it('should parse UnbindCardResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = UnbindCardResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new UnbindCardResponse('success', '200', 'success'))
  })

  it('should parse UnbindCardResponse with empty "status_desc", "status_code"', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.status_desc
    delete sampleCopy.status_code
    const item = JSONItemFromJSON(sampleCopy)

    const response = UnbindCardResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new UnbindCardResponse('success', null, null))
  })

  it('should fail to parse UnbindCardResponse', () => {
    const response = UnbindCardResponse.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
