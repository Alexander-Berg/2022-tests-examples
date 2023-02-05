import { YSError } from '../../../../../../common/ys'
import { CardBindingServiceError } from '../../../../code/busilogics/card-binding-service-error'
import { ExternalError, ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import { CheckBindingPaymentResponse } from '../../../../code/network/diehard-backend/entities/bind/check-binding-payment-response'

describe(CardBindingServiceError, () => {
  it('convert CardBindingServiceError.emptyOAuthToken', () => {
    const error = CardBindingServiceError.emptyOAuthToken()
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.authorization,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })

  it('convert CardBindingServiceError.undefinedStatus', () => {
    const response = new CheckBindingPaymentResponse('not_enough_funds', null, null, 'id', null, null, null)
    const error = CardBindingServiceError.undefinedStatus(response)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.not_enough_funds,
      trigger: ExternalErrorTrigger.diehard,
      code: null,
      status: 'not_enough_funds',
      message: error.message,
    })
  })

  it('convert CardBindingServiceError.challengeInvalidRedirectUrl', () => {
    const response = new CheckBindingPaymentResponse('not_enough_funds', null, null, 'id', null, null, null)
    const error = CardBindingServiceError.challengeInvalidRedirectUrl(response)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.fail_3ds,
      trigger: ExternalErrorTrigger.diehard,
      code: null,
      status: 'not_enough_funds',
      message: error.message,
    })
  })

  it('convert CardBindingServiceError.challengeHandlingError', () => {
    const response = new CheckBindingPaymentResponse('not_enough_funds', null, null, 'id', null, null, null)
    const error = CardBindingServiceError.challengeHandlingError(response, new YSError('message'))
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.fail_3ds,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: 'not_enough_funds',
      message: error.message,
    })
  })
})
