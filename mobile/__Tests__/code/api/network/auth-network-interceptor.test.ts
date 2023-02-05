import { resolve } from '../../../../../../common/xpromise-support'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { Account } from '../../../../../mapi/code/api/entities/account'
import { Token } from '../../../../../mapi/code/api/entities/token'
import { AuthNetworkInterceptor } from '../../../../../mapi/code/api/network/auth-network-interceptor'
import {
  NetworkMethod,
  RequestEncodingKind,
  SealedNetworkRequest,
} from '../../../../../common/code/network/network-request'

describe(AuthNetworkInterceptor, () => {
  it('should update OAuth token', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new AuthNetworkInterceptor(new Account('id'), {
      obtain(account: Account) {
        return resolve(new Token('token'))
      },
    })
    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem(),
          new MapJSONItem().putString('Authorization', 'OAuth token'),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })
})
