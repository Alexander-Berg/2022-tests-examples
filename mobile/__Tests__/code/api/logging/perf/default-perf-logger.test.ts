import { Int64, int64 } from '../../../../../../../common/ys'
import { DefaultPerfLogger } from '../../../../../code/api/logging/perf/default-perf-logger'
import { HighPrecisionTimer } from '../../../../../code/api/logging/perf/high-precision-timer'
import { Registry } from '../../../../../code/registry'

class TestTimer implements HighPrecisionTimer {
  private next: Int64 = int64(0)
  public getCurrentTimestampInMillis(): Int64 {
    return ++this.next
  }
}

describe(DefaultPerfLogger, () => {
  const prefix = DefaultPerfLogger.prefix
  const valueKey = DefaultPerfLogger.value
  const extraKey = DefaultPerfLogger.extra
  afterEach(Registry.drop)

  it('should collect milestones without extras', () => {
    Registry.registerHighPrecisionTimer(new TestTimer())
    const instance = DefaultPerfLogger.instance().start()
    instance.putMilestoneNoExtra('M1')
    instance.putMilestoneNoExtra('M2')
    const logPerfEvents = jest.fn()
    instance.complete(
      {
        logPerfEvents,
      },
      'E',
    )
    const expectedMilestones = new Map()
      .set('M1', new Map().set(valueKey, int64(1)))
      .set('M2', new Map().set(valueKey, int64(2)))
    expect(logPerfEvents).toBeCalledWith(`${prefix}E`, expectedMilestones)
    Registry.drop()
  })
  it('should collect milestones with extras', () => {
    Registry.registerHighPrecisionTimer(new TestTimer())
    const instance = DefaultPerfLogger.instance().start()
    instance.putMilestoneWithExtra('M1').add('X11', true).add('X12', 10)
    instance.putMilestoneWithExtra('M2').add('X21', 5.5).add('X22', 'TEST')
    const logPerfEvents = jest.fn()
    instance.complete(
      {
        logPerfEvents,
      },
      'E',
    )
    const expectedMilestones = new Map()
      .set('M1', new Map().set(valueKey, int64(1)).set(extraKey, new Map().set('X11', true).set('X12', 10)))
      .set('M2', new Map().set(valueKey, int64(2)).set(extraKey, new Map().set('X21', 5.5).set('X22', 'TEST')))
    expect(logPerfEvents).toBeCalledWith(`${prefix}E`, expectedMilestones)
    Registry.drop()
  })
  it('should not complete once completed', () => {
    Registry.registerHighPrecisionTimer(new TestTimer())
    const instance = DefaultPerfLogger.instance().start()
    const logPerfEvents = jest.fn()
    instance.complete(
      {
        logPerfEvents,
      },
      'E1',
    )
    instance.complete(
      {
        logPerfEvents,
      },
      'E2',
    )
    expect(logPerfEvents).toBeCalledTimes(1)
    expect(logPerfEvents).toBeCalledWith(`${prefix}E1`, new Map())
    Registry.drop()
  })
  it('should not add new milestones once completed', () => {
    Registry.registerHighPrecisionTimer(new TestTimer())
    const instance = DefaultPerfLogger.instance().start()
    instance.putMilestoneNoExtra('M1')
    instance.putMilestoneWithExtra('M2').add('X1', 1)
    const logPerfEvents = jest.fn()
    instance.complete(
      {
        logPerfEvents,
      },
      'E1',
    )
    instance.putMilestoneNoExtra('M3')
    instance.putMilestoneWithExtra('M4').add('X2', 2)
    instance.complete(
      {
        logPerfEvents,
      },
      'E1',
    )
    expect(logPerfEvents).toBeCalledTimes(1)
    expect(logPerfEvents).toBeCalledWith(
      `${prefix}E1`,
      new Map()
        .set('M1', new Map().set(valueKey, int64(1)))
        .set('M2', new Map().set(valueKey, int64(2)).set(extraKey, new Map().set('X1', 1))),
    )
    Registry.drop()
  })
  it('should support separate sessions ', () => {
    const timerSpy = jest
      .spyOn(Registry, 'getHighPrecisionTimer')
      .mockReturnValueOnce({
        next: 0,
        getCurrentTimestampInMillis() {
          return (this as any).next++
        },
      } as any)
      .mockReturnValueOnce({
        next: 0,
        getCurrentTimestampInMillis() {
          return (this as any).next++
        },
      } as any)
    const instance1 = DefaultPerfLogger.instance().start()
    const instance2 = DefaultPerfLogger.instance().start()
    instance1.putMilestoneNoExtra('M1')
    instance2.putMilestoneNoExtra('M2')
    const logPerfEvents = jest.fn()
    instance1.complete(
      {
        logPerfEvents,
      },
      'E1',
    )
    instance2.complete(
      {
        logPerfEvents,
      },
      'E1',
    )

    expect(logPerfEvents).toBeCalledTimes(2)
    expect(logPerfEvents.mock.calls[0]).toEqual([`${prefix}E1`, new Map().set('M1', new Map().set(valueKey, 1))])
    expect(logPerfEvents.mock.calls[1]).toEqual([`${prefix}E1`, new Map().set('M2', new Map().set(valueKey, 1))])
    timerSpy.mockRestore()
  })
})
