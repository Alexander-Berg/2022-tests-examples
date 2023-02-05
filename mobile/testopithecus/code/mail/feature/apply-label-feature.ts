import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../common/ys'
import { LabelName } from '../feature/folder-list-features'

export interface ApplyLabel {
  selectLabelsToAdd(labelNames: LabelName[]): Throwing<void>

  deselectLabelsToRemove(labelNames: LabelName[]): Throwing<void>

  tapOnDoneButton(): Throwing<void>

  tapOnCreateLabel(): Throwing<void>

  getSelectedLabels(): Throwing<LabelName[]>

  getLabelList(): Throwing<LabelName[]>
}

export class ApplyLabelFeature extends Feature<ApplyLabel> {
  public static get: ApplyLabelFeature = new ApplyLabelFeature()

  public constructor() {
    super('ApplyLabel', 'Добавление/снятие метки на письмо/тред')
  }
}
