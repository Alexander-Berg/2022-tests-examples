import { ExternalError, ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import { DiehardBackendApiError } from '../../../../code/network/diehard-backend/diehard-backend-api'
import { DiehardStatus3dsResponse } from '../../../../code/network/diehard-backend/entities/diehard-status3ds-response'

describe(DiehardBackendApiError, () => {
  it('convert DiehardBackendApiError authorization_reject', () => {
    const response = new DiehardStatus3dsResponse('authorization_reject', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.payment_authorization_reject,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'authorization_reject',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError expired_card', () => {
    const response = new DiehardStatus3dsResponse('expired_card', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.expired_card,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'expired_card',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError not_enough_funds', () => {
    const response = new DiehardStatus3dsResponse('not_enough_funds', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.not_enough_funds,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'not_enough_funds',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError fail_3ds', () => {
    const response = new DiehardStatus3dsResponse('fail_3ds', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.fail_3ds,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'fail_3ds',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError invalid_processing_request', () => {
    const response = new DiehardStatus3dsResponse('invalid_processing_request', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.invalid_processing_request,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'invalid_processing_request',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError limit_exceeded', () => {
    const response = new DiehardStatus3dsResponse('limit_exceeded', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.limit_exceeded,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'limit_exceeded',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError payment_timeout', () => {
    const response = new DiehardStatus3dsResponse('payment_timeout', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.payment_timeout,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'payment_timeout',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError promocode_already_used', () => {
    const response = new DiehardStatus3dsResponse('promocode_already_used', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.promocode_already_used,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'promocode_already_used',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError restricted_card', () => {
    const response = new DiehardStatus3dsResponse('restricted_card', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.restricted_card,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'restricted_card',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError payment_gateway_technical_error', () => {
    const response = new DiehardStatus3dsResponse('payment_gateway_technical_error', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.payment_gateway_technical_error,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'payment_gateway_technical_error',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError transaction_not_permitted', () => {
    const response = new DiehardStatus3dsResponse('transaction_not_permitted', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.transaction_not_permitted,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'transaction_not_permitted',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError user_cancelled', () => {
    const response = new DiehardStatus3dsResponse('user_cancelled', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.user_cancelled,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'user_cancelled',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError operation_cancelled', () => {
    const response = new DiehardStatus3dsResponse('operation_cancelled', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.payment_cancelled,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'operation_cancelled',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError unknown status', () => {
    const response = new DiehardStatus3dsResponse('any', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.unknown,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'any',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError too many cards binding v2.0', () => {
    const response = new DiehardStatus3dsResponse('too_many_cards', null, null, null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.too_many_cards,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'too_many_cards',
      message: error.message,
    })
  })

  it('convert DiehardBackendApiError too many cards old card_binding', () => {
    const response = new DiehardStatus3dsResponse('error', null, 'too_many_cards', null)
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.too_many_cards,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'error',
      message: error.message,
    })
  })

  it('convert status_3ds = failed', () => {
    const response = new DiehardStatus3dsResponse('error', 'technical_error', 'some desc', 'failed')
    const error = DiehardBackendApiError.fromStatus3dsResponse(response, 500)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.fail_3ds,
      trigger: ExternalErrorTrigger.diehard,
      code: 500,
      status: 'error',
      message: error.message,
    })
  })
})
