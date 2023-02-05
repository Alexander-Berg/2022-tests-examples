import { ExternalError, ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import { MobileBackendErrorResponse } from '../../../../code/network/mobile-backend/entities/mobile-backend-error-response'
import { MobileBackendApiError } from '../../../../code/network/mobile-backend/mobile-backend-api'

describe(MobileBackendApiError, () => {
  it('convert MobileBackendApiError.authorization', () => {
    const response = new MobileBackendErrorResponse('status', 1004, 'id', 'message')
    const error = new MobileBackendApiError(response)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.authorization,
      trigger: ExternalErrorTrigger.mobile_backend,
      code: 1004,
      status: 'status',
      message: error.message,
    })
  })
  it('convert MobileBackendApiError.unknown', () => {
    const response = new MobileBackendErrorResponse('status', 1001, 'id', 'message')
    const error = new MobileBackendApiError(response)
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.unknown,
      trigger: ExternalErrorTrigger.mobile_backend,
      code: 1001,
      status: 'status',
      message: error.message,
    })
  })
})
