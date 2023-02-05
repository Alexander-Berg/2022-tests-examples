import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { Int32, Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderListComponent } from '../../components/folder-list-component'
import { CreateLabelComponent } from '../../components/label-manager/create-label-component'
import { EditLabelComponent } from '../../components/label-manager/edit-label-component'
import { ManageLabelsComponent } from '../../components/label-manager/manage-labels-component'
import { LabelName, LabelNavigatorFeature } from '../../feature/folder-list-features'
import { ContainerDeletionMethod, ManageableLabelFeature } from '../../feature/manageable-container-features'

export class DeleteLabelAction implements MBTAction {
  public static readonly type: MBTActionType = 'DeleteLabel'

  public constructor(
    private labelName: LabelName,
    private deletionMethod: ContainerDeletionMethod = ContainerDeletionMethod.tap,
  ) {}

  public canBePerformed(model: App): Throwing<boolean> {
    const labelNavigatorModel = LabelNavigatorFeature.get.forceCast(model)
    const labels = labelNavigatorModel.getLabelList()
    return labels.includes(this.labelName)
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).deleteLabel(this.labelName, this.deletionMethod)
    ManageableLabelFeature.get.forceCast(application).deleteLabel(this.labelName, this.deletionMethod)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return DeleteLabelAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `DeleteLabel(${this.labelName})`
  }
}

export class SubmitEditedLabelAction implements MBTAction {
  public static readonly type: MBTActionType = 'SubmitEditedLabel'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).submitEditedLabel()
    ManageableLabelFeature.get.forceCast(application).submitEditedLabel()
    return new ManageLabelsComponent()
  }

  public getActionType(): MBTActionType {
    return SubmitEditedLabelAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SubmitEditedLabel'
  }
}

export class SetEditedLabelColorAction implements MBTAction {
  public static readonly type: MBTActionType = 'SetEditedLabelColor'

  public constructor(private colorIndex: Int32) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).setEditedLabelColor(this.colorIndex)
    ManageableLabelFeature.get.forceCast(application).setEditedLabelColor(this.colorIndex)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return SetEditedLabelColorAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SetEditedLabelColor'
  }
}

export class EnterNameForEditedLabelAction implements MBTAction {
  public static readonly type: MBTActionType = 'EnterNameForEditedLabel'

  public constructor(private labelName: LabelName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).enterNameForEditedLabel(this.labelName)
    ManageableLabelFeature.get.forceCast(application).enterNameForEditedLabel(this.labelName)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return EnterNameForEditedLabelAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'EnterNameForEditedLabel'
  }
}

export class CloseEditLabelScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseEditLabelScreen'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).closeEditLabelScreen()
    ManageableLabelFeature.get.forceCast(application).closeEditLabelScreen()
    return new ManageLabelsComponent()
  }

  public getActionType(): MBTActionType {
    return CloseEditLabelScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseEditLabelScreen'
  }
}

export class OpenEditLabelScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenEditLabelScreen'

  public constructor(private labelName: LabelName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).openEditLabelScreen(this.labelName)
    ManageableLabelFeature.get.forceCast(application).openEditLabelScreen(this.labelName)
    return new EditLabelComponent()
  }

  public getActionType(): MBTActionType {
    return OpenEditLabelScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenEditLabelScreen'
  }
}

export class SubmitNewLabelAction implements MBTAction {
  public static readonly type: MBTActionType = 'SubmitNewLabel'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).submitNewLabel()
    ManageableLabelFeature.get.forceCast(application).submitNewLabel()
    return requireNonNull(history.previousDifferentComponent, 'There is no previous different component')
  }

  public getActionType(): MBTActionType {
    return SubmitNewLabelAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SubmitNewLabel'
  }
}

export class SetNewLabelColorAction implements MBTAction {
  public static readonly type: MBTActionType = 'SetNewLabelColor'

  public constructor(private colorIndex: Int32) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).setNewLabelColor(this.colorIndex)
    ManageableLabelFeature.get.forceCast(application).setNewLabelColor(this.colorIndex)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return SetNewLabelColorAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SetNewLabelColor'
  }
}

export class EnterNameForNewLabelAction implements MBTAction {
  public static readonly type: MBTActionType = 'EnterNameForNewLabel'

  public constructor(private labelName: LabelName) {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).enterNameForNewLabel(this.labelName)
    ManageableLabelFeature.get.forceCast(application).enterNameForNewLabel(this.labelName)
    return history.currentComponent
  }

  public getActionType(): MBTActionType {
    return EnterNameForNewLabelAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'EnterNameForNewLabel'
  }
}

export class CloseCreateLabelScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseCreateLabelScreen'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).closeCreateLabelScreen()
    ManageableLabelFeature.get.forceCast(application).closeCreateLabelScreen()
    return new ManageLabelsComponent()
  }

  public getActionType(): MBTActionType {
    return CloseCreateLabelScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseCreateLabelScreen'
  }
}

export class OpenCreateLabelScreenAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenCreateLabelScreen'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).openCreateLabelScreen()
    ManageableLabelFeature.get.forceCast(application).openCreateLabelScreen()
    return new CreateLabelComponent()
  }

  public getActionType(): MBTActionType {
    return OpenCreateLabelScreenAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenCreateLabelScreen'
  }
}

export class OpenLabelManagerAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenLabelManager'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).openLabelManager()
    ManageableLabelFeature.get.forceCast(application).openLabelManager()
    return new ManageLabelsComponent()
  }

  public getActionType(): MBTActionType {
    return OpenLabelManagerAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'OpenLabelManager'
  }
}

export class CloseLabelManagerAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseLabelManager'

  public constructor() {}

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      LabelNavigatorFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      ManageableLabelFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ManageableLabelFeature.get.forceCast(model).closeLabelManager()
    ManageableLabelFeature.get.forceCast(application).closeLabelManager()
    return new FolderListComponent()
  }

  public getActionType(): MBTActionType {
    return CloseLabelManagerAction.type
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseLabelManager'
  }
}
