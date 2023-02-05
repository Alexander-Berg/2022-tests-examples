/* eslint-disable @typescript-eslint/ban-ts-ignore */
import { Throwing } from '../../../../common/ys'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { App, Feature, FeatureID, MBTAction, MBTActionType, MBTComponent, MBTHistory } from './mbt-abstractions'

export abstract class BaseSimpleAction<F, C> implements MBTAction {
  public constructor(private type: MBTActionType) {}

  public supported(modelFeatures: FeatureID[], applicationFearures: FeatureID[]): boolean {
    return this.requiredFeature().includedAll(modelFeatures, applicationFearures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const featuredModel = this.requiredFeature().forceCast(model)
    return this.canBePerformedImpl(featuredModel)
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const currentComponent = history.currentComponent
    const modelFeature: F = this.requiredFeature().forceCast(model)
    const applicationFeature: F = this.requiredFeature().forceCast(application)
    // @ts-ignore
    const component = currentComponent as C
    this.performImpl(modelFeature, component)
    return this.performImpl(applicationFeature, component)
  }

  public canBePerformedImpl(_model: F): Throwing<boolean> {
    return true
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public tostring(): string {
    return this.getActionType()
  }

  public abstract events(): EventusEvent[]
  public abstract requiredFeature(): Feature<F>
  public abstract performImpl(modelOrApplication: F, currentComponent: C): Throwing<MBTComponent> // TODO: M & App
}
