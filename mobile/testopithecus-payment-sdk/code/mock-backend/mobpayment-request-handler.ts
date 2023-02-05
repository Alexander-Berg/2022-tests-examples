import { Encoding } from '../../../common/code/file-system/file-system-types'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { ArrayBufferHelpers } from '../../../common/native-modules/native-modules'
import { VerifyBindingResponse } from '../../../payment-sdk/code/network/mobile-backend/entities/bind/verify-binding-response'
import { InitPaymentResponse } from '../../../payment-sdk/code/network/mobile-backend/entities/init/init-payment-response'
import { RawPaymentMethodsResponse } from '../../../payment-sdk/code/network/mobile-backend/entities/methods/raw-payment-methods-response'
import { extractMockRequest, getHttpOAuth, getRequestHeader } from './mock-backend-utils'
import { MobPaymentError } from './model/mock-data-types'
import { MockTrustModel } from './model/mock-trust-model'
import { decodeInitPaymentRequest, decodeVerifyBindingRequest } from './network/mock-mobpayment-requests'
import {
  makeInitPaymentHttpResponse,
  makeMobPaymentHttpError,
  makePaymentMethodsHttpResponse,
  makeVerifyBindingHttpResponse,
} from './network/mock-mobpayment-responses'

export class MobpaymentRequestHandler implements HttpRequestHandler {
  public constructor(private readonly trustModel: MockTrustModel, private readonly jsonSerializer: JSONSerializer) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    const oAuth = getHttpOAuth(request)
    const checkCvn = getRequestHeader(request.headers, 'X-SDK-Force-CVV') === '1'
    if (request.url === '/mobpayment/v1/payment_methods') {
      const result = this.trustModel.paymentMethods(oAuth, checkCvn)
      return makePaymentMethodsHttpResponse(
        new RawPaymentMethodsResponse('success', false, false, result.methods, result.enabledMethods),
        this.jsonSerializer,
      )
    } else if (request.url === '/mobpayment/v1/init_payment') {
      const body = ArrayBufferHelpers.arrayBufferToString(request.body, Encoding.Utf8)
      if (body.isError()) {
        return makeMobPaymentHttpError('incorrect format', 'body seems to be malformed', this.jsonSerializer)
      }
      const req = extractMockRequest(body.getValue(), this.jsonSerializer, (item) => decodeInitPaymentRequest(item))
      if (req.isError()) {
        return makeMobPaymentHttpError('incorrect format', 'body seems to be malformed', this.jsonSerializer)
      }
      const data = req.getValue()
      const service = getRequestHeader(request.headers, 'X-Service-Token')
      const result = this.trustModel.initPayment(oAuth, data.email, data.token, service, checkCvn)
      if (result.isError()) {
        const initError = result.getError() as MobPaymentError
        return makeMobPaymentHttpError(initError.status, initError.message, this.jsonSerializer)
      }
      const initData = result.getValue()
      return makeInitPaymentHttpResponse(
        new InitPaymentResponse(
          'success',
          initData.purchaseToken,
          null,
          initData.acquirer,
          'production',
          initData.amount,
          'RUB',
          initData.merchantInfo,
          null,
          null,
          false,
          false,
          initData.methods.methods,
          initData.methods.enabledMethods,
        ),
        this.jsonSerializer,
      )
    } else if (request.url === '/mobpayment/v1/verify_binding') {
      const service = getRequestHeader(request.headers, 'X-Service-Token')
      if (oAuth === null || service === null) {
        return makeMobPaymentHttpError('incorrect format', 'body seems to be malformed', this.jsonSerializer)
      }
      const body = ArrayBufferHelpers.arrayBufferToString(request.body, Encoding.Utf8)
      if (body.isError()) {
        return makeMobPaymentHttpError('incorrect format', 'body seems to be malformed', this.jsonSerializer)
      }
      const req = extractMockRequest(body.getValue(), this.jsonSerializer, (item) => decodeVerifyBindingRequest(item))
      if (req.isError()) {
        return makeMobPaymentHttpError('incorrect format', 'body seems to be malformed', this.jsonSerializer)
      }
      const data = req.getValue()
      const result = this.trustModel.verifyBinding(oAuth!, data.bindingId, service!)
      if (result.isError()) {
        const initError = result.getError() as MobPaymentError
        return makeMobPaymentHttpError(initError.status, initError.message, this.jsonSerializer)
      }
      const verifyData = result.getValue()
      return makeVerifyBindingHttpResponse(new VerifyBindingResponse(verifyData), this.jsonSerializer)
    }
    return makeMobPaymentHttpError('incorrect format', 'body seems to be malformed', this.jsonSerializer)
  }
}
