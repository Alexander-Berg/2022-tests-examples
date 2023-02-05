import { Eventus } from '../../../../../eventus/code/events/eventus'
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
import { MessageComponent } from '../../components/message-component'
import { ZeroSuggestComponent } from '../../components/zero-suggest-component'
import { ContainerGetterFeature, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { SearchFeature } from '../../feature/search-features'

export class SearchAllMessagesAction implements MBTAction {
  public static readonly type: MBTActionType = 'SearchAllMessagesAction'

  public canBePerformed(model: App): Throwing<boolean> {
    const isInSearch = SearchFeature.get.forceCast(model).isInSearch()
    return isInSearch
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return SearchAllMessagesAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    SearchFeature.get.forceCast(model).searchAllMessages()
    SearchFeature.get.forceCast(application).searchAllMessages()
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SearchFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return SearchAllMessagesAction.type
  }
}

export class SearchByRequestAction implements MBTAction {
  public static readonly type: MBTActionType = 'SearchByRequestAction'

  public constructor(protected request: string) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const isInSearch = SearchFeature.get.forceCast(model).isInSearch()
    return isInSearch
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return SearchByRequestAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    SearchFeature.get.forceCast(model).searchByQuery(this.request)
    SearchFeature.get.forceCast(application).searchByQuery(this.request)
    return new MaillistComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SearchFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return SearchByRequestAction.type
  }
}

export class CloseSearchAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseSearchAction'

  public canBePerformed(model: App): Throwing<boolean> {
    const isInSearch = SearchFeature.get.forceCast(model).isInSearch()
    return isInSearch
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return CloseSearchAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    SearchFeature.get.forceCast(model).closeSearch()
    SearchFeature.get.forceCast(application).closeSearch()
    const previousDifferentComponent = history.previousDifferentComponent
    if (
      previousDifferentComponent === null ||
      [new ZeroSuggestComponent().tostring(), new MessageComponent().tostring()].includes(
        previousDifferentComponent.tostring(),
      )
    ) {
      return new MaillistComponent()
    }
    return previousDifferentComponent!
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SearchFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return CloseSearchAction.type
  }
}

export class OpenSearchAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenSearchAction'

  public canBePerformed(model: App): Throwing<boolean> {
    const isInSearch = SearchFeature.get.forceCast(model).isInSearch()
    return !isInSearch
  }

  public events(): EventusEvent[] {
    return [Eventus.searchEvents.open()]
  }

  public getActionType(): string {
    return OpenSearchAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    SearchFeature.get.forceCast(model).openSearch()
    SearchFeature.get.forceCast(application).openSearch()
    return new ZeroSuggestComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SearchFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return OpenSearchAction.type
  }
}

export class ClearTextFieldAction implements MBTAction {
  public static readonly type: MBTActionType = 'ClearTextFieldAction'

  public canBePerformed(model: App): Throwing<boolean> {
    const isInSearch = SearchFeature.get.forceCast(model).isInSearch()
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    return isInSearch && currentContainer.type === MessageContainerType.search
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return ClearTextFieldAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    SearchFeature.get.forceCast(model).clearTextField()
    SearchFeature.get.forceCast(application).clearTextField()
    return new ZeroSuggestComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SearchFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return this.getActionType()
  }
}
