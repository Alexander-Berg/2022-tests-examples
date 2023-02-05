import { YSError } from '../../../../../../common/ys'
import {
  ExternalConvertibleError,
  ExternalError,
  ExternalErrorKind,
  ExternalErrorTrigger,
} from '../../../../code/models/external-error'

describe(ExternalError, () => {
  it('build ExternalError', () => {
    const error = new ExternalError(
      ExternalErrorKind.unknown,
      ExternalErrorTrigger.internal_sdk,
      123,
      'status',
      'message',
    )
    expect(error).toEqual({
      kind: ExternalErrorKind.unknown,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: 123,
      status: 'status',
      message: 'message',
    })
  })
})

describe(ExternalError, () => {
  it('convert to ExternalError', () => {
    const error = ExternalError.convert(
      new ExternalConvertibleError(ExternalErrorKind.unknown, ExternalErrorTrigger.internal_sdk, null, null, 'message'),
    )
    expect(error).toEqual({
      kind: ExternalErrorKind.unknown,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: 'message',
    })
  })
  it('build empty ExternalError', () => {
    const error = ExternalError.convert(new YSError('message'))
    expect(error).toEqual({
      kind: ExternalErrorKind.unknown,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: 'message',
    })
  })
})
