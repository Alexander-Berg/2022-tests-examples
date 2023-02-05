import {
  applySpacers,
  CardPaymentSystem,
  CardPaymentSystemChecker,
  cardPaymentSystemFromServerValue,
  CardType,
} from '../../code/busilogics/card-payment-system'

describe(cardPaymentSystemFromServerValue, () => {
  test('parse string values', () => {
    expect(cardPaymentSystemFromServerValue('AmericanExpress')).toBe(CardPaymentSystem.AmericanExpress)
    expect(cardPaymentSystemFromServerValue('DinersClubCarteBlanche')).toBe(CardPaymentSystem.DinersClub)
    expect(cardPaymentSystemFromServerValue('DiscoverCard')).toBe(CardPaymentSystem.DiscoverCard)
    expect(cardPaymentSystemFromServerValue('JCB')).toBe(CardPaymentSystem.JCB)
    expect(cardPaymentSystemFromServerValue('Maestro')).toBe(CardPaymentSystem.Maestro)
    expect(cardPaymentSystemFromServerValue('MasterCard')).toBe(CardPaymentSystem.MasterCard)
    expect(cardPaymentSystemFromServerValue('MIR')).toBe(CardPaymentSystem.MIR)
    expect(cardPaymentSystemFromServerValue('UnionPay')).toBe(CardPaymentSystem.UnionPay)
    expect(cardPaymentSystemFromServerValue('Uzcard')).toBe(CardPaymentSystem.Uzcard)
    expect(cardPaymentSystemFromServerValue('VISA')).toBe(CardPaymentSystem.VISA)
    expect(cardPaymentSystemFromServerValue('VISA_ELECTRON')).toBe(CardPaymentSystem.VISA_ELECTRON)
    expect(cardPaymentSystemFromServerValue('')).toBe(CardPaymentSystem.UNKNOWN)
    expect(cardPaymentSystemFromServerValue('unknown')).toBe(CardPaymentSystem.UNKNOWN)
  })
})

describe(CardPaymentSystemChecker, () => {
  const checker = CardPaymentSystemChecker.instance

  it('should return American Express', () => {
    expect(checker.lookup('3400 0000 0000 0000')).toBe(CardPaymentSystem.AmericanExpress)
    expect(checker.lookup('3700 0000 0000 0000')).toBe(CardPaymentSystem.AmericanExpress)
  })
  it('should return Maestro', () => {
    expect(checker.lookup('5000 0000 0000 0000')).toBe(CardPaymentSystem.Maestro)
    expect(checker.lookup('5600 0000 0000 0000')).toBe(CardPaymentSystem.Maestro)
    expect(checker.lookup('5700 0000 0000 0000')).toBe(CardPaymentSystem.Maestro)
    expect(checker.lookup('5800 0000 0000 0000')).toBe(CardPaymentSystem.Maestro)
    expect(checker.lookup('6300 0000 0000 0000')).toBe(CardPaymentSystem.Maestro)
    expect(checker.lookup('6700 0000 0000 0000')).toBe(CardPaymentSystem.Maestro)
  })
  it('should return MasterCard', () => {
    expect(checker.lookup('5100 0000 0000 0000')).toBe(CardPaymentSystem.MasterCard)
    expect(checker.lookup('5200 0000 0000 0000')).toBe(CardPaymentSystem.MasterCard)
    expect(checker.lookup('5300 0000 0000 0000')).toBe(CardPaymentSystem.MasterCard)
    expect(checker.lookup('5400 0000 0000 0000')).toBe(CardPaymentSystem.MasterCard)
    expect(checker.lookup('5500 0000 0000 0000')).toBe(CardPaymentSystem.MasterCard)
  })
  it('should return Mir', () => {
    expect(checker.lookup('2199 0000 0000 0000')).toBe(CardPaymentSystem.UNKNOWN)
    expect(checker.lookup('2200 0000 0000 0000')).toBe(CardPaymentSystem.MIR)
    expect(checker.lookup('2204 0000 0000 0000')).toBe(CardPaymentSystem.MIR)
    expect(checker.lookup('2205 0000 0000 0000')).toBe(CardPaymentSystem.UNKNOWN)
  })
  it('should return VISA', () => {
    expect(checker.lookup('4000 0000 0000 0000')).toBe(CardPaymentSystem.VISA)
  })
  it('should return unknown', () => {
    expect(checker.lookup('0000 0000 0000 0000')).toBe(CardPaymentSystem.UNKNOWN)
    expect(checker.lookup('')).toBe(CardPaymentSystem.UNKNOWN)
    expect(checker.lookup('abc')).toBe(CardPaymentSystem.UNKNOWN)
    expect(checker.lookup('2')).toBe(CardPaymentSystem.UNKNOWN)
  })
})

