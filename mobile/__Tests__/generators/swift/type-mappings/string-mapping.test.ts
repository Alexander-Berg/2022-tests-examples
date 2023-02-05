import { SwiftStringMapping } from '../../../../src/generators/swift/type-mappings/string-mapping'

const mapping = new SwiftStringMapping()

describe('SwiftStringMapping', () => {
  it('should map string to String', () => {
    expect(mapping.name).toBe('String')
  })
  it('should map function names as is', () => {
    const functionName = 'f' + Math.floor(Math.random() * Math.floor(5))
    expect(mapping.mapFunctionName(functionName)).toBe(functionName)
  })
  it.each([
    ['toLowerCase', 'lowercased'],
    ['toUpperCase', 'uppercased'],
  ])('should map %s method to %s method', (from, to) => {
    expect(mapping.mapFunctionName(from)).toBe(to)
  })
  it('should map "constructor" to mapping name', () => {
    expect(mapping.constructorName).toBe(mapping.name)
  })
})
