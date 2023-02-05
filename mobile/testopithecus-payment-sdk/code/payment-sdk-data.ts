import { reject, resolve } from '../../../common/xpromise-support'
import { Nullable, YSError } from '../../../common/ys'
import { JSONSerializer } from '../../common/code/json/json-serializer'
import { Network } from '../../common/code/network/network'
import { XPromise } from '../../common/code/promise/xpromise'
import { getVoid, resultValue } from '../../common/code/result/result'
import { toPromise } from '../../common/code/utils/result-utils'
import { PaymentMethodsFilter } from '../../payment-sdk/code/busilogics/payment-methods-decorator'
import { AvailableMethods } from '../../payment-sdk/code/models/available-methods'
import { Acquirer } from '../../payment-sdk/code/network/mobile-backend/entities/init/acquirer'
import {
  MerchantAddress,
  MerchantInfo,
} from '../../payment-sdk/code/network/mobile-backend/entities/init/merchant-info'
import { SyncNetwork } from '../../testopithecus-common/code/client/network/sync-network'
import {
  AccountDataPreparer,
  AccountDataPreparerProvider,
} from '../../testopithecus-common/code/mbt/test/account-data-preparer'
import { AccountType2 } from '../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModel, AppModelProvider } from '../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { OAuthUserAccount, UserAccount } from '../../testopithecus-common/code/users/user-pool'
import { BoundCard, BoundCardConstants } from './card-generator'
import { FamilyInfoMode } from './mock-backend/model/mock-data-types'
import { PaymentSdkModel } from './model/payment-sdk-model'
import { PaymentUserService } from './payment-user-service'
import { PersonalInfoMode } from './personal-info-mode'
import { AuthorizationMode } from './sample/sample-configuration'
import { MockPrepareService } from './service/mock-prepare-service'
import { TrustService } from './service/trust-service'
import { YaOplataService } from './service/ya-oplata-service'

export class PaymentDataPreparer implements AccountDataPreparer {
  public constructor(
    public readonly trustService: TrustService,
    private userService: PaymentUserService,
    private yaOplataService: YaOplataService,
    public mockPrepareService: MockPrepareService,
  ) {}

  public readonly merchantLocalizedName: string = 'name'

  private account: Nullable<OAuthUserAccount> = null
  private merchantId: string = Merchants.paymentSDK.merchantId
  private productId: string = Merchants.paymentSDK.productId
  private paymentId: string = ''
  private orderId: string = ''
  private availableMethods: AvailableMethods = AvailableMethods.EMPTY
  private boundCards: BoundCard[] = []
  private code3ds: Nullable<string> = null
  private cvv: string = BoundCardConstants.CVV
  private amount: string = '100.00'
  private forceCvv: boolean = false
  private forcedErrorType: Nullable<PaymentErrorType> = null
  private darkModeEnabled: boolean = false
  private methodsFilter: PaymentMethodsFilter = new PaymentMethodsFilter()
  private cashEnabled: boolean = false
  private personalInfoShowingMode: PersonalInfoMode = PersonalInfoMode.HIDE
  private authorizationMode: AuthorizationMode = AuthorizationMode.authorized
  private markupPurchaseNeeded: boolean = false
  private bindingV2Enabled: boolean = false
  private yaOplata: boolean = false
  private acquirer: Nullable<Acquirer> = null
  private familyInfoMode: FamilyInfoMode = FamilyInfoMode.disabled

  public prepare(account: OAuthUserAccount): XPromise<void> {
    this.account = account

    if (this.markupPurchaseNeeded) {
      return this.createPurchase(account)
        .flatThen((_) => this.cleanAccountAndBindCards(account))
        .flatThen((_) => this.startPurchase(account))
        .flatThen((_) => this.getPaymentMethodList(account))
    }
    return this.createPurchase(account)
      .flatThen((_) => this.cleanAccountAndBindCards(account))
      .flatThen((_) => this.getPaymentMethodList(account))
  }

