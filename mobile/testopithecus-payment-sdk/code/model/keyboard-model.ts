import { Throwing } from '../../../../common/ys'
import { KeyboardProtocol } from '../feature/keyboard-feature'

export class KeyboardModel implements KeyboardProtocol {
  private numericKeyboardShown: boolean = false
  private alphabeticalKeyboardShown: boolean = false

  public setNumericKeyboardStatus(shown: boolean): void {
    this.numericKeyboardShown = shown
  }

  public setAlphabeticalKeyboardStatus(shown: boolean): void {
    this.numericKeyboardShown = shown
  }

  public isNumericKeyboardShown(): Throwing<boolean> {
    return this.numericKeyboardShown
  }

  public isAlphabeticalKeyboardShown(): Throwing<boolean> {
    return this.alphabeticalKeyboardShown
  }

  public isKeyboardShown(): Throwing<boolean> {
    return this.numericKeyboardShown || this.alphabeticalKeyboardShown
  }

  public minimizeKeyboard(): Throwing<void> {
    this.alphabeticalKeyboardShown = false
    this.numericKeyboardShown = false
  }
}
