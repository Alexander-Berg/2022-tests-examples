import {
  SealedNetworkRequest,
  NetworkMethod,
  RequestEncodingKind,
} from '../../../../../common/code/network/network-request'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { ClientPlatform } from '../../../../code/network/mobile-backend/entities/client-info'
import { MobileBackendVersionHeadersInterceptor } from '../../../../code/network/mobile-backend/mobile-backend-version-headers-interceptor'

describe(MobileBackendVersionHeadersInterceptor, () => {
  it('should set all headers', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new MobileBackendVersionHeadersInterceptor(ClientPlatform.ios, 'version')

    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem(),
          new MapJSONItem().putString('X-SDK-PLATFORM', 'ios').putString('X-SDK-VERSION', 'version'),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })
})
