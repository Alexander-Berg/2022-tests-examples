import { currencyCodeFromString, CurrencyCodes, currencyCodeToString } from '../../../code/models/currency-codes'

describe('CurrencyCodes', () => {
  const map: Readonly<{ readonly [key: string]: CurrencyCodes }> = {
    RUB: CurrencyCodes.rub,
    USD: CurrencyCodes.usd,
  }
  it.each(Object.entries(map))('should be deserializable from string %s to CurrencyCodes %s', (key, value) => {
    expect(currencyCodeFromString(key)).toBe(value)
  })
  it('should deserialize into null for unknown string value', () => {
    expect(currencyCodeFromString('unknown')).toBeNull()
  })
  it.each(Object.entries(map))('should be serializable from CurrencyCodes %s to string %s', (key, value) => {
    expect(currencyCodeToString(value)).toBe(key)
  })
})
