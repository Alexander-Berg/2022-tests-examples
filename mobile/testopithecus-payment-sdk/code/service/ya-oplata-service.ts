import { Result } from '../../../common/code/result/result'
import {
  ExternalError,
  ExternalErrorTrigger,
  mobileBackendStatusToKind,
} from '../../../payment-sdk/code/models/external-error'
import { NetworkServiceError } from '../../../payment-sdk/code/network/network-service-error'
import { Int32, Nullable } from '../../../../common/ys'
import { NetworkService, NetworkServiceErrorProcessor } from '../../../payment-sdk/code/network/network-service'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { Network } from '../../../common/code/network/network'
import { XPromise } from '../../../common/code/promise/xpromise'
import { decodeJSONItem, JSONItem, MapJSONItem } from '../../../common/code/json/json-types'
import { YaOplataCreateOrderRequest } from './ya-oplata-requests'

export type PayToken = string

export class YaOplataService {
  public constructor(private readonly networkService: NetworkService) {}

  public static create(network: Network, serializer: JSONSerializer): YaOplataService {
    const errorProcessor = new YaOplataBackendErrorProcessor()
    const networkService = new NetworkService(network, serializer, errorProcessor)
    return new YaOplataService(networkService)
  }

  public createOrder(acquirerToken: string, amount: string): XPromise<PayToken> {
    return this.networkService.performRequest(new YaOplataCreateOrderRequest(acquirerToken, amount), (item) =>
      decodeJSONItem(item, (json) => {
        const map = json.tryCastAsMapJSONItem()
        const data = map.tryGet('data').tryCastAsMapJSONItem()
        return data.tryGetString('pay_token')
      }),
    )
  }
}

export class YaOplataBackendErrorProcessor implements NetworkServiceErrorProcessor {
  public extractError(errorBody: JSONItem, code: Int32): Nullable<NetworkServiceError> {
    const errorResponse = YaOplataErrorResponse.fromJsonItem(errorBody)
    if (errorResponse.isError()) {
      return null
    }
    return new YaOplataBackendError(errorResponse.getValue())
  }

  public validateResponse(body: JSONItem): Nullable<NetworkServiceError> {
    return null
  }

  public wrapError(error: NetworkServiceError): NetworkServiceError {
    return error
  }
}

export class YaOplataBackendError extends NetworkServiceError {
  public constructor(public readonly error: YaOplataErrorResponse) {
    super(
      mobileBackendStatusToKind(error.code),
      ExternalErrorTrigger.internal_sdk,
      error.code,
      `Ya Payment Backend Error: code - ${error.code}, status - ${error.status} : ${error.message ?? 'empty message'}`,
    )
  }
  public convertToExternalError(): ExternalError {
    return new ExternalError(this.kind, this.trigger, this.code, this.error.status, this.message)
  }
}

export class YaOplataErrorResponse {
  public constructor(
    public readonly status: string,
    public readonly code: Int32,
    public readonly message: Nullable<string>,
  ) {}

  public static fromJsonItem(item: JSONItem): Result<YaOplataErrorResponse> {
    return decodeJSONItem(item, (json) => {
      const map = json.tryCastAsMapJSONItem()
      const status = map.tryGetString('status')
      const code = map.tryGetInt32('code')
      const data = map.get('data') as MapJSONItem
      const message = data.getString('message')
      return new YaOplataErrorResponse(status, code, message)
    })
  }
}
