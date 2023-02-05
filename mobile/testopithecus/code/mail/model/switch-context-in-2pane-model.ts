import { MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../common/ys'
import { SwitchContext2Pane } from '../feature/switch-context-in-2pane-feature'

export class SwitchContextIn2paneModel implements SwitchContext2Pane {
  public switchContextTo(component: MBTComponent): Throwing<void> {}
}
