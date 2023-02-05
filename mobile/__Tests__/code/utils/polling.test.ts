import {
  startPolling,
  PollingStep,
  CancellationToken,
  PollingOptions,
  PollingNextIntervalStrategy,
  PollingFixedIntervalStrategy,
  PollingError,
  PollingIncrementalIntervalStrategy,
  startSuccessResultPolling,
} from '../../../code/utils/polling'
import { resolve, reject, take } from '../../../../../common/xpromise-support'
import { resultValue, resultError } from '../../../code/result/result'
import { int64 } from '../../../../../common/ys'
import { flushPromises } from '../../__helpers__/flush-promises'

describe(startPolling, () => {
  afterEach(jest.restoreAllMocks)

  it('should resolve on PollingStep.done', (done) => {
    const promise = startPolling(
      () => resolve('done'),
      () => resultValue(PollingStep.done),
    )

    expect.assertions(1)
    promise.then((res) => {
      expect(res).toBe('done')
      done()
    })
  })

  it('should reject on original promise error', (done) => {
    expect.assertions(2)
    const promise = startPolling(
      () => reject({ message: 'ERROR' }),
      (res) => {
        expect(res.getError()).toStrictEqual({ message: 'ERROR' })
        return resultValue(PollingStep.done)
      },
    )
    promise.failed((e) => {
      expect(e).toStrictEqual({ message: 'ERROR' })
      done()
    })
  })

  it('should reject on check result error', (done) => {
    const promise = startPolling(
      () => resolve('done'),
      () => resultError({ message: 'ERROR' }),
    )
    expect.assertions(1)
    promise.failed((e) => {
      expect(e).toStrictEqual({ message: 'ERROR' })
      done()
    })
  })

  it('should perform simple retry', async () => {
    jest.useFakeTimers()

    const taskFn = jest
      .fn()
      .mockReturnValueOnce(resolve('keep-polling'))
      .mockReturnValueOnce(resolve('keep-polling'))
      .mockReturnValueOnce(resolve('done'))

    const promise = startPolling(taskFn, (res) =>
      resultValue(res.getValue() === 'done' ? PollingStep.done : PollingStep.retry),
    )

    await flushPromises()
    jest.runAllTimers()
    await flushPromises()
    jest.runAllTimers()

    expect(await take(promise)).toBe('done')
    expect(setTimeout).toHaveBeenCalledTimes(2)
    expect((setTimeout as any).mock.calls).toEqual([
      [expect.any(Function), 0],
      [expect.any(Function), 0],
    ])
  })

  it('should perform simple retry on errors', async () => {
    jest.useFakeTimers()

    const taskFn = jest
      .fn()
      .mockImplementationOnce(() => reject({ message: 'keep-polling' }))
      .mockImplementationOnce(() => reject({ message: 'keep-polling-2' }))
      .mockImplementationOnce(() => resolve('done'))

    const promise = startPolling(taskFn, (res) => {
      return resultValue(res.isValue() ? PollingStep.done : PollingStep.retry)
    })

    await flushPromises()
    jest.runAllTimers()
    await flushPromises()
    jest.runAllTimers()

    expect(await take(promise)).toBe('done')
    expect(setTimeout).toHaveBeenCalledTimes(2)
    expect((setTimeout as any).mock.calls).toEqual([
      [expect.any(Function), 0],
      [expect.any(Function), 0],
    ])
  })

  it('should perform retry with interval strategy', async () => {
    jest.useFakeTimers()

    const taskFn = jest
      .fn()
      .mockReturnValueOnce(resolve('keep-polling'))
      .mockReturnValueOnce(resolve('keep-polling'))
      .mockReturnValueOnce(resolve('done'))

    const getNextIntervalMock = jest.fn().mockReturnValueOnce(int64(100)).mockReturnValueOnce(int64(200))
    const nextIntervalStrategy: PollingNextIntervalStrategy = {
      getNextIntervalMs: getNextIntervalMock,
    }

    const promise = startPolling(
      taskFn,
      (res) => resultValue(res.getValue() === 'done' ? PollingStep.done : PollingStep.retry),
      new PollingOptions(null, nextIntervalStrategy, null, null),
    )

    await flushPromises()
    jest.runAllTimers()
    await flushPromises()
    jest.runAllTimers()

    expect(await take(promise)).toBe('done')
    expect(setTimeout).toHaveBeenCalledTimes(2)
    expect((setTimeout as any).mock.calls).toEqual([
      [expect.any(Function), 100],
      [expect.any(Function), 200],
    ])
    expect(getNextIntervalMock.mock.calls).toEqual([[1], [2]])
  })

  it('should fail on cancel', async () => {
    jest.useFakeTimers()

    const taskFn = jest.fn().mockReturnValueOnce(resolve('keep-polling')).mockReturnValueOnce(resolve('done'))

    const cancelToken = new CancellationToken()
    const promise = startPolling(
      taskFn,
      (res) => resultValue(res.getValue() === 'done' ? PollingStep.done : PollingStep.retry),
      new PollingOptions(null, null, null, cancelToken),
    )

    cancelToken.cancel()

    await flushPromises()
    jest.runAllTimers()

    await expect(take(promise)).rejects.toStrictEqual(new PollingError('Polling cancelled'))
  })

  it('should fail on retries limit', async () => {
    jest.useFakeTimers()

    const taskFn = jest
      .fn()
      .mockReturnValueOnce(resolve('keep-polling'))
      .mockReturnValueOnce(resolve('keep-polling'))
      .mockReturnValueOnce(resolve('done'))

    const promise = startPolling(
      taskFn,
      (res) => resultValue(res.getValue() === 'done' ? PollingStep.done : PollingStep.retry),
      new PollingOptions(1, null, null, null),
    )

    await flushPromises()
    jest.runAllTimers()

    await expect(take(promise)).rejects.toStrictEqual(new PollingError('Maximum retries count reached'))
    expect(setTimeout).toHaveBeenCalledTimes(1)
  })

  it('should fail on time limit', async () => {
    jest.useFakeTimers()

    jest.spyOn(Date, 'now').mockReturnValueOnce(0).mockReturnValueOnce(99).mockReturnValueOnce(1000)

    const taskFn = jest
      .fn()
      .mockReturnValueOnce(resolve('keep-polling'))
      .mockReturnValueOnce(resolve('keep-polling'))
      .mockReturnValueOnce(resolve('done'))

    const promise = startPolling(
      taskFn,
      (res) => resultValue(res.getValue() === 'done' ? PollingStep.done : PollingStep.retry),
      new PollingOptions(null, null, int64(100), null),
    )

    await flushPromises()
    jest.runAllTimers()

    await expect(take(promise)).rejects.toStrictEqual(new PollingError('Timeout reached'))
    expect(setTimeout).toHaveBeenCalledTimes(1)
  })
})

