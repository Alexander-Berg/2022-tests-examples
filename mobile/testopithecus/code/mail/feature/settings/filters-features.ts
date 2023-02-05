import { Throwing, Int32, Nullable } from '../../../../../../common/ys'
import { FilterLogicType } from '../../../../../mapi/code/api/entities/filters/filter-requests'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderName, LabelName } from '../folder-list-features'

export class FiltersListFeature extends Feature<FiltersList> {
  public static get: FiltersListFeature = new FiltersListFeature()

  private constructor() {
    super('FiltersList', 'Экран с промо и списком фильтров')
  }
}

export interface FiltersList {
  isPromoShown(): Throwing<boolean>

  getFilterList(): Throwing<FilterView[]>

  tapOnCreateRuleButton(): Throwing<void>

  tapOnFilterByIndex(index: Int32): Throwing<void>
}

export class FilterView {
  public constructor(
    public readonly conditions: string,
    public readonly actions: string,
    public readonly isCanBeEditedOnlyOnComputer: boolean,
    public readonly isEnabled: boolean,
  ) {}

  public static matches(first: FilterView, second: FilterView): boolean {
    return (
      first.conditions === second.conditions &&
      first.actions === second.actions &&
      first.isCanBeEditedOnlyOnComputer === second.isCanBeEditedOnlyOnComputer &&
      first.isEnabled === second.isEnabled
    )
  }

  public tostring(): string {
    return (
      `Conditions: ${this.conditions},\n` +
      `Actions: ${this.actions},\n` +
      `isCanBeEditedOnlyOnComputer: ${this.isCanBeEditedOnlyOnComputer},\n` +
      `isEnabled: ${this.isEnabled}`
    )
  }
}

export class FilterCreateOrUpdateRuleFeature extends Feature<FilterCreateOrUpdateRule> {
  public static get: FilterCreateOrUpdateRuleFeature = new FilterCreateOrUpdateRuleFeature()

  private constructor() {
    super('FilterCreateOrUpdateRule', 'Экран создания/редактирования фильтра')
  }
}

export enum FilterConditionField {
  from,
  subject,
}

export enum FilterActionToggle {
  markAsRead,
  delete,
  applyToExistingEmails,
}

export interface FilterCreateOrUpdateRule {
  tapOnConditionField(conditionField: FilterConditionField): Throwing<void>

  setConditionField(conditionField: FilterConditionField, value: string): Throwing<void>

  getConditionField(conditionField: FilterConditionField): Throwing<string[]>

  isConditionLogicButtonShown(): Throwing<boolean>

  tapOnConditionLogicButton(): Throwing<void>

  getConditionLogic(): Throwing<Nullable<FilterLogicType>>

  getActionToggle(actionToggle: FilterActionToggle): Throwing<boolean>

  setActionToggle(actionToggle: FilterActionToggle, value: boolean): Throwing<void>

  getMoveToFolderValue(): Throwing<Nullable<FolderName>>

  tapOnMoveToFolder(): Throwing<void>

  getApplyLabelValue(): Throwing<Nullable<LabelName>>

  tapOnApplyLabel(): Throwing<void>

  tapOnCreate(): Throwing<void>

  tapOnMore(): Throwing<void>
}

export class FilterConditionLogicFeature extends Feature<FilterConditionLogic> {
  public static get: FilterConditionLogicFeature = new FilterConditionLogicFeature()

  private constructor() {
    super('FilterConditionLogic', 'Модальное окно выбора логики выполнения условий')
  }
}

export interface FilterConditionLogic {
  getLogicTypes(): Throwing<FilterLogicType[]>

  setLogicType(logicType: FilterLogicType): Throwing<void>
}

export class FilterUpdateRuleMoreFeature extends Feature<FilterUpdateRuleMore> {
  public static get: FilterUpdateRuleMoreFeature = new FilterUpdateRuleMoreFeature()

  private constructor() {
    super(
      'FilterUpdateRuleMore',
      'Модальное окно включения/отключения/удаления правила. Открывается с экрана редактирования правила',
    )
  }
}

export interface FilterUpdateRuleMore {
  changeEnableStatus(enable: boolean): Throwing<void>

  delete(): Throwing<void>
}

