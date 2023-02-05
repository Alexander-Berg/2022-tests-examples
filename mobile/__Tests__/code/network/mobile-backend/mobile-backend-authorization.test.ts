import { resultError, resultValue } from '../../../../../common/code/result/result'
import {
  MobileBackendAuthorization,
  MobileBackendInvalidAuthorizationError,
} from '../../../../code/network/mobile-backend/mobile-backend-authorization'

describe(MobileBackendInvalidAuthorizationError, () => {
  it('should return correct error messages', () => {
    expect(MobileBackendInvalidAuthorizationError.missingOauthError()).toStrictEqual(
      new MobileBackendInvalidAuthorizationError(
        'Mobile backend invalid authorization error: "Oauth" field is missing. Please, make sure that both "Oauth" & "Uid" values are provided.',
      ),
    )
    expect(MobileBackendInvalidAuthorizationError.missingUidError()).toStrictEqual(
      new MobileBackendInvalidAuthorizationError(
        'Mobile backend invalid authorization error: "Uid" field is missing. Please, make sure that both "Oauth" & "Uid" values are provided.',
      ),
    )
    expect(MobileBackendInvalidAuthorizationError.missingUidError()).toStrictEqual(
      new MobileBackendInvalidAuthorizationError(
        'Mobile backend invalid authorization error: "Uid" field is missing. Please, make sure that both "Oauth" & "Uid" values are provided.',
      ),
    )
    expect(MobileBackendInvalidAuthorizationError.tokenExchangeError('reason')).toStrictEqual(
      new MobileBackendInvalidAuthorizationError(
        'Mobile backend invalid authorization error: Oauth token exchange failure - "reason"',
      ),
    )
  })
})

describe(MobileBackendAuthorization, () => {
  it('should validate "uid" and "oauth" pair on creation', () => {
    expect(MobileBackendAuthorization.fromAuthorizationPair(null, null)).toStrictEqual(resultValue(null))
    expect(MobileBackendAuthorization.fromAuthorizationPair('foo', null)).toStrictEqual(
      resultError(MobileBackendInvalidAuthorizationError.missingUidError()),
    )
    expect(MobileBackendAuthorization.fromAuthorizationPair(null, 'bar')).toStrictEqual(
      resultError(MobileBackendInvalidAuthorizationError.missingOauthError()),
    )
    expect(MobileBackendAuthorization.fromAuthorizationPair('foo', 'bar')).toStrictEqual(
      resultValue(new MobileBackendAuthorization('foo', 'bar')),
    )
  })
})
