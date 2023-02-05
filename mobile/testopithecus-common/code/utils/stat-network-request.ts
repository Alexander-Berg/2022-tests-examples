import { ArrayJSONItem, MapJSONItem } from '../../../common/code/json/json-types'
import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
} from '../../../common/code/network/network-request'

export class StatNetworkRequest extends BaseNetworkRequest {
  public constructor(private readonly results: ArrayJSONItem) {
    super()
  }

  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public targetPath(): string {
    return '_api/report/data/Mail/Others/MobAutoTestsStat'
  }

  public params(): NetworkParams {
    return new MapJSONItem().put('data', this.results).putString('scale', 's')
  }
}
