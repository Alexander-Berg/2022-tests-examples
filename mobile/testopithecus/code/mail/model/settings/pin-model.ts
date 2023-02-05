import { Throwing } from '../../../../../../common/ys'
import { Pin } from '../../feature/pin-feature'

export class PinModel implements Pin {
  public constructor() {}

  private pin: string = ''

  public changePassword(newPassword: string): Throwing<void> {
    this.pin = newPassword
  }

  public resetPassword(): Throwing<void> {
    this.pin = ''
  }

  public enterPassword(password: string): Throwing<void> {}

  public isLoginUsingPasswordEnabled(): Throwing<boolean> {
    return this.pin !== ''
  }

  public turnOffLoginUsingPassword(): Throwing<void> {
    this.pin = ''
  }

  public turnOnLoginUsingPassword(password: string): Throwing<void> {
    this.pin = password
  }

  public waitForPinToTrigger(): Throwing<void> {
    return
  }
}
