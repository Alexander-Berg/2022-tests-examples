import { Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class DeviceOrientationFeature extends Feature<DeviceOrientationProvider> {
  public static get: DeviceOrientationFeature = new DeviceOrientationFeature()

  private constructor() {
    super('DeviceOrientation', 'Set and get device orientation')
  }
}

export interface DeviceOrientationProvider {
  getDeviceOrientation(): Throwing<DeviceOrientation>

  rotate(orientation: DeviceOrientation): Throwing<void>
}

export enum DeviceOrientation {
  landscape,
  portrait,
}