describe(CardType, () => {
  it('should return all card types', () => {
    expect(CardType.getAllCardTypes()).toEqual([
      {
        paymentSystem: CardPaymentSystem.AmericanExpress,
        patterns: [
          { intervalStart: '34', intervalEnd: null },
          { intervalStart: '37', intervalEnd: null },
        ],
        validLengths: [15],
        cvvLength: 4,
        spacers: [4, 10],
      },
      {
        paymentSystem: CardPaymentSystem.DinersClub,
        patterns: [
          { intervalStart: '300', intervalEnd: '305' },
          { intervalStart: '36', intervalEnd: null },
        ],
        validLengths: [14],
        cvvLength: 3,
        spacers: [4, 10],
      },
      {
        paymentSystem: CardPaymentSystem.DiscoverCard,
        patterns: [
          { intervalStart: '6011', intervalEnd: null },
          { intervalStart: '622126', intervalEnd: '622925' },
          { intervalStart: '644', intervalEnd: '649' },
          { intervalStart: '65', intervalEnd: null },
        ],
        validLengths: [16],
        cvvLength: 3,
        spacers: [4, 8, 12],
      },
      {
        paymentSystem: CardPaymentSystem.JCB,
        patterns: [{ intervalStart: '3528', intervalEnd: '3589' }],
        validLengths: [16],
        cvvLength: 3,
        spacers: [4, 8, 12],
      },
      {
        paymentSystem: CardPaymentSystem.Maestro,
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
      },
      {
        paymentSystem: CardPaymentSystem.MasterCard,
        patterns: [
          { intervalStart: '222100', intervalEnd: '272099' },
          { intervalStart: '51', intervalEnd: '55' },
        ],
        validLengths: [16],
        cvvLength: 3,
        spacers: [4, 8, 12],
      },
      {
        paymentSystem: CardPaymentSystem.MIR,
        patterns: [{ intervalStart: '2200', intervalEnd: '2204' }],
        validLengths: [16, 17, 18, 19],
        cvvLength: 3,
        spacers: [4, 8, 12],
      },
      {
        paymentSystem: CardPaymentSystem.UnionPay,
        patterns: [
          { intervalStart: '35', intervalEnd: null },
          { intervalStart: '62', intervalEnd: null },
          { intervalStart: '88', intervalEnd: null },
        ],
        validLengths: [16, 17, 18, 19],
        cvvLength: 3,
        spacers: [4, 8, 12],
      },
      {
        paymentSystem: CardPaymentSystem.Uzcard,
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
      },
      {
        paymentSystem: CardPaymentSystem.VISA,
        patterns: [{ intervalStart: '4', intervalEnd: null }],
        validLengths: [13, 16, 18, 19],
        cvvLength: 3,
        spacers: [4, 8, 12],
      },
      {
        paymentSystem: CardPaymentSystem.VISA_ELECTRON,
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
      },
      {
        paymentSystem: CardPaymentSystem.UNKNOWN,
        patterns: [],
        validLengths: [12, 13, 14, 15, 16, 17, 18, 19],
        cvvLength: 3,
        spacers: [4, 8, 12],
      },
    ])
  })
  it('should create from card number', () => {
    expect(CardType.cardTypeFromCardNumber('5100')).toStrictEqual(
      CardType.cardTypeByPaymentSystem(CardPaymentSystem.MasterCard),
    )
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
