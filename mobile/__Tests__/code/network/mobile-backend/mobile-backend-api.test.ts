import { reject, resolve } from '../../../../../../common/xpromise-support'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { MockJSONSerializer, MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { JSONItem } from '../../../../../common/code/json/json-types'
import { ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import { VerifyBindingRequest } from '../../../../code/network/mobile-backend/entities/bind/verify-binding-request'
import { VerifyBindingResponse } from '../../../../code/network/mobile-backend/entities/bind/verify-binding-response'
import { ClientPlatform } from '../../../../code/network/mobile-backend/entities/client-info'
import { PaymentsHistoryRequest } from '../../../../code/network/mobile-backend/entities/history/payments-history-request'
import { PaymentsHistoryResponse } from '../../../../code/network/mobile-backend/entities/history/payments-history-response'
import { InitPaymentRequest } from '../../../../code/network/mobile-backend/entities/init/init-payment-request'
import { InitPaymentResponse } from '../../../../code/network/mobile-backend/entities/init/init-payment-response'
import { RawPaymentMethodsRequest } from '../../../../code/network/mobile-backend/entities/methods/raw-payment-methods-request'
import { RawPaymentMethodsResponse } from '../../../../code/network/mobile-backend/entities/methods/raw-payment-methods-response'
import { MobileBackendErrorResponse } from '../../../../code/network/mobile-backend/entities/mobile-backend-error-response'
import {
  MobileBackendApi,
  MobileBackendApiError,
  MobileBackendErrorProcessor,
} from '../../../../code/network/mobile-backend/mobile-backend-api'
import { MobileBackendAuthorization } from '../../../../code/network/mobile-backend/mobile-backend-authorization'
import { NetworkService } from '../../../../code/network/network-service'
import { NetworkServiceError } from '../../../../code/network/network-service-error'
import { sample as VerifyBindingResponseSample } from './entities/bind/verify-binding-response.test'
import { sample as PaymentsHistoryResponseSample } from './entities/history/payments-history-response.test'

import { sample as InitPaymentResponseSample } from './entities/init/init-payment-response.test'

describe(MobileBackendApi, () => {
  it('should execute "initialize payment" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(InitPaymentResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as InitPaymentRequest
    const api = new MobileBackendApi(networkService)

    expect.assertions(2)
    api.initializePayment(request).then((res) => {
      expect(res).toBeInstanceOf(InitPaymentResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "get_methods" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(InitPaymentResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as RawPaymentMethodsRequest
    const api = new MobileBackendApi(networkService)

    expect.assertions(2)
    api.rawPaymentMethods(request).then((res) => {
      expect(res).toBeInstanceOf(RawPaymentMethodsResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "payments history" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(PaymentsHistoryResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as PaymentsHistoryRequest
    const api = new MobileBackendApi(networkService)

    expect.assertions(2)
    api.paymentHistory(request).then((res) => {
      expect(res).toBeInstanceOf(PaymentsHistoryResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "VerifyBinding" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(VerifyBindingResponseSample)).getValue())
    })
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as VerifyBindingRequest
    const api = new MobileBackendApi(networkService)

    expect.assertions(2)
    api.verifyBinding(request).then((res) => {
      expect(res).toBeInstanceOf(VerifyBindingResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should propagate error', (done) => {
    const performRequestMock = jest.fn().mockReturnValue(reject({ message: 'ERROR' }))
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as PaymentsHistoryRequest
    const api = new MobileBackendApi(networkService)

    expect.assertions(1)
    api.paymentHistory(request).failed((e) => {
      expect(e).toStrictEqual({ message: 'ERROR' })
      done()
    })
  })

  it('should create MobileBackendApi', () => {
    const api = MobileBackendApi.create(
      MockNetwork(),
      MockJSONSerializer(),
      'service token',
      () => resolve(new MobileBackendAuthorization('oauth', 'uid')),
      ClientPlatform.android,
      'version',
      false,
      null,
    )
    expect(api).toBeInstanceOf(MobileBackendApi)
  })
})

describe(MobileBackendErrorProcessor, () => {
  it('validateResponse: should do nothing', () => {
    const errorProcessor = new MobileBackendErrorProcessor()
    expect(errorProcessor.validateResponse({} as JSONItem)).toBeNull()
  })
  it('extractError: should extract error', () => {
    const errorProcessor = new MobileBackendErrorProcessor()
    expect(
      errorProcessor.extractError(
        JSONItemFromJSON({
          status: 'success',
          code: 100,
          req_id: 'request id',
          message: 'request message',
        }),
        0,
      ),
    ).toStrictEqual(
      new MobileBackendApiError(new MobileBackendErrorResponse('success', 100, 'request id', 'request message')),
    )
  })
  it('extractError: should return null on malformed input', () => {
    const errorProcessor = new MobileBackendErrorProcessor()
    expect(errorProcessor.extractError(JSONItemFromJSON([]), 0)).toBeNull()
  })
  it('wrapError: should return same MobileBackendApiError', () => {
    const errorProcessor = new MobileBackendErrorProcessor()
    const error = new MobileBackendApiError(new MobileBackendErrorResponse('status', 123, 'id', 'message'))
    expect(errorProcessor.wrapError(error)).toStrictEqual(error)
  })
  it('wrapError: should return wrapped NetworkServiceError', () => {
    const errorProcessor = new MobileBackendErrorProcessor()
    const error = NetworkServiceError.badStatusCode(404, 'message')
    expect(errorProcessor.wrapError(error)).toStrictEqual(
      new NetworkServiceError(ExternalErrorKind.network, ExternalErrorTrigger.mobile_backend, 404, error.message),
    )
  })
})

describe(MobileBackendApiError, () => {
  it('should build error message', () => {
    const error = new MobileBackendApiError(
      new MobileBackendErrorResponse('success', 100, 'request id', 'request message'),
    )
    expect(error.message).toBe(
      'Mobile Backend Error: code - 100, status - success in request request id: request message',
    )
  })
  it('should build error message with defaults', () => {
    const error = new MobileBackendApiError(new MobileBackendErrorResponse('success', 100, 'request id', null))
    expect(error.message).toBe(
      'Mobile Backend Error: code - 100, status - success in request request id: empty message',
    )
  })
})
