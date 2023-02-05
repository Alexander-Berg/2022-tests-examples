import { Throwing } from '../../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class SubscriptionsComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'SubscriptionsComponent'

  public getComponentType(): MBTComponentType {
    return SubscriptionsComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {}

  public tostring(): string {
    return this.getComponentType()
  }
}
