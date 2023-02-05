import { reject } from '../../../../common/xpromise-support'
import { Int32, Nullable, range, Throwing, YSError } from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { Network } from '../../../common/code/network/network'
import { XPromise } from '../../../common/code/promise/xpromise'
import { BankName } from '../../../payment-sdk/code/busilogics/bank-name'
import { Merchant } from '../../../payment-sdk/code/models/merchant'
import { Payer } from '../../../payment-sdk/code/models/payer'
import { Acquirer } from '../../../payment-sdk/code/network/mobile-backend/entities/init/acquirer'
import { SyncNetwork } from '../../../testopithecus-common/code/client/network/sync-network'
import { AccountType2 } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { OAuthUserAccount, UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { extractErrorMessage } from '../../../testopithecus-common/code/utils/utils'
import { BoundCard } from '../card-generator'
import { FamilyInfoMode } from '../mock-backend/model/mock-data-types'
import { PaymentDataPreparer } from '../payment-sdk-data'
import { PaymentUserService } from '../payment-user-service'
import { MockPrepareService } from '../service/mock-prepare-service'
import { TrustService } from '../service/trust-service'
import { YaOplataService } from '../service/ya-oplata-service'
import { PaymentConfig } from './payment-config'

export class SampleConfiguration {
  public static SERVICE_TOKEN: string = 'payment_sdk_19d9962ddd08e7d52a2668cbcd5f7b7e'
  public static ANY_EMAIL: string = 'test.account@ya.ru'
  public static ANY_3DS_CODE: string = '123'

  private authorizationMode: AuthorizationMode = AuthorizationMode.authorized
  private providedAccount: Nullable<OAuthUserAccount> = null
  private boundCardsCount: Int32 = 1

  public static build(
    trustNetwork: Network,
    yaOplataNetwork: Network,
    diehardNetwork: Network,
    mobileBackendNetwork: Network,
    syncNetwork: SyncNetwork,
    jsonSerializer: JSONSerializer,
    passportToken: Nullable<string>,
    tusConsumer: string,
    forcedAuthHost: Nullable<string> = null,
    forced3ds: boolean = false,
  ): SampleConfiguration {
    const trustService = TrustService.create(trustNetwork, jsonSerializer, passportToken)
    const yaOplataService = YaOplataService.create(yaOplataNetwork, jsonSerializer)
    const userService = PaymentUserService.build(
      diehardNetwork,
      mobileBackendNetwork,
      syncNetwork,
      jsonSerializer,
      passportToken,
      tusConsumer,
      forcedAuthHost,
    )
    const mockPrepareService = MockPrepareService.create(trustNetwork, jsonSerializer)
    const paymentDataPreparer = new PaymentDataPreparer(trustService, userService, yaOplataService, mockPrepareService)
    if (forced3ds) {
      paymentDataPreparer.set3ds(SampleConfiguration.ANY_3DS_CODE)
    }
    return new SampleConfiguration(paymentDataPreparer, userService)
  }

  public constructor(
    private readonly paymentDataPreparer: PaymentDataPreparer,
    private readonly userService: PaymentUserService,
  ) {}

  public setBoundCardsCount(count: Int32): SampleConfiguration {
    this.boundCardsCount = count
    return this
  }

  public setAuthorizationMode(mode: AuthorizationMode): SampleConfiguration {
    this.authorizationMode = mode
    return this
  }

  public setProvidedAccount(providedAccount: OAuthUserAccount): SampleConfiguration {
    this.providedAccount = providedAccount
    return this
  }

  public configuration(): XPromise<PaymentConfig> {
    let userAccount: Nullable<OAuthUserAccount>
    try {
      userAccount = this.getUserAccount()
    } catch (e) {
      return reject(new YSError(`Unable to get authorized user, error ${extractErrorMessage(e)}`))
    }
    if (userAccount === null) {
      return reject(new YSError('Unable to get authorized user, account is null'))
    }
    this.paymentDataPreparer.setMerchantId(SampleConfiguration.SERVICE_TOKEN)
    this.paymentDataPreparer.clearBoundCards()
    for (const _ of range(0, this.boundCardsCount)) {
      this.paymentDataPreparer.addBoundCard(BoundCard.generated())
    }
    return this.paymentDataPreparer
      .prepare(userAccount)
      .then(
        (_): PaymentConfig =>
          new PaymentConfig(
            new Payer(userAccount!.oauthToken, userAccount!.account.uid, userAccount!.account.login + '@yandex.ru'),
            new Merchant(this.paymentDataPreparer.getMerchantId(), this.paymentDataPreparer.merchantLocalizedName),
            this.paymentDataPreparer.getPaymentId(),
            this.paymentDataPreparer.getOrderId(),
          ),
      )
  }

  public setMockBank(bank: BankName): XPromise<boolean> {
    return this.paymentDataPreparer.mockPrepareService.setMockBank(bank)
  }

  public setMockFamilyInfoMode(mode: FamilyInfoMode): XPromise<boolean> {
    return this.paymentDataPreparer.mockPrepareService.setMockFamilyInfoMode(mode)
  }

  public useYaOplata(use: boolean): void {
    this.paymentDataPreparer.setUseYaOplata(use)
    this.paymentDataPreparer.setAcquirer(use ? Acquirer.kassa : null)
  }

  private getUserAccount(): Throwing<Nullable<OAuthUserAccount>> {
    switch (this.authorizationMode) {
      case AuthorizationMode.authorized:
        return this.userService.getAuthorizedUser()
      case AuthorizationMode.nonauthorized:
        const account = new UserAccount(SampleConfiguration.ANY_EMAIL, '', '')
        return new OAuthUserAccount(account, null, AccountType2.YandexTest)
      case AuthorizationMode.provided:
        return this.providedAccount
    }
  }
}

export enum AuthorizationMode {
  authorized,
  nonauthorized,
  provided,
}
