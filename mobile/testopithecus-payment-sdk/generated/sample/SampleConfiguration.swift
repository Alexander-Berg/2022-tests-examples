// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM sample/sample-configuration.ts >>>

import Foundation

open class SampleConfiguration {
  public static var SERVICE_TOKEN: String = "payment_sdk_19d9962ddd08e7d52a2668cbcd5f7b7e"
  public static var ANY_EMAIL: String = "test.account@ya.ru"
  public static var ANY_3DS_CODE: String = "123"
  private var authorizationMode: AuthorizationMode = AuthorizationMode.authorized
  private var providedAccount: OAuthUserAccount! = nil
  private var boundCardsCount: Int32 = 1
  private let paymentDataPreparer: PaymentDataPreparer
  private let userService: PaymentUserService
  public init(_ paymentDataPreparer: PaymentDataPreparer, _ userService: PaymentUserService) {
    self.paymentDataPreparer = paymentDataPreparer
    self.userService = userService
  }

  @discardableResult
  open class func build(_ trustNetwork: Network, _ yaOplataNetwork: Network, _ diehardNetwork: Network, _ mobileBackendNetwork: Network, _ syncNetwork: SyncNetwork, _ jsonSerializer: JSONSerializer, _ passportToken: String!, _ tusConsumer: String, _ forcedAuthHost: String! = nil, _ forced3ds: Bool = false) -> SampleConfiguration {
    let trustService = TrustService.create(trustNetwork, jsonSerializer, passportToken)
    let yaOplataService = YaOplataService.create(yaOplataNetwork, jsonSerializer)
    let userService = PaymentUserService.build(diehardNetwork, mobileBackendNetwork, syncNetwork, jsonSerializer, passportToken, tusConsumer, forcedAuthHost)
    let mockPrepareService = MockPrepareService.create(trustNetwork, jsonSerializer)
    let paymentDataPreparer = PaymentDataPreparer(trustService, userService, yaOplataService, mockPrepareService)
    if forced3ds {
      paymentDataPreparer.set3ds(SampleConfiguration.ANY_3DS_CODE)
    }
    return SampleConfiguration(paymentDataPreparer, userService)
  }

  @discardableResult
  open func setBoundCardsCount(_ count: Int32) -> SampleConfiguration {
    self.boundCardsCount = count
    return self
  }

  @discardableResult
  open func setAuthorizationMode(_ mode: AuthorizationMode) -> SampleConfiguration {
    self.authorizationMode = mode
    return self
  }

  @discardableResult
  open func setProvidedAccount(_ providedAccount: OAuthUserAccount) -> SampleConfiguration {
    self.providedAccount = providedAccount
    return self
  }

  @discardableResult
  open func configuration() -> XPromise<PaymentConfig> {
    var userAccount: OAuthUserAccount!
    do {
      userAccount = (try self.getUserAccount())
    } catch {
      let e = error
      return reject(YSError("Unable to get authorized user, error \(extractErrorMessage(e))"))
    }
    if userAccount == nil {
      return reject(YSError("Unable to get authorized user, account is null"))
    }
    self.paymentDataPreparer.setMerchantId(SampleConfiguration.SERVICE_TOKEN)
    self.paymentDataPreparer.clearBoundCards()
    for `_` in stride(from: 0, to: self.boundCardsCount, by: 1) {
      self.paymentDataPreparer.addBoundCard(BoundCard.generated())
    }
    return self.paymentDataPreparer.prepare(userAccount).then({
      (_) -> PaymentConfig in
      PaymentConfig(Payer(userAccount!.oauthToken, userAccount!.account.uid, userAccount!.account.login + "@yandex.ru"), Merchant(self.paymentDataPreparer.getMerchantId(), self.paymentDataPreparer.merchantLocalizedName), self.paymentDataPreparer.getPaymentId(), self.paymentDataPreparer.getOrderId())
    })
  }

  @discardableResult
  open func setMockBank(_ bank: BankName) -> XPromise<Bool> {
    return self.paymentDataPreparer.mockPrepareService.setMockBank(bank)
  }

  @discardableResult
  open func setMockFamilyInfoMode(_ mode: FamilyInfoMode) -> XPromise<Bool> {
    return self.paymentDataPreparer.mockPrepareService.setMockFamilyInfoMode(mode)
  }

  open func useYaOplata(_ use: Bool) -> Void {
    self.paymentDataPreparer.setUseYaOplata(use)
    self.paymentDataPreparer.setAcquirer(use ? Acquirer.kassa : nil)
  }

  @discardableResult
  private func getUserAccount() throws -> OAuthUserAccount! {
    switch self.authorizationMode {
      case AuthorizationMode.authorized:
        return (try self.userService.getAuthorizedUser())
      case AuthorizationMode.nonauthorized:
        let account = UserAccount(SampleConfiguration.ANY_EMAIL, "", "")
        return OAuthUserAccount(account, nil, AccountType2.YandexTest)
      case AuthorizationMode.provided:
        return self.providedAccount
    }
  }

}

public enum AuthorizationMode {
  case authorized
  case nonauthorized
  case provided
}
