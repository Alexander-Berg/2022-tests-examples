import { checkLuhn } from '../../../code/busilogics/check-luhn'

describe(checkLuhn, () => {
  it('should verify card numbers', () => {
    expect(checkLuhn('')).toBe(true)
    expect(checkLuhn('abc')).toBe(false)
    expect(checkLuhn('4561261212345467')).toBe(true)
    expect(checkLuhn('4561261212345464')).toBe(false)
    expect(checkLuhn('1234567812345670')).toBe(true)
    expect(checkLuhn('1234567812345678')).toBe(false)
  })
})
