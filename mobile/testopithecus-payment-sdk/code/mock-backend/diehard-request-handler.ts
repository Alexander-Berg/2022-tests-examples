import { Nullable } from '../../../../common/ys'
import { Encoding } from '../../../common/code/file-system/file-system-types'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { ArrayBufferHelpers } from '../../../common/native-modules/native-modules'
import { BindNewCardResponse } from '../../../payment-sdk/code/network/diehard-backend/entities/bind/bind-new-card-response'
import { NewCardBindingResponse } from '../../../payment-sdk/code/network/diehard-backend/entities/bind/new-card-binding-response'
import { UnbindCardResponse } from '../../../payment-sdk/code/network/diehard-backend/entities/bind/unbind-card-response'
import { CheckPaymentResponse } from '../../../payment-sdk/code/network/diehard-backend/entities/check-payment/check-payment-response'
import { SupplyPaymentResponse } from '../../../payment-sdk/code/network/diehard-backend/entities/supply/supply-payment-response'
import { extractMockRequest, getHttpOAuth, getRequestHeader } from './mock-backend-utils'
import { MockTrustModel } from './model/mock-trust-model'
import {
  decodeBindCardRequest,
  decodeCheckPaymentRequest,
  decodeNewCardBindReuest,
  decodeUnbindCardRequest,
  MockSupplyPaymentRequest,
} from './network/mock-diehard-requests'
import {
  makeBindCardHttpResponse,
  makeBindingV2Response,
  makeCheckPaymentHttpResponse,
  makeDiehardHttpError,
  makeSupplyPaymentHttpResponse,
  makeUnbindCardHttpResponse,
} from './network/mock-diehard-responses'

export class DiehardRequestHandler implements HttpRequestHandler {
  public constructor(private readonly trustModel: MockTrustModel, private readonly jsonSerializer: JSONSerializer) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    const headerAuth = getHttpOAuth(request)
    const service = getRequestHeader(request.headers, 'X-Service-Token')
    const body = ArrayBufferHelpers.arrayBufferToString(request.body, Encoding.Utf8)
    if (body.isError()) {
      return makeDiehardHttpError('invalid_processing_request', this.jsonSerializer)
    }

