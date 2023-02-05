import { Throwing } from '../../../../common/ys'
import { DeviceOrientation, DeviceOrientationProvider } from '../feature/device-orientation-feature'

export class DeviceOrientationModel implements DeviceOrientationProvider {
  private orientation: DeviceOrientation = DeviceOrientation.portrait

  public getDeviceOrientation(): Throwing<DeviceOrientation> {
    return this.orientation
  }

  public rotate(orientation: DeviceOrientation): Throwing<void> {
    this.orientation = orientation
  }
}
