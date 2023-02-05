import { Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class SnapshotValidatingFeature extends Feature<SnapshotValidating> {
  public static get: SnapshotValidatingFeature = new SnapshotValidatingFeature()

  private constructor() {
    super('SnapshotValidatingFeature', 'Фича для сравнения скриншота экрана с эталоном')
  }
}

export interface SnapshotValidating {
  verifyScreen(componentName: string, testName: string): Throwing<void>
}
