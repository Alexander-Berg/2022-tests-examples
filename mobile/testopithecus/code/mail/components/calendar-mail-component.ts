import { Throwing } from '../../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { TabBarComponent } from './tab-bar-component'

export class CalendarMailComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'CalendarMailComponent'

  public getComponentType(): MBTComponentType {
    return CalendarMailComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    await new TabBarComponent().assertMatches(model, application)
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
