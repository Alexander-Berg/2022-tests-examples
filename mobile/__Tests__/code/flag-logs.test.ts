/* eslint-disable @typescript-eslint/unbound-method */
import {
  MockJSONSerializerWrapper,
  RealJSONSerializerWrapper,
} from '../../../common/__tests__/__helpers__/mock-patches'
import { Result } from '../../../common/code/result/result'
import { ExposedFlagLogs } from '../../code/exposed-flag-logs'
import { FlagName } from '../../code/flag'
import {
  filterFlagLogsByRegisteredFlags,
  FlagLogs,
  FlagLogsByFlagNames,
  flagLogsByFlagNamesFromConfugurations,
  FlagsLogger,
  MergedFlagLogs,
  mergeFlagLogsArray,
  MetricaEnvironment,
  prepareMergedFlagLogsForMetricaLogging,
} from '../../code/flag-logs'
import { makeFlagsResponse } from './flags-configuration.test'

function buildExposedFlagLogs(patch: Partial<ExposedFlagLogs> = {}): ExposedFlagLogs {
  return Object.assign(
    {},
    {
      updateWithKnownFlagLogs: jest.fn(),
      appendExposedLogs: jest.fn(),
      getExposedFlagLogs: jest.fn(),
    },
    patch,
  )
}

function buildMetricaEnvironment(patch: Partial<MetricaEnvironment> = {}): MetricaEnvironment {
  return Object.assign(
    {},
    {
      setEnvironmentValue: jest.fn(),
      setEnvironmentValues: jest.fn(),
    },
    patch,
  )
}

