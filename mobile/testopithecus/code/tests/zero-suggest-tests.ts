import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { AssertAction } from '../../../testopithecus-common/code/mbt/actions/assert-action'
import { TestSettings, DeviceType } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { MarkAsRead } from '../mail/actions/base-actions/markable-actions'
import { RotateToLandscape, RotateToPortrait } from '../mail/actions/general/rotatable-actions'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import { GoToLabelAction } from '../mail/actions/left-column/label-navigator-actions'
import { MessageViewBackToMailListAction, OpenMessageAction } from '../mail/actions/opened-message/message-actions'
import {
  ClearTextFieldAction,
  CloseSearchAction,
  OpenSearchAction,
  SearchByRequestAction,
} from '../mail/actions/search/search-actions'
import { SearchByZeroSuggestAction } from '../mail/actions/search/zero-suggest-actions'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class SearchMessagesViaZeroSuggestTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ZeroSuggest. Поиск по тапу на саджест')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(597).androidCase(8292)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj 1').nextMessage('subj 2').nextMessage('subj 3').saveQueryToZeroSuggest('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchByZeroSuggestAction('subj'))
      .then(new CloseSearchAction())
  }
}

export class SearchMessagesViaZeroSuggestIn2PaneTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ZeroSuggest. Поиск по тапу на саджест в 2pane')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(598).androidCase(8294).setTags([DeviceType.Tab])
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj 1').nextMessage('subj 2').nextMessage('subj 3').saveQueryToZeroSuggest('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new RotateToLandscape())
      .then(new OpenSearchAction())
      .then(new SearchByZeroSuggestAction('subj'))
      .then(new CloseSearchAction())
  }
}

export class RotateDeviceInZeroSuggestTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ZeroSuggest. Изменение ориентации устройства')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(602).androidCase(8300)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj 1').nextMessage('subj 2').nextMessage('subj 3').saveQueryToZeroSuggest('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new RotateToLandscape())
      .then(new AssertAction())
      .then(new RotateToPortrait())
  }
}

export class SaveQueryToZeroSuggestTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ZeroSuggest. Сохранение запроса в саджест')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(603).androidCase(8293)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj 1').nextMessage('subj 2').nextMessage('subj 3')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenSearchAction())
      .then(new SearchByRequestAction('subj'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewBackToMailListAction())
      .then(new CloseSearchAction())
      .then(new OpenSearchAction())
      .then(new SearchByZeroSuggestAction('subj'))
  }
}

export class SearchUnreadMessagesViaZeroSuggestTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ZeroSuggest. Поиск непрочитанного письма по тапу на саджест')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(14).androidCase(6197)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.nextMessage('subj 1').nextMessage('subj 2').nextMessage('subj 3').saveQueryToZeroSuggest('subj')
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new MarkAsRead(0))
      .then(new OpenSearchAction())
      .then(new SearchByZeroSuggestAction('subj'))
  }
}

// TODO: register test after fix search model logic
export class ZeroSuggestCaseInsensitiveTest extends RegularYandexMailTestBase {
  public constructor() {
    super('ZeroSuggest. Проверка игнорирования регистра при сохранении запросов')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(605).androidCase(8301)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('ТЕСТ'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('тест'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('ТесТ'),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new GoToLabelAction('label1'))
      .then(new OpenSearchAction())
      .then(new SearchByRequestAction('тест'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewBackToMailListAction())
      .then(new ClearTextFieldAction())
      .then(new SearchByRequestAction('ТЕСТ'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewBackToMailListAction())
      .then(new ClearTextFieldAction())
      .then(new SearchByRequestAction('ТесТ'))
      .then(new OpenMessageAction(0))
      .then(new MessageViewBackToMailListAction())
      .then(new ClearTextFieldAction())
      .then(new CloseSearchAction())
  }
}
