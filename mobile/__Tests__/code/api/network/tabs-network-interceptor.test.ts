import { MapJSONItem } from '../../../../../common/code/json/json-types'
import {
  NetworkMethod,
  RequestEncodingKind,
  SealedNetworkRequest,
} from '../../../../../common/code/network/network-request'
import { TabsNetworkInterceptor } from '../../../../../mapi/code/api/network/tabs-network-interceptor'

describe(TabsNetworkInterceptor, () => {
  it('should not change request if tabs are disabled', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new TabsNetworkInterceptor(() => false)
    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toBe(request)
      done()
    })
  })
  it('should append "withTabs=1" if tabs are enabled', (done) => {
    const request = new SealedNetworkRequest(
      NetworkMethod.get,
      'path',
      new MapJSONItem(),
      new MapJSONItem(),
      new MapJSONItem(),
      { kind: RequestEncodingKind.json },
    )

    const interceptor = new TabsNetworkInterceptor(() => true)
    expect.assertions(1)
    interceptor.intercept(request).then((updatedRequest) => {
      expect(updatedRequest).toStrictEqual(
        new SealedNetworkRequest(
          NetworkMethod.get,
          'path',
          new MapJSONItem(),
          new MapJSONItem().putString('withTabs', '1'),
          new MapJSONItem(),
          { kind: RequestEncodingKind.json },
        ),
      )
      done()
    })
  })
})
