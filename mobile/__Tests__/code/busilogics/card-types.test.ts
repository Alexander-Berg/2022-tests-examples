import { applySpacers, CardNetworkChecker, CardType } from '../../../code/busilogics/card-types'
import { CardNetworks } from '../../../code/models/card-networks'

describe(CardNetworkChecker, () => {
  const checker = CardNetworkChecker.instance

  it('should return American Express', () => {
    expect(checker.lookup('3400 0000 0000 0000')).toBe(CardNetworks.amex)
    expect(checker.lookup('3700 0000 0000 0000')).toBe(CardNetworks.amex)
  })
  it('should return Maestro', () => {
    expect(checker.lookup('5000 0000 0000 0000')).toBe(CardNetworks.maestro)
    expect(checker.lookup('5600 0000 0000 0000')).toBe(CardNetworks.maestro)
    expect(checker.lookup('5700 0000 0000 0000')).toBe(CardNetworks.maestro)
    expect(checker.lookup('5800 0000 0000 0000')).toBe(CardNetworks.maestro)
    expect(checker.lookup('6300 0000 0000 0000')).toBe(CardNetworks.maestro)
    expect(checker.lookup('6700 0000 0000 0000')).toBe(CardNetworks.maestro)
  })
  it('should return MasterCard', () => {
    expect(checker.lookup('5100 0000 0000 0000')).toBe(CardNetworks.masterCard)
    expect(checker.lookup('5200 0000 0000 0000')).toBe(CardNetworks.masterCard)
    expect(checker.lookup('5300 0000 0000 0000')).toBe(CardNetworks.masterCard)
    expect(checker.lookup('5400 0000 0000 0000')).toBe(CardNetworks.masterCard)
    expect(checker.lookup('5500 0000 0000 0000')).toBe(CardNetworks.masterCard)
  })
  it('should return Mir', () => {
    expect(checker.lookup('2199 0000 0000 0000')).toBeNull()
    expect(checker.lookup('2200 0000 0000 0000')).toBe(CardNetworks.mir)
    expect(checker.lookup('2204 0000 0000 0000')).toBe(CardNetworks.mir)
    expect(checker.lookup('2205 0000 0000 0000')).toBeNull()
  })
  it('should return VISA', () => {
    expect(checker.lookup('4000 0000 0000 0000')).toBe(CardNetworks.visa)
  })
  it('should return Discover', () => {
    expect(checker.lookup('6011 0000 0000 0000')).toBe(CardNetworks.discover)
    expect(checker.lookup('6449 0000 0000 0000')).toBe(CardNetworks.discover)
  })
  it('should return Union Pay', () => {
    expect(checker.lookup('3500 0000 0000 0000')).toBe(CardNetworks.unionPay)
    expect(checker.lookup('8800 0000 0000 0000 000')).toBe(CardNetworks.unionPay)
  })
  it('should return UzCard', () => {
    expect(checker.lookup('8600 0300 0000 0000')).toBe(CardNetworks.uzCard)
  })
  it('should return null', () => {
    expect(checker.lookup('0000 0000 0000 0000')).toBeNull()
    expect(checker.lookup('')).toBeNull()
    expect(checker.lookup('abc')).toBeNull()
    expect(checker.lookup('2')).toBeNull()
  })
})

