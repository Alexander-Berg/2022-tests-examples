import { JsonRequestEncoding, NetworkMethod, NetworkRequest } from '../../../../../common/code/network/network-request'
import { mapJSONItemFromObject } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { PayMobileAuthInterceptor } from '../../../../code/network/yandex-pay-backend/pay-mobile-auth-interceptor'

describe(PayMobileAuthInterceptor, () => {
  it('should attach Authorization header if token is provided', async () => {
    const interceptor = new PayMobileAuthInterceptor('SOME_TOKEN')
    const testRequest: NetworkRequest = {
      method: jest.fn().mockReturnValue(NetworkMethod.post),
      targetPath: jest.fn().mockReturnValue('some_path'),
      params: jest.fn().mockReturnValue(
        mapJSONItemFromObject({
          arg1: 'val1',
          arg2: 10,
        }),
      ),
      urlExtra: jest.fn().mockReturnValue(
        mapJSONItemFromObject({
          q1: 'v1',
          q2: true,
        }),
      ),
      headersExtra: jest.fn().mockReturnValue(
        mapJSONItemFromObject({
          p1: 's1',
          p2: 20,
        }),
      ),
      encoding: jest.fn().mockReturnValue(new JsonRequestEncoding()),
    }
    const processed = await interceptor.intercept(testRequest)
    expect(processed.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(processed.headersExtra()).toStrictEqual(
      mapJSONItemFromObject({
        p1: 's1',
        p2: 20,
        Authorization: 'OAuth SOME_TOKEN',
      }),
    )
    expect(processed.method()).toBe(NetworkMethod.post)
    expect(processed.targetPath()).toBe('some_path')
    expect(processed.params()).toStrictEqual(
      mapJSONItemFromObject({
        arg1: 'val1',
        arg2: 10,
      }),
    )
    expect(processed.urlExtra()).toStrictEqual(
      mapJSONItemFromObject({
        q1: 'v1',
        q2: true,
      }),
    )
  })
  it('should not add Authorization header if token is empty', async () => {
    const interceptor = new PayMobileAuthInterceptor('')
    const testRequest: NetworkRequest = {
      method: jest.fn().mockReturnValue(NetworkMethod.post),
      targetPath: jest.fn().mockReturnValue('some_path'),
      params: jest.fn().mockReturnValue(
        mapJSONItemFromObject({
          arg1: 'val1',
          arg2: 10,
        }),
      ),
      urlExtra: jest.fn().mockReturnValue(
        mapJSONItemFromObject({
          q1: 'v1',
          q2: true,
        }),
      ),
      headersExtra: jest.fn().mockReturnValue(
        mapJSONItemFromObject({
          p1: 's1',
          p2: 20,
        }),
      ),
      encoding: jest.fn().mockReturnValue(new JsonRequestEncoding()),
    }
    const processed = await interceptor.intercept(testRequest)
    expect(processed.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(processed.headersExtra()).toStrictEqual(
      mapJSONItemFromObject({
        p1: 's1',
        p2: 20,
      }),
    )
    expect(processed.method()).toBe(NetworkMethod.post)
    expect(processed.targetPath()).toBe('some_path')
    expect(processed.params()).toStrictEqual(
      mapJSONItemFromObject({
        arg1: 'val1',
        arg2: 10,
      }),
    )
    expect(processed.urlExtra()).toStrictEqual(
      mapJSONItemFromObject({
        q1: 'v1',
        q2: true,
      }),
    )
  })
})
