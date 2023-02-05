import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../testopithecus-common/code/utils/assert'
import { Throwing } from '../../../../common/ys'
import { KeyboardFeature } from '../feature/keyboard-feature'
import { PaymentScreenTitleFeature } from '../feature/payment-screen-title-feature'
import { UnbindCardFeature } from '../feature/unbind-card-feature'
import { PaymentSdkConstants } from '../utils/payment-utils'

export class UnbindCardComponent implements MBTComponent {
  public static readonly type: string = 'UnbindCardComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const appUnbindCard = UnbindCardFeature.get.forceCast(application)

    assertTrue(
      appUnbindCard.waitForUnbindCard(PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT),
      `Unbind screen was not load in ${PaymentSdkConstants.SCREEN_APPEARANCE_TIMEOUT} seconds`,
    )

    const modelScreenTitle = PaymentScreenTitleFeature.get.castIfSupported(model)
    const appScreenTitle = PaymentScreenTitleFeature.get.castIfSupported(application)

    if (modelScreenTitle !== null && appScreenTitle !== null) {
      const modelTitle = modelScreenTitle.getTitle()
      const appTitle = appScreenTitle.getTitle()
      assertStringEquals(modelTitle, appTitle, 'Screen title mismatch')
    }

    const modelUnbindCard = UnbindCardFeature.get.castIfSupported(model)

    if (modelUnbindCard !== null) {
      const modelCards = modelUnbindCard.getCards()
      const appCards = appUnbindCard.getCards()

      assertInt32Equals(modelCards.length, appCards.length, 'Incorrect number of bound cards')

      for (const modelCard of modelCards) {
        assertTrue(appCards.includes(modelCard), 'Incorrect bound card')
      }

      const modelDoneButton = modelUnbindCard.isDoneButtonShown()
      const appDoneButton = appUnbindCard.isDoneButtonShown()

      assertBooleanEquals(modelDoneButton, appDoneButton, 'Incorrect done button showing status')
    }

    const modelKeyboard = KeyboardFeature.get.castIfSupported(model)
    const appKeyboard = KeyboardFeature.get.castIfSupported(application)

    if (modelKeyboard !== null && appKeyboard !== null) {
      const modelNumKeyboardShown = modelKeyboard.isNumericKeyboardShown()
      const appNumKeyboardShown = appKeyboard.isNumericKeyboardShown()

      assertBooleanEquals(modelNumKeyboardShown, appNumKeyboardShown, 'Numeric keyboard status is incorrect')

      const modelAlphKeyboardShown = modelKeyboard.isAlphabeticalKeyboardShown()
      const appAlphKeyboardShown = appKeyboard.isAlphabeticalKeyboardShown()

      assertBooleanEquals(modelAlphKeyboardShown, appAlphKeyboardShown, 'Alphabetical keyboard status is incorrect')
    }
  }

  public getComponentType(): MBTComponentType {
    return UnbindCardComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
