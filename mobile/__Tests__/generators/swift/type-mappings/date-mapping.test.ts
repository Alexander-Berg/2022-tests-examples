import { SwiftDateMapping } from '../../../../src/generators/swift/type-mappings/date-mapping'

const mapping = new SwiftDateMapping()

describe(SwiftDateMapping, () => {
  it('should map Date to YSDate', () => {
    expect(mapping.name).toBe('YSDate')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it('should map "constructor" to mapping name', () => {
    expect(mapping.constructorName).toBe(mapping.name)
  })
})
