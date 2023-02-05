import { Int32, Nullable, Throwing } from '../../../../common/ys'
import { AvailableMethods, AvailableMethodsBuilder } from '../../../payment-sdk/code/models/available-methods'
import {
  ApplePay,
  GooglePay,
  MethodsListMode,
  PaymentMethodsList,
  Preselect,
  PreselectCvv,
  SBP,
} from '../feature/payment-methods-list-feature'
import { PaymentMethodName } from '../payment-sdk-data'
import { buildPaymentMethodNameByCardNumber, buildPaymentMethodNameByPaymentMethod } from '../utils/payment-utils'
import { FillNewCardModel, NewCardMode } from './fill-new-card-model'
import { KeyboardModel } from './keyboard-model'
import { PaymentButtonLabel, PaymentButtonModel } from './payment-button-model'
import { PaymentScreenTitleLabel, PaymentScreenTitleModel } from './payment-screen-title-model'

export class PaymentMethodsListModel implements PaymentMethodsList {
  private showKeyboardFirstCvv: boolean = false
  private addedCards: string[] = []

  public constructor(
    private availableMethods: AvailableMethods,
    private readonly paymentScreenTitleModel: PaymentScreenTitleModel,
    private readonly keyboardModel: KeyboardModel,
    private readonly forceCvv: boolean,
    private readonly buttonModel: PaymentButtonModel,
  ) {
    this.showKeyboardFirstCvv = forceCvv
  }

  private selected: Int32 = -1
  private cvv: Nullable<string> = null
  private methodsListMode: MethodsListMode = MethodsListMode.regular

  public waitForPaymentMethods(mSec: Int32): Throwing<boolean> {
    return true
  }

  public getAllMethods(): string[] {
    return new TestPaymentMethodsBuilder()
      .setAvailableMethods(this.availableMethods)
      .setAddedCards(this.addedCards)
      .getAllMethods()
  }

  public getAvailableMethods(): AvailableMethods {
    return this.availableMethods
  }

  public getMethods(): string[] {
    return this.getAllMethods().filter((pm) => ![PaymentMethodName.applePay, PaymentMethodName.googlePay].includes(pm))
  }

  public getCards(): string[] {
    return this.getMethods().filter((pm) => ![PaymentMethodName.otherCard, PaymentMethodName.cash].includes(pm))
  }

  public getSelectedMethodName(): Nullable<string> {
    const methods = this.getMethods()
    return this.selected >= 0 && methods.length > this.selected ? methods[this.selected] : null
  }

  public getSelectedCardName(): Nullable<string> {
    const methods = this.getCards()
    return this.selected >= 0 && methods.length > this.selected ? methods[this.selected] : null
  }

  public addCard(cardNumber: string): Throwing<boolean> {
    if (this.getAllMethods().includes(buildPaymentMethodNameByCardNumber(cardNumber))) {
      return false
    }

    this.addedCards.push(cardNumber)
    return true
  }

  public deleteMethod(method: string): Throwing<void> {
    const paymentMethods = this.availableMethods.paymentMethods.filter(
      (pm) => buildPaymentMethodNameByPaymentMethod(pm) !== method,
    )
    this.availableMethods = this.availableMethods.builder().setPaymentMethods(paymentMethods).build()
    this.addedCards = this.addedCards.filter((card) => buildPaymentMethodNameByCardNumber(card) !== method)
  }

  public getSelected(): Throwing<Int32> {
    return this.selected
  }

  public selectMethod(index: Int32): Throwing<void> {
    if (this.selected !== index) {
      this.cvv = null
      if (this.showKeyboardFirstCvv) {
        this.showKeyboardFirstCvv = false
      } else {
        this.keyboardModel.setNumericKeyboardStatus(false)
      }
      if (this.methodsListMode === MethodsListMode.regular && this.forceCvv) {
        this.buttonModel.setButtonText(PaymentButtonLabel.enterCvv)
      }
    }
    this.selected = index
  }

  public selectSbpMethod(): Throwing<void> {
    this.selected = this.getAllMethods().lastIndexOf(PaymentMethodName.sbp)
    this.keyboardModel.setNumericKeyboardStatus(false)
  }

  public setCvvFieldValue(cvv: string): Throwing<void> {
    this.cvv = cvv
    if (this.methodsListMode === MethodsListMode.regular && this.forceCvv && cvv.length === 3) {
      this.buttonModel.setButtonText(PaymentButtonLabel.pay)
    }
  }

  public getCvvFieldValue(): Throwing<Nullable<string>> {
    return this.methodsListMode === MethodsListMode.preselect
      ? null
      : this.forceCvv && this.cvv === null
      ? ''
      : this.cvv
  }

  public clickNewCard(): Throwing<void> {
    this.keyboardModel.setNumericKeyboardStatus(true)
    this.paymentScreenTitleModel.setTitle(PaymentScreenTitleLabel.cardPayment)
  }

