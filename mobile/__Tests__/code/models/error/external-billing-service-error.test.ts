import { YSError } from '../../../../../../common/ys'
import { BillingServiceError } from '../../../../code/busilogics/billing-service-error'
import { ExternalError, ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import { CheckPaymentResponse } from '../../../../code/network/diehard-backend/entities/check-payment/check-payment-response'

describe(BillingServiceError, () => {
  it('convert BillingServiceError.notInitialized', () => {
    const error = BillingServiceError.notInitialized()
    const convertedError = ExternalError.convert(error)
    expect(convertedError).toEqual({
      kind: ExternalErrorKind.internal_error,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })

  it('convert BillingServiceError.challengeHandlingError', () => {
    const response = new CheckPaymentResponse('expired_card', null, null, null, null, null)
    const error = BillingServiceError.challengeHandlingError(response, new YSError('message'))
    const convertedError = ExternalError.convert(error)
    expect(convertedError).toEqual({
      kind: ExternalErrorKind.fail_3ds,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: 'expired_card',
      message: error.message,
    })
  })

  it('convert BillingServiceError.invalidUrl', () => {
    const response = new CheckPaymentResponse('expired_card', null, null, null, null, null)
    const error = BillingServiceError.invalidUrl('value', 'property', response)
    const convertedError = ExternalError.convert(error)
    expect(convertedError).toEqual({
      kind: ExternalErrorKind.fail_3ds,
      trigger: ExternalErrorTrigger.diehard,
      code: null,
      status: 'expired_card',
      message: error.message,
    })
  })
})
