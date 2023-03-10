// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mock-backend/diehard-request-handler.ts >>>

import Foundation

open class DiehardRequestHandler: HttpRequestHandler {
  private let trustModel: MockTrustModel
  private let jsonSerializer: JSONSerializer
  public init(_ trustModel: MockTrustModel, _ jsonSerializer: JSONSerializer) {
    self.trustModel = trustModel
    self.jsonSerializer = jsonSerializer
  }

  @discardableResult
  open func handleRequest(_ request: HttpRequest) -> HttpResponse {
    let headerAuth: String! = getHttpOAuth(request)
    let service: String! = getRequestHeader(request.headers, "X-Service-Token")
    let body = ArrayBufferHelpers.arrayBufferToString(request.body, Encoding.Utf8)
    if body.isError() {
      return makeDiehardHttpError("invalid_processing_request", self.jsonSerializer)
    }
    if request.url == "/diehard/api/bind_card" {
      return self.handleBindCard(headerAuth, service, body.getValue())
    } else if request.url == "/diehard/api/unbind_card" {
      return self.handleUnbindCard(headerAuth, body.getValue())
    } else if request.url == "/diehard/api/supply_payment_data" {
      return self.handleSupplyPayment(headerAuth, body.getValue())
    } else if request.url == "/diehard/api/check_payment" {
      return self.handleCheckPayment(body.getValue())
    } else if request.url == "/diehard/api/bindings/v2.0/bindings" {
      return self.handleBindingV2(request, body.getValue())
    }
    return makeDiehardHttpError("invalid_processing_request", self.jsonSerializer)
  }

  @discardableResult
  private func handleBindCard(_ headerAuth: String!, _ service: String!, _ body: String) -> HttpResponse {
    let req = extractMockRequest(body, self.jsonSerializer, {
      (item) in
      decodeBindCardRequest("", item)
    })
    if req.isError() {
      return makeDiehardHttpError("invalid_processing_request", self.jsonSerializer)
    }
    let data = req.getValue()
    if headerAuth == nil && data.token == nil {
      return makeDiehardHttpError("authorization_reject", self.jsonSerializer)
    }
    let oAuth = headerAuth ?? data.token!
    let result = self.trustModel.bindCard(oAuth, data.cardNumber, data.expirationMonth, data.expirationYear, data.cvn, service)
    if result.isError() {
      return makeDiehardHttpError(result.getError().message, self.jsonSerializer)
    }
    return makeBindCardHttpResponse(BindNewCardResponse("success", nil, "card bound ok", result.getValue()), self.jsonSerializer)
  }

  @discardableResult
  private func handleUnbindCard(_ headerAuth: String!, _ body: String) -> HttpResponse {
    let req = extractMockRequest(body, self.jsonSerializer, {
      (item) in
      decodeUnbindCardRequest(item)
    })
    if req.isError() {
      return makeDiehardHttpError("invalid_processing_request", self.jsonSerializer)
    }
    let data = req.getValue()
    if headerAuth == nil && data.token == nil {
      return makeDiehardHttpError("authorization_reject", self.jsonSerializer)
    }
    let oAuth = headerAuth ?? data.token!
    let result = self.trustModel.unBindCard(oAuth, data.cardID)
    if result.isError() {
      return makeDiehardHttpError(result.getError().message, self.jsonSerializer)
    }
    return makeUnbindCardHttpResponse(UnbindCardResponse("success", nil, "card unbound ok"), self.jsonSerializer)
  }

  @discardableResult
  private func handleSupplyPayment(_ headerAuth: String!, _ body: String) -> HttpResponse {
    let req = extractMockRequest(body, self.jsonSerializer, {
      (item) in
      MockSupplyPaymentRequest.decodeJson(item)
    })
    if req.isError() {
      return makeDiehardHttpError("invalid_processing_request", self.jsonSerializer)
    }
    let data = req.getValue()
    let oAuth = headerAuth ?? data.token
    if data.paymentMethod == "new_card" {
      if data.cardNumber == nil || data.expirationMonth == nil || data.expirationYear == nil || data.cvn == nil {
        return makeDiehardHttpError("invalid_processing_request", self.jsonSerializer)
      }
      let result = self.trustModel.supplyPaymentByNewCard(oAuth, data.purchaseToken, data.cardNumber!, data.expirationMonth!, data.expirationYear!, data.cvn!, data.bindCard == 1)
      if result.isError() {
        return makeDiehardHttpError(result.getError().message, self.jsonSerializer)
      }
    } else if data.paymentMethod == "sbp_qr" {
      let result = self.trustModel.supplyPaymentBySbp(oAuth, data.purchaseToken)
      if result.isError() {
        return makeDiehardHttpError(result.getError().message, self.jsonSerializer)
      }
    } else {
      let result = self.trustModel.supplyPaymentByStoredCard(oAuth, data.purchaseToken, data.paymentMethod, data.cvn)
      if result.isError() {
        return makeDiehardHttpError(result.getError().message, self.jsonSerializer)
      }
    }
    return makeSupplyPaymentHttpResponse(SupplyPaymentResponse("success", nil, nil), self.jsonSerializer)
  }

  @discardableResult
  private func handleCheckPayment(_ body: String) -> HttpResponse {
    let req = extractMockRequest(body, self.jsonSerializer, {
      (item) in
      decodeCheckPaymentRequest(item)
    })
    if req.isError() {
      return makeDiehardHttpError("invalid_processing_request", self.jsonSerializer)
    }
    let data = req.getValue()
    let result = self.trustModel.checkPayment(data.purchaseToken)
    if result.isError() {
      return makeDiehardHttpError(result.getError().message, self.jsonSerializer)
    }
    let checkData = result.getValue()
    return makeCheckPaymentHttpResponse(CheckPaymentResponse(checkData.status, nil, checkData.statusDesc, checkData.redirect3ds, checkData.sbpPaymentForm, nil), checkData.purchaseToken, checkData.amount, checkData.isBinding, checkData.timestamp, self.jsonSerializer)
  }

  @discardableResult
  private func handleBindingV2(_ request: HttpRequest, _ body: String) -> HttpResponse {
    let oAuth: String! = getRequestHeader(request.headers, "X-Oauth-Token")
    if oAuth == nil {
      return makeDiehardHttpError("authorization_reject", self.jsonSerializer)
    }
    let req = extractMockRequest(body, self.jsonSerializer, {
      (item) in
      decodeNewCardBindReuest(oAuth!, item)
    })
    if req.isError() {
      return makeDiehardHttpError("invalid_processing_request", self.jsonSerializer)
    }
    let data = req.getValue()
    let result = self.trustModel.startV2Binding(oAuth!, data.cardDataEncrypted, data.hashAlgorithm, data.serviceToken)
    if result.isError() {
      return makeDiehardHttpError(result.getError().message, self.jsonSerializer)
    }
    return makeBindingV2Response(NewCardBindingResponse(result.getValue()), self.jsonSerializer)
  }

}

