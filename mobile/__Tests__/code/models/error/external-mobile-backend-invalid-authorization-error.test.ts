import { ExternalError, ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import { MobileBackendInvalidAuthorizationError } from '../../../../code/network/mobile-backend/mobile-backend-authorization'

describe(MobileBackendInvalidAuthorizationError, () => {
  it('convert MobileBackendInvalidAuthorizationError.tokenExchangeError', () => {
    const error = MobileBackendInvalidAuthorizationError.tokenExchangeError('message')
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.authorization,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })

  it('convert MobileBackendInvalidAuthorizationError.missingOauthError', () => {
    const error = MobileBackendInvalidAuthorizationError.missingOauthError()
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.authorization,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })

  it('convert MobileBackendInvalidAuthorizationError.missingUidError', () => {
    const error = MobileBackendInvalidAuthorizationError.missingUidError()
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.authorization,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })
})
