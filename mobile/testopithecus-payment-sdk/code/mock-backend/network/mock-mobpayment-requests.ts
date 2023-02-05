import { decodeJSONItem, JSONItem } from '../../../../common/code/json/json-types'
import { Result } from '../../../../common/code/result/result'
import { VerifyBindingRequest } from '../../../../payment-sdk/code/network/mobile-backend/entities/bind/verify-binding-request'
import { InitPaymentRequest } from '../../../../payment-sdk/code/network/mobile-backend/entities/init/init-payment-request'
import { InitializationParams } from '../../../../payment-sdk/code/network/mobile-backend/entities/init/initialization-params'

export function decodeInitPaymentRequest(item: JSONItem): Result<InitPaymentRequest> {
  return decodeJSONItem(item, (json) => {
    const map = json.tryCastAsMapJSONItem()
    const appInfo = new InitializationParams(
      map.getString('psuid'),
      map.getString('tsid'),
      map.getString('turboapp_id'),
    )
    return new InitPaymentRequest(
      map.tryGetString('token'),
      map.getString('email'),
      map.tryGetBoolean('credit'),
      appInfo,
    )
  })
}

export function decodeVerifyBindingRequest(item: JSONItem): Result<VerifyBindingRequest> {
  return decodeJSONItem(item, (json) => {
    const map = json.tryCastAsMapJSONItem()
    return new VerifyBindingRequest(map.tryGetString('binding_id'))
  })
}
