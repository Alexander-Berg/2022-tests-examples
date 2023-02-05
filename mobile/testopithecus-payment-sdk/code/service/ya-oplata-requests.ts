import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkHeadersExtra,
  NetworkMethod,
  RequestEncoding,
} from '../../../common/code/network/network-request'
import { ArrayJSONItem, MapJSONItem } from '../../../common/code/json/json-types'

export class YaOplataCreateOrderRequest extends BaseNetworkRequest {
  public constructor(public readonly acquirerToken: string, public readonly amount: string) {
    super()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public headersExtra(): NetworkHeadersExtra {
    return super
      .headersExtra()
      .putString('Content-Type', 'application/json')
      .putString('Authorization', this.acquirerToken)
  }

  public targetPath(): string {
    return 'v1/order'
  }

  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }

  public params(): MapJSONItem {
    return new MapJSONItem()
      .putString('caption', 'some caption')
      .putString('description', 'some description')
      .put(
        'items',
        new ArrayJSONItem().add(
          new MapJSONItem()
            .putString('name', 'slon')
            .putString('price', this.amount)
            .putString('nds', 'nds_20')
            .putString('currency', 'RUB')
            .putInt32('amount', 1),
        ),
      )
  }
}