describe(startSuccessResultPolling, () => {
  it('should resolve on PollingStep.done', (done) => {
    const promise = startSuccessResultPolling(
      () => resolve('done'),
      () => resultValue(PollingStep.done),
    )

    expect.assertions(1)
    promise.then((res) => {
      expect(res).toBe('done')
      done()
    })
  })

  it('should reject on original promise error', (done) => {
    expect.assertions(1)
    const promise = startSuccessResultPolling(
      () => reject({ message: 'ERROR' }),
      (res) => {
        fail('should not be called')
      },
    )
    promise.failed((e) => {
      expect(e).toStrictEqual({ message: 'ERROR' })
      done()
    })
  })
})

describe(PollingFixedIntervalStrategy, () => {
  it('should return fixed intervals', () => {
    const strategy = new PollingFixedIntervalStrategy(int64(100))
    expect(strategy.getNextIntervalMs(1)).toBe(int64(100))
    expect(strategy.getNextIntervalMs(2)).toBe(int64(100))
  })
})

describe(PollingIncrementalIntervalStrategy, () => {
  it('should return incremental intervals', () => {
    const strategy = new PollingIncrementalIntervalStrategy(int64(100))
    expect(strategy.getNextIntervalMs(1)).toBe(int64(100))
    expect(strategy.getNextIntervalMs(2)).toBe(int64(200))
  })
})
