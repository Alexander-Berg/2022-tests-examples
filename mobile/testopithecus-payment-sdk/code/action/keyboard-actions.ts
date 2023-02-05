import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../common/ys'
import { KeyboardFeature } from '../feature/keyboard-feature'

export class MinimizeKeyboardAction implements MBTAction {
  public static readonly type: MBTActionType = 'MinimizeKeyboardAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return KeyboardFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const modelKeyboard = KeyboardFeature.get.forceCast(model)
    return modelKeyboard.isKeyboardShown()
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelKeyboard = KeyboardFeature.get.forceCast(model)
    const appKeyboard = KeyboardFeature.get.forceCast(application)
    modelKeyboard.minimizeKeyboard()
    appKeyboard.minimizeKeyboard()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'MinimizeKeyboardAction'
  }

  public getActionType(): string {
    return MinimizeKeyboardAction.type
  }
}
