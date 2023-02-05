import { InvalidArgumentError } from '../../../../code/busilogics/card-validation'
import { ExternalError, ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'

describe(InvalidArgumentError, () => {
  it('convert InvalidArgumentError', () => {
    const error = new InvalidArgumentError('message')
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.card_validation_invalid_argument,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })
})
