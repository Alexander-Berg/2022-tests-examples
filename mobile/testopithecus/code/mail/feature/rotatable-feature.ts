import { Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class RotatableFeature extends Feature<Rotatable> {
  public static get: RotatableFeature = new RotatableFeature()

  private constructor() {
    super(
      'Rotatable',
      'Изменение ориентации устройства. Позволяет изменить ориентацию устройства и проверить текущую ориентацию.',
    )
  }
}

export interface Rotatable {
  isInLandscape(): Throwing<boolean>

  rotateToLandscape(): Throwing<void>

  rotateToPortrait(): Throwing<void>
}
