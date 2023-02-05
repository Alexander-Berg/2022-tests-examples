import { KotlinArrayMapping } from '../../../../src/generators/kotlin/type-mappings/array-mapping'

const mapping = new KotlinArrayMapping()

describe(KotlinArrayMapping, () => {
  it('should map Array to YSArray', () => {
    expect(mapping.name).toBe('YSArray')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it('should map "length" function to "size"', () => {
    const functionName = 'length'
    expect(mapping.mapFunctionName(functionName)).toBe('size')
  })
  it('should map "push" function to "add"', () => {
    const functionName = 'push'
    expect(mapping.mapFunctionName(functionName)).toBe('add')
  })
  it('should map "join" function to "joinToString"', () => {
    const functionName = 'join'
    expect(mapping.mapFunctionName(functionName)).toBe('joinToString')
  })
  it('should map "flatMap" function to "__flatMap"', () => {
    const functionName = 'flatMap'
    expect(mapping.mapFunctionName(functionName)).toBe('__flatMap')
  })

  it('should map "constructor" to "mutableListOf"', () => {
    expect(mapping.constructorName).toBe('mutableListOf')
  })
})
