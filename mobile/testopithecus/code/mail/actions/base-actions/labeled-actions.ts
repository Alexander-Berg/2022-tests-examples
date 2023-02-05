import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { Int32, int64, Nullable, Throwing } from '../../../../../../common/ys'
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
import { CreateLabelComponent } from '../../components/label-manager/create-label-component'
import { MaillistComponent } from '../../components/maillist-component'
import { MessageComponent } from '../../components/message-component'
import { ApplyLabelFeature } from '../../feature/apply-label-feature'
import { MarkableImportant, MarkableImportantFeature } from '../../feature/base-action-features'
import { FolderNavigatorFeature, LabelName, LabelNavigatorFeature } from '../../feature/folder-list-features'
import { MessageView } from '../../feature/mail-view-features'
import { ManageableLabelFeature } from '../../feature/manageable-container-features'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'
import { MoveToFolderFeature } from '../../feature/move-to-folder-feature'

export abstract class BaseLabelAction implements MBTAction {
  protected constructor(protected order: Int32, private type: MBTActionType) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MarkableImportantFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListModel.getMessageList(10)
    const canPerform = this.canBePerformedImpl(messages[this.order])
    return this.order < messages.length && canPerform
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    this.performImpl(MarkableImportantFeature.get.forceCast(model))
    this.performImpl(MarkableImportantFeature.get.forceCast(application))
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return this.type
  }

  public abstract canBePerformedImpl(message: MessageView): Throwing<boolean>

  public abstract performImpl(modelOrApplication: MarkableImportant): Throwing<void>

  public abstract events(): EventusEvent[]

  public abstract tostring(): string
}

export class MarkAsImportant extends BaseLabelAction {
  public static readonly type: MBTActionType = 'MarkAsImportant'

  public constructor(order: Int32) {
    super(order, MarkAsImportant.type)
  }

  public static canMarkImportant(message: MessageView): boolean {
    return !message.important
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsImportant.canMarkImportant(message)
  }

  public performImpl(modelOrApplication: MarkableImportant): Throwing<void> {
    return modelOrApplication.markAsImportant(this.order)
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Eventus.messageListEvents.markMessageAsImportant(this.order, int64(-1)),
    ]
  }

  public tostring(): string {
    return `MarkAsImportant(#${this.order})`
  }
}

export class MarkAsUnimportant extends BaseLabelAction {
  public static readonly type: MBTActionType = 'MarkAsImportant'

  public constructor(order: Int32) {
    super(order, MarkAsUnimportant.type)
  }

  public static canMarkUnimportant(message: MessageView): boolean {
    return message.important
  }

  public canBePerformedImpl(message: MessageView): Throwing<boolean> {
    return MarkAsUnimportant.canMarkUnimportant(message)
  }

  public performImpl(modelOrApplication: MarkableImportant): Throwing<void> {
    return modelOrApplication.markAsUnimportant(this.order)
  }

  public events(): EventusEvent[] {
    return [
      Eventus.messageListEvents.openMessageActions(this.order, int64(-1)),
      Eventus.messageListEvents.markMessageAsNotImportant(this.order, int64(-1)),
    ]
  }

  public tostring(): string {
    return `MarkAsUnimportant(#${this.order})`
  }
}

export class ApplyLabelTapOnCreateLabelAction implements MBTAction {
  public static readonly type: MBTActionType = 'ApplyLabelTapOnCreateLabelAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ApplyLabelFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ApplyLabelFeature.get.forceCast(model).tapOnCreateLabel()
    ApplyLabelFeature.get.forceCast(application).tapOnCreateLabel()
    return new CreateLabelComponent()
  }

  public tostring(): string {
    return `${ApplyLabelTapOnCreateLabelAction.type}`
  }

  public getActionType(): MBTActionType {
    return ApplyLabelTapOnCreateLabelAction.type
  }
}

export class ApplyLabelAddLabelAction implements MBTAction {
  public static readonly type: MBTActionType = 'ApplyLabelAddLabelAction'

  public constructor(private readonly labelNames: LabelName[]) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      FolderNavigatorFeature.get.included(modelFeatures) &&
      MoveToFolderFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const labelList = LabelNavigatorFeature.get.forceCast(model).getLabelList()
    return labelList.filter((label) => this.labelNames.includes(label)).length > 0
  }

  public events(): EventusEvent[] {
    return []
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelApplyLabel = ApplyLabelFeature.get.forceCast(model)
    const appApplyLabel = ApplyLabelFeature.get.forceCast(application)
    modelApplyLabel.selectLabelsToAdd(this.labelNames)
    appApplyLabel.selectLabelsToAdd(this.labelNames)
    modelApplyLabel.tapOnDoneButton()
    appApplyLabel.tapOnDoneButton()
    return this.previousComponents(history)
  }

  private previousComponents(history: MBTHistory): MBTComponent {
    let previousComponent: Nullable<MBTComponent> = null
    for (const component of history.allPreviousComponents.reverse()) {
      if ([MessageComponent.type, MaillistComponent.type].includes(component.tostring())) {
        previousComponent = component
        break
      }
    }
    return requireNonNull(previousComponent, 'No previous component')
  }

  public tostring(): string {
    return `${ApplyLabelAddLabelAction.type}(${this.labelNames})`
  }

  public getActionType(): MBTActionType {
    return ApplyLabelAddLabelAction.type
  }
}
