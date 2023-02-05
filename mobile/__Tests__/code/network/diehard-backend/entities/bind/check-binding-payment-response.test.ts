import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { NullJSONItem } from '../../../../../../../common/code/json/json-types'
import { CheckBindingPaymentResponse } from '../../../../../../code/network/diehard-backend/entities/bind/check-binding-payment-response'

export const sample = JSON.parse(`
{
  "status": "success",
  "status_desc": "paid ok",
  "rrn": "123",
  "redirect_3ds_url": "url",
  "payment_method_full": "card-123"
}
`)

describe(CheckBindingPaymentResponse, () => {
  it('should parse CheckBindingPaymentResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = CheckBindingPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new CheckBindingPaymentResponse('success', null, 'paid ok', 'card-123', '123', 'url', null),
    )
  })

  it('should parse CheckBindingPaymentResponse without optional values', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.status_desc
    delete sampleCopy.rrn
    delete sampleCopy.redirect_3ds_url
    const item = JSONItemFromJSON(sampleCopy)

    const response = CheckBindingPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new CheckBindingPaymentResponse('success', null, null, 'card-123', null, null, null),
    )
  })

  it('should fail to parse CheckBindingPaymentResponse without "payment_method_full"', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.payment_method_full
    const item = JSONItemFromJSON(sampleCopy)

    const response = CheckBindingPaymentResponse.fromJsonItem(item)
    expect(response.isError()).toBe(true)
  })

  it('should fail to parse CheckBindingPaymentResponse', () => {
    const response = CheckBindingPaymentResponse.fromJsonItem(new NullJSONItem())
    expect(response.isError()).toBe(true)
  })
})
