import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../../../testopithecus-common/code/utils/assert'
import { App, MBTComponent, MBTComponentType } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../../common/ys'
import { TranslatorLanguageListSearchFeature } from '../../feature/translator-features'

export class SearchLanguageComponent implements MBTComponent {
  public static readonly type: string = 'SearchLanguageComponent'

  public getComponentType(): MBTComponentType {
    return SearchLanguageComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelTranslatorLanguageListSearch = TranslatorLanguageListSearchFeature.get.castIfSupported(model)
    const appTranslatorLanguageListSearch = TranslatorLanguageListSearchFeature.get.castIfSupported(application)

    if (modelTranslatorLanguageListSearch !== null && appTranslatorLanguageListSearch !== null) {
      const modelSearchQuery = modelTranslatorLanguageListSearch.getSearchQuery()
      const appSearchQuery = appTranslatorLanguageListSearch.getSearchQuery()

      assertStringEquals(modelSearchQuery, appSearchQuery, 'Search query is incorrect')

      const modelSearchTextFieldFocused = modelTranslatorLanguageListSearch.isSearchTextFieldFocused()
      const appSearchTextFieldFocused = appTranslatorLanguageListSearch.isSearchTextFieldFocused()

      assertBooleanEquals(
        modelSearchTextFieldFocused,
        appSearchTextFieldFocused,
        'Search text field focus status is incorrect',
      )

      const modelSearchedLanguageList = modelTranslatorLanguageListSearch.getSearchedLanguageList()
      const appSearchedLanguageList = appTranslatorLanguageListSearch.getSearchedLanguageList()

      assertInt32Equals(
        modelSearchedLanguageList.length,
        appSearchedLanguageList.length,
        'Incorrect number of searched languages',
      )
      for (const modelLanguage of modelSearchedLanguageList) {
        assertTrue(
          appSearchedLanguageList.includes(modelLanguage),
          `Application list of searched languages not include ${modelLanguage}`,
        )
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
