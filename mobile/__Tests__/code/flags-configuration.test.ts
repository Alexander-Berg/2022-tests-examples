import { ArrayJSONItem, StringJSONItem } from '../../../common/code/json/json-types'
import {
  extractStringMap,
  FlagsConfiguration,
  flagsConfigurationFromJSONItem,
  flagsConfigurationsFromArrayJSONItem,
  FlagsConfigurationSource,
  flagsConfigurationSourceFromServerValue,
} from '../../code/api/flags-configuration'
import { FlagsResponse } from '../../code/api/flags-response'
import { JSONItemFromJSON } from '../../../common/__tests__/__helpers__/json-helpers'

export function makeFlagsResponse(json: readonly any[], logs: any = {}): FlagsResponse {
  return new FlagsResponse(
    flagsConfigurationsFromArrayJSONItem(JSONItemFromJSON(json) as ArrayJSONItem),
    extractStringMap(JSONItemFromJSON(logs)),
  )
}

describe(flagsConfigurationSourceFromServerValue, () => {
  it('should map server value type to local', () => {
    expect(flagsConfigurationSourceFromServerValue('global')).toBe(FlagsConfigurationSource.global)
    expect(flagsConfigurationSourceFromServerValue('experiment')).toBe(FlagsConfigurationSource.experiment)
    expect(flagsConfigurationSourceFromServerValue('invalid')).toBe(null)
  })
})

describe(flagsConfigurationFromJSONItem, () => {
  it('should return null if JSON Item is not map', () => {
    expect(flagsConfigurationFromJSONItem(JSONItemFromJSON([]))).toBeNull()
  })
  it('should return null if source field in missing', () => {
    expect(flagsConfigurationFromJSONItem(JSONItemFromJSON({}))).toBeNull()
  })
  it('should return null if no client field', () => {
    expect(
      flagsConfigurationFromJSONItem(
        JSONItemFromJSON({
          CONTEXT: {},
        }),
      ),
    ).toBeNull()
  })
  it('should return null if no source field', () => {
    expect(
      flagsConfigurationFromJSONItem(
        JSONItemFromJSON({
          CONTEXT: {
            MOBMAIL: {},
          },
        }),
      ),
    ).toBeNull()
  })
  it('should return null if source field is invalid', () => {
    expect(
      flagsConfigurationFromJSONItem(
        JSONItemFromJSON({
          CONTEXT: {
            MOBMAIL: {
              source: 'invalid',
            },
          },
        }),
      ),
    ).toBeNull()
  })
  it('should return null if flags field is missing', () => {
    expect(
      flagsConfigurationFromJSONItem(
        JSONItemFromJSON({
          CONTEXT: {
            MOBMAIL: {
              source: 'experiment',
            },
          },
        }),
      ),
    ).toBeNull()
  })
  it('should return null if flags field is not map', () => {
    expect(
      flagsConfigurationFromJSONItem(
        JSONItemFromJSON({
          HANDLER: 'MOBMAIL',
          CONTEXT: {
            MOBMAIL: {
              source: 'experiment',
              flags: [],
            },
          },
        }),
      ),
    ).toBeNull()
  })
  it('should return parsed value', () => {
    expect(
      flagsConfigurationFromJSONItem(
        JSONItemFromJSON({
          HANDLER: 'MOBMAIL',
          CONDITION: 'condition',
          CONTEXT: {
            MOBMAIL: {
              source: 'experiment',
              logs: { test_id: 'value' },
              flags: {},
            },
          },
        }),
      ),
    ).toStrictEqual(
      new FlagsConfiguration(
        FlagsConfigurationSource.experiment,
        'condition',
        new Map([['test_id', 'value']]),
        new Map(),
      ),
    )
  })
})

describe(FlagsConfiguration, () => {
  it('should be convertable to json', () => {
    const item = new FlagsConfiguration(
      FlagsConfigurationSource.experiment,
      'condition',
      new Map([['test_id', 'value']]),
      new Map([['flag', new StringJSONItem('value')]]),
    )
    expect(item.toJson()).toStrictEqual(
      JSONItemFromJSON({
        HANDLER: 'MOBMAIL',
        CONDITION: 'condition',
        CONTEXT: {
          MOBMAIL: {
            source: 'experiment',
            logs: { test_id: 'value' },
            flags: { flag: 'value' },
          },
        },
      }),
    )
  })
})
