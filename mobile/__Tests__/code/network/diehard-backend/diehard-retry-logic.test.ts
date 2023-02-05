import { reject, resolve, take } from '../../../../../../common/xpromise-support'
import { range, YSError } from '../../../../../../common/ys'
import { flushPromises } from '../../../../../common/__tests__/__helpers__/flush-promises'
import { EventusRegistry } from '../../../../../eventus-common/code/eventus-registry'
import { ExternalErrorKind, ExternalErrorTrigger } from '../../../../code/models/external-error'
import { retryDiehardRequestIfNeeded } from '../../../../code/network/diehard-backend/diehard-retry-logic'
import { NetworkServiceError } from '../../../../code/network/network-service-error'

describe(retryDiehardRequestIfNeeded, () => {
  afterEach(jest.restoreAllMocks)

  it('should succeed on underlying promise success', (done) => {
    expect.assertions(2)
    const reportSpy = jest.spyOn(EventusRegistry.eventReporter(), 'report')
    retryDiehardRequestIfNeeded('request_tag', () => resolve('success')).then((res) => {
      expect(res).toBe('success')
      expect(reportSpy).not.toBeCalled()
      done()
    })
  })

  it('should fail if underlying promise resolves to an error which is not NetworkServiceError', (done) => {
    expect.assertions(2)
    const reportSpy = jest.spyOn(EventusRegistry.eventReporter(), 'report')
    retryDiehardRequestIfNeeded('request_tag', () => reject(new YSError('message'))).failed((e) => {
      expect(e).toStrictEqual(new YSError('message'))
      expect(reportSpy).not.toBeCalled()
      done()
    })
  })

  it('should fail if underlying promise resolves to an error which can not be retried', (done) => {
    expect.assertions(2)
    const reportSpy = jest.spyOn(EventusRegistry.eventReporter(), 'report')
    const nonRetryableError = NetworkServiceError.badStatusCode(400, null)
    retryDiehardRequestIfNeeded('request_tag', () => reject(nonRetryableError)).failed((e) => {
      expect(e).toBe(nonRetryableError)
      expect(reportSpy).not.toBeCalled()
      done()
    })
  })

  it('should fail on exceeding retry attempts', async () => {
    jest.useFakeTimers()

    const reportSpy = jest.spyOn(EventusRegistry.eventReporter(), 'report')
    const retryableError = NetworkServiceError.badStatusCode(
      NetworkServiceError.STATUS_CODE_429_TOO_MANY_REQUESTS,
      null,
    )
    const taskFn = jest
      .fn()
      .mockImplementationOnce(() => reject(retryableError))
      .mockImplementationOnce(() => reject(retryableError))
      .mockImplementationOnce(() => reject(retryableError))
      .mockImplementationOnce(() => reject(retryableError))

    const promise = retryDiehardRequestIfNeeded('request_tag', taskFn)

    for (const _ of range(0, 3)) {
      await flushPromises()
      jest.runAllTimers()
    }

    await expect(take(promise)).rejects.toStrictEqual(
      new NetworkServiceError(
        ExternalErrorKind.network,
        ExternalErrorTrigger.diehard,
        null,
        'Polling failed, error: Maximum retries count reached',
      ),
    )
    expect(reportSpy).toBeCalledTimes(1)
    const lastEvent = reportSpy.mock.calls[0][0]
    expect(lastEvent.name).toBe('EVENTUS_retry_diehard_request')
    expect(Object.fromEntries(lastEvent.attributes)).toMatchObject({
      request: 'request_tag',
      attempts: 4,
      result: 'failure',
    })
  })

  it('should succeed after retries', async () => {
    jest.useFakeTimers()

    const reportSpy = jest.spyOn(EventusRegistry.eventReporter(), 'report')
    const retryableError = NetworkServiceError.badStatusCode(
      NetworkServiceError.STATUS_CODE_429_TOO_MANY_REQUESTS,
      null,
    )
    const taskFn = jest
      .fn()
      .mockImplementationOnce(() => reject(retryableError))
      .mockImplementationOnce(() => reject(retryableError))
      .mockImplementationOnce(() => reject(retryableError))
      .mockImplementationOnce(() => resolve('success'))

    const promise = retryDiehardRequestIfNeeded('request_tag', taskFn)

    for (const _ of range(0, 3)) {
      await flushPromises()
      jest.runAllTimers()
    }

    expect(await take(promise)).toBe('success')
    expect(reportSpy).toBeCalledTimes(1)
    const lastEvent = reportSpy.mock.calls[0][0]
    expect(lastEvent.name).toBe('EVENTUS_retry_diehard_request')
    expect(Object.fromEntries(lastEvent.attributes)).toMatchObject({
      request: 'request_tag',
      attempts: 4,
      result: 'success',
    })
  })
})
