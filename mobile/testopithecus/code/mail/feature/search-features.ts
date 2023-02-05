import { Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderName, LabelName } from './folder-list-features'

export class ZeroSuggestFeature extends Feature<ZeroSuggest> {
  public static get: ZeroSuggestFeature = new ZeroSuggestFeature()

  private constructor() {
    super(
      'ZeroSuggest',
      'ZeroSuggest - история успешных запросов. ' +
        'Экран появляется при открытии поиска (после выполнения экшена openSearch()). ' +
        'Запросы сохраняются в саджест только при открытии в поисковой выдаче письма или аттача на просмотр.',
    )
  }
}

export interface ZeroSuggest {
  isShown(): Throwing<boolean>

  getZeroSuggest(): Throwing<string[]>

  searchByZeroSuggest(suggest: string): Throwing<void>
}

export class SearchFeature extends Feature<Search> {
  public static get: SearchFeature = new SearchFeature()

  private constructor() {
    super(
      'Search',
      'Фича поиска писем из поисковой строки. Для мобильных InstantSearch.' +
        ' Весь поиск осуществляется после выполнения экшена openSearch()',
    )
  }
}

export interface Search {
  searchAllMessages(): Throwing<void>

  closeSearch(): Throwing<void>

  clearTextField(): Throwing<void>

  isInSearch(): Throwing<boolean>

  isSearchedForMessages(): Throwing<boolean>

  openSearch(): Throwing<void>

  searchByQuery(query: string): Throwing<void>
}

export class AdvancedSearchFeature extends Feature<AdvancedSearch> {
  public static get: AdvancedSearchFeature = new AdvancedSearchFeature()

  private constructor() {
    super(
      'AdvancedSearch',
      'Фича расширенного поиска. Для мобильных InstantSearch.' +
        ' Весь поиск осуществляется после выполнения экшена openSearch() и поиска по обычному запросу ' +
        '(до этого момента расширенный поиск не видно)',
    )
  }
}

export interface AdvancedSearch {
  addLabelToSearch(labelName: LabelName): Throwing<void>

  addFolderToSearch(folderName: FolderName): Throwing<void>

  searchOnlyImportant(): Throwing<void>
}