  private createPurchase(account: OAuthUserAccount): XPromise<void> {
    return this.yaOplata
      ? this.yaOplataService
          .createOrder(getTokenForAcquirer(this.acquirer!), this.amount)
          .flatCatch((e) => reject(new YSError(`Unable to create order, error: ${e.message}`)))
          .then((token) => {
            this.paymentId = token
            return getVoid()
          })
      : this.trustService
          .createPurchase(account, this.merchantId, this.productId, this.code3ds !== null, this.amount, this.forceCvv)
          .flatCatch((e) => reject(new YSError(`Unable to obtain purchase id from TRUST, error: ${e.message}`)))
          .flatThen((purchase) => {
            this.paymentId = purchase.purchaseId
            this.orderId = purchase.orderId
            return toPromise(resultValue(getVoid()))
          })
  }

  private cleanAccountAndBindCards(account: OAuthUserAccount): XPromise<void> {
    return this.userService
      .cleanAccountAndBindCards(account, this.boundCards, this.merchantId)
      .flatCatch((e) => reject(new YSError(`Unable to clean account and bind cards, error: ${e.message}`)))
      .flatThen((_: void) => this.configureFamilyInfoModeIfNeeded())
  }

  private configureFamilyInfoModeIfNeeded(): XPromise<void> {
    if (this.familyInfoMode === FamilyInfoMode.disabled) {
      return resolve<void>(getVoid())
    }

    return this.mockPrepareService.setMockFamilyInfoMode(this.familyInfoMode).then((_: boolean): void => getVoid())
  }

  private startPurchase(account: OAuthUserAccount): XPromise<void> {
    return this.trustService
      .startPurchase(account, this.merchantId, this.paymentId)
      .flatCatch((e) => reject(new YSError(`Unable to start purchase, error: ${e.message}`)))
  }

  private getPaymentMethodList(account: OAuthUserAccount): XPromise<void> {
    return this.userService
      .getPaymentMethods(account, this.merchantId)
      .flatCatch((e) => reject(new YSError(`Unable to get payment methods, error: ${e.message}`)))
      .then((paymentMethods: AvailableMethods): void => {
        this.availableMethods = paymentMethods
          .builder()
          .setIsGooglePayAvailable(
            paymentMethods.isGooglePayAvailable && this.methodsFilter.isGooglePayAvailable && !this.yaOplata,
          )
          .setIsApplePayAvailable(
            paymentMethods.isGooglePayAvailable && this.methodsFilter.isGooglePayAvailable && !this.yaOplata,
          )
          .setIsCashAvailable(this.cashEnabled)
          .setIsSpbQrAvailable(paymentMethods.isSpbQrAvailable && this.methodsFilter.isSBPAvailable && !this.yaOplata)
          .build()
        return getVoid()
      })
  }

  public getAccount(): OAuthUserAccount {
    return this.account!
  }

  public getMerchantId(): string {
    return this.merchantId
  }

  public setMerchantId(merchantId: string): PaymentDataPreparer {
    this.merchantId = merchantId
    return this
  }

  public setBindingV2(bindingV2Enabled: boolean): PaymentDataPreparer {
    this.bindingV2Enabled = bindingV2Enabled
    return this
  }

  public isBindingV2Enabled(): boolean {
    return this.bindingV2Enabled
  }

  public getPaymentId(): string {
    return this.paymentId
  }

  public getOrderId(): string {
    return this.orderId
  }

  public getAvailableMethods(): AvailableMethods {
    return this.availableMethods
  }

  public getProductId(): string {
    return this.productId
  }

  public setProductId(productId: string): PaymentDataPreparer {
    this.productId = productId
    return this
  }

  public clearBoundCards(): void {
    this.boundCards = []
  }

  public addBoundCard(card: BoundCard): PaymentDataPreparer {
    this.boundCards.push(card)
    return this
  }

  public setFamilyInfoMode(mode: FamilyInfoMode): PaymentDataPreparer {
    this.familyInfoMode = mode
    return this
  }

  public getCvv(): string {
    return this.cvv
  }

  public setCvv(cvv: string): PaymentDataPreparer {
    this.cvv = cvv
    return this
  }

  public setUseYaOplata(use: boolean): PaymentDataPreparer {
    this.yaOplata = use
    return this
  }

  public getAcquirer(): Nullable<Acquirer> {
    return this.acquirer
  }

  public setAcquirer(acquirer: Nullable<Acquirer>): PaymentDataPreparer {
    this.acquirer = acquirer
    return this
  }

