package com.yandex.mail.settings.labels

import com.nhaarman.mockito_kotlin.mock
import com.yandex.mail.asserts.Conditions
import com.yandex.mail.fakeserver.FakeServer
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.storage.entities.EntitiesTestFactory
import com.yandex.mail.tools.Accounts
import com.yandex.mail.ui.presenters.configs.AccountPresenterConfig
import com.yandex.mail.util.BaseIntegrationTest
import io.reactivex.schedulers.Schedulers.trampoline
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(IntegrationTestRunner::class)
class LabelPresenterIntegrationTest : BaseIntegrationTest() {

    val labelView = mock<LabelView>()

    lateinit var labelPresenter: LabelPresenter

    lateinit var presenterConfig: AccountPresenterConfig

    @Before
    fun setUp() {
        init(Accounts.testLoginData)
        user.isThreaded = false

        presenterConfig = createTestPresenterConfig()
        labelPresenter = createLabelPresenter()
    }

    @Test
    fun `addLabel should add label`() {
        labelPresenter.onBindView(labelView)
        labelPresenter.addLabel(LABEL_NAME1, 0xFFFFFF)

        assertThat(account.labels.isPresentLabelWithName(LABEL_NAME1)).isTrue()

        verify(labelView).onFinishLabelEdit()
        verify(labelView, never()).onLabelCreationError()
        verify(labelView, never()).onNetworkProblems()
    }

    @Test
    fun `addLabel should invoke on error on add label with the same name`() {
        labelPresenter.onBindView(labelView)
        // colors doesn't matter
        labelPresenter.addLabel(LABEL_NAME1, 0x000000)
        labelPresenter.addLabel(LABEL_NAME1, 0xFFFFFF)

        assertThat(account.labels.isPresentLabelWithName(LABEL_NAME1)).isTrue()

        verify(labelView).onFinishLabelEdit()
        verify(labelView).onLabelCreationError()
        verify(labelView, never()).onNetworkProblems()
    }

    @Test
    fun `editLabel should edit label`() {
        labelPresenter.onBindView(labelView)

        labelPresenter.addLabel(LABEL_NAME1, 0x000000)
        val serverLid = account.labels.getByName(LABEL_NAME1).serverLid
        val nanoLabel = EntitiesTestFactory.buildNanoMailLabel().copy(lid = serverLid, name = LABEL_NAME1, color = 0)

        labelPresenter.editLabel(nanoLabel, LABEL_NAME2, 0x000000)

        assertThat(account.labels.isPresentLabelWithName(LABEL_NAME1)).isFalse()
        assertThat(account.labels.isPresentLabelWithName(LABEL_NAME2)).isTrue()
        assertThat(account.labels.getByServerLid(serverLid).displayName).isEqualTo(LABEL_NAME2) // id should not change

        verify(labelView, times(2)).onFinishLabelEdit()
        verify(labelView, never()).onLabelEditingError()
        verify(labelView, never()).onNetworkProblems()
    }

    @Test
    fun `editLabel should invoke on error on wrong lid`() {
        labelPresenter.onBindView(labelView)

        val nanoLabel = EntitiesTestFactory.buildNanoMailLabel().copy(lid = "_really_random_lid_", name = LABEL_NAME1, color = 0)
        labelPresenter.editLabel(nanoLabel, "title", 0x000000)

        verify(labelView, never()).onFinishLabelEdit()
        verify(labelView).onLabelEditingError()
        verify(labelView, never()).onNetworkProblems()
    }

    @Test
    fun `deleteLabel should delete label`() {
        labelPresenter.onBindView(labelView)

        labelPresenter.addLabel(LABEL_NAME1, 0x000000)
        val (serverLid) = account.labels.getByName(LABEL_NAME1)

        labelPresenter.deleteLabel(serverLid)

        assertThat(account.labels.isPresentLabelWithName(LABEL_NAME1)).isFalse()

        verify(labelView, times(2)).onFinishLabelEdit()
        verify(labelView, never()).onLabelDeletionError()
        verify(labelView, never()).onNetworkProblems()
    }

    @Test
    fun `deleteLabel should delete right label`() {
        labelPresenter.onBindView(labelView)

        labelPresenter.addLabel(LABEL_NAME1, 0x000000)
        labelPresenter.addLabel(LABEL_NAME2, 0xFFFFFF)

        val (serverLid) = account.labels.getByName(LABEL_NAME2)

        labelPresenter.deleteLabel(serverLid)

        assertThat(account.labels.isPresentLabelWithName(LABEL_NAME1)).isTrue()
        assertThat(account.labels.isPresentLabelWithName(LABEL_NAME2)).isFalse()

        verify(labelView, times(3)).onFinishLabelEdit()
        verify(labelView, never()).onLabelDeletionError()
        verify(labelView, never()).onNetworkProblems()
    }

    @Test
    fun `addLabel should call api`() {
        labelPresenter.onBindView(labelView)
        labelPresenter.addLabel(LABEL_NAME1, 0xFFFFFF)

        assertThat(FakeServer.getInstance().handledRequests).extracting { it.path }
            .areExactly(1, Conditions.matching { path -> path!!.contains("create_label") })
    }

    @Test
    fun `editLabel should call api`() {
        labelPresenter.onBindView(labelView)
        labelPresenter.addLabel(LABEL_NAME1, 0x000000)

        val (serverLid) = account.labels.getByName(LABEL_NAME1)
        val nanoLabel = EntitiesTestFactory.buildNanoMailLabel().copy(lid = serverLid, name = LABEL_NAME1, color = 0)

        labelPresenter.editLabel(nanoLabel, LABEL_NAME2, 0x000000)

        assertThat(FakeServer.getInstance().handledRequests).extracting { it.path }
            .areExactly(1, Conditions.matching { path -> path!!.contains("create_label") })

        assertThat(FakeServer.getInstance().handledRequests).extracting { it.path }
            .areExactly(1, Conditions.matching { path -> path!!.contains("update_label") })
    }

    @Test
    fun `editLabel should not call api if label not changed`() {
        labelPresenter.onBindView(labelView)

        labelPresenter.addLabel(LABEL_NAME1, 0x000000)
        val (serverLid) = account.labels.getByName(LABEL_NAME1)
        val nanoLabel = EntitiesTestFactory.buildNanoMailLabel().copy(lid = serverLid, name = LABEL_NAME1, color = 0)

        labelPresenter.editLabel(nanoLabel, LABEL_NAME1, 0x000000)

        assertThat(FakeServer.getInstance().handledRequests).extracting { it.path }
            .areNot(Conditions.matching { path -> path!!.contains("update_label") })
    }

    @Test
    fun `deleteLabel should call api`() {
        labelPresenter.onBindView(labelView)

        labelPresenter.addLabel(LABEL_NAME1, 0x000000)
        val (serverLid) = account.labels.getByName(LABEL_NAME1)

        labelPresenter.deleteLabel(serverLid)

        assertThat(FakeServer.getInstance().handledRequests).extracting { it.path }
            .areExactly(1, Conditions.matching { path -> path!!.contains("delete_label") })
    }

    private fun createTestPresenterConfig() = AccountPresenterConfig(
        ioScheduler = trampoline(),
        uiScheduler = trampoline(),
        uid = user.uid
    )

    private fun createLabelPresenter() = LabelPresenter(
        IntegrationTestRunner.app(),
        presenterConfig,
        IntegrationTestRunner.app().getAccountComponent(user.uid).api()
    )

    companion object {

        private val LABEL_NAME1 = "label1"

        private val LABEL_NAME2 = "label2"
    }
}
