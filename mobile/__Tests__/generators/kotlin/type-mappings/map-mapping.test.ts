import { KotlinMapMapping } from '../../../../src/generators/kotlin/type-mappings/map-mapping'

const mapping = new KotlinMapMapping()

describe(KotlinMapMapping, () => {
  it('should map Map to YSMap', () => {
    expect(mapping.name).toBe('YSMap')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it('should map forEach method to __forEach method', () => {
    expect(mapping.mapFunctionName('forEach')).toBe('__forEach')
  })
  it('should map "constructor" to mapping name', () => {
    expect(mapping.constructorName).toBe('mutableMapOf')
  })
})
