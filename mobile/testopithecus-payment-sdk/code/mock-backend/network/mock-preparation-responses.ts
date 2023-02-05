import { JSONSerializer } from '../../../../common/code/json/json-serializer'
import { MapJSONItem } from '../../../../common/code/json/json-types'
import { HttpResponse, HttpResponseBuilder } from '../../../../common/code/network/http-layer'

export class TrustPaymentsOrdersResponse {
  public constructor(public readonly orderId: string) {}

  public toHttpResponse(jsonSerializer: JSONSerializer): HttpResponse {
    return new HttpResponseBuilder()
      .setCode(200)
      .setBodyText(
        jsonSerializer
          .serialize(
            new MapJSONItem()
              .putString('status', 'success')
              .putString('status_code', 'created')
              .putString('order_id', this.orderId)
              .putString('product_id', 'some_id')
              .putString('order_status', 'order_created'),
          )
          .getValue(),
      )
      .build()
  }
}

export class TrustPaymentsPaymentsResponse {
  public constructor(public readonly token: string) {}

  public toHttpResponse(jsonSerializer: JSONSerializer): HttpResponse {
    return new HttpResponseBuilder()
      .setCode(200)
      .setBodyText(
        jsonSerializer
          .serialize(
            new MapJSONItem()
              .putString('status', 'success')
              .putString('status_code', 'payment_created')
              .putString('purchase_token', this.token),
          )
          .getValue(),
      )
      .build()
  }
}

export class TusGetAccountResponse {
  public toHttpResponse(jsonSerializer: JSONSerializer): HttpResponse {
    return new HttpResponseBuilder()
      .setCode(200)
      .setBodyText(
        jsonSerializer
          .serialize(
            new MapJSONItem()
              .putString('status', 'ok')
              .putString('passport_environment', 'testing')
              .put(
                'account',
                new MapJSONItem()
                  .putString('delete_at', 'None')
                  .putString('locked_until', '2023-01-26 10:26:58.706900')
                  .putString('login', 'local-test-yandex-team')
                  .putString('password', 'password')
                  .putString('uid', '1234567890'),
              ),
          )
          .getValue(),
      )
      .build()
  }
}

export class TusGetTokenResponse {
  public toHttpResponse(jsonSerializer: JSONSerializer): HttpResponse {
    return new HttpResponseBuilder()
      .setCode(200)
      .setBodyText(
        jsonSerializer
          .serialize(
            new MapJSONItem()
              .putString('access_token', 'AAAADEADBEEFDEADBEEF')
              .putString('expires_in', '99999999')
              .putString('token_type', 'bearer')
              .putString('uid', '1234567890'),
          )
          .getValue(),
      )
      .build()
  }
}

export function makeErrorHttpResponse(message: string, jsonSerializer: JSONSerializer): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(500)
    .setBodyText(
      jsonSerializer
        .serialize(new MapJSONItem().putString('status', 'error').putString('status_desc', message))
        .getValue(),
    )
    .build()
}

export function makeSuccessHttpResponse(jsonSerializer: JSONSerializer): HttpResponse {
  return new HttpResponseBuilder()
    .setCode(200)
    .setBodyText(jsonSerializer.serialize(new MapJSONItem().putString('status', 'success')).getValue())
    .build()
}
