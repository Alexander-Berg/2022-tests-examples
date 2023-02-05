import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import { BankName } from '../../../../../../code/busilogics/bank-name'
import { Acquirer } from '../../../../../../code/network/mobile-backend/entities/init/acquirer'
import { InitPaymentResponse } from '../../../../../../code/network/mobile-backend/entities/init/init-payment-response'
import {
  MerchantAddress,
  MerchantInfo,
} from '../../../../../../code/network/mobile-backend/entities/init/merchant-info'
import { PaymethodMarkup } from '../../../../../../code/network/mobile-backend/entities/init/paymethod-markup'
import {
  EnabledPaymentMethod,
  PaymentMethod,
} from '../../../../../../code/network/mobile-backend/entities/methods/payment-method'
import { isPaymentMethodEnabled } from '../../../../../../code/network/mobile-backend/entities/methods/raw-payment-methods-response'

export const sample = JSON.parse(`
{
  "status": "success",
  "token": "9f6dd5a6623c140dd7fea29b9b9a69da",
  "license_url": "https://yandex.ru/",
  "acquirer": "tinkoff",
  "total": "100.00",
  "environment": "sandbox",
  "currency": "RUB",
  "google_pay_supported": true,
  "apple_pay_supported": true,
  "credit_form_url": "https://yandex.ru/",
  "payment_methods": [
    {
      "region_id": 225,
      "payment_method": "card",
      "binding_ts": "1587049188.042",
      "recommended_verification_type": "standard2_3ds",
      "aliases": [
        "card-xb287c97f2dd7b50529756a44"
      ],
      "expired": false,
      "card_bank": "RBS BANK (ROMANIA), S.A.",
      "system": "MasterCard",
      "id": "card-xb287c97f2dd7b50529756a44",
      "card_country": "ROU",
      "payment_system": "MasterCard",
      "card_level": "",
      "holder": "Card Holder",
      "binding_systems": [
        "trust"
      ],
      "account": "510000****3385",
      "card_id": "card-xb287c97f2dd7b50529756a44",
      "verify_cvv": true
    },
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
  ],
  "enabled_payment_methods": [
    {
      "payment_method": "card",
      "payment_systems": [
        "ApplePay",
        "GooglePay",
        "MIR",
        "Maestro",
        "MasterCard",
        "VISA",
        "VISA_ELECTRON"
      ],
      "currency": "RUB",
      "firm_id": 1
    },
    {
      "payment_method": "sbp_link",
      "payment_systems": null,
      "currency": "RUB",
      "firm_id": 1
    },
    {
      "payment_method": "sbp_qr",
      "payment_systems": null,
      "currency": "RUB",
      "firm_id": 1
    }
  ],
  "merchant": {
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
  },
  "paymethod_markup": {
    "card":"5.1",
    "yandex_account":"5"
  }
}
`)

describe(InitPaymentResponse, () => {
  it('should parse InitPaymentResponse', () => {
    const item = JSONItemFromJSON(sample)

    const response = InitPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new InitPaymentResponse(
        'success',
        '9f6dd5a6623c140dd7fea29b9b9a69da',
        'https://yandex.ru/',
        Acquirer.tinkoff,
        'sandbox',
        '100.00',
        'RUB',
        new MerchantInfo(
          'Test merchant',
          'с 9 до 6',
          '1234567890123',
          new MerchantAddress('Москва', 'RUS', '16', 'Льва Толстого', '119021'),
        ),
        new PaymethodMarkup('5.1'),
        'https://yandex.ru/',
        true,
        true,
        [
          new PaymentMethod(
            'card-xb287c97f2dd7b50529756a44',
            '510000****3385',
            'MasterCard',
            true,
            BankName.UnknownBank,
            null,
            null,
          ),
          new PaymentMethod(
            'card-x2f3d89b4adc4591861937dc5',
            '510000****8601',
            'MasterCard',
            true,
            BankName.UnknownBank,
            null,
            null,
          ),
        ],
        [new EnabledPaymentMethod('card'), new EnabledPaymentMethod('sbp_link'), new EnabledPaymentMethod('sbp_qr')],
      ),
    )
  })

  it('should parse InitPaymentResponse with missing values', () => {
    const sampleCopy = Object.assign({}, sample)
    delete sampleCopy.acquirer
    delete sampleCopy.license_url
    delete sampleCopy.merchant
    delete sampleCopy.paymethod_markup
    delete sampleCopy.credit_form_url
    const item = JSONItemFromJSON(sampleCopy)

    const response = InitPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new InitPaymentResponse(
        'success',
        '9f6dd5a6623c140dd7fea29b9b9a69da',
        null,
        null,
        'sandbox',
        '100.00',
        'RUB',
        null,
        null,
        null,
        true,
        true,
        [
          new PaymentMethod(
            'card-xb287c97f2dd7b50529756a44',
            '510000****3385',
            'MasterCard',
            true,
            BankName.UnknownBank,
            null,
            null,
          ),
          new PaymentMethod(
            'card-x2f3d89b4adc4591861937dc5',
            '510000****8601',
            'MasterCard',
            true,
            BankName.UnknownBank,
            null,
            null,
          ),
        ],
        [new EnabledPaymentMethod('card'), new EnabledPaymentMethod('sbp_link'), new EnabledPaymentMethod('sbp_qr')],
      ),
    )
  })

  it('should parse InitPaymentResponse with malformed values', () => {
    const sampleCopy = Object.assign({}, sample, {
      acquirer: '',
      payment_methods: {},
      enabled_payment_methods: {},
      paymethod_markup: {},
    })
    const item = JSONItemFromJSON(sampleCopy)

    const response = InitPaymentResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new InitPaymentResponse(
        'success',
        '9f6dd5a6623c140dd7fea29b9b9a69da',
        'https://yandex.ru/',
        null,
        'sandbox',
        '100.00',
        'RUB',
        new MerchantInfo(
          'Test merchant',
          'с 9 до 6',
          '1234567890123',
          new MerchantAddress('Москва', 'RUS', '16', 'Льва Толстого', '119021'),
        ),
        new PaymethodMarkup(null),
        'https://yandex.ru/',
        true,
        true,
        [],
        [],
      ),
    )
  })

  it('should fail to parse InitPaymentResponse', () => {
    const response = InitPaymentResponse.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })

  it('should parse null Acquirer', () => {
    const acquirer = InitPaymentResponse.getAcquirerFromString(null)
    expect(acquirer).toStrictEqual(null)
  })

  it('should parse kassa Acquirer', () => {
    const acquirer = InitPaymentResponse.getAcquirerFromString('kassa')
    expect(acquirer).toStrictEqual(Acquirer.kassa)
  })

  it('should parse tinkoff Acquirer', () => {
    const acquirer = InitPaymentResponse.getAcquirerFromString('tinkoff')
    expect(acquirer).toStrictEqual(Acquirer.tinkoff)
  })
})

describe(isPaymentMethodEnabled, () => {
  it('should check enabled payment methods', () => {
    const item = JSONItemFromJSON(sample)
    const response = InitPaymentResponse.fromJsonItem(item).getValue()

    expect(isPaymentMethodEnabled(response, 'sbp_qr')).toBe(true)
    expect(isPaymentMethodEnabled(response, 'unknown')).toBe(false)
  })
})
