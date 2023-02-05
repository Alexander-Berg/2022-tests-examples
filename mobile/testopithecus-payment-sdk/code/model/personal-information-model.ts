import { Nullable, Throwing } from '../../../../common/ys'
import { PersonalInformation } from '../feature/personal-information-feature'
import { AuthorizationMode } from '../sample/sample-configuration'
import { KeyboardModel } from './keyboard-model'
import { ReadPaymentDetailsModel } from './payment-details-model'

export class PersonalInformationModel implements PersonalInformation {
  private name: string = ''
  private lastName: string = ''
  private phoneNumber: string = ''
  private email: string
  private focusedField: Nullable<PersonalInformationField> = null

  public constructor(
    private readonly paymentDetailsModel: ReadPaymentDetailsModel,
    private readonly keyboardModel: KeyboardModel,
  ) {
    this.email =
      this.paymentDetailsModel.getAuthorizationMode() === AuthorizationMode.authorized
        ? this.paymentDetailsModel.getAccount().account.login + '@yandex.ru'
        : ''
  }

  public getFieldValue(field: PersonalInformationField): Throwing<string> {
    switch (field) {
      case PersonalInformationField.firstName:
        return this.name
      case PersonalInformationField.lastName:
        return this.lastName
      case PersonalInformationField.phoneNumber:
        return this.phoneNumber
      case PersonalInformationField.email:
        return this.email
    }
  }

  public getFocusedField(): Nullable<PersonalInformationField> {
    return this.focusedField
  }

  public pasteFieldValue(field: PersonalInformationField, value: string): Throwing<void> {
    this.setFieldValue(field, value)
  }

  public setFieldValue(field: PersonalInformationField, value: string): Throwing<void> {
    switch (field) {
      case PersonalInformationField.firstName:
        this.name = value
        break
      case PersonalInformationField.lastName:
        this.lastName = value
        break
      case PersonalInformationField.phoneNumber:
        this.phoneNumber = value
        break
      case PersonalInformationField.email:
        this.email = value
        break
    }
  }

  public tapOnField(field: PersonalInformationField): Throwing<void> {
    this.focusedField = field
    this.keyboardModel.setAlphabeticalKeyboardStatus(true)
  }

  public resetFields(): Throwing<void> {
    this.name = ''
    this.lastName = ''
    this.phoneNumber = ''
    this.email = ''
    this.focusedField = null
    this.keyboardModel.setAlphabeticalKeyboardStatus(false)
  }
}

export enum PersonalInformationField {
  firstName,
  lastName,
  phoneNumber,
  email,
}
