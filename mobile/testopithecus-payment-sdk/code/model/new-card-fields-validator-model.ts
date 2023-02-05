import { checkLuhn } from '../../../payment-sdk/code/busilogics/check-luhn'
import { stringToInt32, Throwing } from '../../../../common/ys'
import { LengthCardNumberValidator } from '../../../payment-sdk/code/busilogics/card-validation'
import { NewCardFieldsValidator } from '../feature/new-card-fields-validator-feature'
import { FillNewCardModel, NewCardField } from './fill-new-card-model'

export class NewCardFieldsValidatorModel implements NewCardFieldsValidator {
  public constructor(private readonly fillNewCardModel: FillNewCardModel) {}

  private cardNumberValue: string = ''
  private cardNumberErrorShown: boolean = false
  private cvvValue: string = ''
  private cvvErrorShown: boolean = false
  private expirationDateValue: string = ''
  private expirationDateErrorShown: boolean = false

  public getCardNumberErrorText(): Throwing<string> {
    this.cardNumberErrorShown = this.isCardNumberValidationErrorShown()
    return this.cardNumberErrorShown ? NewCardFieldsValidationError.cardNumber.toString() : ''
  }

  public getCvvErrorText(): Throwing<string> {
    this.cvvErrorShown = this.isCvvValidationErrorShown()
    return this.cvvErrorShown ? NewCardFieldsValidationError.cvv.toString() : ''
  }

  public getExpirationDateErrorText(): Throwing<string> {
    this.expirationDateErrorShown = this.isExpirationDateValidationErrorShown()
    return this.expirationDateErrorShown ? NewCardFieldsValidationError.expirationDate.toString() : ''
  }

  private isExpirationDateValid(): Throwing<boolean> {
    const expirationDate = this.fillNewCardModel.getFieldValue(NewCardField.expirationDate)

    const expirationMonth = stringToInt32(expirationDate.slice(0, 2)) ?? 0
    const expirationYear = stringToInt32(expirationDate.slice(2, 4)) ?? 0

    return expirationDate.length === 4 && expirationMonth > 0 && expirationMonth <= 12 && expirationYear >= 22
  }

  private isCardNumberValidationErrorShown(): Throwing<boolean> {
    const cardNumber = this.fillNewCardModel.getFieldValue(NewCardField.cardNumber)
    if (this.cardNumberErrorShown && this.cardNumberValue === cardNumber) {
      return true
    }
    const focusedField = this.fillNewCardModel.getFocusedField()
    this.cardNumberValue = cardNumber
    return (
      cardNumber.length !== 0 &&
      (!LengthCardNumberValidator.validateCardLength(cardNumber) || !checkLuhn(cardNumber)) &&
      focusedField !== NewCardField.cardNumber
    )
  }

  private isExpirationDateValidationErrorShown(): Throwing<boolean> {
    const expirationDate = this.fillNewCardModel.getFieldValue(NewCardField.expirationDate)
    if (this.expirationDateErrorShown && this.expirationDateValue === expirationDate) {
      return true
    }
    this.expirationDateValue = expirationDate
    const focusedField = this.fillNewCardModel.getFocusedField()
    if (focusedField === NewCardField.expirationDate || expirationDate === '') {
      return false
    }
    const isExpirationDateValid = this.isExpirationDateValid()
    return !isExpirationDateValid
  }

  private isCvvValidationErrorShown(): Throwing<boolean> {
    const cvv = this.fillNewCardModel.getFieldValue(NewCardField.cvv)
    if (this.cvvErrorShown && this.cvvValue === cvv) {
      return true
    }
    const focusedField = this.fillNewCardModel.getFocusedField()
    this.cvvValue = cvv
    return cvv.length > 0 && cvv.length < 3 && focusedField !== NewCardField.cvv
  }

  public resetFields(): void {
    this.cardNumberErrorShown = false
    this.expirationDateErrorShown = false
    this.cvvErrorShown = false
    this.cardNumberValue = ''
    this.expirationDateValue = ''
    this.cvvValue = ''
  }
}

export enum NewCardFieldsValidationError {
  cardNumber = 'Check the card number',
  expirationDate = 'Incorrect date',
  cvv = 'Incorrect CVV',
}
