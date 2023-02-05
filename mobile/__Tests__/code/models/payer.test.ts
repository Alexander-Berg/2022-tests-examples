import { Payer } from '../../../code/models/payer'

describe(Payer, () => {
  it('build Payer', () => {
    const payer = new Payer('token', 'uid', 'test@ya.ru')
    expect(payer.oauthToken).toStrictEqual('token')
    expect(payer.uid).toStrictEqual('uid')
    expect(payer.email).toStrictEqual('test@ya.ru')
  })
  it('build Payer without auth', () => {
    const payer = new Payer(null, null, 'test@ya.ru')
    expect(payer.oauthToken).toBeNull()
    expect(payer.uid).toBeNull()
    expect(payer.email).toStrictEqual('test@ya.ru')
  })
})