describe(FlagsLogger, () => {
  describe(FlagsLogger.prototype.updateKnownFlagLogs, () => {
    it('should update active logs', () => {
      const response = makeFlagsResponse(
        [
          {
            HANDLER: 'MOBMAIL',
            CONTEXT: {
              MOBMAIL: {
                source: 'global',
                logs: { ['global config: log key']: 'global config: log value' },
                flags: {
                  ['global config: flag #1']: 'global config: string flag #1 value',
                  ['global config: flag #2']: 'global config: string flag #2 value',
                },
              },
            },
          },
          {
            HANDLER: 'MOBMAIL',
            CONTEXT: {
              MOBMAIL: {
                source: 'experiment',
                logs: { ['experiment config #1: log key']: 'experiment config #1: log value' },
                flags: {
                  ['experiment config #1: flag #1']: 'experiment config #1: string flag #1 value',
                },
              },
            },
          },
          {
            HANDLER: 'MOBMAIL',
            CONTEXT: {
              MOBMAIL: {
                source: 'experiment',
                logs: { ['experiment config #2: log key']: 'experiment config #2: log value' },
                flags: {
                  ['experiment config #2: flag #1']: 'experiment config #2: string flag #2 value',
                  ['experiment config #2: flag #2']: 'experiment config #2: string flag #3 value',
                },
              },
            },
          },
        ],
        {
          exp_boxes: 'boxes',
        },
      )

      const updateWithKnownFlagLogsMock = jest.fn()
      const exposedFlagLogs = buildExposedFlagLogs({
        updateWithKnownFlagLogs: updateWithKnownFlagLogsMock,
        getExposedFlagLogs: jest.fn().mockReturnValue(
          new Map([
            ['exposed.global config: log key', new Set(['global config: log value'])],
            ['exposed.experiment config #2: log key', new Set(['experiment config #2: log value'])],
          ]),
        ),
      })
      const setEnvironmentValuesMock = jest.fn()
      const metricaEnvironment = buildMetricaEnvironment({
        setEnvironmentValues: setEnvironmentValuesMock,
      })

      const flagsLogger = new FlagsLogger(
        new Set(['global config: flag #1', 'experiment config #2: flag #1']),
        exposedFlagLogs,
        metricaEnvironment,
        RealJSONSerializerWrapper(),
      )

      flagsLogger.updateKnownFlagLogs(flagLogsByFlagNamesFromConfugurations(response.configurations))
      flagsLogger.updateGlobalLogs(response.logs)

      expect(setEnvironmentValuesMock.mock.calls[0][0]).toStrictEqual(
        new Map([
          ['known.global config: log key', '["global config: log value"]'],
          ['known.experiment config #2: log key', '["experiment config #2: log value"]'],
        ]),
      )
      expect(updateWithKnownFlagLogsMock).toBeCalledWith(
        new Map([
          ['global config: log key', new Set(['global config: log value'])],
          ['experiment config #2: log key', new Set(['experiment config #2: log value'])],
        ]),
      )
      expect(setEnvironmentValuesMock.mock.calls[1][0]).toStrictEqual(
        new Map([
          ['exposed.global config: log key', '["global config: log value"]'],
          ['exposed.experiment config #2: log key', '["experiment config #2: log value"]'],
        ]),
      )
      expect(setEnvironmentValuesMock.mock.calls[2][0]).toStrictEqual(new Map([['exp_boxes', 'boxes']]))
    })
  })

  describe(FlagsLogger.prototype.logExposedFlagLogs, () => {
    it('should not log exposed FlagLogs on empty input', () => {
      const exposedFlagLogs = buildExposedFlagLogs()
      const metricaEnvironment = buildMetricaEnvironment()

      const flagsLogger = new FlagsLogger(new Set(), exposedFlagLogs, metricaEnvironment, RealJSONSerializerWrapper())

      flagsLogger.logExposedFlagLogs(new Map())

      expect(metricaEnvironment.setEnvironmentValues).not.toBeCalled()
    })

    it('should not log exposed FlagLogs if nothing changed', () => {
      const appendExposedLogsMock = jest.fn().mockReturnValue(false)
      const exposedFlagLogs = buildExposedFlagLogs({
        appendExposedLogs: appendExposedLogsMock,
      })
      const metricaEnvironment = buildMetricaEnvironment()

      const flagsLogger = new FlagsLogger(new Set(), exposedFlagLogs, metricaEnvironment, RealJSONSerializerWrapper())

      flagsLogger.logExposedFlagLogs(new Map([['key', 'value']]))

      expect(appendExposedLogsMock).toBeCalledWith(new Map([['key', 'value']]))
      expect(metricaEnvironment.setEnvironmentValues).not.toBeCalled()
    })

    it('should log exposed FlagLogs', () => {
      const appendExposedLogsMock = jest.fn().mockReturnValue(true)
      const exposedFlagLogs = buildExposedFlagLogs({
        appendExposedLogs: appendExposedLogsMock,
        getExposedFlagLogs: jest.fn().mockReturnValue(
          new Map([
            ['key1', new Set(['value1'])],
            ['key2', new Set(['value2'])],
          ]),
        ),
      })
      const setEnvironmentValuesMock = jest.fn()
      const metricaEnvironment = buildMetricaEnvironment({
        setEnvironmentValues: setEnvironmentValuesMock,
      })

      const flagsLogger = new FlagsLogger(new Set(), exposedFlagLogs, metricaEnvironment, RealJSONSerializerWrapper())

      flagsLogger.logExposedFlagLogs(new Map([['key', 'value']]))

      expect(appendExposedLogsMock).toBeCalledWith(new Map([['key', 'value']]))
      expect(setEnvironmentValuesMock).toBeCalledWith(
        new Map([
          ['key1', '["value1"]'],
          ['key2', '["value2"]'],
        ]),
      )
    })
  })
})

