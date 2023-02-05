import { Throwing } from '../../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { TabBarComponent } from './tab-bar-component'

export class TelemostComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'TelemostComponent'

  public getComponentType(): MBTComponentType {
    return TelemostComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    await new TabBarComponent().assertMatches(model, application)
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
