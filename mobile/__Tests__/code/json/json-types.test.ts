import { Double, Int32, int32ToInt64, Int64, int64 } from '../../../../../common/ys'
import {
  ArrayJSONItem,
  BooleanJSONItem,
  DoubleJSONItem,
  IntegerJSONItem,
  JSONItemGetDebugDescription,
  JSONItemGetValueDebugDescription,
  JSONItemKind,
  JSONItemKindToString,
  JSONItemToDouble,
  JSONItemToInt32,
  JSONItemToInt64,
  MapJSONItem,
  NullJSONItem,
  StringJSONItem,
  JSONItem,
  decodeJSONItem,
  JSONItemGetValue,
} from '../../../code/json/json-types'
import { JSONItemFromJSON } from '../../../__tests__/__helpers__/json-helpers'
import { resultValue } from '../../../code/result/result'

describe(JSONItem, () => {
  it('should perform "is" checks', () => {
    const integer = IntegerJSONItem.fromInt32(10)
    const double = new DoubleJSONItem(10.0)
    const string = new StringJSONItem('string')
    const boolean = new BooleanJSONItem(true)
    const nullItem = new NullJSONItem()
    const map = JSONItemFromJSON({ key: 'value' })
    const array = JSONItemFromJSON(['val1', 'val2'])

    expect(integer.isIntegerJSONItem()).toBe(true)
    expect(double.isDoubleJSONItem()).toBe(true)
    expect(string.isStringJSONItem()).toBe(true)
    expect(boolean.isBooleanJSONItem()).toBe(true)
    expect(nullItem.isNullJSONItem()).toBe(true)
    expect(map.isMapJSONItem()).toBe(true)
    expect(array.isArrayJSONItem()).toBe(true)

    expect(integer.isDoubleJSONItem()).toBe(false)
    expect(double.isStringJSONItem()).toBe(false)
    expect(string.isBooleanJSONItem()).toBe(false)
    expect(boolean.isNullJSONItem()).toBe(false)
    expect(nullItem.isMapJSONItem()).toBe(false)
    expect(map.isArrayJSONItem()).toBe(false)
    expect(array.isIntegerJSONItem()).toBe(false)
  })

  it('should perform casts', () => {
    const integer = IntegerJSONItem.fromInt32(10)
    const double = new DoubleJSONItem(10.0)
    const string = new StringJSONItem('string')
    const boolean = new BooleanJSONItem(true)
    const nullItem = new NullJSONItem()
    const map = JSONItemFromJSON({ key: 'value' })
    const array = JSONItemFromJSON(['val1', 'val2'])

    expect(integer.castAsIntegerJSONItem()).toBeInstanceOf(IntegerJSONItem)
    expect(double.castAsDoubleJSONItem()).toBeInstanceOf(DoubleJSONItem)
    expect(string.castAsStringJSONItem()).toBeInstanceOf(StringJSONItem)
    expect(boolean.castAsBooleanJSONItem()).toBeInstanceOf(BooleanJSONItem)
    expect(nullItem.castAsNullJSONItem()).toBeInstanceOf(NullJSONItem)
    expect(map.castAsMapJSONItem()).toBeInstanceOf(MapJSONItem)
    expect(array.castAsArrayJSONItem()).toBeInstanceOf(ArrayJSONItem)

    expect(integer.castAsDoubleJSONItem()).toBeNull()
    expect(double.castAsStringJSONItem()).toBeNull()
    expect(string.castAsBooleanJSONItem()).toBeNull()
    expect(boolean.castAsNullJSONItem()).toBeNull()
    expect(nullItem.castAsMapJSONItem()).toBeNull()
    expect(map.castAsArrayJSONItem()).toBeNull()
    expect(array.castAsIntegerJSONItem()).toBeNull()
  })

  it('should perform try casts', () => {
    const integer = IntegerJSONItem.fromInt32(10)
    const double = new DoubleJSONItem(10.0)
    const string = new StringJSONItem('string')
    const boolean = new BooleanJSONItem(true)
    const nullItem = new NullJSONItem()
    const map = JSONItemFromJSON({ key: 'value' })
    const array = JSONItemFromJSON(['val1', 'val2'])

    expect(integer.tryCastAsIntegerJSONItem()).toBeInstanceOf(IntegerJSONItem)
    expect(double.tryCastAsDoubleJSONItem()).toBeInstanceOf(DoubleJSONItem)
    expect(string.tryCastAsStringJSONItem()).toBeInstanceOf(StringJSONItem)
    expect(boolean.tryCastAsBooleanJSONItem()).toBeInstanceOf(BooleanJSONItem)
    expect(nullItem.tryCastAsNullJSONItem()).toBeInstanceOf(NullJSONItem)
    expect(map.tryCastAsMapJSONItem()).toBeInstanceOf(MapJSONItem)
    expect(array.tryCastAsArrayJSONItem()).toBeInstanceOf(ArrayJSONItem)

    expect(() => integer.tryCastAsDoubleJSONItem()).toThrowError(
      'Failed to cast JSONItem of kind "integer" to kind "double", json: "<JSONItem kind: integer, value: 10>"',
    )
    expect(() => double.tryCastAsStringJSONItem()).toThrowError(
      'Failed to cast JSONItem of kind "double" to kind "string", json: "<JSONItem kind: double, value: 10>"',
    )
    expect(() => string.tryCastAsBooleanJSONItem()).toThrowError(
      'Failed to cast JSONItem of kind "string" to kind "boolean", json: "<JSONItem kind: string, value: "string">"',
    )
    expect(() => boolean.tryCastAsNullJSONItem()).toThrowError(
      'Failed to cast JSONItem of kind "boolean" to kind "nullItem", json: "<JSONItem kind: boolean, value: true>"',
    )
    expect(() => nullItem.tryCastAsMapJSONItem()).toThrowError(
      'Failed to cast JSONItem of kind "nullItem" to kind "map", json: "<JSONItem kind: nullItem, value: null>"',
    )
    expect(() => map.tryCastAsArrayJSONItem()).toThrowError(
      'Failed to cast JSONItem of kind "map" to kind "array", json: "<JSONItem kind: map, value: {"key": <JSONItem kind: string, value: "value">}>"',
    )
    expect(() => array.tryCastAsIntegerJSONItem()).toThrowError(
      'Failed to cast JSONItem of kind "array" to kind "integer", json: "<JSONItem kind: array, value: [<JSONItem kind: string, value: "val1">, <JSONItem kind: string, value: "val2">]>"',
    )
  })
})

