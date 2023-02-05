import {
  ArrayJSONItem,
  BooleanJSONItem,
  DoubleJSONItem,
  IntegerJSONItem,
  NullJSONItem,
  StringJSONItem,
} from '../../../../common/code/json/json-types'
import { FlagsConfiguration, FlagsConfigurationSource } from '../../../code/api/flags-configuration'
import {
  flagsResponseFromJSONItem,
  flagsResponseWithRawConfigurationsFromJSONItem,
} from '../../../code/api/flags-response'
import { JSONItemFromJSON } from '../../../../common/__tests__/__helpers__/json-helpers'
import { clone } from '../../../../common/__tests__/__helpers__/utils'
import sample from '../sample.json'

describe(flagsResponseWithRawConfigurationsFromJSONItem, () => {
  it('should return null if JSON Item is not map', () => {
    const item = JSONItemFromJSON([sample])
    const result = flagsResponseWithRawConfigurationsFromJSONItem(item)
    expect(result).toBeNull()
  })
  it('should return null if logs are not map', () => {
    const copy = clone(sample)
    copy.logs = []
    const result = flagsResponseWithRawConfigurationsFromJSONItem(JSONItemFromJSON(copy))
    expect(result).toBeNull()
  })
})
describe(flagsResponseFromJSONItem, () => {
  it('should return null if JSON Item is not map', () => {
    const item = JSONItemFromJSON([sample])
    const result = flagsResponseFromJSONItem(item)
    expect(result).toBeNull()
  })
  it('should return null items is not array', () => {
    const copy = clone(sample)
    copy.configurations = {}
    const item = JSONItemFromJSON(copy)
    const result = flagsResponseFromJSONItem(item)!
    expect(result).toBeNull()
  })
  it('should skip invalid flag item values', () => {
    const copy = clone(sample)
    copy.configurations[0] = []
    const json = JSONItemFromJSON(copy)
    const result = flagsResponseFromJSONItem(json)!
    expect(result.configurations).toStrictEqual([
      new FlagsConfiguration(FlagsConfigurationSource.global, null, new Map(), new Map()),
    ])
  })
  it('should parse response if it is properly formed', () => {
    const json = JSONItemFromJSON(sample)
    const result = flagsResponseFromJSONItem(json)!
    expect(result.configurations).toStrictEqual([
      new FlagsConfiguration(
        FlagsConfigurationSource.experiment,
        'condition_value a + b',
        new Map([['test_id', 'value']]),
        new Map(
          Object.entries({
            string_flag: new StringJSONItem('value'),
            integer_flag: IntegerJSONItem.fromInt32(1),
            double_flag: new DoubleJSONItem(2.1),
            boolean_flag: new BooleanJSONItem(true),
            null_flag: new NullJSONItem(),
            invalid_flag: new ArrayJSONItem(),
          }),
        ),
      ),
      new FlagsConfiguration(FlagsConfigurationSource.global, null, new Map(), new Map()),
    ])
  })
})
