import { Serializable } from 'typescript-json-serializer'
import {
  arrayToSet,
  booleanToInt32,
  cast,
  decrementalRange,
  doubleToInt32,
  doubleToInt64,
  doubleToString,
  int32ToBoolean,
  int32ToInt64,
  int32ToString,
  int64,
  int64ToDouble,
  int64ToInt32,
  int64ToString,
  iterableToArray,
  iterableToSet,
  Nullable,
  nullthrows,
  range,
  setToArray,
  stringToDouble,
  stringToInt32,
  stringToInt64,
  TypeSupport,
  undefinedToNull,
  weak,
  weakThis,
  YSError,
  Parcelize,
  int32ToDouble,
} from '../src/ys-tools/ys'

describe(range, () => {
  it('should throw on negative or zero step', () => {
    expect(() => {
      // eslint-disable-next-line no-empty
      for (const _ of range(1, 10, -1)) {
      }
    }).toThrowError("Step argument must be greater than zero. Now it's -1.")
    expect(() => {
      // eslint-disable-next-line no-empty
      for (const _ of range(1, 10, 0)) {
      }
    }).toThrowError("Step argument must be greater than zero. Now it's 0.")
  })
  it('should generate closed range from-to with step', () => {
    const result: number[] = []
    for (const item of range(1, 10, 2)) {
      result.push(item)
    }
    expect(result).toStrictEqual([1, 3, 5, 7, 9])
  })
  it('should generate closed range from-to with step 1 if omitted', () => {
    const result: number[] = []
    for (const item of range(1, 5)) {
      result.push(item)
    }
    expect(result).toStrictEqual([1, 2, 3, 4])
  })
  it('should generate range of zero element if from === to', () => {
    const result: number[] = []
    for (const item of range(1, 1)) {
      result.push(item)
    }
    expect(result).toHaveLength(0)
  })
  it('should generate empty range if from > to', () => {
    const result: number[] = []
    for (const item of range(5, 1)) {
      result.push(item)
    }
    expect(result).toHaveLength(0)
  })
})

describe(decrementalRange, () => {
  it('should throw on negative or zero step', () => {
    expect(() => {
      // eslint-disable-next-line no-empty
      for (const _ of decrementalRange(10, 1, -1)) {
      }
    }).toThrowError("Step argument must be greater than zero. Now it's -1.")
    expect(() => {
      // eslint-disable-next-line no-empty
      for (const _ of decrementalRange(10, 1, 0)) {
      }
    }).toThrowError("Step argument must be greater than zero. Now it's 0.")
  })
  it('should generate decreasing closed range from-to with step', () => {
    const result: number[] = []
    for (const item of decrementalRange(10, 1, 2)) {
      result.push(item)
    }
    expect(result).toStrictEqual([10, 8, 6, 4, 2])
  })
  it('should generate decreasing closed range from-to with step 1 if omitted', () => {
    const result: number[] = []
    for (const item of decrementalRange(5, 1)) {
      result.push(item)
    }
    expect(result).toStrictEqual([5, 4, 3, 2, 1])
  })
  it('should generate range of one element if from === to', () => {
    const result: number[] = []
    for (const item of decrementalRange(1, 1)) {
      result.push(item)
    }
    expect(result).toStrictEqual([1])
  })
  it('should generate empty range if from < to', () => {
    const result: number[] = []
    for (const item of decrementalRange(1, 5)) {
      result.push(item)
    }
    expect(result).toHaveLength(0)
  })
})

describe(weakThis, () => {
  it('should take a function and return it as is', () => {
    function gt(a: number, b: number): boolean {
      return a > b
    }
    expect(weakThis(gt)(20, 10)).toBeTruthy()
    expect(weakThis((s: string) => 'Hello, ' + s)('World')).toStrictEqual('Hello, World')
  })
})

describe(weak, () => {
  it('should do nothing but mark a weak property', () => {
    // tslint:disable:max-classes-per-file
    class Parent {}
    class Child {
      @weak public parent: Nullable<Parent> = null
    }
    // tslint:enable:max-classes-per-file
    const c = new Child()
    expect(c.parent).toBeNull()
  })
})

describe(cast, () => {
  it('should run over the array and cast to another type', () => {
    // tslint:disable:max-classes-per-file
    class A {}
    class B extends A {}
    // tslint:enable:max-classes-per-file
    const a: A[] = [new A(), new A()]
    const b: readonly B[] = cast(a)
    expect(b).toMatchObject([new B(), new B()])
  })
})