describe(IntegerJSONItem, () => {
  it('should be constructible with Int64', () => {
    const value: Int64 = int64(10)
    const sample = IntegerJSONItem.fromInt64(value)
    expect(sample.asInt64()).toBe(value)
  })
  it("should be of kind 'integer'", () => {
    const sample = IntegerJSONItem.fromInt64(int64(10))
    expect(sample.kind).toBe(JSONItemKind.integer)
  })
  it('should return Int32', () => {
    const sample = IntegerJSONItem.fromInt32(10)
    expect(sample.asInt32()).toBe(10)
  })
  it('should return Int64', () => {
    const sample = IntegerJSONItem.fromInt64(int64(10))
    expect(sample.asInt64()).toBe(int64(10))
  })
})

describe(DoubleJSONItem, () => {
  it('should be constructible with Double', () => {
    const value: Double = 10.0
    const sample = new DoubleJSONItem(value)
    expect(sample.value).toBeCloseTo(value)
  })
  it("should be of kind 'double'", () => {
    const sample = new DoubleJSONItem(10)
    expect(sample.kind).toBe(JSONItemKind.double)
  })
})

describe(StringJSONItem, () => {
  it('should be constructible with String', () => {
    const value = 'SAMPLE'
    const sample = new StringJSONItem(value)
    expect(sample.value).toBe(value)
  })
  it("should be of kind 'string'", () => {
    const sample = new StringJSONItem('SAMPLE')
    expect(sample.kind).toBe(JSONItemKind.string)
  })
})

describe(BooleanJSONItem, () => {
  it('should be constructible with Boolean', () => {
    const value = true
    const sample = new BooleanJSONItem(value)
    expect(sample.value).toBe(value)
  })
  it("should be of kind 'boolean'", () => {
    const sample = new BooleanJSONItem(false)
    expect(sample.kind).toBe(JSONItemKind.boolean)
  })
})

describe(NullJSONItem, () => {
  it("should be of kind 'nullItem'", () => {
    const sample = new NullJSONItem()
    expect(sample.kind).toBe(JSONItemKind.nullItem)
  })
})

