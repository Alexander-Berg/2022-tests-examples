/* eslint-disable @typescript-eslint/unbound-method */
import { mockLogger, RealJSONSerializerWrapper } from '../../../common/__tests__/__helpers__/mock-patches'
import { createMockInstance } from '../../../common/__tests__/__helpers__/utils'
import { Log } from '../../../common/code/logging/logger'
import { ConditionParameters } from '../../code/expression/condition-parameter'
import { Variable, Version } from '../../code/expression/variable'
import { BooleanFlag, DoubleFlag, Int32Flag, StringFlag } from '../../code/flag'
import { FlagsLogger } from '../../code/flag-logs'
import { FlagsDeveloperSettings } from '../../code/flags-developer-settings'
import {
  DefaultFlagsProvider,
  FallbackFlagsProvider,
  FlagsProvider,
  FlagsProviderSharedInstance,
} from '../../code/flags-provider'
import { ConditionEvaluator } from '../../code/expression/condition-evaluator'
import { MockSharedPreferences } from '../../../common/__tests__/__helpers__/preferences-mock'
import { makeFlagsResponse } from './flags-configuration.test'
import { FlagDeveloperSettingsEditor, FallbackFlagEditor } from '../../code/flag-editor'

function makeDeveloperSettings(prefs: Map<string, any> = new Map()): FlagsDeveloperSettings {
  const mockPrefs = new MockSharedPreferences(prefs)
  return new FlagsDeveloperSettings(mockPrefs, RealJSONSerializerWrapper())
}

function makeFlagsLogger(patch: Partial<FlagsLogger> = {}): FlagsLogger {
  return createMockInstance(FlagsLogger, patch)
}

const paramsMap = new Map<string, Variable>()
paramsMap.set(ConditionParameters.lang.name, Variable.string('ru'))
paramsMap.set(ConditionParameters.osVersion.name, Variable.version(new Version('4.0.0')))
paramsMap.set(ConditionParameters.appBuildNumber.name, Variable.int(400))
const conditionEvaluator = new ConditionEvaluator(paramsMap)

class SampleConditions {
  public static readonly passingCondition = 'lang == "ru" && osVersion == v("4.0.0") && appBuildNumber == 400'
  public static readonly failingCondition = 'lang == "ru" && osVersion == v("4.0.0") && appBuildNumber != 400'
  public static readonly invalidCondition = 'invalid condition'
}

class SampleFlags {
  public static readonly boolFlag = new BooleanFlag('boolean flag', false)
  public static readonly intFlag = new Int32Flag('int flag', 0)
  public static readonly stringFlag = new StringFlag('string flag', 'default')
  public static readonly doubleFlag = new DoubleFlag('double flag', 0)
  public static readonly unknownFlag = new StringFlag('unknown flag', 'default')
  public static readonly invalidFlagValue = new StringFlag('invalid value flag', 'default')
  public static readonly invalidConditionFlag = new StringFlag('invalid condition flag', 'default')
}

describe(FlagsProviderSharedInstance, () => {
  it('should check default shared flags provider', () => {
    expect(FlagsProviderSharedInstance.instance).toEqual(expect.any(FallbackFlagsProvider))
    expect(SampleFlags.stringFlag.getValue()).toBe('default')
    expect(SampleFlags.stringFlag.getEditor()).toStrictEqual(new FallbackFlagEditor())
    const flagsProvider: FlagsProvider = {} as any
    FlagsProviderSharedInstance.instance = flagsProvider
    expect(FlagsProviderSharedInstance.instance).toBe(flagsProvider)
    FlagsProviderSharedInstance.reset()
    expect(FlagsProviderSharedInstance.instance).toEqual(expect.any(FallbackFlagsProvider))
  })
})

