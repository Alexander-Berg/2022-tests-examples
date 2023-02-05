import { Throwing } from '../../../../../../common/ys'
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
import { FilterNavigatorFeature } from '../../feature/folder-list-features'

export class GoToFilterImportantAction implements MBTAction {
  public static readonly type: MBTActionType = 'GoToFilterImportant'

  public canBePerformed(_model: App): boolean {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return GoToFilterImportantAction.type
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    FilterNavigatorFeature.get.forceCast(model).goToFilterImportant()
    FilterNavigatorFeature.get.forceCast(application).goToFilterImportant()
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FilterNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return 'GoToFilterImportant'
  }
}

export class GoToFilterUnreadAction implements MBTAction {
  public static readonly type: MBTActionType = 'GoToFilterUnread'

  public canBePerformed(_model: App): boolean {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return GoToFilterUnreadAction.type
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    FilterNavigatorFeature.get.forceCast(model).goToFilterUnread()
    FilterNavigatorFeature.get.forceCast(application).goToFilterUnread()
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FilterNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return 'GoToFilterUnread'
  }
}

export class GoToFilterWithAttachmentsAction implements MBTAction {
  public static readonly type: MBTActionType = 'GoToFilterWithAttachments'

  public canBePerformed(_model: App): boolean {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return GoToFilterWithAttachmentsAction.type
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    FilterNavigatorFeature.get.forceCast(model).goToFilterWithAttachments()
    FilterNavigatorFeature.get.forceCast(application).goToFilterWithAttachments()
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FilterNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return 'GoToFilterWithAttachments'
  }
}
