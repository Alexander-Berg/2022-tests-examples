import { int64, Nullable, range, Throwing } from '../../../common/ys'
import { JSONSerializer } from '../../common/code/json/json-serializer'
import { Logger } from '../../common/code/logging/logger'
import { Network } from '../../common/code/network/network'
import { NetworkIntermediate } from '../../common/code/network/network-intermediate'
import { XPromise } from '../../common/code/promise/xpromise'
import { getVoid } from '../../common/code/result/result'
import { executeSequentially } from '../../common/code/utils/xpromise-utils'
import { EmptyPaymentMethodsDecorator } from '../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AvailableMethods } from '../../payment-sdk/code/models/available-methods'
import { DiehardBackendApi } from '../../payment-sdk/code/network/diehard-backend/diehard-backend-api'
import { BindNewCardRequest } from '../../payment-sdk/code/network/diehard-backend/entities/bind/bind-new-card-request'
import { BindNewCardResponse } from '../../payment-sdk/code/network/diehard-backend/entities/bind/bind-new-card-response'
import { RegionIds } from '../../payment-sdk/code/network/diehard-backend/entities/bind/region-ids'
import { UnbindCardRequest } from '../../payment-sdk/code/network/diehard-backend/entities/bind/unbind-card-request'
import { PaymentMethod } from '../../payment-sdk/code/network/mobile-backend/entities/methods/payment-method'
import { RawPaymentMethodsRequest } from '../../payment-sdk/code/network/mobile-backend/entities/methods/raw-payment-methods-request'
import {
  isPaymentMethodEnabled,
  RawPaymentMethodsResponse,
} from '../../payment-sdk/code/network/mobile-backend/entities/methods/raw-payment-methods-response'
import {
  MobileBackendApi,
  MobileBackendErrorProcessor,
} from '../../payment-sdk/code/network/mobile-backend/mobile-backend-api'
import { MobileBackendNetworkInterceptor } from '../../payment-sdk/code/network/mobile-backend/mobile-backend-network-interceptor'
import { NetworkService } from '../../payment-sdk/code/network/network-service'
import { PassportHeaderInterceptor } from '../../payment-sdk/code/network/passport-network-interceptor'
import { SyncNetwork } from '../../testopithecus-common/code/client/network/sync-network'
import { AccountType2 } from '../../testopithecus-common/code/mbt/test/mbt-test'
import { OauthHostsConfig, OauthService } from '../../testopithecus-common/code/users/oauth-service'
import { OAuthUserAccount } from '../../testopithecus-common/code/users/user-pool'
import { UserService } from '../../testopithecus-common/code/users/user-service'
import { UserServiceEnsemble } from '../../testopithecus-common/code/users/user-service-ensemble'
import { BoundCard } from './card-generator'
import { PaymentSdkBackendConfig } from './payment-sdk-credentials'

export class PaymentUserService {
  public static build(
    diehardNetwork: Network,
    mobileBackendNetwork: Network,
    syncNetwork: SyncNetwork,
    jsonSerializer: JSONSerializer,
    passportToken: Nullable<string>,
    tusConsumer: string,
    forcedAuthHost: Nullable<string> = null,
  ): PaymentUserService {
    const userService = new UserService(syncNetwork, jsonSerializer, new EmptyLogger())
    const userAccountsMap = new UserServiceEnsemble(userService, [AccountType2.YandexTest], tusConsumer, [])
    const oauthHostsConfig =
      forcedAuthHost !== null
        ? new OauthHostsConfig(forcedAuthHost!, forcedAuthHost!, forcedAuthHost!)
        : new OauthHostsConfig()
    const oauthService = new OauthService(
      PaymentSdkBackendConfig.applicationCredentials,
      syncNetwork,
      jsonSerializer,
      oauthHostsConfig,
    )
    const diehardBackendApi = DiehardBackendApi.create(diehardNetwork, jsonSerializer, passportToken)
    return new PaymentUserService(
      userAccountsMap,
      oauthService,
      diehardBackendApi,
      mobileBackendNetwork,
      jsonSerializer,
      passportToken,
    )
  }

