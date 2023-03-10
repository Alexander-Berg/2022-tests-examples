// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM tests/manage-labels-tests.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public open class ManageLabelsAddNewLabelTest(): RegularYandexMailTestBase("LabelsManager. Добавление новой метки") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(5874).androidCase(10223)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
    }

    open override fun testScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenLabelManagerAction()).then(OpenCreateLabelScreenAction()).then(EnterNameForNewLabelAction("new label")).then(SetNewLabelColorAction(2)).then(SubmitNewLabelAction()).then(CloseLabelManagerAction())
    }

}

public open class ManageLabelsEditLabelTest(): RegularYandexMailTestBase("LabelsManager. Изменение метки") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(5879).androidCase(10240)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(mutableListOf(LabelData("label1"))).withSubject("subj1")).nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(mutableListOf(LabelData("label1"), LabelData("label2"))).withSubject("subj2"))
    }

    open override fun testScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenLabelManagerAction()).then(OpenEditLabelScreenAction("label1")).then(EnterNameForEditedLabelAction("edited label")).then(SetEditedLabelColorAction(1)).then(SubmitEditedLabelAction()).then(CloseLabelManagerAction())
    }

}

public open class ManageLabelsDeleteOpenedLabelByLongSwipeTest(): RegularYandexMailTestBase("LabelsManager. Удаление метки") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(5884).androidCase(10227)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(mutableListOf(LabelData("label1"))).withSubject("subj1")).nextCustomMessage(MessageSpecBuilder().withDefaults().addLabels(mutableListOf(LabelData("label1"), LabelData("label2"))).withSubject("subj2"))
    }

    open override fun testScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenLabelManagerAction()).then(DeleteLabelAction("label1", ContainerDeletionMethod.tap)).then(CloseLabelManagerAction())
    }

}

public open class ManageLabelsValidateViewTest(): RegularYandexMailTestBase("LabelsManager. Внешний вид экрана Управление метками") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(5869).androidCase(10320)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createLabel(LabelData("label1")).createLabel(LabelData("label2"))
    }

    open override fun testScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenLabelManagerAction()).then(AssertSnapshotAction(this.description))
    }

}

public open class ManageLabelsValidateAddLabelViewTest(): RegularYandexMailTestBase("LabelsManager. Внешний вид экрана Новая метка") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(5870).androidCase(10407)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createLabel(LabelData("label1")).createLabel(LabelData("label2"))
    }

    open override fun testScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenLabelManagerAction()).then(OpenCreateLabelScreenAction())
    }

}

public open class ManageLabelsValidateEditLabelViewTest(): RegularYandexMailTestBase("LabelsManager. Внешний вид экрана Изменить метку") {
    open override fun setupSettings(settings: TestSettings): Unit {
        settings.iosCase(5871).androidCase(10321)
    }

    open override fun prepareAccount(mailbox: MailboxBuilder): Unit {
        mailbox.createLabel(LabelData("label1")).createLabel(LabelData("label2"))
    }

    open override fun testScenario(account: UserAccount): TestPlan {
        return this.yandexLogin(account).then(OpenFolderListAction()).then(OpenLabelManagerAction()).then(OpenEditLabelScreenAction("label1"))
    }

}

