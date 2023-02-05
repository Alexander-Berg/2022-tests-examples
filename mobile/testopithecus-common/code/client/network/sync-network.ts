import { Result } from '../../../../common/code/result/result'
import { Int32, Nullable } from '../../../../../common/ys'
import { NetworkRequest } from '../../../../common/code/network/network-request'

export interface SyncNetwork {
  syncExecute(baseUrl: string, request: NetworkRequest, oauthToken: Nullable<string>): Result<string>

  syncExecuteWithRetries(
    retries: Int32,
    baseUrl: string,
    request: NetworkRequest,
    oauthToken: Nullable<string>,
  ): Result<string>
}
