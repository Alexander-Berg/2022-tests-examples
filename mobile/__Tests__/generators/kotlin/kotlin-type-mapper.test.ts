import { TSBuiltInType } from '../../../src/generators-model/basic-types'
import { KotlinTypeMapper } from '../../../src/generators/kotlin/kotlin-type-mapper'
import { KotlinArrayMapping } from '../../../src/generators/kotlin/type-mappings/array-mapping'
import { KotlinDateMapping } from '../../../src/generators/kotlin/type-mappings/date-mapping'
import { KotlinMapMapping } from '../../../src/generators/kotlin/type-mappings/map-mapping'
import { KotlinNumberMapping } from '../../../src/generators/kotlin/type-mappings/number-mapping'
import { KotlinSetMapping } from '../../../src/generators/kotlin/type-mappings/set-mapping'
import { KotlinStringMapping } from '../../../src/generators/kotlin/type-mappings/string-mapping'
import { TypeMapping } from '../../../src/generators-model/type-mapping'

const typeMapper = (mapping: TSBuiltInType): TypeMapping => KotlinTypeMapper.getTypeMapping(mapping)

describe(KotlinTypeMapper, () => {
  it('should return KotlinStringMapping for String', () => {
    expect(typeMapper(TSBuiltInType.String)).toBeInstanceOf(KotlinStringMapping)
  })
  it('should return KotlinArrayMapping for Arrays', () => {
    expect(typeMapper(TSBuiltInType.Array)).toBeInstanceOf(KotlinArrayMapping)
    expect(typeMapper(TSBuiltInType.ReadonlyArray)).toBeInstanceOf(KotlinArrayMapping)
  })
  it('should return KotlinMapMapping for Map', () => {
    expect(typeMapper(TSBuiltInType.Map)).toBeInstanceOf(KotlinMapMapping)
    expect(typeMapper(TSBuiltInType.ReadonlyMap)).toBeInstanceOf(KotlinMapMapping)
  })
  it('should return KotlinDateMapping for Date', () => {
    expect(typeMapper(TSBuiltInType.Date)).toBeInstanceOf(KotlinDateMapping)
  })
  it('should return KotlinSetMapping for Set', () => {
    expect(typeMapper(TSBuiltInType.Set)).toBeInstanceOf(KotlinSetMapping)
    expect(typeMapper(TSBuiltInType.ReadonlySet)).toBeInstanceOf(KotlinSetMapping)
  })
  it('should return KotlinNumberMapping for Number', () => {
    expect(typeMapper(TSBuiltInType.Number)).toBeInstanceOf(KotlinNumberMapping)
  })
  it('should throw if type mapping is not registered', () => {
    expect(() => typeMapper(TSBuiltInType.Boolean)).toThrowError('Type mapping for Boolean is not yet implemented')
    expect(() => typeMapper(TSBuiltInType.Math)).toThrowError('Type mapping for Math is not yet implemented')
  })
})
