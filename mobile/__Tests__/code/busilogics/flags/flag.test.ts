import { int64 } from '../../../../../../common/ys'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import {
  BooleanJSONItem,
  DoubleJSONItem,
  IntegerJSONItem,
  JSONItem,
  StringJSONItem,
} from '../../../../../common/code/json/json-types'
import {
  AnyFlag,
  ArrayStringFlag,
  BooleanFlag,
  DoubleFlag,
  Flag,
  FlagsRegistry,
  Int32Flag,
  Int64Flag,
  StringFlag,
} from '../../../../../xflags/code/flag'
import { FlagsProvider, FlagsProviderSharedInstance } from '../../../../../xflags/code/flags-provider'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { Flags } from '../../../../../xflags/code/flags'

describe(Int32Flag, () => {
  const flag = Flags.int32('name', 1)
  it('should create flag', () => {
    expect(flag.name).toBe('name')
    expect(flag.defaultValue).toBe(1)
  })
  it('should parse value', () => {
    expect(flag.parse(JSONItemFromJSON(10))).toBe(10)
    expect(flag.parse(JSONItemFromJSON(['invalid']))).toBeNull()
  })
  it('should serialize value', () => {
    expect(flag.serialize(1)).toStrictEqual(IntegerJSONItem.fromInt32(1))
  })
})

describe(Int64Flag, () => {
  const flag = Flags.int64('name', int64(1))
  it('should create flag', () => {
    expect(flag.name).toBe('name')
    expect(flag.defaultValue).toBe(int64(1))
  })
  it('should parse value', () => {
    expect(flag.parse(JSONItemFromJSON(int64(10)))).toBe(int64(10))
    expect(flag.parse(JSONItemFromJSON(['invalid']))).toBeNull()
  })
  it('should serialize value', () => {
    expect(flag.serialize(int64(1))).toStrictEqual(IntegerJSONItem.fromInt64(int64(1)))
  })
})

describe(DoubleFlag, () => {
  const flag = Flags.double('name', 1.1)
  it('should create flag', () => {
    expect(flag.name).toBe('name')
    expect(flag.defaultValue).toBe(1.1)
  })
  it('should parse value', () => {
    expect(flag.parse(JSONItemFromJSON(2.2))).toBe(2.2)
    expect(flag.parse(JSONItemFromJSON(['invalid']))).toBeNull()
  })
  it('should serialize value', () => {
    expect(flag.serialize(1.1)).toStrictEqual(new DoubleJSONItem(1.1))
  })
})

describe(Int32Flag, () => {
  const flag = Flags.string('name', 'default')
  it('should create flag', () => {
    expect(flag.name).toBe('name')
    expect(flag.defaultValue).toBe('default')
  })
  it('should parse value', () => {
    expect(flag.parse(JSONItemFromJSON('value'))).toBe('value')
    expect(flag.parse(JSONItemFromJSON(['invalid']))).toBeNull()
  })
  it('should serialize value', () => {
    expect(flag.serialize('2')).toStrictEqual(new StringJSONItem('2'))
  })
})

describe(ArrayStringFlag, () => {
  const flag = Flags.stringArray('name', ['a', 'b'])
  it('should create flag', () => {
    expect(flag.name).toBe('name')
    expect(flag.defaultValue).toEqual(['a', 'b'])
  })
  it('should parse value', () => {
    expect(flag.parse(JSONItemFromJSON(['c', 'd']))).toEqual(['c', 'd'])
    expect(flag.parse(JSONItemFromJSON([1, 2]))).toEqual(['', ''])
    expect(flag.parse(JSONItemFromJSON(1))).toBeNull()
  })
  it('should serialize value', () => {
    expect(flag.serialize(['c', 'd'])).toStrictEqual(JSONItemFromJSON(['c', 'd']))
  })
})

describe(BooleanFlag, () => {
  const flag = Flags.boolean('name', false)
  it('should create flag', () => {
    expect(flag.name).toBe('name')
    expect(flag.defaultValue).toBe(false)
  })
  it('should parse value', () => {
    expect(flag.parse(JSONItemFromJSON(true))).toBe(true)
    expect(flag.parse(JSONItemFromJSON(['invalid']))).toBeNull()
  })
  it('should serialize value', () => {
    expect(flag.serialize(true)).toStrictEqual(new BooleanJSONItem(true))
  })
})

