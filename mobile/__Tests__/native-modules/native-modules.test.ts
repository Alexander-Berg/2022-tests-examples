import { Encoding } from '../../code/file-system/file-system-types'
import { ArrayBufferHelpers, percentDecode, percentEncode } from '../../native-modules/native-modules'

describe(ArrayBufferHelpers, () => {
  const originalString = 'Test string'

  it('should encode & decode Utf8', () => {
    const buffer = ArrayBufferHelpers.arrayBufferFromString(originalString, Encoding.Utf8).getValue()
    const decodedString = ArrayBufferHelpers.arrayBufferToString(buffer, Encoding.Utf8).getValue()
    expect(decodedString).toBe(originalString)
  })
  it('should encode & decode Base64', () => {
    const buffer = ArrayBufferHelpers.arrayBufferFromString(originalString, Encoding.Utf8).getValue()
    const decodedString = ArrayBufferHelpers.arrayBufferToString(buffer, Encoding.Base64).getValue()
    expect(decodedString).toBe('VGVzdCBzdHJpbmc=')
    const reencodedBuffer = ArrayBufferHelpers.arrayBufferFromString(decodedString, Encoding.Base64).getValue()
    const reencodedString = ArrayBufferHelpers.arrayBufferToString(reencodedBuffer, Encoding.Utf8).getValue()
    expect(reencodedString).toBe(originalString)
  })
})

describe('percent encoding', () => {
  it('should encode', () => {
    expect(percentEncode('ABC', false)).toBe('ABC')
    expect(percentEncode('АБВ', false)).toBe('%D0%90%D0%91%D0%92')

    expect(percentEncode('ABC+abc 123', false)).toBe('ABC%2Babc%20123')
    expect(percentEncode('ABC+abc 123', true)).toBe('ABC%2Babc+123')
  })

  it('should decode', () => {
    expect(percentDecode('ABC', false)).toBe('ABC')
    expect(percentDecode('%D0%90%D0%91%D0%92', false)).toBe('АБВ')

    expect(percentDecode('ABC%2Babc%20123+456', false)).toBe('ABC+abc 123+456')
    expect(percentDecode('ABC%2Babc%20123+456', true)).toBe('ABC+abc 123 456')
  })
})