describe(MapJSONItem, () => {
  it("should be of kind 'map'", () => {
    const sample = new MapJSONItem()
    expect(sample.kind).toBe(JSONItemKind.map)
  })
  it("should return true on 'hasKey' if key exists", () => {
    const value = 10
    const sample = new MapJSONItem()
    sample.putInt32('intkey', value)
    expect(sample.hasKey('intkey')).toBe(true)
  })
  it("should return false on 'hasKey' if key doesn't exist", () => {
    const value = 10
    const sample = new MapJSONItem()
    sample.putInt32('existing', value)
    expect(sample.hasKey('non-existing')).toBe(false)
  })
  it('should put and get integers', () => {
    const sample = new MapJSONItem()
    const value = 10
    sample.putInt32('thekey32', value)
    sample.putInt64('thekey64', int32ToInt64(value))
    expect(sample.getInt32('thekey32')).toBe(value)
    expect(sample.getInt64('thekey64')).toBe(int32ToInt64(value))
    expect(sample.getInt32OrDefault('thekey32', value + 1)).toBe(value)
    expect(sample.getInt64OrDefault('thekey64', int32ToInt64(value) + int64(1))).toBe(int32ToInt64(value))
    expect(sample.getInt32('nonexisting')).toBeNull()
    expect(sample.getInt64('nonexisting')).toBeNull()
    expect(sample.getInt32OrDefault('nonexisting', value)).toBe(value)
    expect(sample.getInt64OrDefault('nonexisting', int32ToInt64(value))).toBe(int32ToInt64(value))
  })
  it('should put int32 if present', () => {
    const sample = new MapJSONItem()
    const value = 10
    sample.putInt32IfPresent('thekey32', null)
    expect(sample.getInt32('thekey32')).toBeNull()
    sample.putInt32IfPresent('thekey32', value)
    expect(sample.getInt32('thekey32')).toBe(value)
  })
  it('should put int64 if present', () => {
    const sample = new MapJSONItem()
    const value = int64(10)
    sample.putInt64IfPresent('thekey64', null)
    expect(sample.getInt64('thekey64')).toBeNull()
    sample.putInt64IfPresent('thekey64', value)
    expect(sample.getInt64('thekey64')).toBe(value)
  })
  it('should put and get integers from Double value', () => {
    const sample = new MapJSONItem()
    const value = 10.5
    const expected = Math.trunc(value)
    sample.putDouble('thekey', value)
    expect(sample.getInt32('thekey')).toBe(expected)
    expect(sample.getInt64('thekey')).toBe(BigInt(expected))
    expect(sample.getInt32OrDefault('thekey', expected + 1)).toBe(expected)
    expect(sample.getInt64OrDefault('thekey', int32ToInt64(expected) + int64(1))).toBe(BigInt(expected))
    expect(sample.getInt32('nonexisting')).toBeNull()
    expect(sample.getInt64('nonexisting')).toBeNull()
    expect(sample.getInt32OrDefault('nonexisting', expected)).toBe(expected)
    expect(sample.getInt64OrDefault('nonexisting', int32ToInt64(expected))).toBe(BigInt(expected))
  })
  it('should return null getting Int if value is not numeric', () => {
    const sample = new MapJSONItem()
    const value = 'Hello'
    sample.putString('thekey', value)
    expect(sample.getInt32('thekey')).toBeNull()
    expect(sample.getInt64('thekey')).toBeNull()
  })
  it('should put and get doubles', () => {
    const value: Double = 10.5
    const sample = new MapJSONItem()
    sample.putDouble('thekey', value)
    expect(sample.getDouble('thekey')).toBeCloseTo(value)
    expect(sample.getDoubleOrDefault('thekey', value + 1)).toBeCloseTo(value)
    expect(sample.getDouble('nonexisting')).toBeNull()
    expect(sample.getDoubleOrDefault('nonexisting', value)).toBeCloseTo(value)
  })
  it('should put double if present', () => {
    const value: Double = 10.5
    const sample = new MapJSONItem()
    sample.putDoubleIfPresent('key', null)
    expect(sample.getDouble('key')).toBeNull()
    sample.putDoubleIfPresent('key', value)
    expect(sample.getDouble('key')).toBe(value)
  })
  it('should put and get doubles from Integer values', () => {
    const value32: Int32 = 10
    const value64: Int64 = int64(20)
    const sample = new MapJSONItem()
    sample.putInt32('thekey32', value32)
    sample.putInt64('thekey64', value64)
    expect(sample.getDouble('thekey32')).toBeCloseTo(value32)
    expect(sample.getDouble('thekey64')).toBeCloseTo(Number(value64))
  })
  it('should return null getting Double if value is not numeric', () => {
    const sample = new MapJSONItem()
    sample.putString('thekey', 'hello')
    expect(sample.getDouble('thekey')).toBeNull()
    expect(sample.getDoubleOrDefault('thekey', 10)).toBeCloseTo(10)
  })
  it('should put and get strings', () => {
    const value = 'hello'
    const sample = new MapJSONItem()
    sample.putString('thekey', value)
    sample.putStringIfPresent('thekey', value)
    sample.putStringIfPresent('thekey', null)
    expect(sample.getString('thekey')).toBe(value)
    expect(sample.getStringOrDefault('thekey', value + 'default')).toBe(value)
    expect(sample.getString('nonexisting')).toBeNull()
    expect(sample.getStringOrDefault('nonexisting', value)).toBe(value)
  })
  it('should put and get booleans', () => {
    const value = true
    const sample = new MapJSONItem()
    sample.putBoolean('thekey', value)
    expect(sample.getBoolean('thekey')).toBe(value)
    expect(sample.getBooleanOrDefault('thekey', !value)).toBe(value)
    expect(sample.getBoolean('nonexisting')).toBeNull()
    expect(sample.getBooleanOrDefault('nonexisting', value)).toBe(value)
  })
  it('should put and get nulls', () => {
    const sample = new MapJSONItem()
    sample.putNull('thekey')
    expect(sample.isNull('thekey')).toBe(true)
    expect(sample.isNull('nonexisting')).toBe(false)
  })
  it('should put and get ArrayJSONItem', () => {
    const value = new ArrayJSONItem()
    value.addInt32(10)
    value.addInt32(20)
    const sample = new MapJSONItem()
    sample.put('thekey', value)
    const result = sample.getArray('thekey')
    expect(sample.getArrayOrDefault('thekey', new ArrayJSONItem().asArray())).toBe(result!)
    expect(result).not.toBeNull()
    expect(result!).toHaveLength(2)
    expect(result![0].kind).toBe(JSONItemKind.integer)
    expect((result![0] as IntegerJSONItem).asInt32()).toBe(10)
    expect(result![1].kind).toBe(JSONItemKind.integer)
    expect((result![1] as IntegerJSONItem).asInt32()).toBe(20)
    expect(sample.getArray('nonexisting')).toBeNull()
    expect(sample.getArrayOrDefault('nonexisting', result!)).toBe(result!)
  })
  it('should not put JSONItem if JSONItem is null', () => {
    const value = null
    const sample = new MapJSONItem()
    sample.putIfPresent('thekey', value)
    expect(sample.getArray('thekey')).toBeNull()
  })
  it('should put JSONItem if JSONItem is not null', () => {
    const value = new ArrayJSONItem()
    value.addInt32(10)
    value.addInt32(20)
    const sample = new MapJSONItem()
    sample.putIfPresent('thekey', value)
    const result = sample.getArray('thekey')
    expect(sample.getArrayOrDefault('thekey', new ArrayJSONItem().asArray())).toBe(result!)
    expect(result).not.toBeNull()
    expect(result!).toHaveLength(2)
    expect(result![0].kind).toBe(JSONItemKind.integer)
    expect((result![0] as IntegerJSONItem).asInt32()).toBe(10)
    expect(result![1].kind).toBe(JSONItemKind.integer)
    expect((result![1] as IntegerJSONItem).asInt32()).toBe(20)
    expect(sample.getArray('nonexisting')).toBeNull()
    expect(sample.getArrayOrDefault('nonexisting', result!)).toBe(result!)
  })
  it('should put and get MapJSONItem', () => {
    const value = new MapJSONItem()
    value.putInt64('k1', int32ToInt64(10))
    value.putInt64('k2', int32ToInt64(20))
    const sample = new MapJSONItem()
    sample.put('thekey', value)
    const result = sample.getMap('thekey')
    expect(sample.getMapOrDefault('thekey', new MapJSONItem().asMap())).toBe(result!)
    expect(result).not.toBeNull()
    expect(result!.size).toBe(2)
    expect(result!.get('k1')).not.toBeNull()
    expect(result!.get('k1')!.kind).toBe(JSONItemKind.integer)
    expect((result!.get('k1')! as IntegerJSONItem).asInt64()).toBe(int32ToInt64(10))
    expect(result!.get('k2')).not.toBeNull()
    expect(result!.get('k2')!.kind).toBe(JSONItemKind.integer)
    expect((result!.get('k2')! as IntegerJSONItem).asInt64()).toBe(int32ToInt64(20))
    expect(sample.getMap('nonexisting')).toBeNull()
    expect(sample.getMapOrDefault('nonexisting', result!)).toBe(result!)
  })
  it('should get raw JSONItem', () => {
    const sample = new MapJSONItem()
    sample.put('intkey', IntegerJSONItem.fromInt32(10))
    sample.put('stringkey', new StringJSONItem('hello'))
    expect(sample.get('intkey')).not.toBeNull()
    expect(sample.get('intkey')!.kind).toBe(JSONItemKind.integer)
    expect((sample.get('intkey')! as IntegerJSONItem).asInt32()).toBe(10)
    expect(sample.get('stringkey')).not.toBeNull()
    expect(sample.get('stringkey')!.kind).toBe(JSONItemKind.string)
    expect((sample.get('stringkey')! as StringJSONItem).value).toBe('hello')
    expect(sample.get('nonexisting')).toBeNull()
  })
  it('should get internal map', () => {
    const sample = new MapJSONItem()
    sample.putInt32('k1', 10)
    sample.putInt32('k2', 20)
    const internalMap = sample.asMap()
    expect(internalMap).not.toBeNull()
    expect(internalMap.size).toBe(2)
    expect(internalMap).toBeInstanceOf(Map)
  })
  it('should return null if types are mismatched', () => {
    const sample = new MapJSONItem()
    sample.putNull('key')
    expect(sample.getMap('key')).toBeNull()
    expect(sample.getInt32('key')).toBeNull()
    expect(sample.getInt64('key')).toBeNull()
    expect(sample.getDouble('key')).toBeNull()
    expect(sample.getBoolean('key')).toBeNull()
    expect(sample.getString('key')).toBeNull()
    expect(sample.getArray('key')).toBeNull()
  })
  it('should convert string number to number types', () => {
    const sample = new MapJSONItem()
    sample.putString('key', '10')
    expect(sample.getInt32('key')).toBe(10)
    expect(sample.getInt64('key')).toBe(int64(10))
    expect(sample.getDouble('key')).toBe(10)
    expect(sample.getString('key')).toBe('10')
  })
  it('should fail to convert string value to number types', () => {
    const sample = new MapJSONItem()
    sample.putString('key', 'letters')
    expect(sample.getInt32('key')).toBeNull()
    expect(sample.getInt64('key')).toBeNull()
    expect(sample.getDouble('key')).toBeNull()
    expect(sample.getString('key')).toBe('letters')
  })
  it('should perform "try get"', () => {
    const item = new MapJSONItem(
      new Map([
        ['integer', IntegerJSONItem.fromInt32(10)],
        ['double', new DoubleJSONItem(10.0)],
        ['string', new StringJSONItem('string')],
        ['boolean', new BooleanJSONItem(true)],
        ['map', JSONItemFromJSON({ key: 'value' })],
        ['array', JSONItemFromJSON(['val1', 'val2'])],
      ]),
    )

    expect(item.tryGet('integer')).toStrictEqual(IntegerJSONItem.fromInt32(10))
    expect(item.tryGetInt32('integer')).toBe(10)
    expect(item.tryGetInt64('integer')).toBe(int64(10))
    expect(item.tryGetDouble('double')).toBe(10.0)
    expect(item.tryGetString('string')).toBe('string')
    expect(item.tryGetBoolean('boolean')).toBe(true)
    expect(item.tryGetMap('map')).toStrictEqual(new Map().set('key', JSONItemFromJSON('value')))
    expect(item.tryGetArray('array')).toStrictEqual([JSONItemFromJSON('val1'), JSONItemFromJSON('val2')])

    expect(() => item.tryGet('nonexisting')).toThrowError(
      /Failed to query MapJSONItem for key "nonexisting", json: "<JSONItem kind: map, value: {.*}>"/,
    )
    expect(() => item.tryGetInt32('string')).toThrowError(
      /Failed to query MapJSONItem for key "string" of kind "integer", json: "<JSONItem kind: map, value: {.*}>"/,
    )
    expect(() => item.tryGetInt64('string')).toThrowError(
      /Failed to query MapJSONItem for key "string" of kind "integer", json: "<JSONItem kind: map, value: {.*}>"/,
    )
    expect(() => item.tryGetDouble('boolean')).toThrowError(
      /Failed to query MapJSONItem for key "boolean" of kind "double", json: "<JSONItem kind: map, value: {.*}>"/,
    )
    expect(() => item.tryGetString('map')).toThrowError(
      /Failed to query MapJSONItem for key "map" of kind "string", json: "<JSONItem kind: map, value: {.*}>"/,
    )
    expect(() => item.tryGetBoolean('array')).toThrowError(
      /Failed to query MapJSONItem for key "array" of kind "boolean", json: "<JSONItem kind: map, value: {.*}>"/,
    )
    expect(() => item.tryGetMap('integer')).toThrowError(
      /Failed to query MapJSONItem for key "integer" of kind "map", json: "<JSONItem kind: map, value: {.*}>"/,
    )
    expect(() => item.tryGetArray('double')).toThrowError(
      /Failed to query MapJSONItem for key "double" of kind "array", json: "<JSONItem kind: map, value: {.*}>"/,
    )
  })
})

