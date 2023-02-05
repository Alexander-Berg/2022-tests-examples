import { Throwing } from '../../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../../testopithecus-common/code/utils/assert'
import { TabBarAndroidFeature, TabBarFeature, TabBarIOSFeature } from '../feature/tab-bar-feature'

export class TabBarComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'TabBarComponent'

  public getComponentType(): MBTComponentType {
    return TabBarComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const tabBarModel = TabBarFeature.get.castIfSupported(model)
    const tabBarApplication = TabBarFeature.get.castIfSupported(application)

    if (tabBarModel !== null && tabBarApplication !== null) {
      const modelTabBarShown = tabBarModel.isShown()
      const appTabBarShown = tabBarApplication.isShown()

      assertBooleanEquals(modelTabBarShown, appTabBarShown, 'Tab bar showing state is incorrect')

      if (modelTabBarShown) {
        const modelTabBarCurrentItem = tabBarModel.getCurrentItem()
        const appTabBarCurrentItem = tabBarApplication.getCurrentItem()

        assertStringEquals(
          modelTabBarCurrentItem.toString(),
          appTabBarCurrentItem.toString(),
          'Current tabbar item is incorrect',
        )

        const tabBarIOSModel = TabBarIOSFeature.get.castIfSupported(model)
        const tabBarIOSApplication = TabBarIOSFeature.get.castIfSupported(application)
        if (tabBarIOSModel !== null && tabBarIOSApplication !== null) {
          const modelTabBarItems = tabBarIOSModel.getItems()
          const appTabBarItems = tabBarIOSApplication.getItems()

          assertInt32Equals(modelTabBarItems.length, appTabBarItems.length, 'Incorrect number of tabbar items')

          for (const appTabBarItem of appTabBarItems) {
            assertTrue(modelTabBarItems.includes(appTabBarItem), `There is no ${appTabBarItem} in model`)
          }

          const modelTabBarCalendarDate = tabBarIOSModel.getCalendarIconDate()
          const appTabBarCalendarDate = tabBarIOSApplication.getCalendarIconDate()

          assertStringEquals(modelTabBarCalendarDate, appTabBarCalendarDate, 'Incorrect calendar date label')
        }

        const tabBarAndroidModel = TabBarAndroidFeature.get.castIfSupported(model)
        const tabBarAndroidApplication = TabBarAndroidFeature.get.castIfSupported(application)
        if (tabBarAndroidModel !== null && tabBarAndroidApplication !== null) {
          const modelTabBarItems = tabBarAndroidModel.getItems()
          const appTabBarItems = tabBarAndroidApplication.getItems()

          assertInt32Equals(modelTabBarItems.length, appTabBarItems.length, 'Incorrect number of tabbar items')

          for (const appTabBarItem of appTabBarItems) {
            assertTrue(modelTabBarItems.includes(appTabBarItem), `There is no ${appTabBarItem} in model`)
          }
        }
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
