import { Throwing } from '../../../../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertTrue,
} from '../../../../../testopithecus-common/code/utils/assert'
import {
  FilterActionToggle,
  FilterConditionField,
  FilterConditionLogicFeature,
  FilterCreateOrUpdateRuleFeature,
} from '../../feature/settings/filters-features'

export class FilterCreateOrUpdateComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'FilterCreateOrUpdateComponent'

  public getComponentType(): MBTComponentType {
    return FilterCreateOrUpdateComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const filterCreateModel = FilterCreateOrUpdateRuleFeature.get.castIfSupported(model)
    const filterCreateApp = FilterCreateOrUpdateRuleFeature.get.castIfSupported(application)

    if (filterCreateModel !== null && filterCreateApp !== null) {
      for (const conditionField of [FilterConditionField.subject, FilterConditionField.from]) {
        const modelFieldValues = filterCreateModel.getConditionField(conditionField)
        const appFieldValues = filterCreateApp.getConditionField(conditionField)

        assertInt32Equals(
          modelFieldValues.length,
          appFieldValues.length,
          `Incorrect number of conditions in field ${conditionField}`,
        )

        for (const value of appFieldValues) {
          assertTrue(
            modelFieldValues.includes(value),
            `There is no value "${value}" in field "${conditionField}" in model`,
          )
        }
      }

      for (const actionToggle of [
        FilterActionToggle.delete,
        FilterActionToggle.applyToExistingEmails,
        FilterActionToggle.markAsRead,
      ]) {
        const modelToggleValue = filterCreateModel.getActionToggle(actionToggle)
        const appToggleValue = filterCreateApp.getActionToggle(actionToggle)

        assertBooleanEquals(modelToggleValue, appToggleValue, `Incorrect state of toggle ${actionToggle}`)
      }

      assertTrue(
        filterCreateModel.getApplyLabelValue() === filterCreateApp.getApplyLabelValue(),
        `Incorrect "apply label" value. Expected: ${filterCreateModel.getApplyLabelValue()}; Actual: ${filterCreateApp.getApplyLabelValue()}`,
      )

      assertTrue(
        filterCreateModel.getMoveToFolderValue() === filterCreateApp.getMoveToFolderValue(),
        `Incorrect "move to folder" value. Expected: ${filterCreateModel.getMoveToFolderValue()}; Actual: ${filterCreateApp.getMoveToFolderValue()}`,
      )

      assertTrue(
        filterCreateModel.getConditionLogic() === filterCreateApp.getConditionLogic(),
        `Incorrect conditional logic. Expected: ${filterCreateModel.getConditionLogic()}; Actual: ${filterCreateApp.getConditionLogic()}`,
      )
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class FilterConditionLogicComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'FilterConditionLogicComponent'

  public getComponentType(): MBTComponentType {
    return FilterConditionLogicComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const filterConditionLogicModel = FilterConditionLogicFeature.get.castIfSupported(model)
    const filterConditionLogicApp = FilterConditionLogicFeature.get.castIfSupported(application)

    if (filterConditionLogicModel !== null && filterConditionLogicApp !== null) {
      const modelLogicTypes = filterConditionLogicModel.getLogicTypes()
      const appLogicTypes = filterConditionLogicApp.getLogicTypes()

      assertInt32Equals(modelLogicTypes.length, appLogicTypes.length, 'Incorrect number of logic types')

      for (const appLogicType of appLogicTypes) {
        assertTrue(modelLogicTypes.includes(appLogicType), `There is no ${appLogicType} logic type in model`)
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class FilterUpdateRuleMoreComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'FilterUpdateRuleMoreComponent'

  public getComponentType(): MBTComponentType {
    return FilterUpdateRuleMoreComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {}

  public tostring(): string {
    return this.getComponentType()
  }
}
