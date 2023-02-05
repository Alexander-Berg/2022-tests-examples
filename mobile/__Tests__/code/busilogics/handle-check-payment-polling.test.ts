import {
  SbpPaymentPollingHandler,
  SbpPollingStrategy,
  RegularPaymentPollingHandler,
} from '../../../code/busilogics/handle-check-payment-polling'
import { CheckPaymentResponse } from '../../../code/network/diehard-backend/entities/check-payment/check-payment-response'
import { PollingStep } from '../../../../common/code/utils/polling'
import { resultValue, resultError } from '../../../../common/code/result/result'
import { BillingServiceError } from '../../../code/busilogics/billing-service-error'
import { YSError } from '../../../../../common/ys'
import { Uris } from '../../../../common/native-modules/native-modules'

const CheckPaymentResponses = {
  success: new CheckPaymentResponse('success', null, null, null, null, null),
  waitForNotification: new CheckPaymentResponse('wait_for_notification', null, null, null, null, null),
  // eslint-disable-next-line prettier/prettier
  redirectUrl: new CheckPaymentResponse(
    'wait_for_notification',
    null,
    null,
    'https://trust-test.yandex.ru/web/redirect_3ds',
    null,
    null,
  ),
  // eslint-disable-next-line prettier/prettier
  wait3dsSuccess: new CheckPaymentResponse(
    'wait_for_notification',
    null,
    null,
    'https://trust-test.yandex.ru/web/redirect_3ds',
    null,
    'success',
  ),
  // eslint-disable-next-line prettier/prettier
  wait3dsFailed: new CheckPaymentResponse(
    'wait_for_notification',
    null,
    null,
    'https://trust-test.yandex.ru/web/redirect_3ds',
    null,
    'failed',
  ),
  paymentFormUrl: new CheckPaymentResponse('wait_for_notification', null, null, null, 'https://qr.nspk.ru/', null),
  undefined: new CheckPaymentResponse('undefined', null, null, null, null, null),
}

describe(RegularPaymentPollingHandler, () => {
  it('should be done on response with "success" status', () => {
    const handler = new RegularPaymentPollingHandler(jest.fn(), jest.fn())
    const result = handler.checkResponse(CheckPaymentResponses.success)
    expect(result).toStrictEqual(resultValue(PollingStep.done))
  })
  it('should return error for response with undefined status', () => {
    const handler = new RegularPaymentPollingHandler(jest.fn(), jest.fn())
    const result = handler.checkResponse(CheckPaymentResponses.undefined)
    expect(result).toStrictEqual(resultError(BillingServiceError.undefinedStatus(CheckPaymentResponses.undefined)))
  })
  it('should retry for response with "wait_for_notification" status', () => {
    const handler = new RegularPaymentPollingHandler(jest.fn(), jest.fn())
    const result = handler.checkResponse(CheckPaymentResponses.waitForNotification)
    expect(result).toStrictEqual(resultValue(PollingStep.retry))
  })
  it('should invoke callback once for response with "wait_for_notification" status and redirect url', () => {
    const callback = jest.fn()
    const handler = new RegularPaymentPollingHandler(callback, jest.fn())
    expect(handler.checkResponse(CheckPaymentResponses.redirectUrl)).toStrictEqual(resultValue(PollingStep.retry))
    expect(handler.checkResponse(CheckPaymentResponses.redirectUrl)).toStrictEqual(resultValue(PollingStep.retry))
    expect(callback).toBeCalledTimes(1)
    expect(callback).toBeCalledWith(Uris.fromString(CheckPaymentResponses.redirectUrl.redirectURL!))
  })
  it('should invoke hide3ds callback once for "wait_for_notification" and 3ds success', () => {
    const hide3dsCallback = jest.fn()
    const handler = new RegularPaymentPollingHandler(jest.fn(), hide3dsCallback)
    expect(handler.checkResponse(CheckPaymentResponses.wait3dsSuccess)).toStrictEqual(resultValue(PollingStep.retry))
    expect(handler.checkResponse(CheckPaymentResponses.wait3dsSuccess)).toStrictEqual(resultValue(PollingStep.retry))
    expect(hide3dsCallback).toBeCalledTimes(1)
    expect(hide3dsCallback).toBeCalledWith('success')
  })
  it('should invoke hide3ds callback once for "wait_for_notification" and 3ds failed', () => {
    const hide3dsCallback = jest.fn()
    const handler = new RegularPaymentPollingHandler(jest.fn(), hide3dsCallback)
    expect(handler.checkResponse(CheckPaymentResponses.wait3dsFailed)).toStrictEqual(resultValue(PollingStep.retry))
    expect(handler.checkResponse(CheckPaymentResponses.wait3dsFailed)).toStrictEqual(resultValue(PollingStep.retry))
    expect(hide3dsCallback).toBeCalledTimes(1)
    expect(hide3dsCallback).toBeCalledWith('failed')
  })
  it('should return error if redirect url is invalid', () => {
    const handler = new RegularPaymentPollingHandler(jest.fn(), jest.fn())
    const response = Object.assign({}, CheckPaymentResponses.redirectUrl, { redirectURL: 'invalid' })
    const result = handler.checkResponse(response)
    expect(result).toStrictEqual(resultError(BillingServiceError.invalidUrl('invalid', 'redirectURL', response)))
  })
  it('should return error if callback throws with YSError', () => {
    const error = new YSError('ERROR')
    const handler = new RegularPaymentPollingHandler(
      jest.fn(() => {
        throw error
      }),
      jest.fn(),
    )
    const result = handler.checkResponse(CheckPaymentResponses.redirectUrl)
    expect(result).toStrictEqual(
      resultError(BillingServiceError.challengeHandlingError(CheckPaymentResponses.redirectUrl, error)),
    )
  })
  it('should return error if callback throws with error', () => {
    const error = 'ERROR'
    const checkPaymentResponse = new CheckPaymentResponse(
      'wait_for_notification',
      'code',
      'desc',
      'https://trust-test.yandex.ru/web/redirect_3ds',
      null,
      null,
    )
    const handler = new RegularPaymentPollingHandler(
      jest.fn(() => {
        throw error
      }),
      jest.fn(),
    )
    const result = handler.checkResponse(checkPaymentResponse)
    expect(result).toStrictEqual(resultError(BillingServiceError.challengeHandlingError(checkPaymentResponse, error)))
  })
  it('should return error on extracting unknown status', (done) => {
    const checkPaymentResponse = new CheckPaymentResponse(
      'some_unknown_status',
      'code',
      'desc',
      'https://testurl.testdomain',
      null,
      null,
    )
    const handler = new RegularPaymentPollingHandler(jest.fn(), jest.fn())
    expect.assertions(1)
    handler.extractPollingResult(checkPaymentResponse).failed((error) => {
      expect(error).toStrictEqual(BillingServiceError.unknownPollingStatus('some_unknown_status'))
      done()
    })
  })
})

