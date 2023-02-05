import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { NullJSONItem } from '../../../../../../../common/code/json/json-types'
import { NewCardBindingResponse } from '../../../../../../code/network/diehard-backend/entities/bind/new-card-binding-response'

export const sample = JSON.parse(`
{
  "uid": "123",
  "binding": {
    "verifications": [],
    "unverified": true,
    "id": "card-123"
  }
}
`)

describe(NewCardBindingResponse, () => {
  it('should parse NewCardBindingResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = NewCardBindingResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new NewCardBindingResponse('card-123'))
  })

  it('should fail to parse NewCardBindingResponse without "binding.id"', () => {
    const bindingCopy = Object.assign({}, sample.binding)
    delete bindingCopy.id
    const sampleCopy = Object.assign({}, sample, { binding: bindingCopy })
    const item = JSONItemFromJSON(sampleCopy)

    const response = NewCardBindingResponse.fromJsonItem(item)
    expect(response.isError()).toBe(true)
  })

  it('should fail to parse NewCardBindingResponse', () => {
    const response = NewCardBindingResponse.fromJsonItem(new NullJSONItem())
    expect(response.isError()).toBe(true)
  })
})