  public get3ds(): Nullable<string> {
    return this.code3ds
  }

  public set3ds(code: string): PaymentDataPreparer {
    this.code3ds = code
    return this
  }

  public enableCash(): PaymentDataPreparer {
    this.cashEnabled = true
    return this
  }

  public isCashEnabled(): boolean {
    return this.cashEnabled
  }

  public forcePaymentError(type: PaymentErrorType): PaymentDataPreparer {
    this.amount = type.toString()
    this.forcedErrorType = type
    return this
  }

  public forcedPaymentErrorType(): Nullable<PaymentErrorType> {
    return this.forcedErrorType
  }

  public getForceCvv(): boolean {
    return this.forceCvv
  }

  public setForceCvv(forceCvv: boolean): PaymentDataPreparer {
    this.forceCvv = forceCvv
    return this
  }

  public isDarkModeEnabled(): boolean {
    return this.darkModeEnabled
  }

  public setDarkMode(enabled: boolean): PaymentDataPreparer {
    this.darkModeEnabled = enabled
    return this
  }

  public getAmount(): string {
    return this.amount
  }

  public setAmount(amount: string): PaymentDataPreparer {
    this.amount = amount
    return this
  }

  public getPaymentMethodsFilter(): PaymentMethodsFilter {
    return this.methodsFilter
  }

  public setPaymentMethodsFilter(filter: PaymentMethodsFilter): PaymentDataPreparer {
    this.methodsFilter = filter
    return this
  }

  public getPersonalInfoShowingMode(): PersonalInfoMode {
    return this.personalInfoShowingMode
  }

  public setPersonalInfoShowingMode(mode: PersonalInfoMode): PaymentDataPreparer {
    this.personalInfoShowingMode = mode
    return this
  }

  public getAuthorizationMode(): AuthorizationMode {
    return this.authorizationMode
  }

  public setAuthorizationMode(mode: AuthorizationMode): PaymentDataPreparer {
    this.authorizationMode = mode
    return this
  }
}

export class PaymentSdkModelProvider implements AppModelProvider {
  public constructor(private readonly preparer: PaymentDataPreparer) {}

  public async takeAppModel(): Promise<AppModel> {
    return new PaymentSdkModel(
      this.preparer.getAccount(),
      this.preparer.getMerchantId(),
      this.preparer.getPaymentId(),
      this.preparer.getAvailableMethods(),
      this.preparer.getAmount(),
      '₽',
      this.preparer.get3ds(),
      this.preparer.getCvv() === BoundCardConstants.CVV,
      this.preparer.forcedPaymentErrorType(),
      this.preparer.getForceCvv(),
      this.preparer.getPaymentMethodsFilter(),
      this.preparer.isDarkModeEnabled(),
      this.preparer.getPersonalInfoShowingMode(),
      this.preparer.getAuthorizationMode(),
      this.preparer.isBindingV2Enabled(),
      this.preparer.isCashEnabled(),
      this.preparer.getAcquirer(),
    )
  }
}

export class PaymentDataPreparerProvider extends AccountDataPreparerProvider<PaymentDataPreparer> {
  public constructor(
    private trustNetwork: Network,
    private yaOplataNetwork: Network,
    private diehardNetwork: Network,
    private mobileBackendNetwork: Network,
    private syncNetwork: SyncNetwork,
    private jsonSerializer: JSONSerializer,
    private tusConsumer: string,
    private forcedOauthHost: Nullable<string> = null,
  ) {
    super()
  }

  public static build(
    trustNetwork: Network,
    yaOplataNetwork: Network,
    diehardNetwork: Network,
    mobileBackendNetwork: Network,
    syncNetwork: SyncNetwork,
    jsonSerializer: JSONSerializer,
    tusConsumer: string,
  ): PaymentDataPreparerProvider {
    return new PaymentDataPreparerProvider(
      trustNetwork,
      yaOplataNetwork,
      diehardNetwork,
      mobileBackendNetwork,
      syncNetwork,
      jsonSerializer,
      tusConsumer,
    )
  }

