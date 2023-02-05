import { int64 } from '../../../../../common/ys'
import { Polytype, PolytypeKind } from '../../../code/utils/polytype'

describe(Polytype, () => {
  it('should be able to hold Int64', () => {
    const value = Polytype.int64(int64(10))
    expect(value.isOf(PolytypeKind.string)).toBe(false)
    expect(value.isOf(PolytypeKind.int64)).toBe(true)
    expect(value.isOf(PolytypeKind.int32)).toBe(false)
    expect(value.asInt64()).toBe(int64(10))
  })
  it('should be able to hold Int32', () => {
    const value = Polytype.int32(10)
    expect(value.isOf(PolytypeKind.string)).toBe(false)
    expect(value.isOf(PolytypeKind.int64)).toBe(false)
    expect(value.isOf(PolytypeKind.int32)).toBe(true)
    expect(value.asInt32()).toBe(10)
  })
  it('should be able to hold string', () => {
    const value = Polytype.string('s')
    expect(value.isOf(PolytypeKind.string)).toBe(true)
    expect(value.isOf(PolytypeKind.int64)).toBe(false)
    expect(value.isOf(PolytypeKind.int32)).toBe(false)
    expect(value.asString()).toBe('s')
  })
  it('should be able to return any', () => {
    const stringValue = Polytype.string('s')
    expect(typeof stringValue.asAny()).toBe('string')
    expect(stringValue.asAny()).toBe('s')
    const int64Value = Polytype.int64(int64(10))
    expect(typeof int64Value.asAny()).toBe('bigint')
    expect(int64Value.asAny()).toBe(int64(10))
    const int32Value = Polytype.int32(10)
    expect(typeof int32Value.asAny()).toBe('number')
    expect(int32Value.asAny()).toBe(10)
  })
  it('should crash if accessed with improper type method', () => {
    const stringValue = Polytype.string('s')
    expect(() => stringValue.asInt64()).toThrowError('Access as Int64 to a value of another kind')
    expect(() => stringValue.asInt32()).toThrowError('Access as Int32 to a value of another kind')
    const int64Value = Polytype.int64(int64(10))
    expect(() => int64Value.asString()).toThrowError('Access as String to a value of another kind')
    expect(() => int64Value.asInt32()).toThrowError('Access as Int32 to a value of another kind')
    const int32Value = Polytype.int32(10)
    expect(() => int32Value.asString()).toThrowError('Access as String to a value of another kind')
    expect(() => int32Value.asInt64()).toThrowError('Access as Int64 to a value of another kind')
  })
})