describe(ArrayJSONItem, () => {
  const INDEX_OUT_OF_BOUNDS_ERROR = 'Index is out of bounds'

  it("should be of kind 'array'", () => {
    const sample = new ArrayJSONItem()
    expect(sample.kind).toBe(JSONItemKind.array)
  })
  it("should return it's length", () => {
    const sample = new ArrayJSONItem()
    expect(sample.getCount()).toBe(0)
    sample.addInt32(10)
    sample.addInt32(20)
    expect(sample.getCount()).toBe(2)
  })
  it('should add and get integers', () => {
    const sample = new ArrayJSONItem()
    sample.addInt32(10)
    sample.addInt64(int32ToInt64(20))
    expect(sample.getInt32(0)).toBe(10)
    expect(sample.getInt64(1)).toBe(int32ToInt64(20))
    expect(() => sample.getInt32(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.getInt64(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should add and get integers from Double value', () => {
    const sample = new ArrayJSONItem()
    const value = 10.5
    const expected = Math.trunc(value)
    sample.addDouble(value)
    expect(sample.getInt32(0)).toBe(expected)
    expect(sample.getInt64(0)).toBe(BigInt(expected))
    expect(() => sample.getInt32(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.getInt64(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should throw when getting integers from non-number value', () => {
    const sample = new ArrayJSONItem()
    const value = 'hello'
    sample.addString(value)
    expect(() => sample.getInt32(0)).toThrowError(`Type is not Int32 at index 0. It's ${JSONItemKind.string}`)
    expect(() => sample.getInt64(0)).toThrowError(`Type is not Int64 at index 0. It's ${JSONItemKind.string}`)
  })
  it('should add and get doubles', () => {
    const value: Double = 10.5
    const sample = new ArrayJSONItem()
    sample.addDouble(value)
    expect(sample.getDouble(0)).toBeCloseTo(value)
    expect(() => sample.getDouble(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.getDouble(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should get doubles if value is Integer', () => {
    const value32: Int32 = 10
    const value64: Int64 = int64(10)
    const sample = new ArrayJSONItem()
    sample.addInt32(value32)
    sample.addInt64(value64)
    expect(sample.getDouble(0)).toBeCloseTo(value32)
    expect(sample.getDouble(1)).toBeCloseTo(Number(value64))
  })
  it('should throw when getting doubles from non-number value', () => {
    const sample = new ArrayJSONItem()
    sample.addString('Hello')
    expect(() => sample.getDouble(0)).toThrowError(`Type is not Double at index 0. It's ${JSONItemKind.string}`)
  })
  it('should add and get strings', () => {
    const value = 'hello'
    const sample = new ArrayJSONItem()
    sample.addString(value)
    expect(sample.getString(0)).toBe(value)
    expect(() => sample.getString(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.getString(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should add and get booleans', () => {
    const value = true
    const sample = new ArrayJSONItem()
    sample.addBoolean(value)
    expect(sample.getBoolean(0)).toBe(value)
    expect(() => sample.getBoolean(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.getBoolean(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should add and get nulls', () => {
    const sample = new ArrayJSONItem()
    sample.addNull()
    expect(sample.isNull(0)).toBe(true)
    expect(() => sample.isNull(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.isNull(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should add and get ArrayJSONItem', () => {
    const value = new ArrayJSONItem()
    value.addInt64(int64(10))
    value.addInt64(int64(20))
    const sample = new ArrayJSONItem()
    sample.add(value)
    const result = sample.getArray(0)
    expect(result).toHaveLength(2)
    expect(result[0].kind).toBe(JSONItemKind.integer)
    expect((result[0] as IntegerJSONItem).asInt64()).toBe(int32ToInt64(10))
    expect(result[1].kind).toBe(JSONItemKind.integer)
    expect((result[1] as IntegerJSONItem).asInt64()).toBe(int32ToInt64(20))
    expect(() => sample.getArray(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.getArray(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should add and get MapJSONItem', () => {
    const value = new MapJSONItem()
    value.putInt32('k1', 10)
    value.putInt32('k2', 20)
    const sample = new ArrayJSONItem()
    sample.add(value)
    const result = sample.getMap(0)
    expect(result.size).toBe(2)
    expect(result.has('k1')).toBe(true)
    expect(result.get('k1')).not.toBeNull()
    expect(result.get('k1')!.kind).toBe(JSONItemKind.integer)
    expect((result.get('k1')! as IntegerJSONItem).asInt32()).toBe(10)
    expect(result.has('k2')).toBe(true)
    expect(result.get('k2')).not.toBeNull()
    expect(result.get('k2')!.kind).toBe(JSONItemKind.integer)
    expect((result.get('k2')! as IntegerJSONItem).asInt32()).toBe(20)
    expect(() => sample.getMap(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.getMap(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should get raw JSONItem', () => {
    const sample = new ArrayJSONItem()
    sample.add(IntegerJSONItem.fromInt32(10))
    sample.add(new StringJSONItem('hello'))
    expect(sample.get(0)).not.toBeNull()
    expect(sample.get(0)!.kind).toBe(JSONItemKind.integer)
    expect((sample.get(0)! as IntegerJSONItem).asInt32()).toBe(10)
    expect(sample.get(1)).not.toBeNull()
    expect(sample.get(1)!.kind).toBe(JSONItemKind.string)
    expect((sample.get(1)! as StringJSONItem).value).toBe('hello')
    expect(() => sample.get(-1)).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
    expect(() => sample.get(sample.getCount())).toThrowError(INDEX_OUT_OF_BOUNDS_ERROR)
  })
  it('should get internal array', () => {
    const sample = new ArrayJSONItem()
    sample.addInt32(10)
    sample.addInt32(20)
    const internalArray = sample.asArray()
    expect(internalArray).not.toBeNull()
    expect(internalArray).toHaveLength(2)
    expect(internalArray).toBeInstanceOf(Array)
  })
  it('should throw if types are mismatched', () => {
    const sample = new ArrayJSONItem()
    sample.addNull()
    expect(() => sample.getMap(0)).toThrowError(
      `Type is not ${JSONItemKind.map} at index 0. It's ${JSONItemKind.nullItem}`,
    )
    expect(() => sample.getInt32(0)).toThrowError(`Type is not Int32 at index 0. It's ${JSONItemKind.nullItem}`)
    expect(() => sample.getInt64(0)).toThrowError(`Type is not Int64 at index 0. It's ${JSONItemKind.nullItem}`)
    expect(() => sample.getDouble(0)).toThrowError(`Type is not Double at index 0. It's ${JSONItemKind.nullItem}`)
    expect(() => sample.getBoolean(0)).toThrowError(
      `Type is not ${JSONItemKind.boolean} at index 0. It's ${JSONItemKind.nullItem}`,
    )
    expect(() => sample.getArray(0)).toThrowError(
      `Type is not ${JSONItemKind.array} at index 0. It's ${JSONItemKind.nullItem}`,
    )
    expect(() => sample.getString(0)).toThrowError(
      `Type is not ${JSONItemKind.string} at index 0. It's ${JSONItemKind.nullItem}`,
    )
  })
})

describe(JSONItemToInt32, () => {
  it('should try to convert numeric JSONItem to Int32 and return the value if successful', () => {
    expect(JSONItemToInt32(IntegerJSONItem.fromInt32(10))).toBe(10)
    expect(JSONItemToInt32(IntegerJSONItem.fromInt64(int64(10)))).toBe(10)
    expect(JSONItemToInt32(new DoubleJSONItem(10.5))).toBe(10)
  })
  it('should return null if underlying JSON Item is not numeric', () => {
    expect(JSONItemToInt32(new StringJSONItem('Hello'))).toBeNull()
    expect(JSONItemToInt32(new BooleanJSONItem(true))).toBeNull()
    expect(JSONItemToInt32(new NullJSONItem())).toBeNull()
    expect(JSONItemToInt32(new MapJSONItem().putString('hello', 'world'))).toBeNull()
    expect(JSONItemToInt32(new ArrayJSONItem().addString('hello'))).toBeNull()
  })
})

describe(JSONItemToInt64, () => {
  it('should try to convert numeric JSONItem to Int64 and return the value if successful', () => {
    expect(JSONItemToInt64(IntegerJSONItem.fromInt64(int64(10)))).toBe(int64(10))
    expect(JSONItemToInt64(IntegerJSONItem.fromInt32(10))).toBe(int64(10))
    expect(JSONItemToInt64(new DoubleJSONItem(10.5))).toBe(int64(10))
  })
  it('should return null if underlying JSON Item is not numeric', () => {
    expect(JSONItemToInt64(new StringJSONItem('Hello'))).toBeNull()
    expect(JSONItemToInt64(new BooleanJSONItem(true))).toBeNull()
    expect(JSONItemToInt64(new NullJSONItem())).toBeNull()
    expect(JSONItemToInt64(new MapJSONItem().putString('hello', 'world'))).toBeNull()
    expect(JSONItemToInt64(new ArrayJSONItem().addString('hello'))).toBeNull()
  })
})

describe(JSONItemToDouble, () => {
  it('should try to convert numeric JSONItem to Double and return the value if successful', () => {
    expect(JSONItemToDouble(IntegerJSONItem.fromInt32(10))).toBe(10.0)
    expect(JSONItemToDouble(new DoubleJSONItem(10.5))).toBe(10.5)
  })
  it('should return null if underlying JSON Item is not numeric', () => {
    expect(JSONItemToDouble(new StringJSONItem('Hello'))).toBeNull()
    expect(JSONItemToDouble(new BooleanJSONItem(true))).toBeNull()
    expect(JSONItemToDouble(new NullJSONItem())).toBeNull()
    expect(JSONItemToDouble(new MapJSONItem().putString('hello', 'world'))).toBeNull()
    expect(JSONItemToDouble(new ArrayJSONItem().addString('hello'))).toBeNull()
  })
})

describe(JSONItemKindToString, () => {
  it('should convert values', () => {
    expect(JSONItemKindToString(JSONItemKind.integer)).toBe('integer')
    expect(JSONItemKindToString(JSONItemKind.double)).toBe('double')
    expect(JSONItemKindToString(JSONItemKind.string)).toBe('string')
    expect(JSONItemKindToString(JSONItemKind.boolean)).toBe('boolean')
    expect(JSONItemKindToString(JSONItemKind.nullItem)).toBe('nullItem')
    expect(JSONItemKindToString(JSONItemKind.map)).toBe('map')
    expect(JSONItemKindToString(JSONItemKind.array)).toBe('array')
  })
})

describe(JSONItemGetValueDebugDescription, () => {
  it('should get debug value description for integer', () => {
    expect(JSONItemGetValueDebugDescription(IntegerJSONItem.fromInt32(100))).toBe('100')
    expect(JSONItemGetValueDebugDescription(IntegerJSONItem.fromInt64(int64(100)))).toBe('100')
  })
  it('should get debug value description for double', () => {
    expect(JSONItemGetValueDebugDescription(new DoubleJSONItem(100.1))).toBe('100.1')
  })
  it('should get debug value description for string', () => {
    expect(JSONItemGetValueDebugDescription(new StringJSONItem('test value'))).toBe('"test value"')
  })
  it('should get debug value description for boolean', () => {
    expect(JSONItemGetValueDebugDescription(new BooleanJSONItem(true))).toBe('true')
    expect(JSONItemGetValueDebugDescription(new BooleanJSONItem(false))).toBe('false')
  })
  it('should get debug value description for nullItem', () => {
    expect(JSONItemGetValueDebugDescription(new NullJSONItem())).toBe('null')
  })
  it('should get debug value description for array', () => {
    expect(JSONItemGetValueDebugDescription(new ArrayJSONItem())).toBe('[]')
    expect(JSONItemGetValueDebugDescription(new ArrayJSONItem().addString('test value').addBoolean(true))).toBe(
      '[<JSONItem kind: string, value: "test value">, <JSONItem kind: boolean, value: true>]',
    )
  })
  it('should get debug value description for map', () => {
    expect(JSONItemGetValueDebugDescription(new MapJSONItem())).toBe('{}')
    expect(
      JSONItemGetValueDebugDescription(
        new MapJSONItem().putString('string key', 'string value').putBoolean('boolean key', true),
      ),
    ).toBe(
      '{"string key": <JSONItem kind: string, value: "string value">, "boolean key": <JSONItem kind: boolean, value: true>}',
    )
  })
})

describe(JSONItemGetDebugDescription, () => {
  it('should print debug description', () => {
    expect(
      JSONItemGetDebugDescription(
        new MapJSONItem().putString('string key', 'string value').putBoolean('boolean key', true),
      ),
    ).toBe(
      '<JSONItem kind: map, value: {"string key": <JSONItem kind: string, value: "string value">, "boolean key": <JSONItem kind: boolean, value: true>}>',
    )
  })
})

describe(decodeJSONItem, () => {
  const item = JSONItemFromJSON({ key: 'value' })

  it('should decode json', () => {
    const result = decodeJSONItem(item, (json) => {
      const map = json.tryCastAsMapJSONItem()
      return map.tryGetString('key')
    })
    expect(result).toStrictEqual(resultValue('value'))
  })
  it('should fail to decode json with regular error', () => {
    const result = decodeJSONItem(item, (json) => {
      const map = json.tryCastAsMapJSONItem()
      return map.tryGetString('pizza')
    })
    expect(result.getError().message).toStrictEqual(
      'Failed to deserialize JSONItem: "<JSONItem kind: map, value: {"key": <JSONItem kind: string, value: "value">}>", error: "Failed to query MapJSONItem for key "pizza" of kind "string", json: "<JSONItem kind: map, value: {"key": <JSONItem kind: string, value: "value">}>""',
    )
  })
  it('should fail to decode json with unknown error', () => {
    const result = decodeJSONItem(item, (json) => {
      // eslint-disable-next-line no-throw-literal
      throw 'unexpected error'
    })
    expect(result.getError().message).toStrictEqual(
      'Failed to deserialize JSONItem: "<JSONItem kind: map, value: {"key": <JSONItem kind: string, value: "value">}>", unkown error: "unexpected error"',
    )
  })
})

describe(JSONItemGetValue, () => {
  it('should get value for integer', () => {
    expect(JSONItemGetValue(IntegerJSONItem.fromInt32(100))).toBe(100)
    expect(JSONItemGetValue(IntegerJSONItem.fromInt64(int64(100)))).toBe(int64(100))
  })
  it('should get value for double', () => {
    expect(JSONItemGetValue(new DoubleJSONItem(100.1))).toBe(100.1)
  })
  it('should get value for string', () => {
    expect(JSONItemGetValue(new StringJSONItem('test value'))).toBe('test value')
  })
  it('should get value for boolean', () => {
    expect(JSONItemGetValue(new BooleanJSONItem(true))).toBe(true)
    expect(JSONItemGetValue(new BooleanJSONItem(false))).toBe(false)
  })
  it('should get value for nullItem', () => {
    expect(JSONItemGetValue(new NullJSONItem())).toBeNull()
  })
  it('should get value for array', () => {
    expect(JSONItemGetValue(new ArrayJSONItem())).toEqual([])
    expect(JSONItemGetValue(new ArrayJSONItem().addString('test value').addBoolean(true))).toEqual(['test value', true])
  })
  it('should get value for map', () => {
    expect(JSONItemGetValue(new MapJSONItem())).toEqual(new Map())
    expect(
      JSONItemGetValue(
        new MapJSONItem().putString('string key', 'string value').putBoolean('boolean key', true).putNull('null key'),
      ),
    ).toEqual(new Map().set('string key', 'string value').set('boolean key', true))
  })
})
