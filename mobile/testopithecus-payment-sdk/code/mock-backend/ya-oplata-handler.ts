import { Encoding } from '../../../common/code/file-system/file-system-types'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { ArrayBufferHelpers } from '../../../common/native-modules/native-modules'
import { getAcquirerByToken } from '../payment-sdk-data'
import { extractMockRequest, getRequestHeader } from './mock-backend-utils'
import { MockTrustModel } from './model/mock-trust-model'
import { makeErrorHttpResponse } from './network/mock-preparation-responses'
import { decodeAmountFromYaOplataCreateOrderRequest } from './network/mock-ya-oplata-requests'
import { YaOplataMockConstants, YaPaymentCreateOrderResponse } from './network/mock-ya-oplata-responses'

export class YaOplataHandler implements HttpRequestHandler {
  public constructor(private readonly trustModel: MockTrustModel, private readonly jsonSerializer: JSONSerializer) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    if (request.url.startsWith('/yaoplata/v1/order')) {
      const acquirer = getAcquirerByToken(getRequestHeader(request.headers, 'Authorization')!)
      const body = ArrayBufferHelpers.arrayBufferToString(request.body, Encoding.Utf8)
      const req = extractMockRequest(body.getValue(), this.jsonSerializer, (item) =>
        decodeAmountFromYaOplataCreateOrderRequest(item),
      )
      const amount = req.getValue()
      this.trustModel.createYaOplataOrder(amount, YaOplataMockConstants.token, acquirer)
      return new YaPaymentCreateOrderResponse(amount).toHttpResponse(this.jsonSerializer)
    }
    return makeErrorHttpResponse('Unstubbed', this.jsonSerializer)
  }
}
