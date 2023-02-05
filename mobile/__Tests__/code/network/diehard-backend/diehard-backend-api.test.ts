/* eslint-disable @typescript-eslint/unbound-method */
import { reject, resolve } from '../../../../../../common/xpromise-support'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { MockJSONSerializer, MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import {
  DiehardBackendApi,
  DiehardBackendApiError,
  DiehardBackendErrorProcessor,
} from '../../../../code/network/diehard-backend/diehard-backend-api'
import { BindAppleTokenRequest } from '../../../../code/network/diehard-backend/entities/bind/bind-apple-token-request'
import { BindGooglePayTokenRequest } from '../../../../code/network/diehard-backend/entities/bind/bind-google-pay-token-request'
import { BindNewCardRequest } from '../../../../code/network/diehard-backend/entities/bind/bind-new-card-request'
import { BindNewCardResponse } from '../../../../code/network/diehard-backend/entities/bind/bind-new-card-response'
import { BindPayTokenResponse } from '../../../../code/network/diehard-backend/entities/bind/bind-pay-token-response'
import { CheckBindingPaymentResponse } from '../../../../code/network/diehard-backend/entities/bind/check-binding-payment-response'
import { NewCardBindingRequest } from '../../../../code/network/diehard-backend/entities/bind/new-card-binding-request'
import { NewCardBindingResponse } from '../../../../code/network/diehard-backend/entities/bind/new-card-binding-response'
import { UnbindCardRequest } from '../../../../code/network/diehard-backend/entities/bind/unbind-card-request'
import { UnbindCardResponse } from '../../../../code/network/diehard-backend/entities/bind/unbind-card-response'
import { CheckPaymentRequest } from '../../../../code/network/diehard-backend/entities/check-payment/check-payment-request'
import { CheckPaymentResponse } from '../../../../code/network/diehard-backend/entities/check-payment/check-payment-response'
import { DiehardStatus3dsResponse } from '../../../../code/network/diehard-backend/entities/diehard-status3ds-response'
import { SupplyApplePayRequest } from '../../../../code/network/diehard-backend/entities/supply/supply-apple-pay-request'
import { SupplyGooglePayRequest } from '../../../../code/network/diehard-backend/entities/supply/supply-google-pay-request'
import { SupplyNewCardRequest } from '../../../../code/network/diehard-backend/entities/supply/supply-new-card-request'
import { SupplyPaymentResponse } from '../../../../code/network/diehard-backend/entities/supply/supply-payment-response'
import { SupplySbpPaymentRequest } from '../../../../code/network/diehard-backend/entities/supply/supply-sbp-payment-request'
import { SupplyStoredCardRequest } from '../../../../code/network/diehard-backend/entities/supply/supply-stored-card-request'
import { NetworkService } from '../../../../code/network/network-service'
import { NetworkServiceError } from '../../../../code/network/network-service-error'

import { sample as BindNewCardResponseSample } from './entities/bind/bind-new-card-response.test'
import { sample as BindPayTokenResponseSample } from './entities/bind/bind-pay-token-response.test'
import { sample as CheckBindingPaymentResponseSample } from './entities/bind/check-binding-payment-response.test'
import { sample as NewCardBindingResponseSample } from './entities/bind/new-card-binding-response.test'
import { sample as UnbindCardResponseSample } from './entities/bind/unbind-card-response.test'
import { sample as CheckPaymentResponseSample } from './entities/check-payment/check-payment-response.test'
import { sample as SupplyPaymentResponseSample } from './entities/supply/supply-payment-response.test'

describe(DiehardBackendApi, () => {
  it('should execute "supplyStoredCard" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(SupplyPaymentResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as SupplyStoredCardRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.supplyStoredCard(request).then((res) => {
      expect(res).toBeInstanceOf(SupplyPaymentResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "supplyNewCard" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(SupplyPaymentResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as SupplyNewCardRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.supplyNewCard(request).then((res) => {
      expect(res).toBeInstanceOf(SupplyPaymentResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "supplyGooglePay" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(SupplyPaymentResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as SupplyGooglePayRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.supplyGooglePay(request).then((res) => {
      expect(res).toBeInstanceOf(SupplyPaymentResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "supplyApplePay" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(SupplyPaymentResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as SupplyApplePayRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.supplyApplePay(request).then((res) => {
      expect(res).toBeInstanceOf(SupplyPaymentResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "supplySbpPay" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(SupplyPaymentResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as SupplySbpPaymentRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.supplySbpPay(request).then((res) => {
      expect(res).toBeInstanceOf(SupplyPaymentResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "checkPayment" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(CheckPaymentResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as CheckPaymentRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.checkPayment(request).then((res) => {
      expect(res).toBeInstanceOf(CheckPaymentResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "bindNewCard" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(BindNewCardResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as BindNewCardRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.bindNewCard(request).then((res) => {
      expect(res).toBeInstanceOf(BindNewCardResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "bindAppleToken" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(BindPayTokenResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as BindAppleTokenRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.bindAppleToken(request).then((res) => {
      expect(res).toBeInstanceOf(BindPayTokenResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "bindGooglePayToken" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(BindPayTokenResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as BindGooglePayTokenRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.bindGooglePayToken(request).then((res) => {
      expect(res).toBeInstanceOf(BindPayTokenResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "unbind" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(UnbindCardResponseSample)).getValue())
    }) as NetworkService['performRequest']
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as UnbindCardRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.unbindCard(request).then((res) => {
      expect(res).toBeInstanceOf(UnbindCardResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "NewCardBinding" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(NewCardBindingResponseSample)).getValue())
    })
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as NewCardBindingRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.newCardBinding(request).then((res) => {
      expect(res).toBeInstanceOf(NewCardBindingResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should execute "checkBindingPayment" request', (done) => {
    const performRequestMock = jest.fn((_, parse) => {
      return resolve(parse(JSONItemFromJSON(CheckBindingPaymentResponseSample)).getValue())
    })
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as CheckPaymentRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(2)
    api.checkBindingPayment(request).then((res) => {
      expect(res).toBeInstanceOf(CheckBindingPaymentResponse)
      expect(performRequestMock).toBeCalledWith(request, expect.any(Function))
      done()
    })
  })

  it('should propagate error', (done) => {
    const performRequestMock = jest.fn().mockReturnValue(reject({ message: 'ERROR' }))
    const networkService = createMockInstance(NetworkService, {
      performRequest: performRequestMock,
    })

    const request = {} as BindNewCardRequest
    const api = new DiehardBackendApi(networkService)

    expect.assertions(1)
    api.bindNewCard(request).failed((e) => {
      expect(e).toStrictEqual({ message: 'ERROR' })
      done()
    })
  })

  it('should create DiehardBackendApi', () => {
    const api = DiehardBackendApi.create(MockNetwork(), MockJSONSerializer(), null)
    expect(api).toBeInstanceOf(DiehardBackendApi)
  })
})

describe(DiehardBackendErrorProcessor, () => {
  describe(DiehardBackendErrorProcessor.prototype.extractError, () => {
    it('should return null on malformed input', () => {
      const errorProcessor = new DiehardBackendErrorProcessor()
      expect(errorProcessor.extractError(JSONItemFromJSON([]), 200)).toBeNull()
    })
    it('should extract error', () => {
      const errorProcessor = new DiehardBackendErrorProcessor()
      expect(
        errorProcessor.extractError(
          JSONItemFromJSON({
            status: 'error',
            status_code: 'code',
            status_desc: 'desc',
          }),
          500,
        ),
      ).toStrictEqual(
        new DiehardBackendApiError(
          ExternalErrorKind.unknown,
          ExternalErrorTrigger.diehard,
          500,
          'error',
          'Diehard Error: http_code - 500, status - error, status_code - code, status_3ds - N/A, description - desc',
        ),
      )
    })
  })

  describe(DiehardBackendErrorProcessor.prototype.validateResponse, () => {
    it('should return null on malformed input', () => {
      const errorProcessor = new DiehardBackendErrorProcessor()
      expect(errorProcessor.validateResponse(JSONItemFromJSON([]))).toBeNull()
    })
    it.each(['success', 'wait_for_notification', 'wait_for_processing'])(
      'should validate regular response',
      (status) => {
        const errorProcessor = new DiehardBackendErrorProcessor()
        expect(
          errorProcessor.validateResponse(
            JSONItemFromJSON({
              status,
              status_code: 'code',
              status_desc: 'desc',
            }),
          ),
        ).toBeNull()
      },
    )
    it('should validate error response', () => {
      const errorProcessor = new DiehardBackendErrorProcessor()
      expect(
        errorProcessor.validateResponse(
          JSONItemFromJSON({
            status: 'error',
            status_code: 'code',
            status_desc: 'desc',
          }),
        ),
      ).toStrictEqual(
        DiehardBackendApiError.fromStatus3dsResponse(new DiehardStatus3dsResponse('error', 'code', 'desc', null), 200),
      )
    })
  })
  describe(DiehardBackendErrorProcessor.prototype.wrapError, () => {
    it('should return same error for DiehardBackendApiError', () => {
      const errorProcessor = new DiehardBackendErrorProcessor()
      const error = DiehardBackendApiError.fromStatus3dsResponse(
        new DiehardStatus3dsResponse('status', null, null, null),
        200,
      )
      expect(errorProcessor.wrapError(error)).toStrictEqual(error)
    })
    it('should extract error', () => {
      const errorProcessor = new DiehardBackendErrorProcessor()
      const error = NetworkServiceError.badStatusCode(404, 'message')
      expect(errorProcessor.wrapError(error)).toStrictEqual(
        new NetworkServiceError(ExternalErrorKind.network, ExternalErrorTrigger.diehard, 404, error.message),
      )
    })
  })
})

describe(DiehardBackendApiError, () => {
  it('should build error message', () => {
    const error = DiehardBackendApiError.fromStatus3dsResponse(
      new DiehardStatus3dsResponse('success', 'code', 'desc', null),
      500,
    )
    expect(error.message).toBe(
      'Diehard Error: http_code - 500, status - success, status_code - code, status_3ds - N/A, description - desc',
    )
  })
  it('should build error message with defaults', () => {
    const error = DiehardBackendApiError.fromStatus3dsResponse(
      new DiehardStatus3dsResponse('error', null, null, null),
      500,
    )
    expect(error.message).toBe(
      'Diehard Error: http_code - 500, status - error, status_code - N/A, status_3ds - N/A, description - N/A',
    )
  })
  it('should build error message with 3ds', () => {
    const error = DiehardBackendApiError.fromStatus3dsResponse(
      new DiehardStatus3dsResponse('error', 'technical_error', null, 'failed'),
      500,
    )
    expect(error.message).toBe(
      'Diehard Error: http_code - 500, status - error, status_code - technical_error, status_3ds - failed, description - N/A',
    )
  })
})
