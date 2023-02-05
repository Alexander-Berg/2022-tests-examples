import { countryCodeFromString, CountryCodes, countryCodeToString } from '../../../code/models/country-codes'

describe('CountryCodes', () => {
  const map: Readonly<{ readonly [key: string]: CountryCodes }> = {
    RU: CountryCodes.ru,
    US: CountryCodes.us,
  }
  it.each(Object.entries(map))('should be deserializable from string %s to CountryCodes %s', (key, value) => {
    expect(countryCodeFromString(key)).toBe(value)
  })
  it('should deserialize into null for unknown string value', () => {
    expect(countryCodeFromString('unknown')).toBeNull()
  })
  it.each(Object.entries(map))('should be serializable from CountryCodes %s to string %s', (key, value) => {
    expect(countryCodeToString(value)).toBe(key)
  })
})
