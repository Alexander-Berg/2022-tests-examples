import { MobileBackendNetworkInterceptor } from '../../../../code/network/mobile-backend/mobile-backend-network-interceptor'
import {
  SealedNetworkRequest,
  NetworkMethod,
  RequestEncodingKind,
} from '../../../../../common/code/network/network-request'
import { MapJSONItem } from '../../../../../common/code/json/json-types'

describe(MobileBackendNetworkInterceptor, () => {
  it('should set all headers', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = MobileBackendNetworkInterceptor.create('oauthToken', 'serviceToken', 'uid')

    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem(),
          new MapJSONItem()
            .putString('Authorization', 'OAuth oauthToken')
            .putString('X-Service-Token', 'serviceToken')
            .putString('X-Uid', 'uid'),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })

  it('should skip empty headers', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = MobileBackendNetworkInterceptor.create('', 'serviceToken', '')

    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem(),
          new MapJSONItem().putString('X-Service-Token', 'serviceToken'),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })

  it('should skip null headers', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = MobileBackendNetworkInterceptor.create(null, 'serviceToken', null)

    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem(),
          new MapJSONItem().putString('X-Service-Token', 'serviceToken'),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })
})
