import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import { CheckPaymentResponse } from '../../../../../../code/network/diehard-backend/entities/check-payment/check-payment-response'

export const sample = JSON.parse(`
{
  "status": "success",
  "status_code": "200",
  "status_desc": "success",
  "redirect_3ds_url": "url",
  "processing_payment_form_url": "https://qr.nspk.ru/"
}
`)

describe(CheckPaymentResponse, () => {
  it('should parse CheckPaymentResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = CheckPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new CheckPaymentResponse('success', '200', 'success', 'url', 'https://qr.nspk.ru/', null),
    )
  })

  it('should parse CheckPaymentResponse with empty optional values', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.status_desc
    delete sampleCopy.status_code
    delete sampleCopy.redirect_3ds_url
    delete sampleCopy.processing_payment_form_url
    const item = JSONItemFromJSON(sampleCopy)

    const response = CheckPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new CheckPaymentResponse('success', null, null, null, null, null))
  })

  it('should fail to parse CheckPaymentResponse', () => {
    const response = CheckPaymentResponse.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
