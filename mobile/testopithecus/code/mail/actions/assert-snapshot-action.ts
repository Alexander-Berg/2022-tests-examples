import { Throwing } from '../../../../../common/ys'
import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { TestopithecusConstants } from '../../../../testopithecus-common/code/utils/utils'
import { SnapshotValidating, SnapshotValidatingFeature } from '../feature/snapshot-validating-feature'

export class AssertSnapshotAction extends BaseSimpleAction<SnapshotValidating, MBTComponent> {
  public static readonly type: MBTActionType = 'AssertSnapshotAction'

  public constructor(
    private readonly name: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(AssertSnapshotAction.type)
  }

  private componentName: string = ''

  public requiredFeature(): Feature<SnapshotValidating> {
    return SnapshotValidatingFeature.get
  }

  public performImpl(modelOrApplication: SnapshotValidating, currentComponent: MBTComponent): Throwing<MBTComponent> {
    this.componentName = currentComponent.tostring()
    modelOrApplication.verifyScreen(this.componentName, this.name)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `${AssertSnapshotAction.type}(component=${this.componentName})`
  }
}
