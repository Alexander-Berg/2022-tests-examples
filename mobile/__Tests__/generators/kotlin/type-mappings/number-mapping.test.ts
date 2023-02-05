import { KotlinNumberMapping } from '../../../../src/generators/kotlin/type-mappings/number-mapping'

const mapping = new KotlinNumberMapping()

describe(KotlinNumberMapping, () => {
  it('should map Number to Long', () => {
    expect(mapping.name).toBe('Long')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it('should map "constructor" to mapping name', () => {
    expect(mapping.constructorName).toBe(mapping.name)
  })
})
