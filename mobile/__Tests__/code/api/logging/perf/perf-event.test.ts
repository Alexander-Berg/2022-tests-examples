import { reject, resolve } from '../../../../../../../common/xpromise-support'
import { Int32, int64, YSError } from '../../../../../../../common/ys'
import { DefaultPerfLoggerSession } from '../../../../../code/api/logging/perf/default-perf-logger'
import { HighPrecisionTimer } from '../../../../../code/api/logging/perf/high-precision-timer'
import { PerfEvent } from '../../../../../code/api/logging/perf/perf-event'
import { PerfLogger } from '../../../../../code/api/logging/perf/perf-logger'
import { MockHighPrecisionTimer } from '../../../../__helpers__/mock-patches'

function incrementalHighPrecisionTimer(step: Int32): HighPrecisionTimer {
  let value = int64(0)
  return MockHighPrecisionTimer(() => {
    const result = value
    value += int64(step)
    return result
  })
}

function buildPerfLogger(timer: HighPrecisionTimer = MockHighPrecisionTimer()): PerfLogger {
  return {
    start: jest.fn(() => new DefaultPerfLoggerSession(timer, 'xmail_', 'value', 'extra')),
  }
}

function buildPerfMetrics(): PerfMetrics {
  return { logPerfEvents: jest.fn() }
}

export interface PerfMetrics {
  logPerfEvents(groupName: string, milestones: ReadonlyMap<string, ReadonlyMap<string, any>>): void
}

describe(PerfEvent, () => {
  it('should start perf logger', () => {
    const perfLogger = buildPerfLogger()
    // tslint:disable-next-line: no-unused-expression
    new PerfEvent('name', buildPerfMetrics(), perfLogger)
    expect(perfLogger.start).toBeCalled()
  })

  it('should put and get extras', () => {
    const event = new PerfEvent('name', buildPerfMetrics(), buildPerfLogger())
    event.putExtra('key', 'value')
    expect(event.getExtra('key')).toBe('value')
    expect(event.getExtra('unknown')).toBeNull()
  })

  it('should report success', () => {
    const timer = incrementalHighPrecisionTimer(100)
    const perfMetrics = buildPerfMetrics()
    const event = new PerfEvent('name', perfMetrics, buildPerfLogger(timer))
    event.putExtra('key', 'value')
    event.reportSuccess()
    expect(perfMetrics.logPerfEvents).toBeCalledWith(
      'xmail_name',
      new Map([
        [
          'milestone_root',
          new Map<string, any>([
            ['value', int64(100)],
            [
              'extra',
              new Map([
                ['result', 'success'],
                ['key', 'value'],
              ]),
            ],
          ]),
        ],
      ]),
    )
  })

  it('should report failure', () => {
    const timer = incrementalHighPrecisionTimer(100)
    const perfMetrics = buildPerfMetrics()
    const event = new PerfEvent('name', perfMetrics, buildPerfLogger(timer))
    event.putExtra('key', 'value')
    event.reportFailure('error message')
    expect(perfMetrics.logPerfEvents).toBeCalledWith(
      'xmail_name',
      new Map([
        [
          'milestone_root',
          new Map<string, any>([
            ['value', int64(100)],
            [
              'extra',
              new Map([
                ['result', 'failed'],
                ['error', 'error message'],
                ['key', 'value'],
              ]),
            ],
          ]),
        ],
      ]),
    )
  })

  it('should report promise success', (done) => {
    const timer = incrementalHighPrecisionTimer(100)
    const perfMetrics = buildPerfMetrics()
    const event = new PerfEvent('name', perfMetrics, buildPerfLogger(timer))
    event.putExtra('key', 'value')
    event.traceExecution(resolve(true)).then((res) => {
      expect(res).toBe(true)
      expect(perfMetrics.logPerfEvents).toBeCalledWith(
        'xmail_name',
        new Map([
          [
            'milestone_root',
            new Map<string, any>([
              ['value', int64(100)],
              [
                'extra',
                new Map([
                  ['result', 'success'],
                  ['key', 'value'],
                ]),
              ],
            ]),
          ],
        ]),
      )
      done()
    })
  })
  it('should report promise failure', (done) => {
    const timer = incrementalHighPrecisionTimer(100)
    const perfMetrics = buildPerfMetrics()
    const event = new PerfEvent('name', perfMetrics, buildPerfLogger(timer))
    event.putExtra('key', 'value')
    event.traceExecution(reject(new YSError('error message'))).failed((error) => {
      expect(error).toStrictEqual(new YSError('error message'))
      expect(perfMetrics.logPerfEvents).toBeCalledWith(
        'xmail_name',
        new Map([
          [
            'milestone_root',
            new Map<string, any>([
              ['value', int64(100)],
              [
                'extra',
                new Map([
                  ['result', 'failed'],
                  ['error', 'error message'],
                  ['key', 'value'],
                ]),
              ],
            ]),
          ],
        ]),
      )
      done()
    })
  })

  it('should report promise failure (default error message)', (done) => {
    const timer = incrementalHighPrecisionTimer(100)
    const perfMetrics = buildPerfMetrics()
    const event = new PerfEvent('name', perfMetrics, buildPerfLogger(timer))
    event.putExtra('key', 'value')
    event.traceExecution(reject(new YSError('FAILURE'))).failed((error) => {
      expect(error).toStrictEqual(new YSError('FAILURE'))
      expect(perfMetrics.logPerfEvents).toBeCalledWith(
        'xmail_name',
        new Map([
          [
            'milestone_root',
            new Map<string, any>([
              ['value', int64(100)],
              [
                'extra',
                new Map([
                  ['result', 'failed'],
                  ['error', 'FAILURE'],
                  ['key', 'value'],
                ]),
              ],
            ]),
          ],
        ]),
      )
      done()
    })
  })
})
