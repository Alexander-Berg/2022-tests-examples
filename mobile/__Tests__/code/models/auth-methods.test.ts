import { AuthMethods, authMethodFromString, authMethodToString } from '../../../code/models/auth-methods'

describe('AuthMethods', () => {
  const map: Readonly<{ readonly [key: string]: AuthMethods }> = {
    PAN_ONLY: AuthMethods.panOnly,
    CLOUD_TOKEN: AuthMethods.cloudToken,
  }
  it.each(Object.entries(map))('should be deserializable from string %s to AuthMethods %s', (key, value) => {
    expect(authMethodFromString(key)).toBe(value)
  })
  it('should deserialize into null for unknown string value', () => {
    expect(authMethodFromString('unknown')).toBeNull()
  })
  it.each(Object.entries(map))('should be serializable from AuthMethods %s to string %s', (key, value) => {
    expect(authMethodToString(value)).toBe(key)
  })
})
