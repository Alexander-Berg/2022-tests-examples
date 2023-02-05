import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import { BindNewCardResponse } from '../../../../../../code/network/diehard-backend/entities/bind/bind-new-card-response'

export const sample = JSON.parse(`
{
  "status": "success",
  "status_code": "200",
  "status_desc": "success",
  "payment_method": "card-1234"
}
`)

describe(BindNewCardResponse, () => {
  it('should parse BindNewCardResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = BindNewCardResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new BindNewCardResponse('success', '200', 'success', 'card-1234'))
  })

  it('should parse BindNewCardResponse with empty "status_desc", "status_code"', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.status_desc
    delete sampleCopy.status_code
    const item = JSONItemFromJSON(sampleCopy)

    const response = BindNewCardResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new BindNewCardResponse('success', null, null, 'card-1234'))
  })

  it('should fail to parse BindNewCardResponse without "payment_method"', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.payment_method
    const item = JSONItemFromJSON(sampleCopy)

    const response = BindNewCardResponse.fromJsonItem(item)
    expect(response.isError()).toBe(true)
  })

  it('should fail to parse BindNewCardResponse', () => {
    const response = BindNewCardResponse.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
