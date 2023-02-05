import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { makeErrorHttpResponse, TusGetAccountResponse, TusGetTokenResponse } from './network/mock-preparation-responses'

export class TusRequestHandler implements HttpRequestHandler {
  public constructor(private readonly jsonSerializer: JSONSerializer) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    if (request.url.startsWith('/tus/1/get_account/')) {
      return new TusGetAccountResponse().toHttpResponse(this.jsonSerializer)
    } else if (request.url.startsWith('/tus/token')) {
      return new TusGetTokenResponse().toHttpResponse(this.jsonSerializer)
    }
    return makeErrorHttpResponse('Unstubbed', this.jsonSerializer)
  }
}
