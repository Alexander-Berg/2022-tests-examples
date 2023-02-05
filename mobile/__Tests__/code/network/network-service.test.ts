import { YSError } from '../../../../../common/ys'
import { MockJSONSerializer, MockNetwork } from '../../../../common/__tests__/__helpers__/mock-patches'
import { ExternalErrorKind, ExternalErrorTrigger } from '../../../code/models/external-error'
import { NetworkService, NetworkServiceErrorProcessor } from '../../../code/network/network-service'
import { MockNetworkResponse } from '../../../../mapi/__tests__/__helpers__/mock-patches'
import { NetworkRequest } from '../../../../common/code/network/network-request'
import { reject, resolve } from '../../../../../common/xpromise-support'
import { NetworkServiceError } from '../../../code/network/network-service-error'
import { resultError, resultValue } from '../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../common/__tests__/__helpers__/json-helpers'

export function MockNetworkServiceErrorProcessor(
  patch: Partial<NetworkServiceErrorProcessor> = {},
): NetworkServiceErrorProcessor {
  return Object.assign(
    {},
    {
      extractError: jest.fn(),
      validateResponse: jest.fn(),
      wrapError: jest.fn().mockImplementation((args) => args),
    },
    patch,
  )
}

describe(NetworkService, () => {
  it('should translate "transport failure" error', (done) => {
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(reject(new YSError('NETWORK ERROR'))),
    })
    const networkService = new NetworkService(network, MockJSONSerializer(), MockNetworkServiceErrorProcessor())

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue(item))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            null,
            'Transport failure: NETWORK ERROR',
            null,
            true,
          ),
        )
        done()
      })
  })

  it('should return "empty body" error', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue(null),
      isSuccessful: jest.fn().mockReturnValue(false),
      code: jest.fn().mockReturnValue(404),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const networkService = new NetworkService(network, MockJSONSerializer(), MockNetworkServiceErrorProcessor())

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue(item))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            404,
            'Bad status code: 404: empty body',
          ),
        )
        done()
      })
  })

  it('should return "deserialize error failed" error', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue({ string: () => 'body' }),
      isSuccessful: jest.fn().mockReturnValue(false),
      code: jest.fn().mockReturnValue(404),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const serializer = MockJSONSerializer({
      deserialize: jest.fn().mockReturnValue(resultError(new YSError('DESERIALIZE FAILED'))),
    })
    const networkService = new NetworkService(network, serializer, MockNetworkServiceErrorProcessor())

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue(item))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            404,
            'Bad status code: 404: Failed to parse error body: "body", error: "DESERIALIZE FAILED"',
          ),
        )
        done()
      })
  })

  it('should return "Failed to extract error body" error', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue({ string: () => 'body' }),
      isSuccessful: jest.fn().mockReturnValue(false),
      code: jest.fn().mockReturnValue(404),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const serializer = MockJSONSerializer({
      deserialize: jest.fn().mockReturnValue(resultValue(JSONItemFromJSON({}))),
    })
    const errorProcessor = MockNetworkServiceErrorProcessor({
      extractError: jest.fn().mockReturnValue(null),
    })
    const networkService = new NetworkService(network, serializer, errorProcessor)

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue(item))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            404,
            'Bad status code: 404: Failed to extract error body: "body", json: "<JSONItem kind: map, value: {}>"',
          ),
        )
        done()
      })
  })
  it('should extract error', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue({ string: () => 'body' }),
      isSuccessful: jest.fn().mockReturnValue(false),
      code: jest.fn().mockReturnValue(404),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const serializer = MockJSONSerializer({
      deserialize: jest.fn().mockReturnValue(resultValue(JSONItemFromJSON({}))),
    })
    const errorProcessor = MockNetworkServiceErrorProcessor({
      extractError: jest
        .fn()
        .mockReturnValue(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            null,
            'EXTRACTED ERROR',
          ),
        ),
    })
    const networkService = new NetworkService(network, serializer, errorProcessor)

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue(item))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            null,
            'EXTRACTED ERROR',
          ),
        )
        done()
      })
  })

  it('should return "no response body" error', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue(null),
      isSuccessful: jest.fn().mockReturnValue(true),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const serializer = MockJSONSerializer()
    const errorProcessor = MockNetworkServiceErrorProcessor()
    const networkService = new NetworkService(network, serializer, errorProcessor)

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue(item))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            null,
            'No payload in network response',
          ),
        )
        done()
      })
  })

  it('should return "deserialize body failed" error', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue({ string: () => 'body' }),
      isSuccessful: jest.fn().mockReturnValue(true),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const serializer = MockJSONSerializer({
      deserialize: jest.fn().mockReturnValue(resultError(new YSError('DESERIALIZE FAILED'))),
    })
    const networkService = new NetworkService(network, serializer, MockNetworkServiceErrorProcessor())

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue(item))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            null,
            'Unable to deserialize JSON object: DESERIALIZE FAILED',
          ),
        )
        done()
      })
  })

  it('should fail with "validate response" error', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue({ string: () => 'body' }),
      isSuccessful: jest.fn().mockReturnValue(true),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const serializer = MockJSONSerializer({
      deserialize: jest.fn().mockReturnValue(resultValue(JSONItemFromJSON({}))),
    })
    const errorProcessor = MockNetworkServiceErrorProcessor({
      validateResponse: jest
        .fn()
        .mockReturnValue(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            null,
            'VALIDATE RESPONSE ERROR',
          ),
        ),
    })
    const networkService = new NetworkService(network, serializer, errorProcessor)

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue(item))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            null,
            'VALIDATE RESPONSE ERROR',
          ),
        )
        done()
      })
  })

  it('should fail with "json parsing" error', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue({ string: () => 'body' }),
      isSuccessful: jest.fn().mockReturnValue(true),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const serializer = MockJSONSerializer({
      deserialize: jest.fn().mockReturnValue(resultValue(JSONItemFromJSON({}))),
    })
    const errorProcessor = MockNetworkServiceErrorProcessor({
      validateResponse: jest.fn().mockReturnValue(null),
    })
    const networkService = new NetworkService(network, serializer, errorProcessor)

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultError({ message: 'ERROR' }))
      .failed((e) => {
        expect(e).toStrictEqual(
          new NetworkServiceError(
            ExternalErrorKind.network,
            ExternalErrorTrigger.internal_sdk,
            null,
            'Unable to parse JSON object: <JSONItem kind: map, value: {}>, error: ERROR',
          ),
        )
        done()
      })
  })

  it('should return parsed result', (done) => {
    const response = MockNetworkResponse({
      body: jest.fn().mockReturnValue({ string: () => 'body' }),
      isSuccessful: jest.fn().mockReturnValue(true),
    })
    const network = MockNetwork({
      executeRaw: jest.fn().mockReturnValue(resolve(response)),
    })
    const serializer = MockJSONSerializer({
      deserialize: jest.fn().mockReturnValue(resultValue(JSONItemFromJSON({}))),
    })
    const errorProcessor = MockNetworkServiceErrorProcessor({
      validateResponse: jest.fn().mockReturnValue(null),
    })
    const networkService = new NetworkService(network, serializer, errorProcessor)

    expect.assertions(1)
    networkService
      .performRequest({} as NetworkRequest, (item) => resultValue('result'))
      .then((res) => {
        expect(res).toBe('result')
        done()
      })
  })
})
