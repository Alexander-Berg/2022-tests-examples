import { TSBuiltInType } from '../../../src/generators-model/basic-types'
import { SwiftTypeMapper } from '../../../src/generators/swift/swift-type-mapper'
import { SwiftArrayMapping } from '../../../src/generators/swift/type-mappings/array-mapping'
import { SwiftDateMapping } from '../../../src/generators/swift/type-mappings/date-mapping'
import { SwiftMapMapping } from '../../../src/generators/swift/type-mappings/map-mapping'
import { SwiftNumberMapping } from '../../../src/generators/swift/type-mappings/number-mapping'
import { SwiftSetMapping } from '../../../src/generators/swift/type-mappings/set-mapping'
import { SwiftStringMapping } from '../../../src/generators/swift/type-mappings/string-mapping'
import { TypeMapping } from '../../../src/generators-model/type-mapping'

const typeMapper = (type: TSBuiltInType): TypeMapping => SwiftTypeMapper.getTypeMapping(type)

describe(SwiftTypeMapper, () => {
  it('should return SwiftStringMapping for String', () => {
    expect(typeMapper(TSBuiltInType.String)).toBeInstanceOf(SwiftStringMapping)
  })
  it('should return SwiftArrayMapping for Arrays', () => {
    expect(typeMapper(TSBuiltInType.Array)).toBeInstanceOf(SwiftArrayMapping)
    expect(typeMapper(TSBuiltInType.ReadonlyArray)).toBeInstanceOf(SwiftArrayMapping)
  })
  it('should return SwiftMapMapping for Map', () => {
    expect(typeMapper(TSBuiltInType.Map)).toBeInstanceOf(SwiftMapMapping)
    expect(typeMapper(TSBuiltInType.ReadonlyMap)).toBeInstanceOf(SwiftMapMapping)
  })
  it('should return SwiftDateMapping for Date', () => {
    expect(typeMapper(TSBuiltInType.Date)).toBeInstanceOf(SwiftDateMapping)
  })
  it('should return SwiftSetMapping for Set', () => {
    expect(typeMapper(TSBuiltInType.Set)).toBeInstanceOf(SwiftSetMapping)
    expect(typeMapper(TSBuiltInType.ReadonlySet)).toBeInstanceOf(SwiftSetMapping)
  })
  it('should return SwiftNumberMapping for Number', () => {
    expect(typeMapper(TSBuiltInType.Number)).toBeInstanceOf(SwiftNumberMapping)
  })
  it('should throw if type mapping is not registered', () => {
    expect(() => typeMapper(TSBuiltInType.Boolean)).toThrowError('Type mapping for Boolean is not yet implemented')
    expect(() => typeMapper(TSBuiltInType.Math)).toThrowError('Type mapping for Math is not yet implemented')
  })
})
