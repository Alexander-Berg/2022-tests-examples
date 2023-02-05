import { reject } from '../../../../common/xpromise-support'
import { YSError } from '../../../../common/ys'
import { XPromise } from '../../../common/code/promise/xpromise'

export function failure(message: string): YSError {
  return {
    message,
  }
}

export function rejected<T = any>(message = 'FAILED'): XPromise<T> {
  return reject(new YSError(message))
}
