import { KotlinStringMapping } from '../../../../src/generators/kotlin/type-mappings/string-mapping'

const mapping = new KotlinStringMapping()

describe(KotlinStringMapping, () => {
  it('should map string to String', () => {
    expect(mapping.name).toBe('String')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it('should map length property to length property', () => {
    expect(mapping.mapFunctionName('length')).toBe('length')
  })
  it('should map "constructor" to mapping name', () => {
    expect(mapping.constructorName).toBe(mapping.name)
  })
})
