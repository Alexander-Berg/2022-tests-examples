import { range, Throwing } from '../../../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertTrue,
} from '../../../../../testopithecus-common/code/utils/assert'
import { FiltersListFeature, FilterView } from '../../feature/settings/filters-features'

export class FilterListComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'FilterListComponent'

  public getComponentType(): MBTComponentType {
    return FilterListComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const filtersListModel = FiltersListFeature.get.castIfSupported(model)
    const filtersListApp = FiltersListFeature.get.castIfSupported(application)

    if (filtersListModel !== null && filtersListApp !== null) {
      assertBooleanEquals(
        filtersListModel.isPromoShown(),
        filtersListApp.isPromoShown(),
        'Prmo shown status is incorrect',
      )

      const filtersModel = filtersListModel.getFilterList()
      const filtersApp = filtersListApp.getFilterList()

      assertInt32Equals(filtersModel.length, filtersApp.length, 'Incorrect number of filters')

      for (const i of range(0, filtersModel.length)) {
        const filterModel = filtersModel[i]
        const filterApp = filtersApp[i]
        assertTrue(
          FilterView.matches(filterModel, filterApp),
          `Different filter. Model: ${filterModel.tostring()}, app: ${filterApp.tostring()}`,
        )
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