describe(DefaultFlagsProvider, () => {
  beforeEach(() => {
    mockLogger()
  })
  afterEach(jest.restoreAllMocks)
  afterEach(FlagsProviderSharedInstance.reset)

  it('should return flag values', () => {
    const response = makeFlagsResponse([
      {
        HANDLER: 'MOBMAIL',
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            flags: {
              [SampleFlags.boolFlag.name]: true,
              [SampleFlags.intFlag.name]: 1,
            },
          },
        },
      },
      {
        HANDLER: 'MOBMAIL',
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            flags: {
              [SampleFlags.stringFlag.name]: 'value',
              [SampleFlags.doubleFlag.name]: 1.1,
              [SampleFlags.invalidFlagValue.name]: { invalid: 'value' },
            },
          },
        },
      },
    ])

    const flagsProvider = new DefaultFlagsProvider(makeFlagsLogger(), conditionEvaluator, makeDeveloperSettings())
    flagsProvider.initValues(response)
    FlagsProviderSharedInstance.instance = flagsProvider

    expect(SampleFlags.boolFlag.getValue()).toBe(true)
    expect(SampleFlags.intFlag.getValue()).toBe(1)
    expect(SampleFlags.stringFlag.getValue()).toBe('value')
    expect(SampleFlags.doubleFlag.getValue()).toBe(1.1)
    expect(SampleFlags.unknownFlag.getValue()).toBe('default')

    // invalid json, return default value and log error
    expect(SampleFlags.invalidFlagValue.getValue()).toBe('default')
    expect(Log.getDefaultLogger()!.error).toBeCalledWith(
      expect.stringContaining(`Couldn't map value to flag "${SampleFlags.invalidFlagValue.name}"`),
    )
  })
  it('should evaluate condition values', () => {
    const response = makeFlagsResponse([
      // passing condition
      {
        HANDLER: 'MOBMAIL',
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            condition: SampleConditions.passingCondition,
            flags: {
              [SampleFlags.boolFlag.name]: true,
              [SampleFlags.intFlag.name]: 1,
            },
          },
        },
      },
      // failing condition
      {
        HANDLER: 'MOBMAIL',
        CONDITION: SampleConditions.failingCondition,
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            flags: {
              [SampleFlags.stringFlag.name]: 'value',
              [SampleFlags.doubleFlag.name]: 1.1,
            },
          },
        },
      },
      // invalid condition
      {
        HANDLER: 'MOBMAIL',
        CONDITION: SampleConditions.invalidCondition,
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            flags: {
              [SampleFlags.invalidConditionFlag.name]: 'value',
            },
          },
        },
      },
    ])

    const flagsProvider = new DefaultFlagsProvider(makeFlagsLogger(), conditionEvaluator, makeDeveloperSettings())
    flagsProvider.initValues(response)
    FlagsProviderSharedInstance.instance = flagsProvider

    // passing conditions, return values from server response
    expect(SampleFlags.boolFlag.getValue()).toBe(true)
    expect(SampleFlags.intFlag.getValue()).toBe(1)

    // failing conditions, return default values
    expect(SampleFlags.stringFlag.getValue()).toBe('default')
    expect(SampleFlags.doubleFlag.getValue()).toBe(0)

    // invalid condition, return default value and log error
    expect(SampleFlags.invalidConditionFlag.getValue()).toBe('default')
    expect(Log.getDefaultLogger()!.error).toBeCalledWith(
      expect.stringContaining(
        `Failed to evaluate condition result for flag "${SampleFlags.invalidConditionFlag.name}"`,
      ),
    )
  })
  it('should respect "developer settings", "experiment" and "global" flags hierarchy and log correct exposure', () => {
    const stringFlag1 = new StringFlag('string flag 1', 'default')
    const stringFlag2 = new StringFlag('string flag 2', 'default')
    const stringFlag3 = new StringFlag('string flag 3', 'default')

    const responseItems = makeFlagsResponse([
      // global config, condition is passing
      {
        HANDLER: 'MOBMAIL',
        CONDITION: SampleConditions.passingCondition,
        CONTEXT: {
          MOBMAIL: {
            source: 'global',
            logs: { ['global config: log key']: 'global config: log value' },
            flags: {
              [stringFlag1.name]: 'global config: string flag #1 value',
              [stringFlag2.name]: 'global config: string flag #2 value',
            },
          },
        },
      },
      // experiment config #1, condition is failing
      {
        HANDLER: 'MOBMAIL',
        CONDITION: SampleConditions.failingCondition,
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            logs: { ['experiment config #1: log key']: 'experiment config #1: log value' },
            flags: {
              [stringFlag1.name]: 'experiment config #1: string flag #1 value',
            },
          },
        },
      },
      // experiment config #2, condition is null (i.e. it's passing by default)
      {
        HANDLER: 'MOBMAIL',
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            logs: { ['experiment config #2: log key']: 'experiment config #2: log value' },
            flags: {
              [stringFlag2.name]: 'experiment config #2: string flag #2 value',
              [stringFlag3.name]: 'experiment config #2: string flag #3 value',
            },
          },
        },
      },
    ])

    const devSettings = makeDeveloperSettings(
      new Map([[stringFlag3.name, JSON.stringify({ value: 'dev settings: string flag #3 value' })]]),
    )
    const logExposedFlagLogs = jest.fn()
    const flagsProvider = new DefaultFlagsProvider(
      makeFlagsLogger({ logExposedFlagLogs }),
      conditionEvaluator,
      devSettings,
    )
    flagsProvider.initValues(responseItems)
    FlagsProviderSharedInstance.instance = flagsProvider

    expect(stringFlag1.getValue()).toBe('global config: string flag #1 value')
    expect(logExposedFlagLogs).toBeCalledWith(new Map().set('global config: log key', 'global config: log value'))
    expect(stringFlag2.getValue()).toBe('experiment config #2: string flag #2 value')
    expect(logExposedFlagLogs).toBeCalledWith(
      new Map().set('experiment config #2: log key', 'experiment config #2: log value'),
    )
    expect(stringFlag3.getValue()).toBe('dev settings: string flag #3 value')
    expect(logExposedFlagLogs).toBeCalledWith(new Map())
  })
  it('should log exposure based on the parameters passed', () => {
    const responseItems = makeFlagsResponse([
      {
        HANDLER: 'MOBMAIL',
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            logs: { key: 'value' },
            flags: {
              [SampleFlags.stringFlag.name]: 'value',
            },
          },
        },
      },
    ])

    const logExposedFlagLogs = jest.fn()
    const flagsProvider = new DefaultFlagsProvider(
      makeFlagsLogger({ logExposedFlagLogs }),
      conditionEvaluator,
      makeDeveloperSettings(),
    )
    flagsProvider.initValues(responseItems)
    FlagsProviderSharedInstance.instance = flagsProvider

    expect(SampleFlags.stringFlag.getValue()).toBe('value')
    expect(logExposedFlagLogs).toBeCalledWith(new Map().set('key', 'value'))

    logExposedFlagLogs.mockReset()

    expect(SampleFlags.stringFlag.getValueWithoutLogging()).toBe('value')
    expect(logExposedFlagLogs).not.toBeCalled()
  })
  it('should return flag editor', () => {
    const devSettings = makeDeveloperSettings()
    const flagsProvider = new DefaultFlagsProvider(makeFlagsLogger(), conditionEvaluator, devSettings)
    FlagsProviderSharedInstance.instance = flagsProvider

    const editor = SampleFlags.boolFlag.getEditor()
    expect(editor).toStrictEqual(new FlagDeveloperSettingsEditor(devSettings, SampleFlags.boolFlag))
  })
})
