import { DefaultExposedFlagLogs } from '../../code/exposed-flag-logs'
import { MergedFlagLogs } from '../../code/flag-logs'
import { MockSharedPreferences } from '../../../common/__tests__/__helpers__/preferences-mock'

describe(DefaultExposedFlagLogs, () => {
  it('should update with known FlagLogs', () => {
    const mockPrefs = new MockSharedPreferences(
      new Map<string, Set<string>>([
        ['key #1', new Set(['1', '2', '3'])],
        ['key #2', new Set(['1', '2'])],
        ['key #3', new Set(['1'])],
        ['key #4', new Set(['1'])],
      ]),
    )

    const knownFlagLogs: MergedFlagLogs = new Map<string, Set<string>>([
      ['key #0', new Set(['0'])],
      ['key #1', new Set(['1'])],
      ['key #2', new Set(['1', '2', '3'])],
      ['key #3', new Set(['2'])],
    ])

    const exposedFlagLogs = new DefaultExposedFlagLogs(mockPrefs)
    exposedFlagLogs.updateWithKnownFlagLogs(knownFlagLogs)

    expect(mockPrefs.getAll()).toStrictEqual(
      new Map<string, Set<string>>([
        ['key #1', new Set(['1'])],
        ['key #2', new Set(['1', '2'])],
      ]),
    )
  })

  it('should append exposed logs', () => {
    const mockPrefs = new MockSharedPreferences(
      new Map<string, Set<string>>([
        ['key #1', new Set(['1', '2'])],
        ['key #2', new Set(['1', '2'])],
      ]),
    )

    const exposedFlagLogs = new DefaultExposedFlagLogs(mockPrefs)

    let updated = false

    updated = exposedFlagLogs.appendExposedLogs(new Map([['key #1', '3']]))
    expect(updated).toBe(true)
    expect(mockPrefs.getAll()).toStrictEqual(
      new Map([
        ['key #1', new Set(['1', '2', '3'])],
        ['key #2', new Set(['1', '2'])],
      ]),
    )

    updated = exposedFlagLogs.appendExposedLogs(new Map([['key #2', '2']]))
    expect(updated).toBe(false)
    expect(mockPrefs.getAll()).toStrictEqual(
      new Map([
        ['key #1', new Set(['1', '2', '3'])],
        ['key #2', new Set(['1', '2'])],
      ]),
    )

    updated = exposedFlagLogs.appendExposedLogs(new Map([['key #3', '1']]))
    expect(updated).toBe(true)
    expect(mockPrefs.getAll()).toStrictEqual(
      new Map([
        ['key #1', new Set(['1', '2', '3'])],
        ['key #2', new Set(['1', '2'])],
        ['key #3', new Set(['1'])],
      ]),
    )
  })

  it('should return all exposed flag logs', () => {
    const mockPrefs = new MockSharedPreferences(
      new Map<string, Set<string>>([
        ['key #1', new Set(['1', '2'])],
        ['key #2', new Set(['1', '2'])],
      ]),
    )

    const exposedFlagLogs = new DefaultExposedFlagLogs(mockPrefs)
    const result = exposedFlagLogs.getExposedFlagLogs()

    expect(result).toStrictEqual(
      new Map<string, Set<string>>([
        ['key #1', new Set(['1', '2'])],
        ['key #2', new Set(['1', '2'])],
      ]),
    )
  })
})