  public tapOnCvvField(): Throwing<void> {
    this.keyboardModel.setNumericKeyboardStatus(true)
  }

  public resetFields(): void {
    this.keyboardModel.setNumericKeyboardStatus(false)
    this.cvv = null
    if (this.methodsListMode === MethodsListMode.regular) {
      this.selected = -1
    }
  }

  public getMethodsListMode(): Throwing<MethodsListMode> {
    return this.methodsListMode
  }

  public setMethodsListMode(mode: MethodsListMode): Throwing<void> {
    this.methodsListMode = mode
  }
}

export class TestPaymentMethodsBuilder {
  public constructor() {}

  private availableMethods: AvailableMethods = new AvailableMethodsBuilder().build()
  private addedCards: readonly string[] = []

  public setAvailableMethods(value: AvailableMethods): TestPaymentMethodsBuilder {
    this.availableMethods = value
    return this
  }

  public setAddedCards(value: readonly string[]): TestPaymentMethodsBuilder {
    this.addedCards = value
    return this
  }

  public getAllMethods(): string[] {
    const result: string[] = []
    for (const method of this.availableMethods.paymentMethods) {
      result.push(buildPaymentMethodNameByPaymentMethod(method))
    }
    for (const cardNumber of this.addedCards) {
      result.push(buildPaymentMethodNameByCardNumber(cardNumber))
    }

    if (this.availableMethods.isGooglePayAvailable) {
      result.push(PaymentMethodName.googlePay)
    }

    if (this.availableMethods.isApplePayAvailable) {
      result.push(PaymentMethodName.applePay)
    }

    if (this.availableMethods.isCashAvailable) {
      result.push(PaymentMethodName.cash)
    }

    if (this.availableMethods.isSpbQrAvailable) {
      result.push(PaymentMethodName.sbp)
    }

    if (result.length > 0) {
      result.push(PaymentMethodName.otherCard)
    }

    return result
  }
}

export class PreselectModel implements Preselect {
  public constructor(
    private readonly paymentScreenTitleModel: PaymentScreenTitleModel,
    private readonly keyboardModel: KeyboardModel,
    private readonly paymentMethodsListModel: PaymentMethodsListModel,
    private readonly paymentButtonModel: PaymentButtonModel,
    private readonly fillNewCardModel: FillNewCardModel,
  ) {}
  private cashSelected: boolean = false

  public isCashSelected(): Throwing<boolean> {
    return this.cashSelected
  }

  public selectCash(): Throwing<void> {
    this.cashSelected = true
  }

  public tapOnSelectButton(): Throwing<void> {
    this.paymentScreenTitleModel.setTitle(PaymentScreenTitleLabel.paymentMethod)
  }

  public tapOnOtherCard(): Throwing<void> {
    this.keyboardModel.setNumericKeyboardStatus(true)
    this.paymentScreenTitleModel.setTitle(PaymentScreenTitleLabel.addCard)
    this.paymentButtonModel.setButtonText(PaymentButtonLabel.addCard)
    this.fillNewCardModel.setNewCardMode(NewCardMode.bind)
  }

  public unbindCard(index: Int32): Throwing<void> {
    this.paymentMethodsListModel.deleteMethod(this.paymentMethodsListModel.getMethods()[index])
  }

  public tapOnAddCard(): Throwing<void> {
    this.paymentScreenTitleModel.setTitle(PaymentScreenTitleLabel.paymentMethod)
    this.paymentButtonModel.setButtonText(PaymentButtonLabel.select)
    this.paymentButtonModel.pressButton()
    this.fillNewCardModel.setNewCardMode(null)
  }
}

export class PreselectCvvModel implements PreselectCvv {
  public constructor(
    private readonly paymentMethodsListModel: PaymentMethodsListModel,
    private readonly forceCvv: boolean,
  ) {}

  public getCardName(): Throwing<string> {
    return this.paymentMethodsListModel.getSelectedCardName()!
  }

  public getCvvFieldValue(): Throwing<Nullable<string>> {
    const cvv = this.paymentMethodsListModel.getCvvFieldValue()
    return this.forceCvv && cvv === null ? '' : cvv
  }

  public waitForPreselectCvv(mSec: Int32): Throwing<boolean> {
    return true
  }
}

export class ApplePayModel implements ApplePay {
  public constructor(private readonly availableMethods: AvailableMethods) {}

  public isAvailable(): boolean {
    return this.availableMethods.isApplePayAvailable
  }
}

export class GooglePayModel implements GooglePay {
  public constructor(private readonly availableMethods: AvailableMethods) {}

  public isAvailable(): boolean {
    return this.availableMethods.isGooglePayAvailable
  }
}

export class SBPModel implements SBP {
  public constructor(private readonly availableMethods: AvailableMethods) {}

  public isAvailable(): boolean {
    return this.availableMethods.isSpbQrAvailable
  }
}
