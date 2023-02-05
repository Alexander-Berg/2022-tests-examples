import { YSError } from '../../../../../../common/ys'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { ExternalError, ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import { NetworkServiceError } from '../../../../code/network/network-service-error'

describe(NetworkServiceError, () => {
  it('convert NetworkServiceError.transportFailure', () => {
    const error = NetworkServiceError.transportFailure(new YSError('message'))
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.network,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })

  it('convert NetworkServiceError.badStatusCode', () => {
    const error = NetworkServiceError.badStatusCode(123, 'message')
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.network,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: 123,
      status: null,
      message: error.message,
    })
  })

  it('convert NetworkServiceError.noResponseBody', () => {
    const error = NetworkServiceError.noResponseBody()
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.network,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })

  it('convert NetworkServiceError.unableToDeserialize', () => {
    const error = NetworkServiceError.unableToDeserialize(new YSError('message'))
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.network,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })

  it('convert NetworkServiceError.unableToParse', () => {
    const error = NetworkServiceError.unableToParse(new MapJSONItem(), new YSError('message'))
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.network,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })

  it('convert NetworkServiceError.pollingFailed', () => {
    const error = NetworkServiceError.pollingFailed(new YSError('message'))
    expect(ExternalError.convert(error)).toEqual({
      kind: ExternalErrorKind.network,
      trigger: ExternalErrorTrigger.internal_sdk,
      code: null,
      status: null,
      message: error.message,
    })
  })
})

describe(NetworkServiceError, () => {
  it('replace error trigger', () => {
    const error = NetworkServiceError.badStatusCode(123, 'message')
    const convertedError = error.errorWithTrigger(ExternalErrorTrigger.diehard)
    expect(convertedError.trigger).toEqual(ExternalErrorTrigger.diehard)
  })

  it('check isRetryable()', () => {
    expect(
      NetworkServiceError.badStatusCode(NetworkServiceError.STATUS_CODE_429_TOO_MANY_REQUESTS, null).isRetryable(),
    ).toBe(true)
    expect(NetworkServiceError.transportFailure(new YSError('Network connection lost')).isRetryable()).toBe(true)
    expect(NetworkServiceError.badStatusCode(404, null).isRetryable()).toBe(false)
  })
})
