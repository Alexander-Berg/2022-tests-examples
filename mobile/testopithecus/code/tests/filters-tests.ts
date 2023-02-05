import {
  FilterAction,
  FilterActionType,
  FilterCondition,
  FilterConditionKey,
  FilterLetterType,
  FilterLogicType,
} from '../../../mapi/code/api/entities/filters/filter-requests'
import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { MBTPlatform, TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import {
  AccountSettingsOpenFiltersAction,
  OpenAccountSettingsAction,
} from '../mail/actions/settings/account-settings-actions'
import { OpenSettingsAction } from '../mail/actions/settings/root-settings-actions'
import { FolderName, LabelName } from '../mail/feature/folder-list-features'
import { FilterRuleBuilder, MailboxBuilder } from '../mail/mailbox-preparer'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class ValidateFiltersListPt1Test extends RegularYandexMailTestBase {
  public constructor() {
    super('Filters. Просмотр правил обработки писем (часть 1)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(12800).ignoreOn(MBTPlatform.Android)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFilter(
        new FilterRuleBuilder()
          .setCondition(new FilterCondition(FilterConditionKey.from, 'aaa'))
          .setCondition(new FilterCondition(FilterConditionKey.from, 'bbb'))
          .setLogic(FilterLogicType.and)
          .setAction(new FilterAction(FilterActionType.markRead))
          .build(),
      )
      .createFilter(
        new FilterRuleBuilder()
          .setCondition(new FilterCondition(FilterConditionKey.from, 'ccc'))
          .setAction(new FilterAction(FilterActionType.delete))
          .setEnable(false)
          .build(),
      )
      .createFilter(
        new FilterRuleBuilder()
          .setCondition(new FilterCondition(FilterConditionKey.subject, 'ddd'))
          .setAction(new FilterAction(FilterActionType.markRead))
          .build(),
      )
      .createFilter(
        new FilterRuleBuilder()
          .setCondition(new FilterCondition(FilterConditionKey.subject, 'eee'))
          .setCondition(new FilterCondition(FilterConditionKey.subject, 'fff'))
          .setLogic(FilterLogicType.or)
          .setAction(new FilterAction(FilterActionType.delete))
          .setEnable(false)
          .build(),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new AccountSettingsOpenFiltersAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ValidateFiltersListPt2Test extends RegularYandexMailTestBase {
  public constructor() {
    super('Filters. Просмотр правил обработки писем (часть 2)')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(12800).ignoreOn(MBTPlatform.Android)
  }

  private folderName: FolderName = 'Folder'
  private labelName: LabelName = 'Label'

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .createFolder(this.folderName)
      .createLabel(new LabelData(this.labelName))
      .createFilter(
        new FilterRuleBuilder()
          .setCondition(new FilterCondition(FilterConditionKey.subject, 'ggg'))
          .setCondition(new FilterCondition(FilterConditionKey.from, 'hhh'))
          .setLogic(FilterLogicType.and)
          .setAction(new FilterAction(FilterActionType.applyLabel, this.labelName))
          .build(),
      )
      .createFilter(
        new FilterRuleBuilder()
          .setCondition(new FilterCondition(FilterConditionKey.subject, 'iii'))
          .setCondition(new FilterCondition(FilterConditionKey.from, 'jjj'))
          .setLogic(FilterLogicType.or)
          .setAction(new FilterAction(FilterActionType.moveToFolder, this.folderName))
          .setEnable(false)
          .build(),
      )
      .createFilter(
        new FilterRuleBuilder()
          .setLetter(FilterLetterType.clearspam)
          .setCondition(new FilterCondition(FilterConditionKey.cc, 'kkk'))
          .setAction(new FilterAction(FilterActionType.markRead))
          .build(),
      )
      .createFilter(
        new FilterRuleBuilder().setAction(new FilterAction(FilterActionType.delete)).setEnable(false).build(),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenSettingsAction())
      .then(new OpenAccountSettingsAction(0))
      .then(new AccountSettingsOpenFiltersAction())
      .then(new AssertSnapshotAction(this.description))
  }
}
