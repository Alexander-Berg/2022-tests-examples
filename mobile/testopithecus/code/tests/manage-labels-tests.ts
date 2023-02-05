import { LabelData } from '../../../mapi/code/api/entities/label/label'
import { TestSettings } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { UserAccount } from '../../../testopithecus-common/code/users/user-pool'
import { AssertSnapshotAction } from '../mail/actions/assert-snapshot-action'
import { OpenFolderListAction } from '../mail/actions/left-column/folder-navigator-actions'
import {
  CloseLabelManagerAction,
  DeleteLabelAction,
  EnterNameForEditedLabelAction,
  EnterNameForNewLabelAction,
  OpenCreateLabelScreenAction,
  OpenEditLabelScreenAction,
  OpenLabelManagerAction,
  SetEditedLabelColorAction,
  SetNewLabelColorAction,
  SubmitEditedLabelAction,
  SubmitNewLabelAction,
} from '../mail/actions/left-column/manage-labels-actions'
import { ContainerDeletionMethod } from '../mail/feature/manageable-container-features'
import { MailboxBuilder, MessageSpecBuilder } from '../mail/mailbox-preparer'
import { RegularYandexMailTestBase } from './abstract-mail-tests'

export class ManageLabelsAddNewLabelTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LabelsManager. Добавление новой метки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5874).androidCase(10223)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {}

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenLabelManagerAction())
      .then(new OpenCreateLabelScreenAction())
      .then(new EnterNameForNewLabelAction('new label'))
      .then(new SetNewLabelColorAction(2))
      .then(new SubmitNewLabelAction())
      .then(new CloseLabelManagerAction())
  }
}

export class ManageLabelsEditLabelTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LabelsManager. Изменение метки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5879).androidCase(10240)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('subj1'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1'), new LabelData('label2')])
          .withSubject('subj2'),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenLabelManagerAction())
      .then(new OpenEditLabelScreenAction('label1'))
      .then(new EnterNameForEditedLabelAction('edited label'))
      .then(new SetEditedLabelColorAction(1))
      .then(new SubmitEditedLabelAction())
      .then(new CloseLabelManagerAction())
  }
}

export class ManageLabelsDeleteOpenedLabelByLongSwipeTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LabelsManager. Удаление метки')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5884).androidCase(10227)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1')])
          .withSubject('subj1'),
      )
      .nextCustomMessage(
        new MessageSpecBuilder()
          .withDefaults()
          .addLabels([new LabelData('label1'), new LabelData('label2')])
          .withSubject('subj2'),
      )
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenLabelManagerAction())
      .then(new DeleteLabelAction('label1', ContainerDeletionMethod.tap))
      .then(new CloseLabelManagerAction())
  }
}

export class ManageLabelsValidateViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LabelsManager. Внешний вид экрана Управление метками')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5869).androidCase(10320)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createLabel(new LabelData('label1')).createLabel(new LabelData('label2'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenLabelManagerAction())
      .then(new AssertSnapshotAction(this.description))
  }
}

export class ManageLabelsValidateAddLabelViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LabelsManager. Внешний вид экрана Новая метка')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5870).androidCase(10407)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createLabel(new LabelData('label1')).createLabel(new LabelData('label2'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenLabelManagerAction())
      .then(new OpenCreateLabelScreenAction())
  }
}

export class ManageLabelsValidateEditLabelViewTest extends RegularYandexMailTestBase {
  public constructor() {
    super('LabelsManager. Внешний вид экрана Изменить метку')
  }

  public setupSettings(settings: TestSettings): void {
    settings.iosCase(5871).androidCase(10321)
  }

  public prepareAccount(mailbox: MailboxBuilder): void {
    mailbox.createLabel(new LabelData('label1')).createLabel(new LabelData('label2'))
  }

  public testScenario(account: UserAccount): TestPlan {
    return this.yandexLogin(account)
      .then(new OpenFolderListAction())
      .then(new OpenLabelManagerAction())
      .then(new OpenEditLabelScreenAction('label1'))
  }
}
