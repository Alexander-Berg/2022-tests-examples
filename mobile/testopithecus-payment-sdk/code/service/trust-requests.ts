import { ArrayJSONItem, MapJSONItem } from '../../../common/code/json/json-types'
import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
  UrlRequestEncoding,
} from '../../../common/code/network/network-request'
import { OAuthUserAccount } from '../../../testopithecus-common/code/users/user-pool'

export abstract class BaseTrustRequest extends BaseNetworkRequest {
  protected constructor(protected readonly user: OAuthUserAccount, protected readonly merchant: string) {
    super()
  }

  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public headersExtra(): MapJSONItem {
    const headers = new MapJSONItem().putString('X-Service-Token', this.merchant)
    if (this.user.account.uid.length > 0) {
      headers.putString('X-Uid', this.user.account.uid)
    }
    return headers
  }
}

export class CreateOrderRequest extends BaseTrustRequest {
  public constructor(user: OAuthUserAccount, merchant: string, private readonly product: string) {
    super(user, merchant)
  }

  public targetPath(): string {
    return 'trust-payments/v2/orders'
  }

  public params(): NetworkParams {
    return new MapJSONItem().putString('product_id', this.product)
  }
}

export class CreatePurchaseRequest extends BaseTrustRequest {
  public constructor(
    user: OAuthUserAccount,
    merchant: string,
    private readonly orderId: string,
    private readonly force3ds: boolean,
    private readonly amount: string,
    private readonly forceCvv: boolean,
  ) {
    super(user, merchant)
  }

  public targetPath(): string {
    return 'trust-payments/v2/payments'
  }

  public params(): MapJSONItem {
    return new MapJSONItem()
      .putString('return_path', 'https://yandex.ru/')
      .putString('user_email', this.user.account.login + '@yandex.ru')
      .putString('user_phone', '89998887766')
      .putInt32('wait_for_cvn', this.forceCvv ? 1 : 0)
      .put(
        'pass_params',
        new MapJSONItem().put(
          'terminal_route_data',
          new MapJSONItem().putInt32('service_force_3ds', this.force3ds ? 1 : 0),
        ),
      )
      .put(
        'orders',
        new ArrayJSONItem().add(
          new MapJSONItem()
            .putString('currency', 'RUB')
            .putString('fiscal_nds', 'nds_18')
            .putString('fiscal_title', 'test_fiscal_title')
            .putString('price', this.amount)
            .putString('service_order_id', this.orderId),
        ),
      )
  }
}

export class StartPurchaseRequest extends BaseTrustRequest {
  public static readonly PATH_MATCH_REGEX: string = 'trust-payments/v2/payments/([0-9]+)/start'

  public constructor(user: OAuthUserAccount, merchant: string, private readonly purchaseId: string) {
    super(user, merchant)
  }

  public targetPath(): string {
    return 'trust-payments/v2/payments/' + this.purchaseId + '/start'
  }

  public params(): MapJSONItem {
    return super.params().putString('purchaseId', this.purchaseId)
  }
}

export class PaymentMethodsRequest extends BaseTrustRequest {
  public constructor(user: OAuthUserAccount, merchant: string) {
    super(user, merchant)
  }

  public method(): NetworkMethod {
    return NetworkMethod.get
  }

  public targetPath(): string {
    return 'trust-payments/v2/payment-methods'
  }

  public params(): NetworkParams {
    return super.params()
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }
}
