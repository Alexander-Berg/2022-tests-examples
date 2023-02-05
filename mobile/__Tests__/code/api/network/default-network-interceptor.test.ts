import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { DefaultNetworkInterceptor } from '../../../../code/api/network/default-network-interceptor'
import { NetworkExtra } from '../../../../code/api/network/network-extra'
import {
  NetworkMethod,
  RequestEncodingKind,
  SealedNetworkRequest,
} from '../../../../../common/code/network/network-request'
import { platformToClient } from '../../../../../common/code/network/platform'
import { MockPlatform } from '../../../__helpers__/mock-patches'

describe(DefaultNetworkInterceptor, () => {
  it('should update values for request', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new DefaultNetworkInterceptor(
      MockPlatform(),
      'user_agent',
      () => new NetworkExtra(true, '12345'),
    )

    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem()
            .putString('client', platformToClient(MockPlatform()))
            .putString('app_state', 'foreground')
            .putString('uuid', '12345'),
          new MapJSONItem().putString('User-Agent', 'user_agent'),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })
  it('should update default values for request with empty UUID', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new DefaultNetworkInterceptor(MockPlatform(), 'user_agent', () => new NetworkExtra(false, ''))

    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem().putString('client', platformToClient(MockPlatform())).putString('app_state', 'background'),
          new MapJSONItem().putString('User-Agent', 'user_agent'),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })
})
