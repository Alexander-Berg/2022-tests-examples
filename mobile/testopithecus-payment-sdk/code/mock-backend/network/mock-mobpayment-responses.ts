import { undefinedToNull } from '../../../../../common/ys'
import { JSONSerializer } from '../../../../common/code/json/json-serializer'
import { ArrayJSONItem, MapJSONItem } from '../../../../common/code/json/json-types'
import { HttpResponse, HttpResponseBuilder } from '../../../../common/code/network/http-layer'
import { VerifyBindingResponse } from '../../../../payment-sdk/code/network/mobile-backend/entities/bind/verify-binding-response'
import { InitPaymentResponse } from '../../../../payment-sdk/code/network/mobile-backend/entities/init/init-payment-response'
import { PaymentMethod } from '../../../../payment-sdk/code/network/mobile-backend/entities/methods/payment-method'
import { RawPaymentMethodsResponse } from '../../../../payment-sdk/code/network/mobile-backend/entities/methods/raw-payment-methods-response'

function makeMethodsArray(data: readonly PaymentMethod[]): ArrayJSONItem {
  const res = new ArrayJSONItem()
  data.forEach((value) => {
    const method = new MapJSONItem()
      .putInt32('region_id', 225)
      .putString('payment_method', 'card')
      .putString('binding_ts', '1111111111.111')
      .putString('recommended_verification_type', 'standard2_3ds')
      .putBoolean('expired', false)
      .putString('card_bank', value.bank.toString())
      .put('aliases', new ArrayJSONItem().addString(value.identifier))
      .putString('system', value.system)
      .putString('card_country', 'ROU')
      .putString('payment_system', value.system)
      .putString('card_level', '')
      .putString('holder', 'Card Holder')
      .put('binding_systems', new ArrayJSONItem().addString('trust'))
      .putString('id', value.identifier)
      .putString('card_id', value.identifier)
      .putBoolean('verify_cvv', value.verifyCvv)
      .putInt32('last_paid', 1)
      .putInt32('last_service_paid', 1)
      .putString('account', value.account)
    if (value.familyInfo !== null) {
      const familyInfo = value.familyInfo!
      const payerInfoJson = new MapJSONItem()
        .putString('uid', familyInfo.familyAdminUid)
        .put(
          'family_info',
          new MapJSONItem()
            .putString('family_id', familyInfo.familyId)
            .putInt32('expenses', familyInfo.expenses)
            .putInt32('limit', familyInfo.limit)
            .putString('currency', familyInfo.currency)
            .putString('frame', familyInfo.frame)
            .putBoolean('unlimited', familyInfo.isUnlimited),
        )
      method.put('payer_info', payerInfoJson)
    }
    res.add(method)
  })
  return res
}

function makePaymentMethods(data: RawPaymentMethodsResponse): MapJSONItem {
  const enabledPaymentMethods = new ArrayJSONItem()
  for (const method of data.enabledPaymentMethods) {
    if (method.paymentMethod === 'card') {
      enabledPaymentMethods.add(
        new MapJSONItem()
          .putString('payment_method', 'card')
          .put(
            'payment_systems',
            new ArrayJSONItem()
              .addString('MIR')
              .addString('Maestro')
              .addString('MasterCard')
              .addString('VISA')
              .addString('VISA_ELECTRON'),
          )
          .putString('currency', 'RUB')
          .putInt32('firm_id', 1),
      )
    } else {
      enabledPaymentMethods.add(
        new MapJSONItem()
          .putString('payment_method', method.paymentMethod)
          .putString('currency', 'RUB')
          .putInt32('firm_id', 1),
      )
    }
  }
  return new MapJSONItem()
    .putString('status', data.status)
    .putBoolean('google_pay_supported', data.googlePaySupported)
    .putBoolean('apple_pay_supported', data.applePaySupported)
    .put('payment_methods', makeMethodsArray(data.paymentMethods))
    .put('enabled_payment_methods', enabledPaymentMethods)
}

export function makePaymentMethodsHttpResponse(
  data: RawPaymentMethodsResponse,
  jsonSerializer: JSONSerializer,
): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(jsonSerializer.serialize(makePaymentMethods(data)).getValue())
    .build()
}

function makeMerchantInfo(data: InitPaymentResponse): MapJSONItem {
  const isMerchantInfoExists = data.merchantInfo !== null
  const result = new MapJSONItem()
    .putString('name', isMerchantInfoExists ? data.merchantInfo!.name : '')
    .putString('schedule_text', isMerchantInfoExists ? data.merchantInfo!.scheduleText : '')
    .putString('ogrn', isMerchantInfoExists ? data.merchantInfo!.ogrn : '')

  const isMerchantAddressExists = isMerchantInfoExists && data.merchantInfo!.merchantAddress !== null
  return result.put(
    'legal_address',
    new MapJSONItem()
      .putString('city', isMerchantAddressExists ? data.merchantInfo!.merchantAddress!.city : '')
      .putString('country', isMerchantAddressExists ? data.merchantInfo!.merchantAddress!.country : '')
      .putString('home', isMerchantAddressExists ? data.merchantInfo!.merchantAddress!.home : '')
      .putString('street', isMerchantAddressExists ? data.merchantInfo!.merchantAddress!.street : '')
      .putString('zip', isMerchantAddressExists ? data.merchantInfo!.merchantAddress!.zip : ''),
  )
}

export function makeInitPaymentHttpResponse(data: InitPaymentResponse, jsonSerializer: JSONSerializer): HttpResponse {
  const isYaOplata = data.acquirer !== null
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(
      jsonSerializer
        .serialize(
          makePaymentMethods(data)
            .putStringIfPresent('acquirer', undefinedToNull(data.acquirer?.toString()))
            .putString('token', data.token)
            .putString('license_url', isYaOplata ? 'https://yandex.ru/legal/payer_termsofuse' : '')
            .putString('total', data.total)
            .putString('currency', data.currency)
            .putString('environment', data.environment)
            .putNull('google_pay')
            .put('paymethod_markup', new MapJSONItem())
            .putString('payment_url', `https://trust-test.yandex.ru/web/payment?purchase_token=${data.token}`)
            .putStringIfPresent('credit_form_url', data.creditFormUrl)
            .put('merchant', makeMerchantInfo(data)),
        )
        .getValue(),
    )
    .build()
}

export function makeVerifyBindingHttpResponse(
  data: VerifyBindingResponse,
  jsonSerializer: JSONSerializer,
): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(
      jsonSerializer
        .serialize(new MapJSONItem().putString('status', 'success').putString('purchase_token', data.purchaseToken))
        .getValue(),
    )
    .build()
}

export function makeMobPaymentHttpError(status: string, message: string, jsonSerializer: JSONSerializer): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(500)
    .setBodyText(
      jsonSerializer
        .serialize(
          new MapJSONItem()
            .putString('status', status)
            .putString('message', message)
            .putInt32('code', 1010)
            .putString('req_id', '111-222'),
        )
        .getValue(),
    )
    .build()
}
