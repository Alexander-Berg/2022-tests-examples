import { Feature, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../common/ys'

export class SwitchContext2PaneFeature extends Feature<SwitchContext2Pane> {
  public static get: SwitchContext2PaneFeature = new SwitchContext2PaneFeature()

  private constructor() {
    super(
      'Смена контекста в 2pane',
      'В 2pane на экране может быть несколько компонентов, фича помогает сменить контекст модели, в реализации оставляем пустышку',
    )
  }
}

export interface SwitchContext2Pane {
  switchContextTo(component: MBTComponent): Throwing<void>
}
