/* eslint-disable @typescript-eslint/unbound-method */
import { mockLogger, RealJSONSerializerWrapper } from '../../../common/__tests__/__helpers__/mock-patches'
import { Log } from '../../../common/code/logging/logger'
import { Int32Flag } from '../../code/flag'
import { FlagsDeveloperSettings } from '../../code/flags-developer-settings'
import { JSONItemFromJSON } from '../../../common/__tests__/__helpers__/json-helpers'
import { MockSharedPreferences } from '../../../common/__tests__/__helpers__/preferences-mock'

describe(FlagsDeveloperSettings, () => {
  beforeEach(() => {
    mockLogger()
  })
  afterEach(jest.restoreAllMocks)

  it('should init values', () => {
    const mockPrefs = new MockSharedPreferences(
      new Map<string, any>([
        ['int flag', JSON.stringify({ value: 1000 })],
        ['double flag', JSON.stringify({ value: 99.9 })],
        ['bool flag', JSON.stringify({ value: true })],
        ['string flag', JSON.stringify({ value: 'value' })],
        ['invalid flag', 'invalid { json'],
        ['invalid type value', ['invalid value']],
        ['invalid json value flag', JSON.stringify(['value'])],
      ]),
    )
    const devSettings = new FlagsDeveloperSettings(mockPrefs, RealJSONSerializerWrapper())
    expect(devSettings.getAllValues()).toStrictEqual(new Map())

    devSettings.initValues()
    expect(devSettings.getAllValues()).toStrictEqual(
      new Map([
        ['int flag', JSONItemFromJSON(1000)],
        ['double flag', JSONItemFromJSON(99.9)],
        ['bool flag', JSONItemFromJSON(true)],
        ['string flag', JSONItemFromJSON('value')],
      ]),
    )
    expect((Log.getDefaultLogger()!.error as any).mock.calls).toEqual([
      [expect.stringContaining('Couldn\'t deserialize value for flag "invalid flag"')],
      [expect.stringContaining('Couldn\'t deserialize value for flag "invalid json value flag"')],
    ])
  })

  it('should get value for flag', () => {
    const mockPrefs = new MockSharedPreferences(
      new Map([
        ['int flag', JSON.stringify({ value: 1000 })],
        ['invalid flag', JSON.stringify({ value: true })],
      ]),
    )
    const devSettings = new FlagsDeveloperSettings(mockPrefs, RealJSONSerializerWrapper())

    devSettings.initValues()

    const intFlag = new Int32Flag('int flag', 0)
    const invalidFlag = new Int32Flag('invalid flag', 0)
    const unknownFlag = new Int32Flag('unknown flag', 0)

    expect(devSettings.getValueForFlag(intFlag)).toBe(1000)
    expect(devSettings.getValueForFlag(invalidFlag)).toBeNull()
    expect(devSettings.getValueForFlag(unknownFlag)).toBeNull()
    expect(Log.getDefaultLogger()!.error).toBeCalledWith(
      expect.stringContaining('Couldn\'t map value to flag "invalid flag"'),
    )
  })

  it('should set value for flag', () => {
    const mockPrefs = new MockSharedPreferences(new Map())
    const devSettings = new FlagsDeveloperSettings(mockPrefs, RealJSONSerializerWrapper())

    const intFlag = new Int32Flag('int flag', 0)
    devSettings.setEditorValueForFlag(1000, intFlag)
    expect(mockPrefs.getAll()).toStrictEqual(new Map([['int flag', JSON.stringify({ value: 1000 })]]))
    devSettings.setEditorValueForFlag(null, intFlag)
    expect(mockPrefs.getAll()).toStrictEqual(new Map())
  })
})
