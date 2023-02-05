import { JSONSerializer } from '../../../../common/code/json/json-serializer'
import { MapJSONItem } from '../../../../common/code/json/json-types'
import { HttpResponse, HttpResponseBuilder } from '../../../../common/code/network/http-layer'
import { BindNewCardResponse } from '../../../../payment-sdk/code/network/diehard-backend/entities/bind/bind-new-card-response'
import { NewCardBindingResponse } from '../../../../payment-sdk/code/network/diehard-backend/entities/bind/new-card-binding-response'
import { UnbindCardResponse } from '../../../../payment-sdk/code/network/diehard-backend/entities/bind/unbind-card-response'
import { CheckPaymentResponse } from '../../../../payment-sdk/code/network/diehard-backend/entities/check-payment/check-payment-response'
import { SupplyPaymentResponse } from '../../../../payment-sdk/code/network/diehard-backend/entities/supply/supply-payment-response'

export function makeBindCardHttpResponse(data: BindNewCardResponse, jsonSerializer: JSONSerializer): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(
      jsonSerializer
        .serialize(
          new MapJSONItem()
            .putString('status', data.status)
            .putString('payment_method', data.paymentMethodId)
            .putStringIfPresent('status_desc', data.statusDescription)
            .putStringIfPresent('status_code', data.statusCode)
            .putString('rrn', '510851')
            .putString('refund_status', 'inqueue')
            .putString('trust_payment_id', '60091a2f910d3922de75725b'),
        )
        .getValue(),
    )
    .build()
}

export function makeUnbindCardHttpResponse(data: UnbindCardResponse, jsonSerializer: JSONSerializer): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(
      jsonSerializer
        .serialize(
          new MapJSONItem()
            .putString('status', data.status)
            .putStringIfPresent('status_desc', data.statusDescription)
            .putStringIfPresent('status_code', data.statusCode),
        )
        .getValue(),
    )
    .build()
}

export function makeSupplyPaymentHttpResponse(
  data: SupplyPaymentResponse,
  jsonSerializer: JSONSerializer,
): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(
      jsonSerializer
        .serialize(
          new MapJSONItem()
            .putString('status', data.status)
            .putStringIfPresent('status_desc', data.statusDescription)
            .putStringIfPresent('status_code', data.statusCode),
        )
        .getValue(),
    )
    .build()
}

export function makeCheckPaymentHttpResponse(
  data: CheckPaymentResponse,
  purchaseToken: string,
  amount: string,
  isBinding: boolean,
  timestamp: string,
  jsonSerializer: JSONSerializer,
): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(
      jsonSerializer
        .serialize(
          new MapJSONItem()
            .putString('rrn', '510851')
            .putString('uid', '1234567890')
            .putString('payment_method', 'card')
            .putString('user_phone', '+79999999999')
            .putString('terminal_id', '11112222')
            .putString('currency', 'RUB')
            .putBoolean('is_binding_payment', isBinding)
            .putString('paysys_sent_ts', timestamp)
            .putString('fiscal_status', '')
            .putString('cardholder', 'Card Holder')
            .putInt32('fiscal_is_eligible', 0)
            .putInt32('balance_service_id', 111)
            .putString('payment_dt', timestamp)
            .putString('transaction_id', 'xxx')
            .putString('payment_timeout', '1200')
            .putString('status', data.status)
            .putStringIfPresent('status_desc', data.statusDescription)
            .putStringIfPresent('status_code', data.statusCode)
            .putString('payment_method_full', 'card-xtodo')
            .putString('approval_code', '123456')
            .putNull('payment_mode')
            .putString('purchase_token', purchaseToken)
            .putString('payment_id', 'deadbeef')
            .putString('payment_result_dt', timestamp)
            .putString('card_id', 'todo')
            .putString('masked_pan', '500000****0705')
            .putString('amount', amount)
            .putString('paysys_ready_ts', timestamp)
            .putString('payment_type', 'common_payment')
            .putString('start_dt', timestamp)
            .putStringIfPresent('redirect_3ds_url', data.redirectURL)
            .putStringIfPresent('processing_payment_form_url', data.paymentFormUrl)
            .putString('user_email', 'email@ya.ru'),
        )
        .getValue(),
    )
    .build()
}

export function makeBindingV2Response(data: NewCardBindingResponse, jsonSerializer: JSONSerializer): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(
      jsonSerializer
        .serialize(new MapJSONItem().put('binding', new MapJSONItem().putString('id', data.bindingId)))
        .getValue(),
    )
    .build()
}

export function makeDiehardHttpError(message: string, jsonSerializer: JSONSerializer): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(500)
    .setBodyText(
      jsonSerializer
        .serialize(new MapJSONItem().putString('status', message).putString('status_desc', 'error'))
        .getValue(),
    )
    .build()
}
