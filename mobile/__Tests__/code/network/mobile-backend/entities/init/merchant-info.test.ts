import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import {
  MerchantAddress,
  MerchantInfo,
} from '../../../../../../code/network/mobile-backend/entities/init/merchant-info'

describe(MerchantInfo, () => {
  const sample = JSON.parse(`
  {
    "name": "Test merchant",
    "schedule_text": "с 9 до 6",
    "ogrn": "1234567890123",
    "legal_address": {
      "city": "Москва",
      "country": "RUS",
      "home": "16",
      "street": "Льва Толстого",
      "zip": "119021"
    }
  }
  `)

  it('should parse MerchantInfo', () => {
    const item = JSONItemFromJSON(sample)
    const response = MerchantInfo.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new MerchantInfo(
        'Test merchant',
        'с 9 до 6',
        '1234567890123',
        new MerchantAddress('Москва', 'RUS', '16', 'Льва Толстого', '119021'),
      ),
    )
  })

  it('should parse MerchantInfo with missing values', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.legal_address
    const item = JSONItemFromJSON(sampleCopy)
    const response = MerchantInfo.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new MerchantInfo('Test merchant', 'с 9 до 6', '1234567890123', null))
  })

  it('should fail to parse MerchantInfo', () => {
    const response = MerchantInfo.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})

describe(MerchantAddress, () => {
  const sample = JSON.parse(`
  {
    "city": "Москва",
    "country": "RUS",
    "home": "16",
    "street": "Льва Толстого",
    "zip": "119021"
  }
  `)

  it('should parse MerchantAddress', () => {
    const item = JSONItemFromJSON(sample)
    const response = MerchantAddress.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(new MerchantAddress('Москва', 'RUS', '16', 'Льва Толстого', '119021'))
  })

  it('should fail to parse MerchantAddress', () => {
    const response = MerchantAddress.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
