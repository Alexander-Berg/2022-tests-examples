import { SwiftArrayMapping } from '../../../../src/generators/swift/type-mappings/array-mapping'

const mapping = new SwiftArrayMapping()

describe(SwiftArrayMapping, () => {
  it('should map Array to YSArray', () => {
    expect(mapping.name).toBe('YSArray')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it('should map "constructor" to mapping name', () => {
    expect(mapping.constructorName).toBe(mapping.name)
  })
})
