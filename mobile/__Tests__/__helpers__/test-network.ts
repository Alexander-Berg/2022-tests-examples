import { Nullable } from '../../../../common/ys'
import { JSONItem } from '../../../common/code/json/json-types'
import { Network } from '../../../common/code/network/network'
import { NetworkRequest } from '../../../common/code/network/network-request'
import { NetworkResponse } from '../../../common/code/network/network-response'
import { XPromise } from '../../../common/code/promise/xpromise'

export class TestNetwork implements Network {
  public execute(request: NetworkRequest): XPromise<JSONItem> {
    throw new Error('Method not implemented.')
  }
  public executeRaw(request: NetworkRequest): XPromise<NetworkResponse> {
    throw new Error('Method not implemented.')
  }
  public resolveURL(request: NetworkRequest): Nullable<string> {
    throw new Error('Method not implemented.')
  }
}
