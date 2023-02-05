import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertBooleanEquals, assertStringEquals, assertTrue } from '../../../testopithecus-common/code/utils/assert'
import { Throwing } from '../../../../common/ys'
import { removeNewlines } from '../../../common/code/utils/strings'
import { PaymentButtonFeature } from '../feature/payment-button-feature'
import { PaymentResultFeature } from '../feature/payment-result-feature'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class PaymentResultComponent implements MBTComponent {
  public static readonly type: string = 'Represent result/loading screen'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelPaymentResultFeature = PaymentResultFeature.get.castIfSupported(model)
    const appPaymentResultFeature = PaymentResultFeature.get.castIfSupported(application)

    if (modelPaymentResultFeature !== null && appPaymentResultFeature !== null) {
      assertTrue(
        appPaymentResultFeature.waitForCompletion(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
        `Payment not completed in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} milliseconds`,
      )

      const modelMessage = removeNewlines(modelPaymentResultFeature.getResultMessage())
      const appMessage = removeNewlines(appPaymentResultFeature.getResultMessage())
      assertStringEquals(modelMessage, appMessage, 'Result message mismatch')
    }

    const modelPaymentButton = PaymentButtonFeature.get.castIfSupported(model)
    const appPaymentButton = PaymentButtonFeature.get.castIfSupported(application)

    if (modelPaymentButton !== null && appPaymentButton !== null) {
      const modelPaymentButtonEnabled = modelPaymentButton.isEnabled()
      const appPaymentButtonEnabled = appPaymentButton.isEnabled()

      assertBooleanEquals(modelPaymentButtonEnabled, appPaymentButtonEnabled, 'Close button status is incorrect')
    }
  }

  public getComponentType(): MBTComponentType {
    return PaymentResultComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
