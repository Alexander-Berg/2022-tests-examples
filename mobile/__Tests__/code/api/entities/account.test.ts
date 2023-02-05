import { Account } from '../../../../code/api/entities/account'

describe(Account, () => {
  it('should provide uuid', () => {
    const uuid = '12345'
    const account = new Account(uuid)
    expect(account.uuid).toStrictEqual(uuid)
  })
})
