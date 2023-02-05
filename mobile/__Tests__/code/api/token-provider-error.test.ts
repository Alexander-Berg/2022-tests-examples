import { TokenProviderError } from '../../../code/api/token-provider-error'

describe(TokenProviderError, () => {
  it('should return "Token Provider Error" in message', () => {
    expect(new TokenProviderError().message).toBe('Token Provider Error')
  })
})
