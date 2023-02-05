import { int64 } from '../../../../../../common/ys'
import { idFromString, idToString } from '../../../../code/api/common/id'

describe(idFromString, () => {
  it('should convert string to ID', () => {
    expect(idFromString(null)).toBeNull()
    expect(idFromString('')).toBeNull()
    expect(idFromString('1234567890')).toBe(BigInt(1234567890))
    expect(idFromString('abc')).toBeNull()
  })
})
describe(idToString, () => {
  it('should convert ID to string', () => {
    expect(idToString(null)).toBeNull()
    expect(idToString(int64(10000))).toBe('10000')
  })
})
