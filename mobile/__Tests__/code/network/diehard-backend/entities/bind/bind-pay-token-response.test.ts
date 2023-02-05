import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import { BindPayTokenResponse } from '../../../../../../code/network/diehard-backend/entities/bind/bind-pay-token-response'

export const sample = JSON.parse(`
{
  "status": "success",
  "status_code": "200",
  "status_desc": "success",
  "payment_method": "pay-orderTag",
  "trust_payment_id": "485793487943487"
}
`)

describe(BindPayTokenResponse, () => {
  it('should parse BindPayTokenResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = BindPayTokenResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new BindPayTokenResponse('success', '200', 'success', 'pay-orderTag', '485793487943487'),
    )
  })

  it('should parse BindPayTokenResponse with empty "status_desc", "status_code"', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.status_desc
    delete sampleCopy.status_code
    const item = JSONItemFromJSON(sampleCopy)

    const response = BindPayTokenResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new BindPayTokenResponse('success', null, null, 'pay-orderTag', '485793487943487'),
    )
  })

  it('should fail to parse BindPayTokenResponse without "payment_method"', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.payment_method
    const item = JSONItemFromJSON(sampleCopy)

    const response = BindPayTokenResponse.fromJsonItem(item)
    expect(response.isError()).toBe(true)
  })

  it('should fail to parse BindPayTokenResponse', () => {
    const response = BindPayTokenResponse.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
