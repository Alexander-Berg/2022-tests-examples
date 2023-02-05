import { Int32, Throwing } from '../../../../common/ys'
import { UnbindCard } from '../feature/unbind-card-feature'
import { PaymentMethodName } from '../payment-sdk-data'
import { PaymentButtonLabel, PaymentButtonModel } from './payment-button-model'
import { PaymentMethodsListModel } from './payment-methods-list-model'

export class UnbindCardModel implements UnbindCard {
  public constructor(
    private readonly paymentMethodsListModel: PaymentMethodsListModel,
    private readonly paymentButtonModel: PaymentButtonModel,
  ) {}

  private doneButtonShown: boolean = false
  private editButtonShown: boolean = false

  public waitForUnbindCard(mSec: Int32): Throwing<boolean> {
    return true
  }

  public getCards(): string[] {
    return this.paymentMethodsListModel
      .getMethods()
      .filter((pm) => ![PaymentMethodName.otherCard, PaymentMethodName.sbp].includes(pm))
      .filter((pm) => !pm.startsWith(PaymentMethodName.familyPayPrefix))
  }

  public unbindCard(index: Int32): Throwing<void> {
    this.paymentMethodsListModel.deleteMethod(this.getCards()[index])
  }

  public isDoneButtonShown(): Throwing<boolean> {
    return this.doneButtonShown
  }

  public tapOnDoneButton(): Throwing<void> {
    this.doneButtonShown = false
    this.editButtonShown = this.checkHasCardsToUnbind()
    this.paymentButtonModel.setButtonText(PaymentButtonLabel.select)
  }

  public isEditButtonShown(): Throwing<boolean> {
    return this.editButtonShown
  }

  public checkHasCardsToUnbind(): boolean {
    return this.getCards().length > 0
  }

  public setEditButtonShowingStatus(shown: boolean): void {
    this.editButtonShown = shown
  }

  public tapOnEditButton(): Throwing<void> {
    this.doneButtonShown = true
    this.editButtonShown = false
  }
}
