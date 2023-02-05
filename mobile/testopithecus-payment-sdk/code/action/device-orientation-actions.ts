import { TestopithecusConstants } from '../../../testopithecus-common/code/utils/utils'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../common/ys'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import {
  DeviceOrientation,
  DeviceOrientationFeature,
  DeviceOrientationProvider,
} from '../feature/device-orientation-feature'

export class RotateDeviceAction extends BaseSimpleAction<DeviceOrientationProvider, MBTComponent> {
  public static readonly type: MBTActionType = 'RotateDeviceAction'

  public constructor(
    private readonly orientation: DeviceOrientation,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(RotateDeviceAction.type)
  }
  public performImpl(
    modelOrApplication: DeviceOrientationProvider,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.rotate(this.orientation)
    return currentComponent
  }

  public requiredFeature(): Feature<DeviceOrientationProvider> {
    return DeviceOrientationFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}
