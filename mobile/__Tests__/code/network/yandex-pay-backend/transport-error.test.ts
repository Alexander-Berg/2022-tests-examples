import { YSError } from '../../../../../../common/ys'
import { TransportError, TransportErrorCodes } from '../../../../code/network/yandex-pay-backend/transport-errors'

describe(TransportError, () => {
  it('should build NoPayload error', () => {
    const actual = TransportError.payloadError('PAYLOAD ERROR')
    expect(actual.code).toBe(TransportErrorCodes.noPayload)
    expect(actual.message).toBe('PAYLOAD ERROR')
    expect(actual.underlyingError).toBeNull()
  })
  it('should build JsonDeserializationError error', () => {
    const actual = TransportError.serializationError('SERIALIZATION ERROR', new YSError('MESSAGE'))
    expect(actual.code).toBe(TransportErrorCodes.jsonSerializationError)
    expect(actual.message).toBe('SERIALIZATION ERROR')
    expect(actual.underlyingError).toStrictEqual(new YSError('MESSAGE'))
  })
  it('should build DataFormatError error', () => {
    const actual = TransportError.dataFormatError('DATA ERROR')
    expect(actual.code).toBe(TransportErrorCodes.dataFormatError)
    expect(actual.message).toBe('DATA ERROR')
    expect(actual.underlyingError).toBeNull()
  })
  it('should build BadStatus error', () => {
    const actual = TransportError.badStatusCode('BAD STATUS')
    expect(actual.code).toBe(TransportErrorCodes.badStatusCode)
    expect(actual.message).toBe('BAD STATUS')
    expect(actual.underlyingError).toBeNull()
  })
})
