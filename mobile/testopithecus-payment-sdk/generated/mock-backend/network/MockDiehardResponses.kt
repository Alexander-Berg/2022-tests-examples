// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mock-backend/network/mock-diehard-responses.ts >>>

package com.yandex.xplat.testopithecus.payment.sdk

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.testopithecus.common.*
import com.yandex.xplat.payment.sdk.*

public fun makeBindCardHttpResponse(data: BindNewCardResponse, jsonSerializer: JSONSerializer): HttpResponse {
    return HttpResponseBuilder().setCode(200).setBodyText(jsonSerializer.serialize(MapJSONItem().putString("status", data.status).putString("payment_method", data.paymentMethodId).putStringIfPresent("status_desc", data.statusDescription).putStringIfPresent("status_code", data.statusCode).putString("rrn", "510851").putString("refund_status", "inqueue").putString("trust_payment_id", "60091a2f910d3922de75725b")).getValue()).build()
}

public fun makeUnbindCardHttpResponse(data: UnbindCardResponse, jsonSerializer: JSONSerializer): HttpResponse {
    return HttpResponseBuilder().setCode(200).setBodyText(jsonSerializer.serialize(MapJSONItem().putString("status", data.status).putStringIfPresent("status_desc", data.statusDescription).putStringIfPresent("status_code", data.statusCode)).getValue()).build()
}

public fun makeSupplyPaymentHttpResponse(data: SupplyPaymentResponse, jsonSerializer: JSONSerializer): HttpResponse {
    return HttpResponseBuilder().setCode(200).setBodyText(jsonSerializer.serialize(MapJSONItem().putString("status", data.status).putStringIfPresent("status_desc", data.statusDescription).putStringIfPresent("status_code", data.statusCode)).getValue()).build()
}

public fun makeCheckPaymentHttpResponse(data: CheckPaymentResponse, purchaseToken: String, amount: String, isBinding: Boolean, timestamp: String, jsonSerializer: JSONSerializer): HttpResponse {
    return HttpResponseBuilder().setCode(200).setBodyText(jsonSerializer.serialize(MapJSONItem().putString("rrn", "510851").putString("uid", "1234567890").putString("payment_method", "card").putString("user_phone", "+79999999999").putString("terminal_id", "11112222").putString("currency", "RUB").putBoolean("is_binding_payment", isBinding).putString("paysys_sent_ts", timestamp).putString("fiscal_status", "").putString("cardholder", "Card Holder").putInt32("fiscal_is_eligible", 0).putInt32("balance_service_id", 111).putString("payment_dt", timestamp).putString("transaction_id", "xxx").putString("payment_timeout", "1200").putString("status", data.status).putStringIfPresent("status_desc", data.statusDescription).putStringIfPresent("status_code", data.statusCode).putString("payment_method_full", "card-xtodo").putString("approval_code", "123456").putNull("payment_mode").putString("purchase_token", purchaseToken).putString("payment_id", "deadbeef").putString("payment_result_dt", timestamp).putString("card_id", "todo").putString("masked_pan", "500000****0705").putString("amount", amount).putString("paysys_ready_ts", timestamp).putString("payment_type", "common_payment").putString("start_dt", timestamp).putStringIfPresent("redirect_3ds_url", data.redirectURL).putStringIfPresent("processing_payment_form_url", data.paymentFormUrl).putString("user_email", "email@ya.ru")).getValue()).build()
}

public fun makeBindingV2Response(data: NewCardBindingResponse, jsonSerializer: JSONSerializer): HttpResponse {
    return HttpResponseBuilder().setCode(200).setBodyText(jsonSerializer.serialize(MapJSONItem().put("binding", MapJSONItem().putString("id", data.bindingId))).getValue()).build()
}

public fun makeDiehardHttpError(message: String, jsonSerializer: JSONSerializer): HttpResponse {
    return HttpResponseBuilder().setCode(500).setBodyText(jsonSerializer.serialize(MapJSONItem().putString("status", message).putString("status_desc", "error")).getValue()).build()
}

