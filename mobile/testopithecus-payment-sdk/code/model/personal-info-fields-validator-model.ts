import { Throwing } from '../../../../common/ys'
import { PersonalInformationFieldsValidator } from '../feature/personal-information-fields-validator-feature'
import { isCharDigit } from '../utils/payment-utils'
import { PersonalInformationField, PersonalInformationModel } from './personal-information-model'

export class PersonalInfoFieldsValidatorModel implements PersonalInformationFieldsValidator {
  private phoneNumber: string = ''
  private phoneNumberErrorShown: boolean = false
  private email: string = ''
  private emailErrorShown: boolean = false

  public constructor(private readonly personalInformationModel: PersonalInformationModel) {}

  public getEmailErrorText(): Throwing<string> {
    this.emailErrorShown = this.isEmailValidationErrorShown()
    return this.emailErrorShown ? PersonalInformationValidationError.email.toString() : ''
  }

  public getPhoneNumberErrorText(): Throwing<string> {
    this.phoneNumberErrorShown = this.isPhoneNumberValidationErrorShown()
    return this.phoneNumberErrorShown ? PersonalInformationValidationError.phoneNumber.toString() : ''
  }

  private isPhoneNumberValid(): Throwing<boolean> {
    const phoneNumber = this.personalInformationModel.getFieldValue(PersonalInformationField.phoneNumber)
    return (
      phoneNumber.length === 0 ||
      (this.allCharDigit(phoneNumber) &&
        (((phoneNumber.startsWith('7') || phoneNumber.startsWith('8')) && phoneNumber.length === 11) ||
          (phoneNumber.startsWith('+7') && phoneNumber.length === 12)))
    )
  }

  private allCharDigit(str: string): boolean {
    return str.split('').filter((item) => isCharDigit(item) || item === '+').length === str.length
  }

  private isPhoneNumberValidationErrorShown(): Throwing<boolean> {
    const phoneNumber = this.personalInformationModel.getFieldValue(PersonalInformationField.phoneNumber)
    if (this.phoneNumberErrorShown && this.phoneNumber === phoneNumber) {
      return true
    }
    this.phoneNumber = phoneNumber
    const focusedField = this.personalInformationModel.getFocusedField()
    const isPhoneNumberValid = this.isPhoneNumberValid()
    return !isPhoneNumberValid && focusedField !== PersonalInformationField.phoneNumber
  }

  private isEmailValid(): Throwing<boolean> {
    const email = this.personalInformationModel.getFieldValue(PersonalInformationField.email).split('')
    return (
      email.length === 0 ||
      (email.includes('.') && email.length >= 5 && email.filter((item) => item === '@').length === 1)
    )
  }

  private isEmailValidationErrorShown(): Throwing<boolean> {
    const email = this.personalInformationModel.getFieldValue(PersonalInformationField.email)
    if (this.emailErrorShown && this.email === email) {
      return true
    }
    this.email = email
    const focusedField = this.personalInformationModel.getFocusedField()
    const isEmailValid = this.isEmailValid()
    return !isEmailValid && focusedField !== PersonalInformationField.email
  }

  public resetFields(): void {
    this.emailErrorShown = false
    this.email = ''
    this.phoneNumberErrorShown = false
    this.phoneNumber = ''
  }
}

export enum PersonalInformationValidationError {
  phoneNumber = 'Enter your phone number in the format +70123456789',
  email = 'Enter an email address in the format mail@example.com',
}