  public constructor(
    private readonly userAccountsMap: UserServiceEnsemble,
    private readonly oauthService: OauthService,
    private readonly diehardBackendApi: DiehardBackendApi,
    private readonly network: Network,
    private readonly jsonSerializer: JSONSerializer,
    private readonly passportToken: Nullable<string>,
  ) {}

  public getAuthorizedUser(): Throwing<Nullable<OAuthUserAccount>> {
    const min10 = int64(10 * 60 * 1000)
    const min2 = int64(2 * 60 * 1000)
    const type = AccountType2.YandexTest

    const lock = this.userAccountsMap.getAccountByType(type).tryAcquire(min10, min2)
    if (lock === null) {
      return null
    }

    const account = lock.lockedAccount()
    const token = this.oauthService.getToken(account, type)
    return new OAuthUserAccount(account, token, type)
  }

  public bindCard(user: OAuthUserAccount, card: BoundCard, serviceToken: string): XPromise<void> {
    const request = new BindNewCardRequest(
      user.oauthToken,
      serviceToken,
      card.cardNumber,
      card.expirationMonth,
      card.expirationYear,
      card.cvv,
      RegionIds.russia,
    )
    return this.diehardBackendApi.bindNewCard(request).then((_: BindNewCardResponse): void => getVoid())
  }

  public unbindCards(user: OAuthUserAccount, serviceToken: string): XPromise<void> {
    const request = new RawPaymentMethodsRequest()
    return this.mobileBackendAPI(user, serviceToken)
      .rawPaymentMethods(request)
      .then((response: RawPaymentMethodsResponse) => response.paymentMethods)
      .flatThen(
        (methods: readonly PaymentMethod[]): XPromise<void> => {
          const promises: (() => XPromise<void>)[] = []
          for (const i of range(0, methods.length)) {
            const request = new UnbindCardRequest(user.oauthToken, methods[i].identifier)
            promises.push(() => this.diehardBackendApi.unbindCard(request).then((_) => getVoid()))
          }
          return executeSequentially<void>(promises).then((_) => getVoid())
        },
      )
  }

  public getPaymentMethods(user: OAuthUserAccount, serviceToken: string): XPromise<AvailableMethods> {
    const request = new RawPaymentMethodsRequest()
    return this.mobileBackendAPI(user, serviceToken)
      .rawPaymentMethods(request)
      .then(
        (response: RawPaymentMethodsResponse): AvailableMethods =>
          new AvailableMethods(
            response.paymentMethods,
            response.applePaySupported,
            response.googlePaySupported,
            isPaymentMethodEnabled(response, 'sbp_qr'),
            false,
          ),
      )
      .flatThen(
        (methods: AvailableMethods): XPromise<AvailableMethods> => new EmptyPaymentMethodsDecorator().decorate(methods),
      )
  }

  public cleanAccountAndBindCards(user: OAuthUserAccount, cards: BoundCard[], serviceToken: string): XPromise<void> {
    return this.unbindCards(user, serviceToken)
      .then((_) => getVoid())
      .flatThen(
        (_): XPromise<void> => {
          const promises: (() => XPromise<void>)[] = []
          for (const i of range(0, cards.length)) {
            promises.push(() => this.bindCard(user, cards[i], serviceToken))
          }
          return executeSequentially(promises).then((_) => getVoid())
        },
      )
  }

  private mobileBackendAPI(user: OAuthUserAccount, serviceToken: string): MobileBackendApi {
    const interceptor = MobileBackendNetworkInterceptor.create(user.oauthToken, serviceToken, user.account.uid)
    const passportInterceptor = new PassportHeaderInterceptor(this.passportToken)
    const authorizedNetwork = new NetworkIntermediate(this.network, [interceptor, passportInterceptor])
    const errorProcessor = new MobileBackendErrorProcessor()
    const networkService = new NetworkService(authorizedNetwork, this.jsonSerializer, errorProcessor)
    return new MobileBackendApi(networkService)
  }
}

export class EmptyLogger implements Logger {
  public error(message: string): void {}

  public info(message: string): void {}

  public warn(message: string): void {}
}
