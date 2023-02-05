import { JSONSerializer } from '../../../../common/code/json/json-serializer'
import { ArrayJSONItem, MapJSONItem } from '../../../../common/code/json/json-types'
import { HttpResponse, HttpResponseBuilder } from '../../../../common/code/network/http-layer'

export class YaOplataMockConstants {
  public static readonly token: string = 'payment:gAAAABg='
}

export class YaPaymentCreateOrderResponse {
  public constructor(public price: string) {}

  public toHttpResponse(jsonSerializer: JSONSerializer): HttpResponse {
    return new HttpResponseBuilder()
      .setCode(200)
      .setBodyText(
        jsonSerializer
          .serialize(
            new MapJSONItem()
              .putString('status', 'success')
              .putInt32('code', 200)
              .put(
                'data',
                new MapJSONItem()
                  .putString('pay_token', YaOplataMockConstants.token)
                  .putInt32('order_id', 1234)
                  .put(
                    'items',
                    new ArrayJSONItem().add(
                      new MapJSONItem().putString('price', this.price).putString('currency', 'RUB'),
                    ),
                  ),
              ),
          )
          .getValue(),
      )
      .build()
  }
}
