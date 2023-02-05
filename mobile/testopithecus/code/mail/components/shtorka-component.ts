import { Throwing } from '../../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertInt32Equals, assertTrue } from '../../../../testopithecus-common/code/utils/assert'
import { ShtorkaAndroidFeature, ShtorkaFeature, ShtorkaIOSFeature } from '../feature/tab-bar-feature'

export class ShtorkaComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'ShtorkaComponent'

  public getComponentType(): MBTComponentType {
    return ShtorkaComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const shtorkaModel = ShtorkaFeature.get.castIfSupported(model)
    const shtorkaApplication = ShtorkaFeature.get.castIfSupported(application)

    if (shtorkaModel !== null && shtorkaApplication !== null) {
      const modelBannerType = shtorkaModel.getShownBannerType()
      const appBannerType = shtorkaApplication.getShownBannerType()

      assertTrue(modelBannerType === appBannerType, 'Shtorka banner type is incorrect')
    }

    const shtorkaIOSModel = ShtorkaIOSFeature.get.castIfSupported(model)
    const shtorkaIOSApplication = ShtorkaIOSFeature.get.castIfSupported(application)

    if (shtorkaIOSModel !== null && shtorkaIOSApplication !== null) {
      const modelShtorkaItems = shtorkaIOSModel.getItems()
      const appShtorkaItems = shtorkaIOSApplication.getItems()

      assertInt32Equals(modelShtorkaItems.length, appShtorkaItems.length, 'Incorrect number of shtorka items')

      for (const appShtorkaItem of appShtorkaItems) {
        assertTrue(modelShtorkaItems.includes(appShtorkaItem), `There is no ${appShtorkaItem} in model`)
      }
    }

    const shtorkaAndroidModel = ShtorkaAndroidFeature.get.castIfSupported(model)
    const shtorkaAndroidApplication = ShtorkaAndroidFeature.get.castIfSupported(application)

    if (shtorkaAndroidModel !== null && shtorkaAndroidApplication !== null) {
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
