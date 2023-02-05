import { Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class KeyboardFeature extends Feature<KeyboardProtocol> {
  public static get: KeyboardFeature = new KeyboardFeature()

  private constructor() {
    super('KeyboardFeature', 'Check is keyboard shown')
  }
}

export interface KeyboardProtocol {
  isNumericKeyboardShown(): Throwing<boolean>

  isAlphabeticalKeyboardShown(): Throwing<boolean>

  isKeyboardShown(): Throwing<boolean>

  minimizeKeyboard(): Throwing<void>
}