    if (request.url === '/diehard/api/bind_card') {
      return this.handleBindCard(headerAuth, service, body.getValue())
    } else if (request.url === '/diehard/api/unbind_card') {
      return this.handleUnbindCard(headerAuth, body.getValue())
    } else if (request.url === '/diehard/api/supply_payment_data') {
      return this.handleSupplyPayment(headerAuth, body.getValue())
    } else if (request.url === '/diehard/api/check_payment') {
      return this.handleCheckPayment(body.getValue())
    } else if (request.url === '/diehard/api/bindings/v2.0/bindings') {
      return this.handleBindingV2(request, body.getValue())
    }
    return makeDiehardHttpError('invalid_processing_request', this.jsonSerializer)
  }

  private handleBindCard(headerAuth: Nullable<string>, service: Nullable<string>, body: string): HttpResponse {
    const req = extractMockRequest(body, this.jsonSerializer, (item) => decodeBindCardRequest('', item))
    if (req.isError()) {
      return makeDiehardHttpError('invalid_processing_request', this.jsonSerializer)
    }
    const data = req.getValue()
    if (headerAuth === null && data.token === null) {
      return makeDiehardHttpError('authorization_reject', this.jsonSerializer)
    }
    const oAuth = headerAuth ?? data.token!
    const result = this.trustModel.bindCard(
      oAuth,
      data.cardNumber,
      data.expirationMonth,
      data.expirationYear,
      data.cvn,
      service,
    )
    if (result.isError()) {
      return makeDiehardHttpError(result.getError().message, this.jsonSerializer)
    }
    return makeBindCardHttpResponse(
      new BindNewCardResponse('success', null, 'card bound ok', result.getValue()),
      this.jsonSerializer,
    )
  }

  private handleUnbindCard(headerAuth: Nullable<string>, body: string): HttpResponse {
    const req = extractMockRequest(body, this.jsonSerializer, (item) => decodeUnbindCardRequest(item))
    if (req.isError()) {
      return makeDiehardHttpError('invalid_processing_request', this.jsonSerializer)
    }
    const data = req.getValue()
    if (headerAuth === null && data.token === null) {
      return makeDiehardHttpError('authorization_reject', this.jsonSerializer)
    }
    const oAuth = headerAuth ?? data.token!
    const result = this.trustModel.unBindCard(oAuth, data.cardID)
    if (result.isError()) {
      return makeDiehardHttpError(result.getError().message, this.jsonSerializer)
    }
    return makeUnbindCardHttpResponse(new UnbindCardResponse('success', null, 'card unbound ok'), this.jsonSerializer)
  }

  private handleSupplyPayment(headerAuth: Nullable<string>, body: string): HttpResponse {
    const req = extractMockRequest(body, this.jsonSerializer, (item) => MockSupplyPaymentRequest.decodeJson(item))
    if (req.isError()) {
      return makeDiehardHttpError('invalid_processing_request', this.jsonSerializer)
    }
    const data = req.getValue()
    const oAuth = headerAuth ?? data.token
    if (data.paymentMethod === 'new_card') {
      if (
        data.cardNumber === null ||
        data.expirationMonth === null ||
        data.expirationYear === null ||
        data.cvn === null
      ) {
        return makeDiehardHttpError('invalid_processing_request', this.jsonSerializer)
      }
      const result = this.trustModel.supplyPaymentByNewCard(
        oAuth,
        data.purchaseToken,
        data.cardNumber!,
        data.expirationMonth!,
        data.expirationYear!,
        data.cvn!,
        data.bindCard === 1,
      )
      if (result.isError()) {
        return makeDiehardHttpError(result.getError().message, this.jsonSerializer)
      }
    } else if (data.paymentMethod === 'sbp_qr') {
      const result = this.trustModel.supplyPaymentBySbp(oAuth, data.purchaseToken)
      if (result.isError()) {
        return makeDiehardHttpError(result.getError().message, this.jsonSerializer)
      }
    } else {
      const result = this.trustModel.supplyPaymentByStoredCard(oAuth, data.purchaseToken, data.paymentMethod, data.cvn)
      if (result.isError()) {
        return makeDiehardHttpError(result.getError().message, this.jsonSerializer)
      }
    }
    return makeSupplyPaymentHttpResponse(new SupplyPaymentResponse('success', null, null), this.jsonSerializer)
  }

  private handleCheckPayment(body: string): HttpResponse {
    const req = extractMockRequest(body, this.jsonSerializer, (item) => decodeCheckPaymentRequest(item))
    if (req.isError()) {
      return makeDiehardHttpError('invalid_processing_request', this.jsonSerializer)
    }
    const data = req.getValue()
    const result = this.trustModel.checkPayment(data.purchaseToken)
    if (result.isError()) {
      return makeDiehardHttpError(result.getError().message, this.jsonSerializer)
    }
    const checkData = result.getValue()
    return makeCheckPaymentHttpResponse(
      new CheckPaymentResponse(
        checkData.status,
        null,
        checkData.statusDesc,
        checkData.redirect3ds,
        checkData.sbpPaymentForm,
        null,
      ),
      checkData.purchaseToken,
      checkData.amount,
      checkData.isBinding,
      checkData.timestamp,
      this.jsonSerializer,
    )
  }

  private handleBindingV2(request: HttpRequest, body: string): HttpResponse {
    const oAuth = getRequestHeader(request.headers, 'X-Oauth-Token')
    if (oAuth === null) {
      return makeDiehardHttpError('authorization_reject', this.jsonSerializer)
    }
    const req = extractMockRequest(body, this.jsonSerializer, (item) => decodeNewCardBindReuest(oAuth!, item))
    if (req.isError()) {
      return makeDiehardHttpError('invalid_processing_request', this.jsonSerializer)
    }
    const data = req.getValue()
    const result = this.trustModel.startV2Binding(oAuth!, data.cardDataEncrypted, data.hashAlgorithm, data.serviceToken)
    if (result.isError()) {
      return makeDiehardHttpError(result.getError().message, this.jsonSerializer)
    }
    return makeBindingV2Response(new NewCardBindingResponse(result.getValue()), this.jsonSerializer)
  }
}