describe(flagLogsByFlagNamesFromConfugurations, () => {
  it('should create FlagLogs from items', () => {
    const response = makeFlagsResponse([
      {
        HANDLER: 'MOBMAIL',
        CONTEXT: {
          MOBMAIL: {
            source: 'global',
            logs: { ['global config: log key']: 'global config: log value' },
            flags: {
              ['global config: flag #1']: 'global config: string flag #1 value',
              ['global config: flag #2']: 'global config: string flag #2 value',
            },
          },
        },
      },
      {
        HANDLER: 'MOBMAIL',
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            logs: { ['experiment config #1: log key']: 'experiment config #1: log value' },
            flags: {
              ['experiment config #1: flag #1']: 'experiment config #1: string flag #1 value',
            },
          },
        },
      },
      {
        HANDLER: 'MOBMAIL',
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            logs: { ['experiment config #2: log key']: 'experiment config #2: log value' },
            flags: {
              ['experiment config #2: flag #1']: 'experiment config #2: string flag #2 value',
              ['experiment config #2: flag #2']: 'experiment config #2: string flag #3 value',
            },
          },
        },
      },
    ])

    expect(flagLogsByFlagNamesFromConfugurations(response.configurations)).toStrictEqual(
      new Map<FlagName, FlagLogs>([
        ['global config: flag #1', new Map([['global config: log key', 'global config: log value']])],
        ['global config: flag #2', new Map([['global config: log key', 'global config: log value']])],
        [
          'experiment config #1: flag #1',
          new Map([['experiment config #1: log key', 'experiment config #1: log value']]),
        ],
        [
          'experiment config #2: flag #1',
          new Map([['experiment config #2: log key', 'experiment config #2: log value']]),
        ],
        [
          'experiment config #2: flag #2',
          new Map([['experiment config #2: log key', 'experiment config #2: log value']]),
        ],
      ]),
    )
  })
})

describe(filterFlagLogsByRegisteredFlags, () => {
  it('should filter FlagLogs', () => {
    const logsByFlagNames: FlagLogsByFlagNames = new Map([
      ['global config: flag #1', new Map([['global config: log key', 'global config: log value']])],
      ['global config: flag #2', new Map([['global config: log key', 'global config: log value']])],
      [
        'experiment config #1: flag #1',
        new Map([['experiment config #1: log key', 'experiment config #1: log value']]),
      ],
      [
        'experiment config #2: flag #1',
        new Map([['experiment config #2: log key', 'experiment config #2: log value']]),
      ],
      [
        'experiment config #2: flag #2',
        new Map([['experiment config #2: log key', 'experiment config #2: log value']]),
      ],
    ])

    expect(
      filterFlagLogsByRegisteredFlags(
        logsByFlagNames,
        new Set(['global config: flag #1', 'experiment config #2: flag #1']),
      ),
    ).toStrictEqual([
      new Map([['global config: log key', 'global config: log value']]),
      new Map([['experiment config #2: log key', 'experiment config #2: log value']]),
    ])
  })
})

describe(mergeFlagLogsArray, () => {
  it('should merge FlagLogs', () => {
    const flagLogsArray: readonly FlagLogs[] = [
      new Map([['key #1', 'value #1']]),
      new Map([['key #1', 'value #2']]),
      new Map([['key #2', 'value #3']]),
      new Map([['key #2', 'value #3']]),
      new Map([['key #3', 'value #4']]),
    ]

    expect(mergeFlagLogsArray(flagLogsArray)).toStrictEqual(
      new Map([
        ['key #1', new Set(['value #1', 'value #2'])],
        ['key #2', new Set(['value #3'])],
        ['key #3', new Set(['value #4'])],
      ]),
    )
  })
})

describe(prepareMergedFlagLogsForMetricaLogging, () => {
  it('should prepare FlagLogs', () => {
    const mergedFlagLogs: MergedFlagLogs = new Map([
      ['key #1', new Set(['value #1', 'value #2'])],
      ['key #2', new Set(['value #3'])],
      ['key #3', new Set(['value #4'])],
    ])

    expect(prepareMergedFlagLogsForMetricaLogging(RealJSONSerializerWrapper(), mergedFlagLogs)).toStrictEqual(
      new Map([
        ['key #1', '["value #1","value #2"]'],
        ['key #2', '["value #3"]'],
        ['key #3', '["value #4"]'],
      ]),
    )
  })

  it('should skip values if serialization failed', () => {
    const mergedFlagLogs: MergedFlagLogs = new Map([['key', new Set(['value'])]])

    expect(
      prepareMergedFlagLogsForMetricaLogging(
        MockJSONSerializerWrapper({
          serialize: jest.fn().mockReturnValue(new Result(null, new Error('serialization error'))),
        }),
        mergedFlagLogs,
      ),
    ).toStrictEqual(new Map())
  })
})
