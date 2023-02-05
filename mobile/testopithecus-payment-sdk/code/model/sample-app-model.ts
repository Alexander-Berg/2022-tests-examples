import { Throwing } from '../../../../common/ys'
import { PaymentMethodsFilter } from '../../../payment-sdk/code/busilogics/payment-methods-decorator'
import { OAuthUserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { MethodsListMode } from '../feature/payment-methods-list-feature'
import { PaymentAdditionalSettings, SampleApp } from '../feature/sample-app-feature'
import { PersonalInfoMode } from '../personal-info-mode'
import { AuthorizationMode } from '../sample/sample-configuration'
import { FillNewCardModel, NewCardMode } from './fill-new-card-model'
import { KeyboardModel } from './keyboard-model'
import { PaymentButtonLabel, PaymentButtonModel } from './payment-button-model'
import { ReadPaymentDetailsModel } from './payment-details-model'
import { PaymentMethodsListModel } from './payment-methods-list-model'
import { PaymentScreenTitleLabel, PaymentScreenTitleModel } from './payment-screen-title-model'
import { UnbindCardModel } from './unbind-card-model'

export class SampleAppModel implements SampleApp {
  private additionalSettings: PaymentAdditionalSettings = new PaymentAdditionalSettings(
    false,
    new PaymentMethodsFilter(),
    false,
    PersonalInfoMode.HIDE,
    AuthorizationMode.authorized,
    false,
    false,
  )

  public constructor(
    private readonly paymentScreenTitleModel: PaymentScreenTitleModel,
    private readonly readPaymentDetails: ReadPaymentDetailsModel,
    private readonly paymentButtonModel: PaymentButtonModel,
    private readonly fillNewCardModel: FillNewCardModel,
    private readonly paymentMethodsListModel: PaymentMethodsListModel,
    private readonly unbindCardModel: UnbindCardModel,
    private readonly keyboardModel: KeyboardModel,
  ) {}

  public startSampleApp(
    user: OAuthUserAccount,
    merchantId: string,
    paymentId: string,
    additionalSettings: PaymentAdditionalSettings,
  ): Throwing<void> {
    this.additionalSettings = additionalSettings
  }

  private updateTitle(): Throwing<void> {
    let title = ''
    if (this.readPaymentDetails.isPersonalInfoShown()) {
      title = PaymentScreenTitleLabel.personalInformation
    } else if (this.paymentMethodsListModel.getMethods().length > 0) {
      title = PaymentScreenTitleLabel.paymentMethod
    } else {
      title =
        this.fillNewCardModel.getNewCardMode() === NewCardMode.preselect
          ? PaymentScreenTitleLabel.addCard
          : PaymentScreenTitleLabel.cardPayment
    }
    this.paymentScreenTitleModel.setTitle(title)
  }

  public bindCard(): Throwing<void> {
    this.paymentScreenTitleModel.setTitle(PaymentScreenTitleLabel.addCard)
    this.paymentButtonModel.setEnabledInModel(false)
    this.paymentButtonModel.setButtonText(PaymentButtonLabel.addCard)
    this.fillNewCardModel.setNewCardMode(NewCardMode.bind)
    this.keyboardModel.setNumericKeyboardStatus(true)
  }

  public startPreselectPayment(): Throwing<void> {
    this.paymentMethodsListModel.setMethodsListMode(MethodsListMode.preselect)
    this.fillNewCardModel.setNewCardMode(NewCardMode.preselect)
    this.updateTitle()
    this.paymentButtonModel.setButtonText(
      this.paymentMethodsListModel.getMethods().length > 0 ? PaymentButtonLabel.select : PaymentButtonLabel.addCard,
    )
    this.unbindCardModel.setEditButtonShowingStatus(this.unbindCardModel.checkHasCardsToUnbind())
    if (this.paymentMethodsListModel.getMethods().length === 0) {
      this.keyboardModel.setNumericKeyboardStatus(true)
    }
  }

  public startRegularPayment(): Throwing<void> {
    this.paymentMethodsListModel.setMethodsListMode(MethodsListMode.regular)
    this.fillNewCardModel.setNewCardMode(NewCardMode.pay)
    this.updateTitle()
    this.paymentButtonModel.setButtonText(PaymentButtonLabel.pay)
    this.paymentButtonModel.setLabelText(
      PaymentButtonLabel.label(this.readPaymentDetails.getCurrency(), this.readPaymentDetails.getAmount()),
    )
    const title = this.paymentScreenTitleModel.getTitle()
    if (
      this.paymentMethodsListModel.getMethods().length === 0 &&
      PaymentScreenTitleLabel.personalInformation !== title
    ) {
      this.keyboardModel.setNumericKeyboardStatus(true)
    }
    if (this.additionalSettings.forceCvv && this.paymentMethodsListModel.getCards().length !== 0) {
      this.keyboardModel.setNumericKeyboardStatus(true)
    }
  }

  public unbindCard(): Throwing<void> {
    // TODO: not yet implemented
  }

  public waitForAppReady(): Throwing<boolean> {
    return true
  }
}
