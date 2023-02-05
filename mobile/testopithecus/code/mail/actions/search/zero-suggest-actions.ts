import { Throwing } from '../../../../../../common/ys'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MaillistComponent } from '../../components/maillist-component'
import { ZeroSuggestFeature } from '../../feature/search-features'

export class SearchByZeroSuggestAction implements MBTAction {
  public static readonly type: MBTActionType = 'SearchByZeroSuggestAction'

  public constructor(protected request: string) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ZeroSuggestFeature.get.forceCast(model).isShown()
  }

  public events(): EventusEvent[] {
    return [Eventus.searchEvents.searchByZeroSuggest(this.request.length)]
  }

  public getActionType(): string {
    return SearchByZeroSuggestAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ZeroSuggestFeature.get.forceCast(model).searchByZeroSuggest(this.request)
    ZeroSuggestFeature.get.forceCast(application).searchByZeroSuggest(this.request)
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ZeroSuggestFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return this.getActionType()
  }
}
