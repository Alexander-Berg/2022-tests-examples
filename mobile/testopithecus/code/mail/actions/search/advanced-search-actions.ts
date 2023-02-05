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
import { FolderName, LabelName } from '../../feature/folder-list-features'
import { ContainerGetterFeature, MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { AdvancedSearchFeature } from '../../feature/search-features'

export class AddFolderToSearchAction implements MBTAction {
  public static readonly type: MBTActionType = 'AddFolderToSearchAction'

  public constructor(protected folderName: FolderName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ContainerGetterFeature.get.castIfSupported(model)!.getCurrentContainer().type === MessageContainerType.search
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return AddFolderToSearchAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AdvancedSearchFeature.get.forceCast(model).addFolderToSearch(this.folderName)
    AdvancedSearchFeature.get.forceCast(application).addFolderToSearch(this.folderName)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AdvancedSearchFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return AddFolderToSearchAction.type
  }
}

export class AddLabelToSearchAction implements MBTAction {
  public static readonly type: MBTActionType = 'AddLabelToSearchAction'

  public constructor(protected labelName: LabelName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return ContainerGetterFeature.get.castIfSupported(model)!.getCurrentContainer().type === MessageContainerType.search
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return AddLabelToSearchAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AdvancedSearchFeature.get.forceCast(model).addFolderToSearch(this.labelName)
    AdvancedSearchFeature.get.forceCast(application).addFolderToSearch(this.labelName)
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AdvancedSearchFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return AddLabelToSearchAction.type
  }
}

export class SearchOnlyImportantAction implements MBTAction {
  public static readonly type: MBTActionType = 'SearchOnlyImportantAction'

  public canBePerformed(model: App): Throwing<boolean> {
    return ContainerGetterFeature.get.castIfSupported(model)!.getCurrentContainer().type === MessageContainerType.search
  }

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): string {
    return SearchOnlyImportantAction.type
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    AdvancedSearchFeature.get.forceCast(model).searchOnlyImportant()
    AdvancedSearchFeature.get.forceCast(application).searchOnlyImportant()
    return history.currentComponent
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return AdvancedSearchFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public tostring(): string {
    return SearchOnlyImportantAction.type
  }
}