describe(Serializable, () => {
  it('should do nothing but mark a serializable class', () => {
    // tslint:disable:max-classes-per-file
    @Serializable()
    class A {
      public f(): boolean {
        return true
      }
    }
    // tslint:enable:max-classes-per-file
    const a = new A()
    expect(a.f()).toBeTruthy()
  })
})

describe(undefinedToNull, () => {
  it('should cast undefined to null', () => {
    expect(undefinedToNull(undefined)).toBeNull()
  })
  it('should cast null to null', () => {
    expect(undefinedToNull(null)).toBeNull()
  })
  it('should cast type T (except null and undefined) to T', () => {
    expect(undefinedToNull('')).toBe('')
    expect(undefinedToNull(10)).toBe(10)
    expect(undefinedToNull({ a: 10 })).toEqual({ a: 10 })
  })
})

describe(nullthrows, () => {
  it('should thrown for undefined', () => {
    expect(() => nullthrows(undefined)).toThrowError('Got unexpected null or undefined')
  })
  it('should thrown for null', () => {
    expect(() => nullthrows(null)).toThrowError('Got unexpected null or undefined')
  })
  it('should cast type T (except null and undefined) to T', () => {
    expect(nullthrows('')).toBe('')
    expect(nullthrows(10)).toBe(10)
    expect(nullthrows({ a: 10 })).toEqual({ a: 10 })
  })
})

describe(int64, () => {
  it('should convert Int32 values to Int64', () => {
    expect(int64(10)).toBe(BigInt(10))
  })
  it('should pass on Int64 values', () => {
    expect(int64(BigInt(10))).toBe(BigInt(10))
  })
})

describe(stringToInt64, () => {
  it('should return parsed Int64 value from string', () => {
    expect(stringToInt64('100')).toBe(int64(100))
    expect(stringToInt64('-100')).toBe(-int64(100))
    expect(stringToInt64('0')).toBe(int64(0))
    expect(stringToInt64('-0')).toBe(-int64(0))
  })
  it('should return null if string cannot be parsed to Int64', () => {
    expect(stringToInt64('a')).toBeNull()
    expect(stringToInt64('')).toBeNull()
  })
})

describe(stringToInt32, () => {
  it('should return parsed Int32 value from string', () => {
    expect(stringToInt32('100')).toBe(100)
    expect(stringToInt32('-100')).toBe(-100)
    expect(stringToInt32('0')).toBe(0)
    expect(stringToInt32('-0')).toBe(-0)
  })
  it('should return null if string cannot be parsed to Int32', () => {
    expect(stringToInt32('a')).toBeNull()
    expect(stringToInt32('')).toBeNull()
  })
})

describe(stringToDouble, () => {
  it('should return parsed Double value from string', () => {
    expect(stringToDouble('100.5')).toBeCloseTo(100.5, 4)
    expect(stringToDouble('-100.5')).toBeCloseTo(-100.5, 4)
    expect(stringToDouble('0.5')).toBeCloseTo(0.5, 4)
    expect(stringToDouble('-0.5')).toBeCloseTo(-0.5, 4)
  })
  it('should return null if string cannot be parsed to Double', () => {
    expect(stringToDouble('a')).toBeNull()
    expect(stringToDouble('')).toBeNull()
  })
})

describe(int32ToInt64, () => {
  it('should convert Int32 to Int64, which converts to BigInt in TS', () => {
    expect(int32ToInt64(100)).toBe(int64(100))
  })
})

describe(int32ToDouble, () => {
  it('should convert Int32 to Double', () => {
    expect(int32ToDouble(100)).toBe(100.0)
  })
})

describe(int64ToInt32, () => {
  it('should convert Int64 to Int32, which converts to Number in TS', () => {
    expect(int64ToInt32(int64(100))).toBe(100)
  })
})

describe(int64ToDouble, () => {
  it('should convert Int64 to Double, which converts to Number in TS', () => {
    expect(int64ToDouble(int64(100))).toBe(100.0)
  })
})

describe(doubleToInt64, () => {
  it('should convert Double to Int64, which truncates in TS', () => {
    expect(doubleToInt64(100.5)).toBe(int64(100))
  })
})

describe(doubleToInt32, () => {
  it('should convert Double to Int32, which truncates in TS', () => {
    expect(doubleToInt32(100.5)).toBe(100)
  })
})

describe(int64ToString, () => {
  it('should convert Int64 to String', () => {
    expect(int64ToString(int64(10000))).toBe('10000')
  })
})

describe(int32ToString, () => {
  it('should convert Int32 to String', () => {
    expect(int32ToString(10000)).toBe('10000')
  })
})

