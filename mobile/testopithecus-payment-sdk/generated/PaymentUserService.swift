// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM payment-user-service.ts >>>

import Foundation

open class PaymentUserService {
  private let userAccountsMap: UserServiceEnsemble
  private let oauthService: OauthService
  private let diehardBackendApi: DiehardBackendApi
  private let network: Network
  private let jsonSerializer: JSONSerializer
  private let passportToken: String!
  public init(_ userAccountsMap: UserServiceEnsemble, _ oauthService: OauthService, _ diehardBackendApi: DiehardBackendApi, _ network: Network, _ jsonSerializer: JSONSerializer, _ passportToken: String!) {
    self.userAccountsMap = userAccountsMap
    self.oauthService = oauthService
    self.diehardBackendApi = diehardBackendApi
    self.network = network
    self.jsonSerializer = jsonSerializer
    self.passportToken = passportToken
  }

  @discardableResult
  open class func build(_ diehardNetwork: Network, _ mobileBackendNetwork: Network, _ syncNetwork: SyncNetwork, _ jsonSerializer: JSONSerializer, _ passportToken: String!, _ tusConsumer: String, _ forcedAuthHost: String! = nil) -> PaymentUserService {
    let userService = UserService(syncNetwork, jsonSerializer, EmptyLogger())
    let userAccountsMap = UserServiceEnsemble(userService, YSArray(AccountType2.YandexTest), tusConsumer, YSArray())
    let oauthHostsConfig = forcedAuthHost != nil ? OauthHostsConfig(forcedAuthHost!, forcedAuthHost!, forcedAuthHost!) : OauthHostsConfig()
    let oauthService = OauthService(PaymentSdkBackendConfig.applicationCredentials, syncNetwork, jsonSerializer, oauthHostsConfig)
    let diehardBackendApi = DiehardBackendApi.create(diehardNetwork, jsonSerializer, passportToken)
    return PaymentUserService(userAccountsMap, oauthService, diehardBackendApi, mobileBackendNetwork, jsonSerializer, passportToken)
  }

  @discardableResult
  open func getAuthorizedUser() throws -> OAuthUserAccount! {
    let min10 = int64(10 * 60 * 1000)
    let min2 = int64(2 * 60 * 1000)
    let type = AccountType2.YandexTest
    let lock: UserLock! = self.userAccountsMap.getAccountByType(type).tryAcquire(min10, min2)
    if lock == nil {
      return nil
    }
    let account = lock.lockedAccount()
    let token: String! = (try self.oauthService.getToken(account, type))
    return OAuthUserAccount(account, token, type)
  }

  @discardableResult
  open func bindCard(_ user: OAuthUserAccount, _ card: BoundCard, _ serviceToken: String) -> XPromise<Void> {
    let request = BindNewCardRequest(user.oauthToken, serviceToken, card.cardNumber, card.expirationMonth, card.expirationYear, card.cvv, RegionIds.russia)
    return self.diehardBackendApi.bindNewCard(request).then({
      (_: BindNewCardResponse) -> Void in
      getVoid()
    })
  }

  @discardableResult
  open func unbindCards(_ user: OAuthUserAccount, _ serviceToken: String) -> XPromise<Void> {
    let request = RawPaymentMethodsRequest()
    return self.mobileBackendAPI(user, serviceToken).rawPaymentMethods(request).then({
      (response: RawPaymentMethodsResponse) in
      response.paymentMethods
    }).flatThen({
      (methods: YSArray<PaymentMethod>) -> XPromise<Void> in
      let promises: YSArray<() -> XPromise<Void>> = YSArray()
      for i in stride(from: 0, to: methods.length, by: 1) {
        let request = UnbindCardRequest(user.oauthToken, methods[i].identifier)
        promises.push({
          () in
          self.diehardBackendApi.unbindCard(request).then({
            (_) in
            getVoid()
          })
        })
      }
      return (executeSequentially(promises) as XPromise<YSArray<Void>>).then({
        (_) in
        getVoid()
      })
    })
  }

  @discardableResult
  open func getPaymentMethods(_ user: OAuthUserAccount, _ serviceToken: String) -> XPromise<AvailableMethods> {
    let request = RawPaymentMethodsRequest()
    return self.mobileBackendAPI(user, serviceToken).rawPaymentMethods(request).then({
      (response: RawPaymentMethodsResponse) -> AvailableMethods in
      AvailableMethods(response.paymentMethods, response.applePaySupported, response.googlePaySupported, isPaymentMethodEnabled(response, "sbp_qr"), false)
    }).flatThen({
      (methods: AvailableMethods) -> XPromise<AvailableMethods> in
      EmptyPaymentMethodsDecorator().decorate(methods)
    })
  }

  @discardableResult
  open func cleanAccountAndBindCards(_ user: OAuthUserAccount, _ cards: YSArray<BoundCard>, _ serviceToken: String) -> XPromise<Void> {
    return self.unbindCards(user, serviceToken).then({
      (_) in
      getVoid()
    }).flatThen({
      (_) -> XPromise<Void> in
      let promises: YSArray<() -> XPromise<Void>> = YSArray()
      for i in stride(from: 0, to: cards.length, by: 1) {
        promises.push({
          () in
          self.bindCard(user, cards[i], serviceToken)
        })
      }
      return executeSequentially(promises).then({
        (_) in
        getVoid()
      })
    })
  }

  @discardableResult
  private func mobileBackendAPI(_ user: OAuthUserAccount, _ serviceToken: String) -> MobileBackendApi {
    let interceptor = MobileBackendNetworkInterceptor.create(user.oauthToken, serviceToken, user.account.uid)
    let passportInterceptor = PassportHeaderInterceptor(self.passportToken)
    let authorizedNetwork = NetworkIntermediate(self.network, YSArray(interceptor, passportInterceptor))
    let errorProcessor = MobileBackendErrorProcessor()
    let networkService = NetworkService(authorizedNetwork, self.jsonSerializer, errorProcessor)
    return MobileBackendApi(networkService)
  }

}

open class EmptyLogger: Logger {
  open func error(_ message: String) -> Void {
  }

  open func info(_ message: String) -> Void {
  }

  open func warn(_ message: String) -> Void {
  }

}

