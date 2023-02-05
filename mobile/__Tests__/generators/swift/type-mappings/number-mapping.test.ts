import { SwiftNumberMapping } from '../../../../src/generators/swift/type-mappings/number-mapping'

const mapping = new SwiftNumberMapping()

describe(SwiftNumberMapping, () => {
  it('should map Number to Int64', () => {
    expect(mapping.name).toBe('Int64')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it('should map "constructor" to mapping name', () => {
    expect(mapping.constructorName).toBe(mapping.name)
  })
})
