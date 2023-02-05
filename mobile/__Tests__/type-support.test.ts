import { isBuiltInType, stringifyValue } from '../src/generators-model/basic-types'
import { int64 } from '../src/ys-tools/ys'

describe('helpers', () => {
  it('should stringify numbers', () => {
    expect(stringifyValue(10)).toBe('10')
    expect(stringifyValue(0)).toBe('0')
    expect(stringifyValue(int64(10))).toBe('10')
  })
  it('should add quotations in strings', () => {
    expect(stringifyValue('ABC')).toBe('"ABC"')
    expect(stringifyValue('"ABC"')).toBe('"\\"ABC\\""')
  })
  it('should be able to identify mappable builtin types', () => {
    expect(isBuiltInType('Number')).toBe(true)
    expect(isBuiltInType('BigInt')).toBe(true)
    expect(isBuiltInType('Boolean')).toBe(true)
    expect(isBuiltInType('String')).toBe(true)
    expect(isBuiltInType('Array')).toBe(true)
    expect(isBuiltInType('Map')).toBe(true)
    expect(isBuiltInType('Set')).toBe(true)
    expect(isBuiltInType('Date')).toBe(true)
    expect(isBuiltInType('Math')).toBe(true)
    expect(isBuiltInType('SomeType')).toBe(false)
    expect(isBuiltInType('')).toBe(false)
  })
})
