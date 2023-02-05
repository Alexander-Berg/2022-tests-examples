import { Nullable, Throwing, Int32 } from '../../../../common/ys'
import { FillNewCard } from '../feature/fill-new-card-feature'
import { normalizeCardNumber, normalizeExpirationDate } from '../utils/payment-utils'
import { KeyboardModel } from './keyboard-model'
import { PaymentScreenTitleLabel, PaymentScreenTitleModel } from './payment-screen-title-model'

export class FillNewCardModel implements FillNewCard {
  private cardNumber: string = ''
  private expirationDate: string = ''
  private cvv: string = ''
  private saveCardCheckbox: boolean = true
  private focusedField: Nullable<NewCardField> = NewCardField.cardNumber
  private mode: Nullable<NewCardMode> = null

  public constructor(
    private readonly paymentScreenTitleModel: PaymentScreenTitleModel,
    private readonly isSomePaymentMethodsAvailable: boolean,
    private readonly keyboardModel: KeyboardModel,
  ) {}

  public waitForNewCardScreen(mSec: Int32): Throwing<boolean> {
    return true
  }

  public getFieldValue(field: NewCardField): Throwing<string> {
    switch (field) {
      case NewCardField.cardNumber:
        return this.cardNumber
      case NewCardField.expirationDate:
        return this.expirationDate
      case NewCardField.cvv:
        return this.cvv
    }
  }

  public setFieldValue(field: NewCardField, value: string): Throwing<void> {
    switch (field) {
      case NewCardField.cardNumber:
        this.cardNumber = normalizeCardNumber(value)
        break
      case NewCardField.expirationDate:
        this.expirationDate = normalizeExpirationDate(value)
        break
      case NewCardField.cvv:
        this.cvv = value
        break
    }
  }

  public pasteFieldValue(field: NewCardField, value: string): Throwing<void> {
    this.setFieldValue(field, value)
  }

  public tapOnField(field: NewCardField): Throwing<void> {
    this.focusedField = field
    this.keyboardModel.setNumericKeyboardStatus(true)
  }

  public setSaveCardCheckboxEnabled(value: boolean): Throwing<void> {
    this.saveCardCheckbox = value
  }

  public isSaveCardCheckboxEnabled(): Throwing<boolean> {
    return this.saveCardCheckbox
  }

  public resetFields(): Throwing<void> {
    this.cardNumber = ''
    this.expirationDate = ''
    this.cvv = ''
    this.saveCardCheckbox = true
    this.focusedField = NewCardField.cardNumber
    this.keyboardModel.setNumericKeyboardStatus(false)
  }

  public isAllFieldsFilled(): boolean {
    return this.cardNumber !== '' && this.expirationDate !== '' && this.cvv !== ''
  }

  public getNewCardMode(): Nullable<NewCardMode> {
    return this.mode
  }

  public setNewCardMode(mode: Nullable<NewCardMode>): void {
    this.mode = mode
  }

  public getFocusedField(): Nullable<NewCardField> {
    return this.focusedField
  }

  public tapOnBackButton(): Throwing<void> {
    this.paymentScreenTitleModel.setTitle(PaymentScreenTitleLabel.paymentMethod)
    this.keyboardModel.setNumericKeyboardStatus(false)
    this.keyboardModel.setAlphabeticalKeyboardStatus(false)
  }

  public isBackButtonShown(): Throwing<boolean> {
    return this.isSomePaymentMethodsAvailable
  }
}

export enum NewCardField {
  cardNumber = 'cardNumber',
  expirationDate = 'expirationDate',
  cvv = 'cvv',
}

export enum NewCardMode {
  bind,
  pay,
  preselect,
}
