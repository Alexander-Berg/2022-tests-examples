import { Token } from '../../../../code/api/entities/token'

describe(Token, () => {
  it('should generate authorization header', () => {
    const sample = '12345'
    const token = new Token(sample)
    expect(token.asHeaderValue()).toStrictEqual('OAuth ' + sample)
  })
})