describe(doubleToString, () => {
  it('should convert Double to String', () => {
    expect(doubleToString(10000.5)).toBe('10000.5')
  })
})

describe(booleanToInt32, () => {
  it('should convert boolean to Int32', () => {
    expect(booleanToInt32(true)).toBe(1)
    expect(booleanToInt32(false)).toBe(0)
  })
})

describe(int32ToBoolean, () => {
  it('should convert Int32 to boolean', () => {
    expect(int32ToBoolean(1)).toBe(true)
    expect(int32ToBoolean(0)).toBe(false)
    expect(int32ToBoolean(-100)).toBe(true)
    expect(int32ToBoolean(100)).toBe(true)
  })
})

describe(setToArray, () => {
  it('should convert Set to Array', () => {
    expect(setToArray(new Set([1, 2, 3]).add(4).add(5))).toStrictEqual([1, 2, 3, 4, 5])
    expect(setToArray(new Set([1, 2, 2, 3, 3, 3]))).toStrictEqual([1, 2, 3])
    expect(setToArray(new Set())).toStrictEqual([])
  })
})

describe(arrayToSet, () => {
  it('should convert Array to Set', () => {
    expect(arrayToSet([1, 2, 3].concat(4).concat(5))).toStrictEqual(new Set([1, 2, 3, 4, 5]))
    expect(arrayToSet([1, 2, 2, 3, 3, 3])).toStrictEqual(new Set([1, 2, 3]))
    expect(arrayToSet([])).toStrictEqual(new Set())
  })
})

describe(iterableToArray, () => {
  it('should convert Iterable to Array', () => {
    expect(iterableToArray(new Map().set('a', '1').set('b', '2').keys())).toStrictEqual(['a', 'b'])
    expect(iterableToArray(new Map().values())).toStrictEqual([])
  })
})

describe(iterableToSet, () => {
  it('should convert Iterable to Set', () => {
    expect(iterableToSet(new Map().set('a', '1').set('b', '2').keys())).toStrictEqual(new Set(['a', 'b']))
    expect(iterableToSet(new Map().values())).toStrictEqual(new Set())
  })
})

