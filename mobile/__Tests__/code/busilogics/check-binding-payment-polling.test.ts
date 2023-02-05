/* eslint-disable @typescript-eslint/unbound-method */
import { YSError } from '../../../../../common/ys'
import { resultError, resultValue } from '../../../../common/code/result/result'
import { Uri } from '../../../../common/code/uri/uri'
import { PollingStep } from '../../../../common/code/utils/polling'
import { Uris } from '../../../../common/native-modules/native-modules'
import { ChallengeCallback } from '../../../code/busilogics/billing-service'
import { CardBindingServiceError } from '../../../code/busilogics/card-binding-service-error'
import { CheckBindingPaymentPollingHandler } from '../../../code/busilogics/check-binding-payment-polling'
import { CheckBindingPaymentResponse } from '../../../code/network/diehard-backend/entities/bind/check-binding-payment-response'

const callback: ChallengeCallback = { show3ds(uri: Uri): void {}, hide3ds(): void {} }

const Responses = {
  success: new CheckBindingPaymentResponse('success', null, null, 'card-123', null, null, null),
  waitForNotification: new CheckBindingPaymentResponse(
    'wait_for_notification',
    null,
    null,
    'card-123',
    null,
    null,
    null,
  ),
  waitForNotification3dsSuccess: new CheckBindingPaymentResponse(
    'wait_for_notification',
    null,
    null,
    'card-123',
    null,
    null,
    'success',
  ),
  waitForNotification3dsFailed: new CheckBindingPaymentResponse(
    'wait_for_notification',
    null,
    null,
    'card-123',
    null,
    null,
    'failed',
  ),
  // eslint-disable-next-line prettier/prettier
  redirect: new CheckBindingPaymentResponse(
    'wait_for_notification',
    null,
    'in progress',
    'card-123',
    null,
    'https://trust-test.yandex.ru/web/redirect_3ds',
    null,
  ),
  unknown: new CheckBindingPaymentResponse('unknown_error', null, null, 'card-123', null, null, null),
}

describe(CheckBindingPaymentPollingHandler, () => {
  it('should be done on response with "success" status', () => {
    const handler = new CheckBindingPaymentPollingHandler(callback)
    const result = handler.checkResponse(Responses.success)

    expect(result).toStrictEqual(resultValue(PollingStep.done))
  })

  it('should return error for response with unknown status', () => {
    const handler = new CheckBindingPaymentPollingHandler(callback)
    const result = handler.checkResponse(Responses.unknown)

    expect(result).toStrictEqual(resultError(CardBindingServiceError.undefinedStatus(Responses.unknown)))
  })

  it('should retry for response with "wait_for_notification" status', () => {
    const handler = new CheckBindingPaymentPollingHandler(callback)
    const result = handler.checkResponse(Responses.waitForNotification)

    expect(result).toStrictEqual(resultValue(PollingStep.retry))
  })

  it('should retry for response with "wait_for_notification"/success 3ds status', () => {
    const handler = new CheckBindingPaymentPollingHandler(callback)
    const result = handler.checkResponse(Responses.waitForNotification3dsSuccess)

    expect(result).toStrictEqual(resultValue(PollingStep.retry))
  })

  it('should retry for response with "wait_for_notification"/failed 3ds status', () => {
    const handler = new CheckBindingPaymentPollingHandler(callback)
    const result = handler.checkResponse(Responses.waitForNotification3dsFailed)

    expect(result).toStrictEqual(resultValue(PollingStep.retry))
  })

  it('should invoke callback once for response with "wait_for_notification" status and redirect url', () => {
    const callback: ChallengeCallback = { show3ds: jest.fn(), hide3ds: jest.fn() }
    const handler = new CheckBindingPaymentPollingHandler(callback)

    expect(handler.checkResponse(Responses.redirect)).toStrictEqual(resultValue(PollingStep.retry))
    expect(handler.checkResponse(Responses.redirect)).toStrictEqual(resultValue(PollingStep.retry))
    expect(callback.show3ds).toBeCalledTimes(1)
    expect(callback.show3ds).toBeCalledWith(Uris.fromString(Responses.redirect.redirectUrl!))
  })

  it('should return error if redirect url is invalid', () => {
    const handler = new CheckBindingPaymentPollingHandler(callback)
    const response = Object.assign({}, Responses.redirect, { redirectUrl: 'invalid' })
    const result = handler.checkResponse(response)

    expect(result).toStrictEqual(resultError(CardBindingServiceError.challengeInvalidRedirectUrl(response)))
  })

  it.each([
    ['YSError', new YSError('ERROR')],
    ['unknown error', 'ERROR'],
  ])('should return error if callback throws %s', (_, error) => {
    const callback: ChallengeCallback = {
      show3ds: jest.fn(() => {
        throw error
      }),
      hide3ds: jest.fn(),
    }
    const handler = new CheckBindingPaymentPollingHandler(callback)
    const result = handler.checkResponse(Responses.redirect)

    expect(result).toStrictEqual(resultError(CardBindingServiceError.challengeHandlingError(Responses.redirect, error)))
  })
})
