import { NewCard } from '../../../code/models/new-card'

describe(NewCard, () => {
  it('build NewCard', () => {
    const card = new NewCard('1234', '12', '21', '123', true)
    expect(card.cardNumber).toStrictEqual('1234')
    expect(card.expirationMonth).toStrictEqual('12')
    expect(card.expirationYear).toStrictEqual('21')
    expect(card.cvn).toStrictEqual('123')
    expect(card.shouldBeStored).toBeTruthy()
  })
})
