import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { CountingTracker } from '../../../../code/busilogics/trackers/counting-tracker'

function createCountingTracker(): CountingTracker {
  const prefs = new MockSharedPreferences()
  return new CountingTracker(prefs)
}

describe(CountingTracker, () => {
  it('getActionCount defaultValue', () => {
    const countingTracker = createCountingTracker()
    expect(countingTracker.getActionCount('action')).toStrictEqual(0)
  })
  it('getActionCount should increment count on notifyActionPerformed', () => {
    const countingTracker = createCountingTracker()
    countingTracker.notifyActionPerformed('action')
    expect(countingTracker.getActionCount('action')).toStrictEqual(1)
  })
  it('canPerformAction should return true if action performed less times than specified in maxCount', () => {
    const countingTracker = createCountingTracker()
    expect(countingTracker.canPerformAction('action', 7)).toBeTruthy()
  })
  it('canPerformAction should return false if action performed more times than specified in maxCount', () => {
    const countingTracker = createCountingTracker()
    const maxCount = 10
    for (let i = 0; i < maxCount; i++) {
      countingTracker.notifyActionPerformed('action')
    }
    expect(countingTracker.canPerformAction('action', maxCount)).toBeFalsy()
  })
  it('removeActionCounter should delete', function () {
    const countingTracker = createCountingTracker()
    countingTracker.notifyActionPerformed('action')
    expect(countingTracker.getActionCount('action')).toStrictEqual(1)
    countingTracker.removeActionCounter('action')
    expect(countingTracker.getActionCount('action')).toStrictEqual(0)
  })
})
