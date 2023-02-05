import {
  CardBinRange,
  CardBinRangeBuilder,
  CardValidationError,
  CardValidationException,
  CompositeCardFieldValidator,
  CardFieldValidator,
  DefaultCardNumberValidator,
  CardField,
  CardNumberField,
  DefaultCardCvnValidator,
  DefaultCardExpirationDateValidator,
  CardMinExpirationDateValidator,
  CardBinRangeValidator,
  CardValidators,
  SuccessCardFieldValidator,
  FailureCardFieldValidator,
  DefaultCardEmailValidator,
  DefaultCardPhoneValidator,
  LuhnCardNumberValidator,
  LengthCardCvnValidator,
  LengthCardNumberValidator,
} from '../../../code/busilogics/card-validation'
import { CardNetworks } from '../../../code/models/card-networks'

describe(CardFieldValidator, () => {
  describe(CompositeCardFieldValidator, () => {
    it('should delegate to validators', () => {
      const validator = new CompositeCardFieldValidator<CardNumberField>()
      expect(validator.validate(CardField.number(''))).toBeNull()

      validator.addValidator(new SuccessCardFieldValidator())
      expect(validator.validate(CardField.number(''))).toBeNull()

      validator.addValidator(new FailureCardFieldValidator(CardValidationError.default))
      expect(validator.validate(CardField.number(''))).toBe(CardValidationError.default)
    })
  })

  it('should create composite validator from regular one', () => {
    const successValidator = new SuccessCardFieldValidator().composite()
    expect(successValidator.validate(CardField.number(''))).toBeNull()

    const failureValidator = new FailureCardFieldValidator(CardValidationError.default).composite()
    expect(failureValidator.validate(CardField.number(''))).toBe(CardValidationError.default)
  })

  describe('default validators', () => {
    describe(DefaultCardNumberValidator, () => {
      it('should validate values', () => {
        const validator = new DefaultCardNumberValidator()

        expect(validator.validate(CardField.number('1100001932369726'))).toBeNull()
        expect(validator.validate(CardField.number('abc'))).toBe(CardValidationError.default)
      })
    })

    describe(LengthCardNumberValidator, () => {
      it('should validate values', () => {
        const validator = new LengthCardNumberValidator([16])
        expect(validator.validate(CardField.number('510000000000000'))).toBe(CardValidationError.default)
        expect(validator.validate(CardField.number('5100000000000000'))).toBeNull()
        expect(validator.validate(CardField.number('51000000000000000'))).toBe(CardValidationError.default)
      })

      it('should create with CardType', () => {
        const validator = LengthCardNumberValidator.withCardNetwork(CardNetworks.masterCard)
        expect(validator.validate(CardField.number('5100000000000000'))).toBeNull()
      })

      it('should validate card number', () => {
        expect(LengthCardNumberValidator.validateCardLength('5100000000000000')).toBe(true)
      })

      it('should check against unknown Card Type for unknown card network', () => {
        expect(LengthCardNumberValidator.validateCardLength('0000000000000000')).toBe(true)
        expect(LengthCardNumberValidator.validateCardLength('00000000000000000000')).toBe(false)
      })
    })

    describe(DefaultCardExpirationDateValidator, () => {
      it('should validate values', () => {
        expect(
          new DefaultCardExpirationDateValidator(new Date('2020-09-01')).validate(CardField.expirationDate('10', '20')),
        ).toBeNull()
        expect(
          new DefaultCardExpirationDateValidator(new Date('2020-10-01')).validate(CardField.expirationDate('10', '20')),
        ).toBeNull()
        expect(
          new DefaultCardExpirationDateValidator(new Date('2020-11-01')).validate(CardField.expirationDate('10', '20')),
        ).toBe(CardValidationError.default)
        expect(
          new DefaultCardExpirationDateValidator(new Date('2021-09-01')).validate(CardField.expirationDate('10', '20')),
        ).toBe(CardValidationError.default)
        expect(
          new DefaultCardExpirationDateValidator(new Date('2020-01-01')).validate(CardField.expirationDate('13', '21')),
        ).toBe(CardValidationError.default)
        expect(
          new DefaultCardExpirationDateValidator(new Date('2020-01-01')).validate(CardField.expirationDate('00', '21')),
        ).toBe(CardValidationError.default)
        expect(
          new DefaultCardExpirationDateValidator(new Date('2020-09-01')).validate(CardField.expirationDate('10', '71')),
        ).toBe(CardValidationError.default)
        expect(
          new DefaultCardExpirationDateValidator(new Date('2020-01-01')).validate(
            CardField.expirationDate('foo', '21'),
          ),
        ).toBe(CardValidationError.default)
        expect(
          new DefaultCardExpirationDateValidator(new Date('2020-01-01')).validate(
            CardField.expirationDate('01', 'foo'),
          ),
        ).toBe(CardValidationError.default)

        // silence default arg coverage
        new DefaultCardExpirationDateValidator()
      })
    })

    describe(DefaultCardCvnValidator, () => {
      it('should validate values', () => {
        const validator = new DefaultCardCvnValidator()

        expect(validator.validate(CardField.cvn('123'))).toBeNull()
        expect(validator.validate(CardField.cvn('abc'))).toBe(CardValidationError.default)
      })
    })

    describe(LengthCardCvnValidator, () => {
      it('should validate values', () => {
        const validator = new LengthCardCvnValidator(3)

        expect(validator.validate(CardField.cvn('12'))).toBe(CardValidationError.default)
        expect(validator.validate(CardField.cvn('123'))).toBeNull()
        expect(validator.validate(CardField.cvn('1234'))).toBe(CardValidationError.default)
      })
      it('should create with CardType', () => {
        const validator = LengthCardCvnValidator.withCardNetwork(CardNetworks.masterCard)
        expect(validator.validate(CardField.number('123'))).toBeNull()
      })
    })

    describe(DefaultCardEmailValidator, () => {
      it('should validate values', () => {
        const validator = new DefaultCardEmailValidator((email) =>
          email === 'expected' ? null : CardValidationError.default,
        )
        expect(validator.validate(CardField.email('expected'))).toBeNull()
        expect(validator.validate(CardField.email(''))).toBe(CardValidationError.default)
      })
    })

    describe(DefaultCardPhoneValidator, () => {
      it('should validate values', () => {
        const validator = new DefaultCardPhoneValidator((phone) =>
          phone === 'expected' ? null : CardValidationError.default,
        )
        expect(validator.validate(CardField.phone('expected'))).toBeNull()
        expect(validator.validate(CardField.phone(''))).toBe(CardValidationError.default)
      })
    })
  })

  describe('custom validators', () => {
    describe(CardMinExpirationDateValidator, () => {
      it('should validate values', () => {
        const dateError = CardValidationError.custom('date error')
        expect(() => CardMinExpirationDateValidator.create(21, 0, dateError)).toThrowError('Invalid month specified: 0')

        const validator = CardMinExpirationDateValidator.create(2021, 8, dateError)
        expect(validator.validate(CardField.expirationDate('10', '21'))).toBeNull()
        expect(validator.validate(CardField.expirationDate('', '21'))).toBe(dateError)
        expect(validator.validate(CardField.expirationDate('6', '21'))).toBe(dateError)
        expect(validator.validate(CardField.expirationDate('8', 'year'))).toBe(dateError)
        expect(validator.validate(CardField.expirationDate('1', '22'))).toBeNull()
      })
    })

    describe(CardBinRangeValidator, () => {
      it('should validate values', () => {
        const binError = CardValidationError.custom('bin error')
        const binRange = new CardBinRangeBuilder()
          .addRange('51000000', '51999999')
          .addRange('19999999', '22000000')
          .build()
        const validator = new CardBinRangeValidator(binRange, binError)
        expect(validator.validate(CardField.number('111111'))).toBe(binError)
        expect(validator.validate(CardField.number('200000'))).toBeNull()
        expect(validator.validate(CardField.number('5100007846533373'))).toBeNull()
        expect(validator.validate(CardField.number('2099999933334444'))).toBeNull()
        expect(validator.validate(CardField.number('2116452233334444'))).toBeNull()
        expect(validator.validate(CardField.number('5000004996214392'))).toBe(binError)
        expect(validator.validate(CardField.number('51000078465333731'))).toBeNull()
        expect(validator.validate(CardField.number('abc'))).toBe(binError)
        expect(validator.validate(CardField.number(''))).toBe(binError)
      })
    })

    describe(LuhnCardNumberValidator, () => {
      it('should validate values', () => {
        const validator = new LuhnCardNumberValidator()
        expect(validator.validate(CardField.number('4561261212345467'))).toBeNull()
        expect(validator.validate(CardField.number('abc'))).toBe(CardValidationError.default)
      })
    })
  })
})

