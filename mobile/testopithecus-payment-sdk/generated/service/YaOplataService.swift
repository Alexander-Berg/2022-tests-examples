// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM service/ya-oplata-service.ts >>>

import Foundation

public typealias PayToken = String

open class YaOplataService {
  private let networkService: NetworkService
  public init(_ networkService: NetworkService) {
    self.networkService = networkService
  }

  @discardableResult
  open class func create(_ network: Network, _ serializer: JSONSerializer) -> YaOplataService {
    let errorProcessor = YaOplataBackendErrorProcessor()
    let networkService = NetworkService(network, serializer, errorProcessor)
    return YaOplataService(networkService)
  }

  @discardableResult
  open func createOrder(_ acquirerToken: String, _ amount: String) -> XPromise<PayToken> {
    return self.networkService.performRequest(YaOplataCreateOrderRequest(acquirerToken, amount), {
      (item) in
      decodeJSONItem(item, {
        (json) in
        let map = (try json.tryCastAsMapJSONItem())
        let data = (try (try map.tryGet("data")).tryCastAsMapJSONItem())
        return (try data.tryGetString("pay_token"))
      })
    })
  }

}

open class YaOplataBackendErrorProcessor: NetworkServiceErrorProcessor {
  @discardableResult
  open func extractError(_ errorBody: JSONItem, _ code: Int32) -> NetworkServiceError! {
    let errorResponse = YaOplataErrorResponse.fromJsonItem(errorBody)
    if errorResponse.isError() {
      return nil
    }
    return YaOplataBackendError(errorResponse.getValue())
  }

  @discardableResult
  open func validateResponse(_ body: JSONItem) -> NetworkServiceError! {
    return nil
  }

  @discardableResult
  open func wrapError(_ error: NetworkServiceError) -> NetworkServiceError {
    return error
  }

}

open class YaOplataBackendError: NetworkServiceError {
  public let error: YaOplataErrorResponse
  public init(_ error: YaOplataErrorResponse) {
    self.error = error
    super.init(mobileBackendStatusToKind(error.code), ExternalErrorTrigger.internal_sdk, error.code, "Ya Payment Backend Error: code - \(error.code), status - \(error.status) : \(error.message ?? "empty message")")
  }

  @discardableResult
  open override func convertToExternalError() -> ExternalError {
    return ExternalError(self.kind, self.trigger, self.code, self.error.status, self.message)
  }

}

open class YaOplataErrorResponse {
  public let status: String
  public let code: Int32
  public let message: String!
  public init(_ status: String, _ code: Int32, _ message: String!) {
    self.status = status
    self.code = code
    self.message = message
  }

  @discardableResult
  open class func fromJsonItem(_ item: JSONItem) -> Result<YaOplataErrorResponse> {
    return decodeJSONItem(item, {
      (json) in
      let map = (try json.tryCastAsMapJSONItem())
      let status = (try map.tryGetString("status"))
      let code = (try map.tryGetInt32("code"))
      let data = map.`get`("data") as! MapJSONItem
      let message: String! = data.getString("message")
      return YaOplataErrorResponse(status, code, message)
    })
  }

}

