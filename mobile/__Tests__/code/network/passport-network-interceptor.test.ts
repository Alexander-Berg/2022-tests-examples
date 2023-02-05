import { MapJSONItem } from '../../../../common/code/json/json-types'
import {
  NetworkMethod,
  RequestEncodingKind,
  SealedNetworkRequest,
} from '../../../../common/code/network/network-request'
import { PassportHeaderInterceptor } from '../../../../payment-sdk/code/network/passport-network-interceptor'

describe(PassportHeaderInterceptor, () => {
  it('should update Passport token', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new PassportHeaderInterceptor('non_null_token')
    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem(),
          new MapJSONItem().putString('Webauth-Authorization', 'OAuth non_null_token'),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })
})

describe(PassportHeaderInterceptor, () => {
  it('should not update Passport token', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new PassportHeaderInterceptor(null)
    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(NetworkMethod.get, 'path', new MapJSONItem(), new MapJSONItem(), new MapJSONItem(), {
          kind: RequestEncodingKind.json,
        }),
      )
      done()
    })
  })
})
