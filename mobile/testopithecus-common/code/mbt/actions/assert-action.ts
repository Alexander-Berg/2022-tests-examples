import { Throwing } from '../../../../../common/ys'
import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import { App, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from '../mbt-abstractions'

export class AssertAction implements MBTAction {
  public static type: MBTActionType = 'Assert'

  public supported(_modelFeatures: FeatureID[], _applicationFeatures: FeatureID[]): boolean {
    return true
  }

  public canBePerformed(_model: App): boolean {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const currentComponent = history.currentComponent
    await currentComponent.assertMatches(model, application)
    return currentComponent
  }

  public getActionType(): MBTActionType {
    return AssertAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return this.getActionType()
  }
}
