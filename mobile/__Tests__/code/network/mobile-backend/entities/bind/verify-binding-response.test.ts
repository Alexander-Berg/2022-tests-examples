import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { NullJSONItem } from '../../../../../../../common/code/json/json-types'
import { VerifyBindingResponse } from '../../../../../../code/network/mobile-backend/entities/bind/verify-binding-response'

export const sample = JSON.parse(`
{
  "purchase_token": "123"
}
`)

describe(VerifyBindingResponse, () => {
  it('should parse VerifyBindingResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = VerifyBindingResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new VerifyBindingResponse('123'))
  })

  it('should fail to parse VerifyBindingResponse without "purchase_token"', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.purchase_token
    const item = JSONItemFromJSON(sampleCopy)

    const response = VerifyBindingResponse.fromJsonItem(item)
    expect(response.isError()).toBe(true)
  })

  it('should fail to parse NewCardBindingResponse', () => {
    const response = VerifyBindingResponse.fromJsonItem(new NullJSONItem())
    expect(response.isError()).toBe(true)
  })
})
