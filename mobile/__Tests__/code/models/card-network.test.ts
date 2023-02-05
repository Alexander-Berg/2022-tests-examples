import { CardNetworks, cardNetworkFromString, cardNetworkToString } from '../../../code/models/card-networks'

describe('CardNetworks', () => {
  const map: Readonly<{ readonly [key: string]: CardNetworks }> = {
    AMEX: CardNetworks.amex,
    DISCOVER: CardNetworks.discover,
    JCB: CardNetworks.jcb,
    MASTERCARD: CardNetworks.masterCard,
    VISA: CardNetworks.visa,
    MIR: CardNetworks.mir,
    UNIONPAY: CardNetworks.unionPay,
    UZCARD: CardNetworks.uzCard,
    MAESTRO: CardNetworks.maestro,
    VISAELECTRON: CardNetworks.visaElectron,
  }
  it.each(Object.entries(map))('should deserialize string %s to CardNetwork %s', (key, value) => {
    expect(cardNetworkFromString(key)).toBe(value)
  })
  it('should deserialize into null for unknown string value', () => {
    expect(cardNetworkFromString('unknown')).toBeNull()
  })
  it.each(Object.entries(map))('should serialize CardNetwork %s to string %s', (key, value) => {
    expect(cardNetworkToString(value)).toBe(key)
  })
})
