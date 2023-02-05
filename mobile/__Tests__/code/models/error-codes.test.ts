import { errorCodeFromString, ErrorCodes, errorCodeToString } from '../../../code/models/error-codes'

describe('ErrorCodes', () => {
  const map: Readonly<{ readonly [key: string]: ErrorCodes }> = {
    AMOUNT_LIMIT_EXCEEDED: ErrorCodes.amountLimitExceeded,
    AMOUNT_MISMATCH: ErrorCodes.amountMismatch,
    CARD_NETWORK_NOT_SUPPORTED: ErrorCodes.cardNetworkNotSupported,
    CARD_NOT_FOUND: ErrorCodes.cardNotFound,
    CODE_CHECK_FAILED: ErrorCodes.codeCheckFailed,
    GATEWAY_NOT_FOUND: ErrorCodes.gatewayNotFound,
    INSECURE_MERCHANT_ORIGIN: ErrorCodes.insecureMerchantOrigin,
    INVALID_AMOUNT: ErrorCodes.invalidAmount,
    INVALID_COUNTRY: ErrorCodes.invalidCountry,
    INVALID_CURRENCY: ErrorCodes.invalidCurrency,
    INVALID_VERSION: ErrorCodes.invalidVersion,
    MERCHANT_NOT_FOUND: ErrorCodes.merchantNotFound,
    MERCHANT_ORIGIN_ERROR: ErrorCodes.merchantOriginError,
  }
  it.each(Object.entries(map))('should be deserializable from string %s to ErrorCodes %s', (key, value) => {
    expect(errorCodeFromString(key)).toBe(value)
  })
  it('should deserialize into null for unknown string value', () => {
    expect(errorCodeFromString('unknown')).toBeNull()
  })
  it.each(Object.entries(map))('should be serializable from ErrorCodes %s to string %s', (key, value) => {
    expect(errorCodeToString(value)).toBe(key)
  })
})
