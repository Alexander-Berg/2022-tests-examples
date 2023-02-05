import {
  HttpRequest,
  HttpRequestHandler,
  HttpResponse,
  HttpResponseBuilder,
} from '../../../common/code/network/http-layer'
import { Uris } from '../../../common/native-modules/native-modules'
import { MockTrustModel } from './model/mock-trust-model'

export class SbpRequestHandler implements HttpRequestHandler {
  public constructor(private readonly trustModel: MockTrustModel) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    if (request.url.startsWith('/sbp_pay')) {
      const url = Uris.fromString(request.url)
      if (url === null) {
        return new HttpResponseBuilder().setCode(500).build()
      }
      const purchaseToken = url!.getQueryParameter('purchase_token')
      if (purchaseToken === null) {
        return new HttpResponseBuilder().setCode(500).build()
      }
      const result = this.trustModel.confirmSbpPaid(purchaseToken)
      return new HttpResponseBuilder().setCode(result ? 200 : 500).build()
    }
    return new HttpResponseBuilder().setCode(500).build()
  }
}
