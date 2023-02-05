import { Throwing } from '../../../../../../common/ys'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderNavigatorFeature } from '../../feature/folder-list-features'
import { ContainerGetterFeature } from '../../feature/message-list/container-getter-feature'
import { MessageListDisplay, MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { TabsFeature } from '../../feature/tabs-feature'
import { DefaultFolderName } from '../../model/folder-data-model'

export class RefreshMessageListAction extends BaseSimpleAction<MessageListDisplay, MBTComponent> {
  public static readonly type: MBTActionType = 'RefreshMessageList'

  public constructor() {
    super(RefreshMessageListAction.type)
  }

  public requiredFeature(): Feature<MessageListDisplay> {
    return MessageListDisplayFeature.get
  }

  public performImpl(modelOrApplication: MessageListDisplay, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.refreshMessageList()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.refreshMessageList()]
  }

  public tostring(): string {
    return 'RefreshMessageList'
  }
}

export class IsDisplayNotificationTabsAction implements MBTAction {
  public static readonly type: MBTActionType = 'IsDisplayNotificationTabs'
  public constructor(private tabsName: string) {}
  public canBePerformed(model: App): Throwing<boolean> {
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    const isInTabs = FolderNavigatorFeature.get.forceCast(model).isInTabsMode()
    return (
      isInTabs &&
      currentContainer.name === DefaultFolderName.inbox &&
      (this.tabsName === DefaultFolderName.mailingLists || this.tabsName === DefaultFolderName.socialNetworks)
    )
  }
  public events(): EventusEvent[] {
    return []
  }
  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    TabsFeature.get.forceCast(model).isDisplayNotificationTabs(this.tabsName)
    TabsFeature.get.forceCast(application).isDisplayNotificationTabs(this.tabsName)
    return history.currentComponent
  }
  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return TabsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }
  public getActionType(): MBTActionType {
    return IsDisplayNotificationTabsAction.type
  }
  public tostring(): string {
    return this.getActionType()
  }
}

export class OpenTabByTabNotificationAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenTabByTabNotification'
  public constructor(private tabsName: string) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    const isInTabs = FolderNavigatorFeature.get.forceCast(model).isInTabsMode()
    const isDisplayTabsNotification = TabsFeature.get.forceCast(model).isDisplayNotificationTabs(this.tabsName)
    return isInTabs && currentContainer.name === DefaultFolderName.inbox && isDisplayTabsNotification
  }
  public events(): EventusEvent[] {
    return []
  }
  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    TabsFeature.get.forceCast(model).goToTabByNotification(this.tabsName)
    TabsFeature.get.forceCast(application).goToTabByNotification(this.tabsName)
    return history.currentComponent
  }
  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return TabsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }
  public getActionType(): MBTActionType {
    return OpenTabByTabNotificationAction.type
  }
  public tostring(): string {
    return this.getActionType()
  }
}

export class GetPositionTabsNotificationInMessageList implements MBTAction {
  public static readonly type: MBTActionType = 'GetPositionTabsNotificationInMessageList'
  public constructor(private tabsName: string) {}
  public canBePerformed(model: App): Throwing<boolean> {
    const currentContainer = ContainerGetterFeature.get.forceCast(model).getCurrentContainer()
    const isInTabs = FolderNavigatorFeature.get.forceCast(model).isInTabsMode()
    return (
      isInTabs &&
      currentContainer.name === DefaultFolderName.inbox &&
      (this.tabsName === DefaultFolderName.mailingLists || this.tabsName === DefaultFolderName.socialNetworks)
    )
  }
  public events(): EventusEvent[] {
    return []
  }
  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    TabsFeature.get.forceCast(model).getPositionTabsNotification(this.tabsName)
    TabsFeature.get.forceCast(application).getPositionTabsNotification(this.tabsName)
    return history.currentComponent
  }
  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return TabsFeature.get.includedAll(modelFeatures, applicationFeatures)
  }
  public getActionType(): MBTActionType {
    return GetPositionTabsNotificationInMessageList.type
  }
  public tostring(): string {
    return this.getActionType()
  }
}

export class SwipeDownMessageListAction extends BaseSimpleAction<MessageListDisplay, MBTComponent> {
  public static readonly type: MBTActionType = 'SwipeDownMessageList'

  public constructor() {
    super(SwipeDownMessageListAction.type)
  }

  public requiredFeature(): Feature<MessageListDisplay> {
    return MessageListDisplayFeature.get
  }

  public performImpl(modelOrApplication: MessageListDisplay, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.swipeDownMessageList()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SwipeDownMessageList'
  }
}
