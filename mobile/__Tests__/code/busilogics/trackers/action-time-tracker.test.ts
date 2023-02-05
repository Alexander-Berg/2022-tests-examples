import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { Int64, int64 } from '../../../../../../common/ys'
import { HighPrecisionTimer } from '../../../../code/api/logging/perf/high-precision-timer'
import { ActionTimeTracker } from '../../../../code/busilogics/trackers/action-time-tracker'

class TestTimer implements HighPrecisionTimer {
  public value: Int64
  public constructor(startValue: Int64 = int64(0)) {
    this.value = startValue
  }
  public getCurrentTimestampInMillis(): Int64 {
    return this.value
  }
}

function makeActionTimeTracker(timer: HighPrecisionTimer = new TestTimer()): ActionTimeTracker {
  const prefs = new MockSharedPreferences()
  return new ActionTimeTracker(prefs, timer)
}

describe(ActionTimeTracker, () => {
  it('getTime should return never on unknown', () => {
    const tracker = makeActionTimeTracker()
    expect(tracker.getTime('test')).toStrictEqual(ActionTimeTracker.NEVER)
  })
  it('getTime should return time on known', () => {
    const timer = new TestTimer(int64(100))
    const tracker = makeActionTimeTracker(timer)
    tracker.updateTime('test')
    expect(tracker.getTime('test')).toStrictEqual(int64(100))
  })
  it('updateTime should change only one action', () => {
    const timer = new TestTimer()
    const tracker = makeActionTimeTracker(timer)
    timer.value = int64(100)
    tracker.updateTime('test1')
    timer.value = int64(200)
    tracker.updateTime('test2')
    expect(tracker.getTime('test1')).toStrictEqual(int64(100))
    expect(tracker.getTime('test2')).toStrictEqual(int64(200))
  })
  it('happenedAgo true for unknown', () => {
    const timer = new TestTimer(int64(100))
    const tracker = makeActionTimeTracker(timer)
    expect(tracker.happenedAgo(int64(1), 'test')).toBeTruthy()
  })
  it('happenedAgo true for old', () => {
    const timer = new TestTimer()
    const tracker = makeActionTimeTracker(timer)
    timer.value = int64(100)
    tracker.updateTime('test')
    timer.value = int64(101 + 1000)
    expect(tracker.happenedAgo(int64(1000), 'test')).toBeTruthy()
  })
  it('happenedAgo false for recent', () => {
    const timer = new TestTimer()
    const tracker = makeActionTimeTracker(timer)
    timer.value = int64(100)
    tracker.updateTime('test')
    timer.value = int64(200)
    expect(tracker.happenedAgo(int64(1000), 'test')).toBe(false)
  })
  it('happenedAgo false for disabled', () => {
    const tracker = makeActionTimeTracker()
    tracker.updateTime('test')
    expect(tracker.happenedAgo(int64(1), 'test')).toBe(false)
  })
  it('happenedBefore false', () => {
    const tracker = makeActionTimeTracker()
    expect(tracker.happenedBefore('test')).toBe(false)
  })
  it('happenedBefore true', () => {
    const tracker = makeActionTimeTracker()
    tracker.updateTime('test')
    expect(tracker.happenedBefore('test')).toBe(true)
  })
  it('timeLeft should work', () => {
    const timer = new TestTimer()
    const tracker = makeActionTimeTracker(timer)

    const timeIntervalMillis = 20
    const currentTimeMillis = 0

    timer.value = int64(currentTimeMillis)
    tracker.updateTime('test')
    expect(tracker.timeLeft('test', int64(timeIntervalMillis))).toStrictEqual(int64(timeIntervalMillis))

    timer.value = int64(currentTimeMillis + timeIntervalMillis / 2)
    expect(tracker.timeLeft('test', int64(timeIntervalMillis))).toStrictEqual(int64(timeIntervalMillis / 2))

    timer.value = int64(currentTimeMillis + timeIntervalMillis)
    expect(tracker.timeLeft('test', int64(timeIntervalMillis))).toStrictEqual(int64(0))
  })
  it('timeLeft should return zero if action did not happen before', () => {
    const tracker = makeActionTimeTracker()
    expect(tracker.timeLeft('test', int64(20))).toStrictEqual(int64(0))
  })
  it('timeLeft can increase if current time set to past', function () {
    const timer = new TestTimer()
    const tracker = makeActionTimeTracker(timer)

    const timeIntervalMillis = 20
    const currentTimeMillis = 0

    timer.value = int64(currentTimeMillis)
    tracker.updateTime('test')

    timer.value = int64(currentTimeMillis - timeIntervalMillis / 2)
    expect(tracker.timeLeft('test', int64(timeIntervalMillis))).toStrictEqual(
      int64(timeIntervalMillis + timeIntervalMillis / 2),
    )

    timer.value = int64(currentTimeMillis - timeIntervalMillis)
    expect(tracker.timeLeft('test', int64(timeIntervalMillis))).toStrictEqual(
      int64(timeIntervalMillis + timeIntervalMillis),
    )
  })
  it('remove should remove value', () => {
    const timer = new TestTimer(int64(100))
    const tracker = makeActionTimeTracker(timer)
    tracker.updateTime('test')
    expect(tracker.getTime('test')).toStrictEqual(int64(100))
    tracker.remove('test')
    expect(tracker.getTime('test')).toStrictEqual(int64(ActionTimeTracker.NEVER))
  })
  it('should throw error', () => {
    const tracker = makeActionTimeTracker()
    expect(() => tracker.setTime('test', int64(-1))).toThrow(Error)
  })
})