export class FilterConditionText {
  // Заголовок условия фильтра, когда нет условий, для всех писем
  public static readonly allEmails: string = 'All emails'
  // Заголовок условия фильтра, когда нет условий, для всех писем с вложениями
  public static readonly allEmailsWithAttachments: string = 'All emails with attachments'
  // Заголовок условия фильтра, когда нет условий, для всех писем без вложений
  public static readonly allEmailsWithouAttachments: string = 'All emails without attachments'
  // Название поля "Тело письма" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly body: string = '"Body of the email"'
  // Название поля "Копия" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly cc: string = '"Cc"'
  // Название поля "Название вложения" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly attachmentName: string = '"Attachment name"'
  // Название поля "От кого" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly from: string = '"From"'
  // Название поля "Тема" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly subject: string = '"Subject"'
  // Название поля "Кому" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly to: string = '"To"'
  // Название поля "Кому или копия" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly toOrCc: string = '"To or cc"'
  // Название пользовательского поля как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра. Вместо %s название заголовка
  public static readonly header: string = 'header "%@"'
  // Заголовок условия фильтра, когда нет условий, для всех писем, кроме спама
  public static readonly allEmailsExceptSpam: string = 'All emails except spam'
  // Заголовок условия фильтра, когда нет условий, для всех писем, кроме спама с вложениями
  public static readonly allEmailsWithAttachmentsExceptSpam: string = 'All emails with attachments except spam'
  // Заголовок условия фильтра, когда нет условий, для всех писем, кроме спама без вложений
  public static readonly allEmailsWithoutAttachmentsExceptSpam: string = 'All emails without attachments except spam'
  // Название операции над полем "содержит" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly contains: string = 'contains'
  // Название операции над полем "не совпадает с" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly doesntMatch: string = "doesn't match"
  // Название операции над полем "совпадает с" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly matches: string = 'matches'
  // Название операции над полем "не содержит" как часть условия в шаблоне "<Если/И/Или> <Название поля> <содержит/не содержит/совпадает/не совпадает>" в карточке фильтра
  public static readonly doesntContain: string = "doesn't contain"
  // Заголовок условия фильтра, когда нет условий, только для спама
  public static readonly onlySpam: string = 'Only spam'
  // Заголовок условия фильтра, когда нет условий, только для спама с вложениями
  public static readonly onlySpamWithAttachments: string = 'Only spam with attachments'
  // Заголовок условия фильтра, когда нет условий, только для спама без вложений
  public static readonly onlySpamWithoutAttachments: string = 'Only spam without attachments'
  // Связывающее "и" разных условии в карточке фильтра. Выглядит следующим образом: Если <Условие 1> и <Условие 2>...
  public static readonly and: string = 'and'
  // Связывающее "Если" разных условии в карточке фильтра. Выглядит следующим образом: Если <Условие 1> и <Условие 2>...
  public static readonly if: string = 'If'
  // Связывающее "или" разных условии в карточке фильтра. Выглядит следующим образом: Если <Условие 1> или <Условие 2>...
  public static readonly or: string = 'or'
  // Шаблон для условия подпадания под фильтр письма. На русском "<Поле, которое проверяется> <содержит/не содержит/совпадает с/не совпадает с> <текст, с которым сверяется поле>". В поля %1$@, %2$@, %3%@ подставляются соответственные поля
  public static readonly template: string = '%1$@ %2$@ "%3$@"'
}

export class FilterActionText {
  // Название действия с письмами "удалить", прошедшими фильтры
  public static readonly delete: string = '— Delete'
  // Название действия с письмами "переслать письмо", прошедшими фильтры. Вместо %s - название адреса для пересылки
  public static readonly forward: string = '— Forward email to "%@"'
  // Название действия с письмами "пометить меткой", прошедшими фильтры. Вместо %s - название метки
  public static readonly applyLabel: string = '— Add label "%@"'
  // Название действия с письмами "пометить прочитанным", прошедшими фильтры
  public static readonly markAsRead: string = '— Mark as read'
  // Название действия с письмами "переместить в папку", прошедшими фильтры. Вместо %s - название папки для перемещения
  public static readonly moveToFolder: string = '— Move to folder "%@"'
  // Название действия с письмами " уведомить", прошедшими фильтры. Вместо %s - название адреса для нотификации
  public static readonly notify: string = '— Notify at %@'
  // Название действия с письмами "автоматический ответ", прошедшими фильтры
  public static readonly reply: string = '— Automatic response'
  // Название действия с письмами "не применять остальные правила" (не применять другие фильтры, если этот фильтр прошел), прошедшими фильтры
  public static readonly actionStop: string = "— Don't apply other rules"
}

// Заголовок на карточке, сообщающий о том, что фильтр можно редактировать только на десктопной версии Почты
// 'filters.filter_list_item_desktop_only' = 'Can only be edited on a computer'
