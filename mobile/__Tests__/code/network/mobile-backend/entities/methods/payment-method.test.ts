import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import { BankName } from '../../../../../../code/busilogics/bank-name'
import {
  FamilyInfo,
  FamilyInfoFrame,
  PartnerInfo,
  PaymentMethod,
  stringToFamilyInfoFrame,
} from '../../../../../../code/network/mobile-backend/entities/methods/payment-method'

describe(PaymentMethod, () => {
  const sample = JSON.parse(`
  {
    "region_id": 225,
    "payment_method": "card",
    "binding_ts": "1586966781.400",
    "recommended_verification_type": "standard2_3ds",
    "aliases": [
      "card-x2f3d89b4adc4591861937dc5"
    ],
    "expired": false,
    "card_bank": "RBS BANK (ROMANIA), S.A.",
    "system": "MasterCard",
    "id": "card-x2f3d89b4adc4591861937dc5",
    "card_country": "ROU",
    "payment_system": "MasterCard",
    "card_level": "",
    "holder": "Card Holder",
    "binding_systems": [
      "trust"
    ],
    "account": "510000****8601",
    "card_id": "card-x2f3d89b4adc4591861937dc5",
    "verify_cvv": true
  }
  `)

  const sample_with_family_info = JSON.parse(`
    {
      "region_id": 225,
      "payment_method": "card",
      "binding_ts": "1592304371.950",
      "recommended_verification_type": "standard2_3ds",
      "aliases": [
        "card-xb688fd585b3e1ee603a78797"
      ],
      "expired": false,
      "card_bank": "RBS BANK (ROMANIA), S.A.",
      "system": "MasterCard",
      "id": "card-xb688fd585b3e1ee603a78797",
      "card_country": "ROU",
      "payment_system": "MasterCard",
      "card_level": "",
      "holder": "CARD HOLDER",
      "binding_systems": [
        "trust"
      ],
      "account": "510000****9341",
      "card_id": "card-xb688fd585b3e1ee603a78797",
      "verify_cvv": true,
      "payer_info": {
        "uid": "4051330158",
        "family_info": {
          "family_id": "f1000",
          "expenses": 100,
          "limit": 1000,
          "currency": "RUB",
          "frame": "month",
          "unlimited": false
        }
      }
    }
  `)

  const sample_yandex_bank = JSON.parse(`
    {
    "region_id": 225,
    "payment_method": "card",
    "binding_ts": "1586966781.400",
    "recommended_verification_type": "",
    "aliases": [
      "card-xe49e305c2c0fb1aba1f81511"
    ],
    "expired": false,
    "card_bank": "UnknownBank",
    "system": "YandexBank",
    "id": "card-xe49e305c2c0fb1aba1f81511",
    "card_country": "RUS",
    "payment_system": "YandexBank",
    "card_level": "",
    "holder": "Card Holder",
    "binding_systems": [
      "trust"
    ],
    "account": "555400****1406",
    "card_id": "card-x2f3d89b4adc4591861937dc5",
    "verify_cvv": true,
    "payer_info": null,
    "partner_info": {
      "is_yabank_card": true,
      "is_yabank_card_owner": true
    }
  }
  `)

  it('should parse PaymentMethod', () => {
    const item = JSONItemFromJSON(sample)
    const response = PaymentMethod.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new PaymentMethod(
        'card-x2f3d89b4adc4591861937dc5',
        '510000****8601',
        'MasterCard',
        true,
        BankName.UnknownBank,
        null,
        null,
      ),
    )
  })

  it('should parse Family Info', () => {
    const item = JSONItemFromJSON(sample_with_family_info)
    const response = PaymentMethod.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new PaymentMethod(
        'card-xb688fd585b3e1ee603a78797',
        '510000****9341',
        'MasterCard',
        true,
        BankName.UnknownBank,
        new FamilyInfo('4051330158', 'f1000', 100, 1000, 'RUB', 'month', false),
        null,
      ),
    )
  })

  it('should parse Partner Info', () => {
    const item = JSONItemFromJSON(sample_yandex_bank)
    const response = PaymentMethod.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new PaymentMethod(
        'card-xe49e305c2c0fb1aba1f81511',
        '555400****1406',
        'YandexBank',
        true,
        BankName.UnknownBank,
        null,
        new PartnerInfo(true, true),
      ),
    )
  })

  it('FamilyInfo should calculate available balance', () => {
    const item = JSONItemFromJSON(sample_with_family_info)
    const familyInfo = PaymentMethod.fromJsonItem(item).getValue().familyInfo
    expect(familyInfo?.available()).toEqual(9)
  })

  it('should fail to parse PaymentMethod', () => {
    const response = PaymentMethod.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })

  it('should succeed to parse PaymentMethod if FamilyInfo is malformed', () => {
    const copy = Object.assign({}, sample_with_family_info)
    delete copy.payer_info.family_info.limit
    const item = JSONItemFromJSON(copy)
    const method = PaymentMethod.fromJsonItem(item).getValue()
    expect(method).not.toBeNull()
    expect(method.familyInfo).toBeNull()
  })

  it('should convert FamilyInfoFrame', () => {
    expect(stringToFamilyInfoFrame('day')).toBe(FamilyInfoFrame.day)
    expect(stringToFamilyInfoFrame('WEEK')).toBe(FamilyInfoFrame.week)
    expect(stringToFamilyInfoFrame('MoNtH')).toBe(FamilyInfoFrame.month)
    expect(stringToFamilyInfoFrame('undefined')).toBeNull()
  })
})