  public providePaymentDataPreparer(): PaymentDataPreparer {
    const trustService = TrustService.create(this.trustNetwork, this.jsonSerializer, null)
    const yaOplataService = YaOplataService.create(this.yaOplataNetwork, this.jsonSerializer)
    const userService = PaymentUserService.build(
      this.diehardNetwork,
      this.mobileBackendNetwork,
      this.syncNetwork,
      this.jsonSerializer,
      null,
      this.tusConsumer,
      this.forcedOauthHost,
    )
    const mockPrepareService = MockPrepareService.create(this.trustNetwork, this.jsonSerializer)
    return new PaymentDataPreparer(trustService, userService, yaOplataService, mockPrepareService)
  }

  public provide(lockedAccount: UserAccount, type: AccountType2): PaymentDataPreparer {
    return this.providePaymentDataPreparer()
  }

  public provideModelDownloader(
    fulfilledPreparers: PaymentDataPreparer[],
    accountsWithTokens: OAuthUserAccount[],
  ): AppModelProvider {
    return new PaymentSdkModelProvider(fulfilledPreparers[0])
  }
}

export enum PaymentErrorType {
  notEnoughFunds = '1099.00',
  invalidTransaction = '1092.00',
  force3ds = '1093.00',
  restrictedCard36 = '1097.00',
  restrictedCard62 = '1096.00',
  transactionNotPermittedToCard57 = '1094.00',
  transactionNotPermittedToCard58 = '1095.00',
  doNotHonor = '1090.00',
}

export class PaymentMethodName {
  public static readonly cash: string = 'Cash'
  public static readonly applePay: string = 'Apple Pay'
  public static readonly googlePay: string = 'Google Pay'
  public static readonly sbp: string = 'Faster Payments Systems'
  public static readonly otherCard: string = 'Another card'
  public static readonly familyPayPrefix: string = 'Family'
}

export class MerchantWithProductId {
  public constructor(public readonly merchantId: string, public readonly productId: string) {}
}

export class Merchants {
  public static readonly paymentSDK: MerchantWithProductId = new MerchantWithProductId(
    'payment_sdk_19d9962ddd08e7d52a2668cbcd5f7b7e',
    '6735968470625602946',
  )
  public static readonly beru: MerchantWithProductId = new MerchantWithProductId(
    'blue_market_payments_5fac16d65c83b948a5b10577f373ea7c',
    '6101710988743309398',
  )
  public static readonly zapravki: MerchantWithProductId = new MerchantWithProductId(
    'zapravki_ec6942354de13b309fd5324e965a94f9',
    '88d270966150417cb4db5093f802f5ce_goods',
  )
}

export class PaymentSdkTusConsumer {
  // Read about TUS here: https://wiki.yandex-team.ru/test-user-service/#tusconsumer
  public static readonly testTusConsumer: string = 'payment_sdk_tests'
  public static readonly sampleTusConsumer: string = 'payment_sdk_sample'
}

export function getTokenForAcquirer(acquirer: Acquirer): string {
  switch (acquirer) {
    case Acquirer.kassa:
      return '15ef6295-4eeb-4f13-8c24-b725bac3d73b'
    case Acquirer.tinkoff:
      return 'da4ad816-d160-4f47-bc08-af4b0d76756c'
  }
}

export function getAcquirerByToken(token: string): Acquirer {
  switch (token) {
    case '15ef6295-4eeb-4f13-8c24-b725bac3d73b':
      return Acquirer.kassa
    case 'da4ad816-d160-4f47-bc08-af4b0d76756c':
      return Acquirer.tinkoff
    default:
      throw new Error(`Unknown token: ${token}`)
  }
}

export function getMerchantInfoByAcquirer(acquirer: Acquirer): MerchantInfo {
  switch (acquirer) {
    case Acquirer.kassa:
      return new MerchantInfo(
        'Индивидуальный предприниматель Soft Kitty Little ball of fur',
        'c 00:00 до 05:00',
        '310287914385811',
        new MerchantAddress('Москва', 'RUS', '7', 'Вознесенский переулок', '195027'),
      )
    case Acquirer.tinkoff:
      return new MerchantInfo(
        'Индивидуальный предприниматель sdasdasd asdasda asdasd',
        'Круглосуточно',
        '1234567890123',
        new MerchantAddress('Москва', 'RUS', '16', 'Льва Толстого', '109129'),
      )
  }
}