describe(CardBinRangeBuilder, () => {
  it('should throw error if provided configuration is incorrect', () => {
    const binRangeBuilder = new CardBinRangeBuilder().addRange('51000000', '51999999')
    expect(binRangeBuilder.addRange('19999999', '29999999')).toBe(binRangeBuilder)
    expect(binRangeBuilder.build()).toStrictEqual([
      new CardBinRange(19999999, 29999999),
      new CardBinRange(51000000, 51999999),
    ])
    expect(() => new CardBinRangeBuilder().addRange('0', '1')).toThrow(CardValidationException)
    expect(() => binRangeBuilder.addRange('5200000', '53000000')).toThrow(CardValidationException)
    expect(() => binRangeBuilder.addRange('53999999', '52000000')).toThrow(CardValidationException)
  })
})

describe(CardValidators, () => {
  it('should create card validation', () => {
    const passingValidator = new SuccessCardFieldValidator()
    const validators = new CardValidators(
      passingValidator,
      passingValidator,
      passingValidator,
      passingValidator,
      passingValidator,
    )

    expect(validators.cvnValidator.validate(CardField.cvn(''))).toBeNull()
    expect(validators.expirationDateValidator.validate(CardField.expirationDate('', ''))).toBeNull()
    expect(validators.emailValidator.validate(CardField.email(''))).toBeNull()
    expect(validators.phoneValidator.validate(CardField.phone(''))).toBeNull()
    expect(validators.numberValidator.validate(CardField.number(''))).toBeNull()
  })
})
