import { Throwing } from '../../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { TabBarComponent } from './tab-bar-component'

export class NotesWebViewComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'NotesWebViewComponent'

  public getComponentType(): MBTComponentType {
    return NotesWebViewComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    await new TabBarComponent().assertMatches(model, application)
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
