import { Throwing } from '../../../../../common/ys'
import { SnapshotValidating } from '../feature/snapshot-validating-feature'

export class SnapshotValidatingModel implements SnapshotValidating {
  public constructor() {}

  public verifyScreen(componentName: string, testName: string): Throwing<void> {
    // do nothing
  }
}
