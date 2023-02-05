import { Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'

export interface Pin {
  turnOnLoginUsingPassword(password: string): Throwing<void>

  turnOffLoginUsingPassword(): Throwing<void>

  changePassword(newPassword: string): Throwing<void>

  resetPassword(): Throwing<void>

  enterPassword(password: string): Throwing<void>

  isLoginUsingPasswordEnabled(): Throwing<boolean>

  waitForPinToTrigger(): Throwing<void>
}

export class PinFeature extends Feature<Pin> {
  public static get: PinFeature = new PinFeature()

  private constructor() {
    super('Pin', 'Включение/отключение входа по паролю, установка/изменение/сброс пароля')
  }
}
