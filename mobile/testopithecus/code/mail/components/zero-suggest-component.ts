import { assertInt32Equals, assertStringEquals } from '../../../../testopithecus-common/code/utils/assert'
import { range, Throwing } from '../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { ZeroSuggestFeature } from '../feature/search-features'
import { TabBarComponent } from './tab-bar-component'

export class ZeroSuggestComponent implements MBTComponent {
  public static readonly type: string = 'ZeroSuggestComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const zeroSuggestModel = ZeroSuggestFeature.get.castIfSupported(model)
    const zeroSuggestApplication = ZeroSuggestFeature.get.castIfSupported(application)
    if (zeroSuggestModel !== null && zeroSuggestApplication !== null) {
      const actualZeroSuggest = zeroSuggestApplication.getZeroSuggest()
      const expectedZeroSuggest = zeroSuggestModel.getZeroSuggest()

      assertInt32Equals(
        expectedZeroSuggest.length,
        actualZeroSuggest.length,
        'The number of suggestions in the list is incorrect',
      )

      for (const i of range(0, actualZeroSuggest.length)) {
        assertStringEquals(expectedZeroSuggest[i], actualZeroSuggest[i], 'Zero suggest is incorrect')
      }
    }

    await new TabBarComponent().assertMatches(model, application)
  }

  public tostring(): string {
    return 'ZeroSuggestComponent'
  }

  public getComponentType(): string {
    return ZeroSuggestComponent.type
  }
}

export class AllZeroSuggestActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    return actions
  }
}
