import {
  SealedNetworkRequest,
  NetworkMethod,
  RequestEncodingKind,
} from '../../../../../common/code/network/network-request'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { MobileBackendAdditionalHeadersInterceptor } from '../../../../code/network/mobile-backend/mobile-backend-additional-headers-interceptor'

describe(MobileBackendAdditionalHeadersInterceptor, () => {
  it('should set X-SDK-Force-CVV header', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new MobileBackendAdditionalHeadersInterceptor(true)

    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem(),
          new MapJSONItem().putInt32('X-SDK-Force-CVV', 1),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })

  it('should not change original request if parameter "forceCVV" is false', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new MobileBackendAdditionalHeadersInterceptor(false)

    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(request)
      done()
    })
  })
})
