import { Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import {
  Feature,
  MBTAction,
  MBTActionType,
  MBTComponent,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Rotatable, RotatableFeature } from '../../feature/rotatable-feature'

export abstract class RotatableAction extends BaseSimpleAction<Rotatable, MBTComponent> {
  public constructor(type: MBTActionType) {
    super(type)
  }

  public static addActions(actions: MBTAction[]): void {
    actions.push(new RotateToLandscape())
    actions.push(new RotateToPortrait())
  }

  public requiredFeature(): Feature<Rotatable> {
    return RotatableFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }

  public performImpl(modelOrApplication: Rotatable, currentComponent: MBTComponent): Throwing<MBTComponent> {
    this.rotate(modelOrApplication)
    return currentComponent
  }

  public abstract rotate(modelOrApplication: Rotatable): Throwing<void>
}

export class RotateToLandscape extends RotatableAction {
  public static readonly type: MBTActionType = 'RotateToLandscape'

  public constructor() {
    super(RotateToLandscape.type)
  }

  public canBePerformedImpl(model: Rotatable): Throwing<boolean> {
    const isInLandscape = model.isInLandscape()
    return !isInLandscape
  }

  public rotate(modelOrApplication: Rotatable): Throwing<void> {
    modelOrApplication.rotateToLandscape()
  }
}

export class RotateToPortrait extends RotatableAction {
  public static readonly type: MBTActionType = 'RotateToPortrait'

  public constructor() {
    super(RotateToPortrait.type)
  }

  public canBePerformedImpl(model: Rotatable): Throwing<boolean> {
    const isInLandscape = model.isInLandscape()
    return isInLandscape
  }

  public rotate(modelOrApplication: Rotatable): Throwing<void> {
    modelOrApplication.rotateToPortrait()
  }
}
