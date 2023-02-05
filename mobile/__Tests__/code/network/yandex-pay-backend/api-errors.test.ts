import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'

describe(APIError, () => {
  it('should return true for errors in response', () => {
    expect(APIError.isError('fail', 500)).toBe(true)
  })
  it('should return false if no error in response', () => {
    expect(APIError.isError('success', 200)).toBe(false)
  })
})
