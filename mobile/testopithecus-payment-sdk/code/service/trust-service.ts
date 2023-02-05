import { Nullable, YSError } from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { decodeJSONItem } from '../../../common/code/json/json-types'
import { Network } from '../../../common/code/network/network'
import { NetworkIntermediate } from '../../../common/code/network/network-intermediate'
import { XPromise } from '../../../common/code/promise/xpromise'
import { DiehardBackendErrorProcessor } from '../../../payment-sdk/code/network/diehard-backend/diehard-backend-api'
import { NetworkService } from '../../../payment-sdk/code/network/network-service'
import { PassportHeaderInterceptor } from '../../../payment-sdk/code/network/passport-network-interceptor'
import { OAuthUserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { CreateOrderRequest, CreatePurchaseRequest, StartPurchaseRequest } from './trust-requests'

export class TrustService {
  public constructor(private readonly networkService: NetworkService) {}

  public static create(network: Network, serializer: JSONSerializer, passportToken: Nullable<string>): TrustService {
    const passportInterceptor = new PassportHeaderInterceptor(passportToken)
    const authorizedNetwork = new NetworkIntermediate(network, [passportInterceptor])
    // Trust returns same status fields as Diehard so we use the same error processor
    const errorProcessor = new DiehardBackendErrorProcessor()
    const networkService = new NetworkService(authorizedNetwork, serializer, errorProcessor)
    return new TrustService(networkService)
  }

  public createPurchase(
    user: OAuthUserAccount,
    merchant: string,
    product: string,
    force3ds: boolean,
    amount: string,
    forceCvv: boolean,
  ): XPromise<Purchase> {
    return this.networkService
      .performRequest(new CreateOrderRequest(user, merchant, product), (item) =>
        decodeJSONItem(item, (json) => {
          const map = json.tryCastAsMapJSONItem()
          return map.tryGetString('order_id')
        }),
      )
      .flatThen((orderId) =>
        this.networkService.performRequest(
          new CreatePurchaseRequest(user, merchant, orderId, force3ds, amount, forceCvv),
          (item) =>
            decodeJSONItem(item, (json) => {
              const map = json.tryCastAsMapJSONItem()
              const purchaseToken = map.tryGetString('purchase_token')
              return new Purchase(orderId, purchaseToken)
            }),
        ),
      )
  }

  public startPurchase(user: OAuthUserAccount, merchant: string, purchaseId: string): XPromise<void> {
    return this.networkService.performRequest(new StartPurchaseRequest(user, merchant, purchaseId), (item) =>
      decodeJSONItem(item, (json) => {
        const status = json.tryCastAsMapJSONItem().getString('status')
        if (status !== 'success') {
          throw new YSError('Unable to start purchase')
        }
      }),
    )
  }
}

export class Purchase {
  public constructor(public readonly orderId: string, public readonly purchaseId: string) {}
}