describe(CardType, () => {
  it('should return all card types', () => {
    expect(CardType.getAllCardTypes()).toEqual([
      {
        cardNetwork: CardNetworks.amex,
        patterns: [
          { intervalStart: '34', intervalEnd: null },
          { intervalStart: '37', intervalEnd: null },
        ],
        validLengths: [15],
        cvvLength: 4,
        spacers: [4, 10],
      } as CardType,
      {
        cardNetwork: CardNetworks.discover,
        patterns: [
          { intervalStart: '6011', intervalEnd: null },
          { intervalStart: '622126', intervalEnd: '622925' },
          { intervalStart: '644', intervalEnd: '649' },
          { intervalStart: '65', intervalEnd: null },
        ],
        validLengths: [16],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
      {
        cardNetwork: CardNetworks.jcb,
        patterns: [{ intervalStart: '3528', intervalEnd: '3589' }],
        validLengths: [16],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
      {
        cardNetwork: CardNetworks.maestro,
        patterns: [
          { intervalStart: '50', intervalEnd: null },
          { intervalStart: '56', intervalEnd: '59' },
          { intervalStart: '61', intervalEnd: null },
          { intervalStart: '63', intervalEnd: null },
          { intervalStart: '66', intervalEnd: '69' },
        ],
        validLengths: [12, 13, 14, 15, 16, 17, 18, 19],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
      {
        cardNetwork: CardNetworks.masterCard,
        patterns: [
          { intervalStart: '222100', intervalEnd: '272099' },
          { intervalStart: '51', intervalEnd: '55' },
        ],
        validLengths: [16],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
      {
        cardNetwork: CardNetworks.mir,
        patterns: [{ intervalStart: '2200', intervalEnd: '2204' }],
        validLengths: [16, 17, 18, 19],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
      {
        cardNetwork: CardNetworks.unionPay,
        patterns: [
          { intervalStart: '35', intervalEnd: null },
          { intervalStart: '62', intervalEnd: null },
          { intervalStart: '88', intervalEnd: null },
        ],
        validLengths: [16, 17, 18, 19],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
      {
        cardNetwork: CardNetworks.uzCard,
        patterns: [
          { intervalStart: '860002', intervalEnd: '860006' },
          { intervalStart: '860008', intervalEnd: '860009' },
          { intervalStart: '860011', intervalEnd: '860014' },
          { intervalStart: '860020', intervalEnd: null },
          { intervalStart: '860030', intervalEnd: '860031' },
          { intervalStart: '860033', intervalEnd: '860034' },
          { intervalStart: '860038', intervalEnd: null },
          { intervalStart: '860043', intervalEnd: null },
          { intervalStart: '860048', intervalEnd: '860051' },
          { intervalStart: '860053', intervalEnd: null },
          { intervalStart: '860055', intervalEnd: '860060' },
        ],
        validLengths: [16],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
      {
        cardNetwork: CardNetworks.visa,
        patterns: [{ intervalStart: '4', intervalEnd: null }],
        validLengths: [13, 16, 18, 19],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
      {
        cardNetwork: CardNetworks.visaElectron,
        patterns: [
          { intervalStart: '4026', intervalEnd: null },
          { intervalStart: '417500', intervalEnd: null },
          { intervalStart: '4405', intervalEnd: null },
          { intervalStart: '4508', intervalEnd: null },
          { intervalStart: '4844', intervalEnd: null },
          { intervalStart: '4913', intervalEnd: null },
          { intervalStart: '4917', intervalEnd: null },
        ],
        validLengths: [16],
        cvvLength: 3,
        spacers: [4, 8, 12],
      } as CardType,
    ])
  })
  it('should create from card number', () => {
    expect(CardType.cardTypeFromCardNumber('5100')).toStrictEqual(CardType.cardTypeByNetwork(CardNetworks.masterCard))
  })
  it('should return unknown type for unknown card number', () => {
    expect(CardType.cardTypeFromCardNumber('ABCD')).toStrictEqual(CardType.UNKNOWN)
  })
  it('should provide sensible details for unknown card type', () => {
    expect(CardType.UNKNOWN).toEqual({
      cardNetwork: null,
      patterns: [],
      validLengths: [12, 13, 14, 15, 16, 17, 18, 19],
      cvvLength: 3,
      spacers: CardType.REGULAR_SPACERS,
    } as CardType)
  })
})

describe(applySpacers, () => {
  it('should apply spacers', () => {
    expect(applySpacers('12345678', [])).toBe('12345678')
    expect(applySpacers('12345678', [4])).toBe('1234 5678')
    expect(applySpacers('12345678', [4, 8])).toBe('1234 5678')
    expect(applySpacers('12345678', [4, 8, 12])).toBe('1234 5678')
    expect(applySpacers('12345678', [2, 4, 6, 8])).toBe('12 34 56 78')
  })
})
