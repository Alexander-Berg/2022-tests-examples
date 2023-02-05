import { Encoding } from '../../../common/code/file-system/file-system-types'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { ArrayBufferHelpers, Uris } from '../../../common/native-modules/native-modules'
import { StartPurchaseRequest } from '../service/trust-requests'
import { extractMockRequest, getRequestHeader } from './mock-backend-utils'
import { stringToFamilyInfoMode } from './model/mock-data-types'
import { MockTrustModel } from './model/mock-trust-model'
import { TrustPaymentsPaymentsRequest } from './network/mock-preparation-requests'
import {
  makeErrorHttpResponse,
  makeSuccessHttpResponse,
  TrustPaymentsOrdersResponse,
  TrustPaymentsPaymentsResponse,
} from './network/mock-preparation-responses'

export class TrustRequestHandler implements HttpRequestHandler {
  public constructor(private readonly trustModel: MockTrustModel, private readonly jsonSerializer: JSONSerializer) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    if (request.url === '/trust/trust-payments/v2/orders') {
      const orderId = this.trustModel.createOrder(getRequestHeader(request.headers, 'X-Service-Token'))
      return new TrustPaymentsOrdersResponse(orderId).toHttpResponse(this.jsonSerializer)
    } else if (request.url === '/trust/trust-payments/v2/payments') {
      const body = ArrayBufferHelpers.arrayBufferToString(request.body, Encoding.Utf8)
      const req = extractMockRequest(body.getValue(), this.jsonSerializer, (item) =>
        TrustPaymentsPaymentsRequest.decodeJson(item),
      )
      if (req.isError()) {
        return makeErrorHttpResponse('Wrong format', this.jsonSerializer)
      }
      const data = req.getValue()
      const result = this.trustModel.setupOrder(data.serviceOrderId, data.price, data.forced3ds)
      if (result.isError()) {
        return makeErrorHttpResponse(result.getError().message, this.jsonSerializer)
      }
      return new TrustPaymentsPaymentsResponse(result.getValue()).toHttpResponse(this.jsonSerializer)
    } else if (request.url.search(StartPurchaseRequest.PATH_MATCH_REGEX) !== -1) {
      const purchaseId = request.url.match(StartPurchaseRequest.PATH_MATCH_REGEX)![1]
      if (!this.trustModel.checkHasPurchase(purchaseId)) {
        return makeErrorHttpResponse('Wrong format', this.jsonSerializer)
      }
      return makeSuccessHttpResponse(this.jsonSerializer)
    } else if (request.url.startsWith('/trust/mock-trust-bank')) {
      const name = Uris.fromString(request.url)?.getQueryParameter('bank') ?? null
      if (name === null) {
        return makeErrorHttpResponse('Wrong format', this.jsonSerializer)
      }
      const result = this.trustModel.setStartMockBank(name!)
      if (result.isError()) {
        return makeErrorHttpResponse('Wrong format', this.jsonSerializer)
      }
      return makeSuccessHttpResponse(this.jsonSerializer)
    } else if (request.url.startsWith('/trust/mock-family-info-mode')) {
      const modeParam = Uris.fromString(request.url)?.getQueryParameter('mode') ?? null
      if (modeParam === null) {
        return makeErrorHttpResponse('Wrong format', this.jsonSerializer)
      }
      const mode = stringToFamilyInfoMode(modeParam!)
      if (mode === null) {
        return makeErrorHttpResponse('Wrong format', this.jsonSerializer)
      }
      this.trustModel.setFamilyInfoMode(mode!)
      return makeSuccessHttpResponse(this.jsonSerializer)
    }
    return makeErrorHttpResponse('Unstubbed', this.jsonSerializer)
  }
}
