import { reject, resolve } from '../../../../../../common/xpromise-support'
import { YSError } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { XPromise } from '../../../../../common/code/promise/xpromise'
import { NetworkInterceptor, NetworkIntermediate } from '../../../../../common/code/network/network-intermediate'
import {
  NetworkMethod,
  NetworkRequest,
  RequestEncodingKind,
  SealedNetworkRequest,
} from '../../../../../common/code/network/network-request'

describe(NetworkIntermediate, () => {
  const networkDelegate = MockNetwork({
    resolveURL: jest.fn().mockReturnValue('RESOLVED'),
    execute: jest.fn().mockReturnValue(reject(new YSError('NO MATTER'))),
    executeRaw: jest.fn().mockReturnValue(reject(new YSError('NO MATTER'))),
  })

  class ParamsInterceptor implements NetworkInterceptor {
    public intercept(originalRequest: NetworkRequest): XPromise<NetworkRequest> {
      return resolve(
        new SealedNetworkRequest(
          originalRequest.method(),
          originalRequest.targetPath(),
          originalRequest.params().putString('new_key', 'extra_value'),
          originalRequest.urlExtra(),
          originalRequest.headersExtra(),
          originalRequest.encoding(),
        ),
      )
    }
  }

  class UrlExtraInterceptor implements NetworkInterceptor {
    public intercept(originalRequest: NetworkRequest): XPromise<NetworkRequest> {
      return resolve(
        new SealedNetworkRequest(
          originalRequest.method(),
          originalRequest.targetPath(),
          originalRequest.params(),
          originalRequest.urlExtra().putString('new_key', 'extra_value'),
          originalRequest.headersExtra(),
          originalRequest.encoding(),
        ),
      )
    }
  }

  it('should update network request on "execute()" call', (done) => {
    const baseRequest = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const network = new NetworkIntermediate(networkDelegate, [new ParamsInterceptor(), new UrlExtraInterceptor()])
    expect.assertions(1)
    network.execute(baseRequest).failed((_) => {
      expect((networkDelegate.execute as any).mock.calls[0][0]).toStrictEqual(
        new SealedNetworkRequest(
          baseRequest.method(),
          baseRequest.targetPath(),
          baseRequest.params().putString('new_key', 'extra_value'),
          baseRequest.urlExtra().putString('new_key', 'extra_value'),
          baseRequest.headersExtra(),
          baseRequest.encoding(),
        ),
      )
      done()
    })
  })
  it('should update network request on "executeRaw()" call', (done) => {
    const baseRequest = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const network = new NetworkIntermediate(networkDelegate, [new ParamsInterceptor(), new UrlExtraInterceptor()])
    expect.assertions(1)
    network.executeRaw(baseRequest).failed((_) => {
      expect((networkDelegate.execute as any).mock.calls[0][0]).toStrictEqual(
        new SealedNetworkRequest(
          baseRequest.method(),
          baseRequest.targetPath(),
          baseRequest.params().putString('new_key', 'extra_value'),
          baseRequest.urlExtra().putString('new_key', 'extra_value'),
          baseRequest.headersExtra(),
          baseRequest.encoding(),
        ),
      )
      done()
    })
  })
  it('should update network request on "resolveURL()" call', () => {
    const baseRequest = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const network = new NetworkIntermediate(networkDelegate, [new ParamsInterceptor(), new UrlExtraInterceptor()])
    network.resolveURL(baseRequest)
    expect((networkDelegate.resolveURL as any).mock.calls[0][0]).toStrictEqual(
      new SealedNetworkRequest(
        baseRequest.method(),
        baseRequest.targetPath(),
        baseRequest.params().putString('new_key', 'extra_value'),
        baseRequest.urlExtra().putString('new_key', 'extra_value'),
        baseRequest.headersExtra(),
        baseRequest.encoding(),
      ),
    )
  })
})
