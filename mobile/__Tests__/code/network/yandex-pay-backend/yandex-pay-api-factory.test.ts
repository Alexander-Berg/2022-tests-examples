import { resolve } from '../../../../../../common/xpromise-support'
import { JSONSerializer } from '../../../../../common/code/json/json-serializer'
import { Network } from '../../../../../common/code/network/network'
import { NetworkIntermediate } from '../../../../../common/code/network/network-intermediate'
import { JsonRequestEncoding, NetworkMethod, NetworkRequest } from '../../../../../common/code/network/network-request'
import { NetworkResponse, NetworkResponseBody } from '../../../../../common/code/network/network-response'
import { resultValue } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { YandexPayApiFactory } from '../../../../code/network/yandex-pay-backend/yandex-pay-api-factory'
import { TestNetwork } from '../../../__helpers__/test-network'
import { TestSerializer } from '../../../__helpers__/test-serializer'

describe(YandexPayApiFactory, () => {
  afterEach(() => jest.restoreAllMocks())

  it('should create Y.Pay API for user with Token and UID (non-null)', async () => {
    const payload = {
      status: 'success',
      code: 200,
    }
    const result: NetworkResponse = {
      code: () => 200,
      headers: () => new Map(),
      isSuccessful: () => true,
      body: () =>
        ({
          string: () => JSON.stringify(payload),
        } as NetworkResponseBody),
    }
    const executeRaw = jest.fn().mockReturnValue(resolve(result))
    const deserialize = jest.fn().mockReturnValue(resultValue(JSONItemFromJSON(payload)))
    const factory = new YandexPayApiFactory(
      createMockInstance(TestNetwork, {
        executeRaw,
      }),
      createMockInstance(TestSerializer, {
        deserialize,
      }),
    )
    const actual = factory.createForUser('AUTH_TOKEN', 1001)

    const network: NetworkIntermediate = (actual as any).network
    expect(network).toBeInstanceOf(NetworkIntermediate)
    const baseRequest: NetworkRequest = {
      method: jest.fn().mockReturnValue(NetworkMethod.post),
      targetPath: jest.fn().mockReturnValue('path'),
      params: jest.fn().mockReturnValue(
        JSONItemFromJSON({
          item1: 'value1',
          item2: 10,
        }),
      ),
      urlExtra: jest.fn().mockReturnValue(
        JSONItemFromJSON({
          url_field: 'url_value',
        }),
      ),
      headersExtra: jest.fn().mockReturnValue(
        JSONItemFromJSON({
          header1: 'hv1',
        }),
      ),
      encoding: jest.fn().mockReturnValue(new JsonRequestEncoding()),
    }
    const actualResult = await network.executeRaw(baseRequest)
    expect(actualResult.body()!.string()).toBe(result.body()!.string())
    expect(actualResult.code()).toBe(result.code())
    expect(actualResult.headers()).toStrictEqual(result.headers())
    expect(executeRaw).toBeCalledTimes(1)
    const actualRequest: NetworkRequest = executeRaw.mock.calls[0][0]
    expect(actualRequest.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(actualRequest.headersExtra()).toStrictEqual(
      JSONItemFromJSON({
        header1: 'hv1',
        Authorization: 'OAuth AUTH_TOKEN',
      }),
    )
    expect(actualRequest.method()).toBe(NetworkMethod.post)
    expect(actualRequest.params()).toStrictEqual(
      JSONItemFromJSON({
        item1: 'value1',
        item2: 10,
      }),
    )
    expect(actualRequest.targetPath()).toBe('path')
    expect(actualRequest.urlExtra()).toStrictEqual(
      JSONItemFromJSON({
        url_field: 'url_value',
        default_uid: 1001,
      }),
    )
  })
  it('should create Y.Pay API for user with Token and UID (non-null)', async () => {
    const payload = {
      status: 'success',
      code: 200,
    }
    const result: NetworkResponse = {
      code: () => 200,
      headers: () => new Map(),
      isSuccessful: () => true,
      body: () =>
        ({
          string: () => JSON.stringify(payload),
        } as NetworkResponseBody),
    }
    const executeRaw = jest.fn().mockReturnValue(resolve(result))
    const networkMock: Network = createMockInstance(TestNetwork, {
      executeRaw,
    })
    const serializerMock: JSONSerializer = createMockInstance(TestSerializer, {
      deserialize: jest.fn().mockReturnValue(resultValue(JSONItemFromJSON(payload))),
    })
    const factory = new YandexPayApiFactory(networkMock, serializerMock)
    const actual = factory.createForUser('AUTH_TOKEN')

    const network: NetworkIntermediate = (actual as any).network
    expect(network).toBeInstanceOf(NetworkIntermediate)
    const baseRequest: NetworkRequest = {
      method: jest.fn().mockReturnValue(NetworkMethod.post),
      targetPath: jest.fn().mockReturnValue('path'),
      params: jest.fn().mockReturnValue(
        JSONItemFromJSON({
          item1: 'value1',
          item2: 10,
        }),
      ),
      urlExtra: jest.fn().mockReturnValue(
        JSONItemFromJSON({
          url_field: 'url_value',
        }),
      ),
      headersExtra: jest.fn().mockReturnValue(
        JSONItemFromJSON({
          header1: 'hv1',
        }),
      ),
      encoding: jest.fn().mockReturnValue(new JsonRequestEncoding()),
    }
    const actualResult = await network.executeRaw(baseRequest)
    expect(actualResult.body()!.string()).toBe(result.body()!.string())
    expect(actualResult.code()).toBe(result.code())
    expect(actualResult.headers()).toStrictEqual(result.headers())
    expect(executeRaw).toBeCalledTimes(1)
    const actualRequest: NetworkRequest = executeRaw.mock.calls[0][0]
    expect(actualRequest.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(actualRequest.headersExtra()).toStrictEqual(
      JSONItemFromJSON({
        header1: 'hv1',
        Authorization: 'OAuth AUTH_TOKEN',
      }),
    )
    expect(actualRequest.method()).toBe(NetworkMethod.post)
    expect(actualRequest.params()).toStrictEqual(
      JSONItemFromJSON({
        item1: 'value1',
        item2: 10,
      }),
    )
    expect(actualRequest.targetPath()).toBe('path')
    expect(actualRequest.urlExtra()).toStrictEqual(
      JSONItemFromJSON({
        url_field: 'url_value',
      }),
    )
  })
})
