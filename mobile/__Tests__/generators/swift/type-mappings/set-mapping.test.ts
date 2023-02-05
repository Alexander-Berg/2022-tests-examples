import { SwiftSetMapping } from '../../../../src/generators/swift/type-mappings/set-mapping'

const mapping = new SwiftSetMapping()

describe(SwiftSetMapping, () => {
  it('should map Set to YSSet', () => {
    expect(mapping.name).toBe('YSSet')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it('should map "constructor" to mapping name', () => {
    expect(mapping.constructorName).toBe(mapping.name)
  })
})