describe(AnyFlag, () => {
  it('should delegate to original flag', () => {
    const flag = createMockInstance(StringFlag, {
      name: 'name',
      defaultValue: 'default',
      getValue: jest.fn().mockReturnValue('from "getValue"'),
      getValueWithoutLogging: jest.fn().mockReturnValue('from "getValueWithoutLogging"'),
      parse: jest.fn().mockReturnValue('from "parse"'),
      serialize: jest.fn().mockReturnValue(new StringJSONItem('serialized')),
    })

    const anyFlag = AnyFlag.from(flag)

    expect(anyFlag.name).toBe('name')
    expect(anyFlag.defaultValue).toBe('default')
    expect(anyFlag.getValue()).toBe('from "getValue"')
    expect(flag.getValue).toBeCalled()
    expect(anyFlag.getValueWithoutLogging()).toBe('from "getValueWithoutLogging"')
    expect(flag.getValueWithoutLogging).toBeCalled()
    expect(anyFlag.parse(new StringJSONItem('some value'))).toBe('from "parse"')
    expect(flag.parse).toBeCalledWith(new StringJSONItem('some value'))
    expect(anyFlag.serialize('some value')).toStrictEqual(new StringJSONItem('serialized'))
    expect(flag.serialize).toBeCalledWith('some value')
  })
})

describe(Flag, () => {
  afterEach(FlagsProviderSharedInstance.reset)

  class DummyFlag extends Flag<any> {
    public parse(json: JSONItem) {
      throw new Error('Method not implemented.')
    }

    public serialize(value: any): JSONItem {
      throw new Error('Method not implemented.')
    }
  }

  it('should get value with logging', () => {
    const expected = { expected: 'value' }
    const flagsProvider: FlagsProvider = {
      getValueForFlag: jest.fn().mockReturnValue(expected),
      getFlagEditorForFlag: jest.fn(),
      getRuntimeEditorForFlag: jest.fn(),
    }
    FlagsProviderSharedInstance.instance = flagsProvider

    const flag = new DummyFlag('name', ['default value'])
    const result = flag.getValue()

    expect(result).toBe(expected)
    expect(flagsProvider.getValueForFlag).toBeCalledWith(flag, true)
  })
  it('should get value without logging', () => {
    const expected = { expected: 'value' }
    const flagsProvider: FlagsProvider = {
      getValueForFlag: jest.fn().mockReturnValue(expected),
      getFlagEditorForFlag: jest.fn(),
      getRuntimeEditorForFlag: jest.fn(),
    }
    FlagsProviderSharedInstance.instance = flagsProvider

    const flag = new DummyFlag('name', ['default value'])
    const result = flag.getValueWithoutLogging()

    expect(result).toBe(expected)
    expect(flagsProvider.getValueForFlag).toBeCalledWith(flag, false)
  })
  it('should return flag editor', () => {
    const expected = { expected: 'value' }
    const flagsProvider: FlagsProvider = {
      getValueForFlag: jest.fn(),
      getFlagEditorForFlag: jest.fn().mockReturnValue(expected),
      getRuntimeEditorForFlag: jest.fn(),
    }
    FlagsProviderSharedInstance.instance = flagsProvider

    const flag = new DummyFlag('name', ['default value'])
    const result = flag.getEditor()

    expect(result).toBe(expected)
    expect(flagsProvider.getFlagEditorForFlag).toBeCalledWith(flag)
  })
  it('should store flag to registry after registration', () => {
    const registry = new FlagsRegistry()

    const name = 'registered'
    expect(registry.get(name)).toBeNull()
    const flag = Flags.boolean('registered', true)
    registry.register(flag)
    expect(registry.get(name)!.original).toStrictEqual(flag)
  })
  it('should return all flags', () => {
    const registry = new FlagsRegistry()

    const booleanFlag = Flags.boolean('boolean flag', true)
    const intFlag = Flags.int32('int flag', 1)
    registry.register(booleanFlag)
    registry.register(intFlag)
    expect(registry.getAllFlags().map((flag) => flag.original)).toStrictEqual([booleanFlag, intFlag])
  })
})
