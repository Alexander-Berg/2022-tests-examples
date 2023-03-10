// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM service/trust-requests.ts >>>

import Foundation

open class BaseTrustRequest: BaseNetworkRequest {
  public let user: OAuthUserAccount
  public let merchant: String
  public init(_ user: OAuthUserAccount, _ merchant: String) {
    self.user = user
    self.merchant = merchant
    super.init()
  }

  @discardableResult
  open override func encoding() -> RequestEncoding {
    return JsonRequestEncoding()
  }

  @discardableResult
  open override func method() -> NetworkMethod {
    return NetworkMethod.post
  }

  @discardableResult
  open override func headersExtra() -> MapJSONItem {
    let headers = MapJSONItem().putString("X-Service-Token", self.merchant)
    if self.user.account.uid.length > 0 {
      headers.putString("X-Uid", self.user.account.uid)
    }
    return headers
  }

}

open class CreateOrderRequest: BaseTrustRequest {
  private let product: String
  public init(_ user: OAuthUserAccount, _ merchant: String, _ product: String) {
    self.product = product
    super.init(user, merchant)
  }

  @discardableResult
  open override func targetPath() -> String {
    return "trust-payments/v2/orders"
  }

  @discardableResult
  open override func params() -> NetworkParams {
    return MapJSONItem().putString("product_id", self.product)
  }

}

open class CreatePurchaseRequest: BaseTrustRequest {
  private let orderId: String
  private let force3ds: Bool
  private let amount: String
  private let forceCvv: Bool
  public init(_ user: OAuthUserAccount, _ merchant: String, _ orderId: String, _ force3ds: Bool, _ amount: String, _ forceCvv: Bool) {
    self.orderId = orderId
    self.force3ds = force3ds
    self.amount = amount
    self.forceCvv = forceCvv
    super.init(user, merchant)
  }

  @discardableResult
  open override func targetPath() -> String {
    return "trust-payments/v2/payments"
  }

  @discardableResult
  open override func params() -> MapJSONItem {
    return MapJSONItem().putString("return_path", "https://yandex.ru/").putString("user_email", self.user.account.login + "@yandex.ru").putString("user_phone", "89998887766").putInt32("wait_for_cvn", self.forceCvv ? 1 : 0).put("pass_params", MapJSONItem().put("terminal_route_data", MapJSONItem().putInt32("service_force_3ds", self.force3ds ? 1 : 0))).put("orders", ArrayJSONItem().add(MapJSONItem().putString("currency", "RUB").putString("fiscal_nds", "nds_18").putString("fiscal_title", "test_fiscal_title").putString("price", self.amount).putString("service_order_id", self.orderId)))
  }

}

open class StartPurchaseRequest: BaseTrustRequest {
  public static let PATH_MATCH_REGEX: String = "trust-payments/v2/payments/([0-9]+)/start"
  private let purchaseId: String
  public init(_ user: OAuthUserAccount, _ merchant: String, _ purchaseId: String) {
    self.purchaseId = purchaseId
    super.init(user, merchant)
  }

  @discardableResult
  open override func targetPath() -> String {
    return "trust-payments/v2/payments/" + self.purchaseId + "/start"
  }

  @discardableResult
  open override func params() -> MapJSONItem {
    return super.params().putString("purchaseId", self.purchaseId)
  }

}

open class PaymentMethodsRequest: BaseTrustRequest {
  public override init(_ user: OAuthUserAccount, _ merchant: String) {
    super.init(user, merchant)
  }

  @discardableResult
  open override func method() -> NetworkMethod {
    return NetworkMethod.`get`
  }

  @discardableResult
  open override func targetPath() -> String {
    return "trust-payments/v2/payment-methods"
  }

  @discardableResult
  open override func params() -> NetworkParams {
    return super.params()
  }

  @discardableResult
  open override func encoding() -> RequestEncoding {
    return UrlRequestEncoding()
  }

}

