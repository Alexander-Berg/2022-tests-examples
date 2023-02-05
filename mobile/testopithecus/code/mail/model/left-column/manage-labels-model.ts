import { Int32, Nullable, Throwing } from '../../../../../../common/ys'
import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { LabelName } from '../../feature/folder-list-features'
import { ContainerDeletionMethod, ManageableLabel } from '../../feature/manageable-container-features'
import { MailAppModelHandler } from '../mail-model'

export class ManageLabelsModel implements ManageableLabel {
  public constructor(private accHandler: MailAppModelHandler) {}

  private nameOfCreatedLabel: Nullable<LabelName> = null
  private colorIndex: Nullable<Int32> = null

  private oldNameOfEditedLabel: Nullable<LabelName> = null
  private newNameOfEditedLabel: Nullable<LabelName> = null

  private oldColorIndexOfEditedLabel: Nullable<Int32> = null
  private newColorIndexOfEditedLabel: Nullable<Int32> = null

  public openLabelManager(): Throwing<void> {}

  public closeLabelManager(): Throwing<void> {}

  public deleteLabel(labelName: LabelName, deletionMethod: ContainerDeletionMethod): Throwing<void> {
    this.accHandler.getCurrentAccount().messagesDB.removeLabel(labelName)
  }

  public getLabelList(): Throwing<LabelName[]> {
    return this.accHandler.getCurrentAccount().messagesDB.getLabelList()
  }

  public closeCreateLabelScreen(): Throwing<void> {
    this.dropAll()
  }

  public closeEditLabelScreen(): Throwing<void> {
    this.dropAll()
  }

  public enterNameForEditedLabel(labelName: LabelName): Throwing<void> {
    this.newNameOfEditedLabel = labelName
  }

  public enterNameForNewLabel(labelName: LabelName): Throwing<void> {
    this.nameOfCreatedLabel = labelName
  }

  public getCurrentEditedLabelColorIndex(): Throwing<Int32> {
    if (this.newColorIndexOfEditedLabel === null) {
      return this.oldColorIndexOfEditedLabel!
    }
    return this.newColorIndexOfEditedLabel!
  }

  public getCurrentEditedLabelName(): Throwing<LabelName> {
    if (this.newNameOfEditedLabel === null) {
      return this.oldNameOfEditedLabel!
    }
    return this.newNameOfEditedLabel!
  }

  public getCurrentNewLabelColorIndex(): Throwing<Int32> {
    return requireNonNull(this.colorIndex, 'Color is not set')
  }

  public getCurrentNewLabelName(): Throwing<LabelName> {
    if (this.nameOfCreatedLabel === null) {
      return ''
    }
    return this.nameOfCreatedLabel!
  }

  public openCreateLabelScreen(): Throwing<void> {
    this.colorIndex = 0
  }

  public openEditLabelScreen(labelName: LabelName): Throwing<void> {
    this.oldNameOfEditedLabel = labelName
    this.oldColorIndexOfEditedLabel = 0 // TODO: не будет работать, если цвет был изменен
  }

  public setEditedLabelColor(index: Int32): Throwing<void> {
    this.newColorIndexOfEditedLabel = index
  }

  public setNewLabelColor(index: Int32): Throwing<void> {
    this.colorIndex = index
  }

  public submitEditedLabel(): Throwing<void> {
    this.accHandler
      .getCurrentAccount()
      .messagesDB.renameLabel(
        requireNonNull(this.oldNameOfEditedLabel, 'Old label name is not set'),
        requireNonNull(this.newNameOfEditedLabel, 'New label name is not set'),
      )
    this.dropAll()
  }

  public submitNewLabel(): Throwing<void> {
    this.accHandler
      .getCurrentAccount()
      .messagesDB.createLabel(requireNonNull(this.nameOfCreatedLabel, 'Label name is not set'))
    this.dropAll()
  }

  private dropAll(): void {
    this.oldColorIndexOfEditedLabel = null
    this.newColorIndexOfEditedLabel = null
    this.oldNameOfEditedLabel = null
    this.newNameOfEditedLabel = null
  }
}
