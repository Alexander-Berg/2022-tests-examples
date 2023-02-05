import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import { SupplyPaymentResponse } from '../../../../../../code/network/diehard-backend/entities/supply/supply-payment-response'

export const sample = JSON.parse(`
{
  "status": "success",
  "status_code": "200",
  "status_desc": "success"
}
`)

describe(SupplyPaymentResponse, () => {
  it('should parse SupplyPaymentResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = SupplyPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new SupplyPaymentResponse('success', '200', 'success'))
  })

  it('should parse SupplyPaymentResponse with empty "status_desc", "status_code"', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.status_desc
    delete sampleCopy.status_code
    const item = JSONItemFromJSON(sampleCopy)

    const response = SupplyPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new SupplyPaymentResponse('success', null, null))
  })

  it('should fail to parse SupplyPaymentResponse', () => {
    const response = SupplyPaymentResponse.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