describe(TypeSupport, () => {
  it('should check strings', () => {
    expect(TypeSupport.isString('string')).toBe(true)
    expect(TypeSupport.isString(true)).toBe(false)
    expect(TypeSupport.isString(1)).toBe(false)
    expect(TypeSupport.isString(1.1)).toBe(false)
    expect(TypeSupport.isString(int64(1))).toBe(false)
  })
  it('should cast to string or return null', () => {
    expect(TypeSupport.asString('string')).toBe('string')
    expect(TypeSupport.asString(true)).toBeNull()
    expect(TypeSupport.asString(1)).toBeNull()
    expect(TypeSupport.asString(1.1)).toBeNull()
    expect(TypeSupport.asString(int64(1))).toBeNull()
  })
  it('should cast to string or throw error', () => {
    expect(TypeSupport.tryCastAsString('string')).toBe('string')
    expect(() => TypeSupport.tryCastAsString(true)).toThrowError('Non string value: true')
    expect(() => TypeSupport.tryCastAsString(1)).toThrowError('Non string value: 1')
    expect(() => TypeSupport.tryCastAsString(1.1)).toThrowError('Non string value: 1.1')
    expect(() => TypeSupport.tryCastAsString(int64(1))).toThrowError('Non string value: 1')
  })
  it('should check booleans', () => {
    expect(TypeSupport.isBoolean('string')).toBe(false)
    expect(TypeSupport.isBoolean(true)).toBe(true)
    expect(TypeSupport.isBoolean(1)).toBe(false)
    expect(TypeSupport.isBoolean(1.1)).toBe(false)
    expect(TypeSupport.isBoolean(int64(1))).toBe(false)
  })
  it('should cast to boolean or return null', () => {
    expect(TypeSupport.asBoolean('string')).toBeNull()
    expect(TypeSupport.asBoolean(true)).toBe(true)
    expect(TypeSupport.asBoolean(1)).toBeNull()
    expect(TypeSupport.asBoolean(1.1)).toBeNull()
    expect(TypeSupport.asBoolean(int64(1))).toBeNull()
  })
  it('should cast to boolean or throw error', () => {
    expect(() => TypeSupport.tryCastAsBoolean('string')).toThrowError('Non boolean value: string')
    expect(TypeSupport.tryCastAsBoolean(true)).toBe(true)
    expect(() => TypeSupport.tryCastAsBoolean(1)).toThrowError('Non boolean value: 1')
    expect(() => TypeSupport.tryCastAsBoolean(1.1)).toThrowError('Non boolean value: 1.1')
    expect(() => TypeSupport.tryCastAsBoolean(int64(1))).toThrowError('Non boolean value: 1')
  })
  it('should check int32', () => {
    expect(TypeSupport.isInt32('string')).toBe(false)
    expect(TypeSupport.isInt32(true)).toBe(false)
    expect(TypeSupport.isInt32(1)).toBe(true)
    expect(TypeSupport.isInt32(1.1)).toBe(false)
    expect(TypeSupport.isInt32(int64(1))).toBe(false)
  })
  it('should cast to Int32 or return null', () => {
    expect(TypeSupport.asInt32('string')).toBeNull()
    expect(TypeSupport.asInt32(true)).toBeNull()
    expect(TypeSupport.asInt32(1)).toBe(1)
    expect(TypeSupport.asInt32(1.1)).toBe(1)
    expect(TypeSupport.asInt32(int64(1))).toBe(1)
  })
  it('should cast to Int32 or throw error', () => {
    expect(() => TypeSupport.tryCastAsInt32('string')).toThrowError('Non Int32 value: string')
    expect(() => TypeSupport.tryCastAsInt32(true)).toThrowError('Non Int32 value: true')
    expect(TypeSupport.tryCastAsInt32(1)).toBe(1)
    expect(() => TypeSupport.tryCastAsInt32(1.1)).toThrowError('Non Int32 value: 1.1')
    expect(() => TypeSupport.tryCastAsInt32(int64(1))).toThrowError('Non Int32 value: 1')
  })
  it('should check int64', () => {
    expect(TypeSupport.isInt64('string')).toBe(false)
    expect(TypeSupport.isInt64(true)).toBe(false)
    expect(TypeSupport.isInt64(1)).toBe(false)
    expect(TypeSupport.isInt64(1.1)).toBe(false)
    expect(TypeSupport.isInt64(int64(1))).toBe(true)
  })
  it('should cast to Int64 or return null', () => {
    expect(TypeSupport.asInt64('string')).toBeNull()
    expect(TypeSupport.asInt64(true)).toBeNull()
    expect(TypeSupport.asInt64(1)).toBe(int64(1))
    expect(TypeSupport.asInt64(1.1)).toBe(int64(1))
    expect(TypeSupport.asInt64(int64(1))).toBe(int64(1))
  })
  it('should cast to Int64 or throw error', () => {
    expect(() => TypeSupport.tryCastAsInt64('string')).toThrowError('Non Int64 value: string')
    expect(() => TypeSupport.tryCastAsInt64(true)).toThrowError('Non Int64 value: true')
    expect(() => TypeSupport.tryCastAsInt64(1)).toThrowError('Non Int64 value: 1')
    expect(() => TypeSupport.tryCastAsInt64(1.1)).toThrowError('Non Int64 value: 1.1')
    expect(TypeSupport.tryCastAsInt64(int64(1))).toBe(int64(1))
  })
  it('should check Double', () => {
    expect(TypeSupport.isDouble('string')).toBe(false)
    expect(TypeSupport.isDouble(true)).toBe(false)
    expect(TypeSupport.isDouble(1)).toBe(true)
    expect(TypeSupport.isDouble(1.1)).toBe(true)
    expect(TypeSupport.isDouble(int64(1))).toBe(false)
  })
  it('should cast to Double or return null', () => {
    expect(TypeSupport.asDouble('string')).toBeNull()
    expect(TypeSupport.asDouble(true)).toBeNull()
    expect(TypeSupport.asDouble(1)).toBe(1.0)
    expect(TypeSupport.asDouble(1.1)).toBe(1.1)
    expect(TypeSupport.asDouble(int64(1))).toBe(1.0)
  })
  it('should cast to Double or throw error', () => {
    expect(() => TypeSupport.tryCastAsDouble('string')).toThrowError('Non Double value: string')
    expect(() => TypeSupport.tryCastAsDouble(true)).toThrowError('Non Double value: true')
    expect(TypeSupport.tryCastAsDouble(1)).toBe(1)
    expect(TypeSupport.tryCastAsDouble(1.1)).toBe(1.1)
    expect(() => TypeSupport.tryCastAsDouble(int64(1))).toThrowError('Non Double value: 1')
  })
})

describe(YSError, () => {
  it('should hold message', () => {
    const e = new YSError('pizza')
    expect(e.message).toBe('pizza')
  })
})

describe(Parcelize, () => {
  it('silence code coverage', () => {
    function foo(): void {}
    expect(Parcelize(foo)).toBe(undefined)
  })
})
