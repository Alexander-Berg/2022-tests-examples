import { assertTrue } from '../../../testopithecus-common/code/utils/assert'
import { App, MBTComponent, MBTComponentType } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../common/ys'
import { SampleAppFeature } from '../feature/sample-app-feature'

export class SampleAppComponent implements MBTComponent {
  public static readonly type: string = 'SampleAppComponent'

  public constructor() {}

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const sampleApp = SampleAppFeature.get.forceCast(application)
    assertTrue(sampleApp.waitForAppReady(), 'Unable to open Sample app')
  }

  public getComponentType(): MBTComponentType {
    return SampleAppComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