describe(SbpPaymentPollingHandler, () => {
  it('should be done on response with "success" status', () => {
    const handler = new SbpPaymentPollingHandler(SbpPollingStrategy.resolveOnSuccess, jest.fn())
    const result = handler.checkResponse(CheckPaymentResponses.success)
    expect(result).toStrictEqual(resultValue(PollingStep.done))
  })
  it('should return error for response with undefined status', () => {
    const handler = new SbpPaymentPollingHandler(SbpPollingStrategy.resolveOnSuccess, jest.fn())
    const result = handler.checkResponse(CheckPaymentResponses.undefined)
    expect(result).toStrictEqual(resultError(BillingServiceError.undefinedStatus(CheckPaymentResponses.undefined)))
  })
  it('should retry for response with "wait_for_notification" status', () => {
    const handler = new SbpPaymentPollingHandler(SbpPollingStrategy.resolveOnSuccess, jest.fn())
    const result = handler.checkResponse(CheckPaymentResponses.waitForNotification)
    expect(result).toStrictEqual(resultValue(PollingStep.retry))
  })
  it('should invoke callback once for response with "wait_for_notification" status and payment form url', () => {
    const callback = jest.fn()
    const handler = new SbpPaymentPollingHandler(SbpPollingStrategy.resolveOnSuccess, callback)
    expect(handler.checkResponse(CheckPaymentResponses.paymentFormUrl)).toStrictEqual(resultValue(PollingStep.retry))
    expect(handler.checkResponse(CheckPaymentResponses.paymentFormUrl)).toStrictEqual(resultValue(PollingStep.retry))
    expect(callback).toBeCalledTimes(1)
    expect(callback).toBeCalledWith(Uris.fromString(CheckPaymentResponses.paymentFormUrl.paymentFormUrl!))
  })
  it(`should be done on response with "wait_for_notification" status and payment form url and "${SbpPollingStrategy.resolveOnSbpUrl}" straregy`, () => {
    const callback = jest.fn()
    const handler = new SbpPaymentPollingHandler(SbpPollingStrategy.resolveOnSbpUrl, callback)
    const result = handler.checkResponse(CheckPaymentResponses.paymentFormUrl)
    expect(result).toStrictEqual(resultValue(PollingStep.done))
    expect(callback).toBeCalledTimes(1)
    expect(callback).toBeCalledWith(Uris.fromString(CheckPaymentResponses.paymentFormUrl.paymentFormUrl!))
  })
  it('should return error if redirect url is invalid', () => {
    const handler = new SbpPaymentPollingHandler(SbpPollingStrategy.resolveOnSbpUrl, jest.fn())
    const response = Object.assign({}, CheckPaymentResponses.paymentFormUrl, { paymentFormUrl: 'invalid' })
    const result = handler.checkResponse(response)
    expect(result).toStrictEqual(resultError(BillingServiceError.invalidUrl('invalid', 'paymentFormUrl', response)))
  })
  it('should return error if callback throws with YSError', () => {
    const error = new YSError('ERROR')
    const handler = new SbpPaymentPollingHandler(
      SbpPollingStrategy.resolveOnSuccess,
      jest.fn(() => {
        throw error
      }),
    )
    const result = handler.checkResponse(CheckPaymentResponses.paymentFormUrl)
    expect(result).toStrictEqual(
      resultError(BillingServiceError.challengeHandlingError(CheckPaymentResponses.paymentFormUrl, error)),
    )
  })
  it('should return error if callback throws with error', () => {
    const error = 'ERROR'
    const handler = new SbpPaymentPollingHandler(
      SbpPollingStrategy.resolveOnSuccess,
      jest.fn(() => {
        throw error
      }),
    )
    const result = handler.checkResponse(CheckPaymentResponses.paymentFormUrl)
    expect(result).toStrictEqual(
      resultError(BillingServiceError.challengeHandlingError(CheckPaymentResponses.paymentFormUrl, error)),
    )
  })
  it('should return error on extracting unknown status', (done) => {
    const checkPaymentResponse = new CheckPaymentResponse(
      'some_unknown_status',
      'code',
      'desc',
      'https://testurl.testdomain',
      null,
      null,
    )
    const handler = new SbpPaymentPollingHandler(SbpPollingStrategy.resolveOnSbpUrl, jest.fn())
    expect.assertions(1)
    handler.extractPollingResult(checkPaymentResponse).failed((error) => {
      expect(error).toStrictEqual(BillingServiceError.unknownPollingStatus('some_unknown_status'))
      done()
    })
  })
})
