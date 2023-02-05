import { Int32, Nullable } from '../../../../../common/ys'
import { decodeJSONItem, JSONItem } from '../../../../common/code/json/json-types'
import { Result } from '../../../../common/code/result/result'
import { BindNewCardRequest } from '../../../../payment-sdk/code/network/diehard-backend/entities/bind/bind-new-card-request'
import { NewCardBindingRequest } from '../../../../payment-sdk/code/network/diehard-backend/entities/bind/new-card-binding-request'
import { UnbindCardRequest } from '../../../../payment-sdk/code/network/diehard-backend/entities/bind/unbind-card-request'
import { CheckPaymentRequest } from '../../../../payment-sdk/code/network/diehard-backend/entities/check-payment/check-payment-request'

export function decodeBindCardRequest(service: Nullable<string>, item: JSONItem): Result<BindNewCardRequest> {
  return decodeJSONItem(item, (json) => {
    const paramsMap = json.tryCastAsMapJSONItem().tryGet('params').tryCastAsMapJSONItem()
    return new BindNewCardRequest(
      paramsMap.getString('token'),
      service,
      paramsMap.tryGetString('card_number'),
      paramsMap.tryGetString('expiration_month'),
      paramsMap.tryGetString('expiration_year'),
      paramsMap.tryGetString('cvn'),
      paramsMap.tryGetInt32('region_id'),
    )
  })
}

export function decodeUnbindCardRequest(item: JSONItem): Result<UnbindCardRequest> {
  return decodeJSONItem(item, (json) => {
    const paramsMap = json.tryCastAsMapJSONItem().tryGet('params').tryCastAsMapJSONItem()
    return new UnbindCardRequest(paramsMap.getString('token'), paramsMap.tryGetString('card'))
  })
}

export class MockSupplyPaymentRequest {
  public constructor(
    public readonly token: Nullable<string>,
    public readonly purchaseToken: string,
    public readonly paymentMethod: string,
    public readonly cardNumber: Nullable<string>,
    public readonly expirationMonth: Nullable<string>,
    public readonly expirationYear: Nullable<string>,
    public readonly cvn: Nullable<string>,
    public readonly bindCard: Nullable<Int32>,
  ) {}

  public static decodeJson(item: JSONItem): Result<MockSupplyPaymentRequest> {
    return decodeJSONItem(item, (json) => {
      const paramsMap = json.tryCastAsMapJSONItem().tryGet('params').tryCastAsMapJSONItem()
      return new MockSupplyPaymentRequest(
        paramsMap.getString('token'),
        paramsMap.tryGetString('purchase_token'),
        paramsMap.tryGetString('payment_method'),
        paramsMap.getString('card_number'),
        paramsMap.getString('expiration_month'),
        paramsMap.getString('expiration_year'),
        paramsMap.getString('cvn'),
        paramsMap.getInt32('bind_card'),
      )
    })
  }
}

export function decodeCheckPaymentRequest(item: JSONItem): Result<CheckPaymentRequest> {
  return decodeJSONItem(item, (json) => {
    const paramsMap = json.tryCastAsMapJSONItem().tryGet('params').tryCastAsMapJSONItem()
    return new CheckPaymentRequest(paramsMap.tryGetString('purchase_token'))
  })
}

export function decodeNewCardBindReuest(headerOauthToken: string, item: JSONItem): Result<NewCardBindingRequest> {
  return decodeJSONItem(item, (json) => {
    const map = json.tryCastAsMapJSONItem()
    return new NewCardBindingRequest(
      headerOauthToken,
      map.tryGetString('service_token'),
      map.tryGetString('hash_algo'),
      map.tryGetString('card_data_encrypted'),
      map.tryGetInt32('region_id'),
    )
  })
}
