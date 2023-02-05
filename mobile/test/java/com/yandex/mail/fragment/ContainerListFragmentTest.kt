package com.yandex.mail.fragment

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.yandex.mail.ActivityWithDrawer
import com.yandex.mail.BaseMailApplication
import com.yandex.mail.LoginData
import com.yandex.mail.asserts.IntentConditions
import com.yandex.mail.containers_list.ContainerListFragment
import com.yandex.mail.containers_list.ContainersAdapter
import com.yandex.mail.containers_list.ContainersAdapterIntegrationTest.Companion.folderItem
import com.yandex.mail.containers_list.ContainersAdapterIntegrationTest.Companion.labelItem
import com.yandex.mail.entity.FolderType
import com.yandex.mail.entity.FolderType.Companion.isTabContainer
import com.yandex.mail.fakeserver.AccountWrapper
import com.yandex.mail.fakeserver.FakeServer.Companion.getInstance
import com.yandex.mail.message_container.Container2
import com.yandex.mail.message_container.CustomContainer
import com.yandex.mail.message_container.FolderContainer
import com.yandex.mail.model.AccountModel
import com.yandex.mail.network.tasks.ArchiveTask
import com.yandex.mail.provider.Constants
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.settings.SettingsActivity
import com.yandex.mail.storage.entities.USER
import com.yandex.mail.tools.Accounts
import com.yandex.mail.tools.FragmentController
import com.yandex.mail.tools.TestFragmentActivity
import com.yandex.mail.tools.User
import com.yandex.mail.util.BaseIntegrationTest
import com.yandex.mail.util.mailbox.Mailbox
import com.yandex.mail.util.mailbox.MailboxEditor
import com.yandex.mail360.purchase.BuySubscriptionSource
import io.reactivex.Scheduler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(IntegrationTestRunner::class)
class ContainerListFragmentTest : BaseIntegrationTest() {

    private lateinit var controller: FragmentController<ContainerListFragment>

    @Before
    fun beforeEachTest() {
        init(Accounts.testLoginData)
    }

    @Test
    fun adapter_displaysFolders() {
        prepareFragment()
        val folderItems = allAdapterItems.filterIsInstance(
            ContainersAdapter.FolderItem::class.java
        )
        val adapterFids = folderItems.map { item: ContainersAdapter.FolderItem -> item.folder.fid }
        val serverFids = getServerFids(account)
        assertThat(adapterFids).hasSameElementsAs(serverFids)
    }

    @Test
    fun adapter_displaysFolderOutgoing() {
        Mailbox.threaded(this)
            .folder(
                MailboxEditor.Folder.createFolder().folderId(outgoingFid())
                    .addReadMessages(1)
                    .addUnreadMessages(1)
            )
            .applyAndSync()
        prepareFragment()
        val folderItems = allAdapterItems.filterIsInstance(
            ContainersAdapter.FolderItem::class.java
        )
        val adapterFids = folderItems.map { item: ContainersAdapter.FolderItem -> item.folder.fid }
        val serverFids = getServerFids(account, false)
        assertThat(adapterFids).hasSameElementsAs(serverFids)
    }

    @Test
    fun adapter_displaysChildFolders() {
        val childFolder = account.newFolder("Inbox|Child").parent(serverInbox().serverFid).build()
        val gchildFolder = account.newFolder("Inbox|Child|Grandchild").parent(childFolder.serverFid).build()
        account.addFolders(childFolder, gchildFolder)
        user.fetchContainers()
        prepareFragment()
        val folderItems = allAdapterItems.filterIsInstance(
            ContainersAdapter.FolderItem::class.java
        )
        val adapterFids = folderItems.map { item: ContainersAdapter.FolderItem -> item.folder.fid }
        val serverFids = getServerFids(account)
        assertThat(adapterFids).hasSameElementsAs(serverFids)
    }

    private fun sendAccountChangedEvent(uid: Long) {
        BaseMailApplication.getApplicationComponent(RuntimeEnvironment.application).accountModel().selectAccount(uid)
    }

    private fun clickOnItem(text: String) {
        val item = adapter.currentList.first { it.displayName == text }
        if (item is ContainersAdapter.ButtonItem) {
            item.action.run()
        } else {
            controller.get().switchToContainer(item.getEmailSource(false)!!, false)
        }
    }

    private fun collapse(text: String) {
        val item = adapter.currentList.first { it.displayName == text } as ContainersAdapter.FolderItem
        controller.get().onExpandFolderClicked(item.folder, !item.isExpanded)
    }

    private fun prepareFragment(intent: Intent?) {
        controller = FragmentController.of(ContainerListFragment(), TestActivity::class.java, intent)
        controller.create().start()
        sendAccountChangedEvent(user.uid)
        controller.resume().visible()
    }

    private fun prepareFragment() {
        prepareFragment(null)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private val adapter: ContainersAdapter
        get() = controller.get()!!.adapter

    private val allAdapterItems: List<ContainersAdapter.Item>
        get() {
            val adapter = adapter
            val items: MutableList<ContainersAdapter.Item> = ArrayList(adapter.itemCount)
            for (position in 0 until adapter.itemCount) {
                items.add(adapter.currentList[position])
            }
            return items
        }

    @Test
    @Throws(Exception::class)
    fun adapter_displaysUserLabels() {
        val label1 = account.newLabel("whatever").type(USER).build()
        val label2 = account.newLabel("alala").type(USER).build()
        account.addLabels(label1, label2)
        user.fetchContainers()
        prepareFragment()
        val labelItems = allAdapterItems.filterIsInstance(
            ContainersAdapter.LabelItem::class.java
        )
        val adapterLids = labelItems.map { item: ContainersAdapter.LabelItem -> item.label.lid }
        assertThat(adapterLids).contains(label1.serverLid, label2.serverLid)
    }

    @Test
    fun should_postContainer2UpdatedOnNewContainers() {
        prepareFragment()
        val serverUserFolder = account.newFolder("whatever").build()
        account.addFolders(serverUserFolder)
        user.fetchContainers()

        // we should receive a notification of updated containers at this point
        val (updatedContainer) = containerChangeEvents[containerChangeEvents.size - 1]
        assertThat(updatedContainer).isEqualTo(inbox().folderContainer)
    }

    @Test
    fun should_postContainer2ChangedInboxOnLoadedFolders() {
        user.isThreaded = true
        prepareFragment()
        val (newContainer) = containerChangeEvents[0]
        assertThat(newContainer).isEqualTo(inbox().folderContainer)
    }

    @Test
    fun should_postContainer2ChangedOnMailActivityIntent() {
        val switchIntent = Intent("whatever")
        switchIntent.putExtra(Constants.FID_EXTRA, spam().serverFid.toLong())
        switchIntent.putExtra(ContainerListFragment.UID_FOR_FOLDER_TO_SWITCH, user.uid)
        prepareFragment(switchIntent)
        val (newContainer) = containerChangeEvents[containerChangeEvents.size - 1]
        assertThat(newContainer).isEqualTo(spam().folderContainer)
    }

    @Test
    fun should_postContainer2ChangedOnContainerClick() {
        prepareFragment()
        clickOnItem("Inbox")
        val (newContainer) = containerChangeEvents[0]
        assertThat(newContainer).isEqualTo(inbox().folderContainer)
    }

    @Test
    fun should_openSettingsOnSettingsButtonClick() {
        prepareFragment()
        clickOnItem("Settings")
        val settingsIntent = Shadows.shadowOf(controller.get()!!.activity).nextStartedActivity
        assertThat(settingsIntent)
            .has(IntentConditions.component(ComponentName(IntegrationTestRunner.app(), SettingsActivity::class.java.name)))
    }

    @Test
    fun should_updateFoldersAndLabels() {
        prepareFragment()
        val serverUserFolder = account.newFolder("whatever").build()
        val serverUserLabel = account.newLabel("somelabel").build()
        account.addFolders(serverUserFolder)
        account.addLabels(serverUserLabel)
        user.fetchContainers()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(allAdapterItems).haveExactly(1, folderItem(serverUserFolder.serverFid.toLong()))
        assertThat(allAdapterItems).haveExactly(1, labelItem(serverUserLabel.serverLid))
    }

    @Test
    fun fragment_switchesToInboxIfSelectedFolderIsDeleted() {
        val serverUserFolder = account.newFolder("whatever").build()
        account.addFolders(serverUserFolder)
        user.fetchContainers()
        prepareFragment()
        clickOnItem("whatever")
        account.removeFolder(serverUserFolder)
        user.fetchContainers()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val (newContainer) = containerChangeEvents[containerChangeEvents.size - 1]
        assertThat(newContainer).isEqualTo(inbox().folderContainer)
    }

    @Test
    fun fragment_retainsWithAttachesIfSomeFolderDeleted() {
        val serverUserFolder = account.newFolder("whatever").build()
        account.addFolders(serverUserFolder)
        user.fetchContainers()
        prepareFragment()
        clickOnItem("With attachments")
        account.removeFolder(serverUserFolder)
        user.fetchContainers()
        assertThat(controller.get()!!.adapter.getSelectedItem()?.getEmailSource(false)).isEqualTo(CustomContainer(CustomContainer.Type.WITH_ATTACHMENTS, false))
    }

    @Test
    fun presenter_shouldNotStartBeforeOnStart() {
        controller = FragmentController.of(ContainerListFragment(), TestActivity::class.java, Intent())
        controller.create()
        sendAccountChangedEvent(user.uid)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(controller.get()!!.presenter.isPaused).isTrue
        controller.start()
        assertThat(controller.get()!!.presenter.isPaused).isFalse
    }

    private fun configurationChange_restoresContainer_helper(container: Container2) {
        triggerConfigurationChange()
        val (newContainer) = containerChangeEvents[0]
        assertThat(containerChangeEvents).hasSize(1)
        assertThat(newContainer).isEqualTo(container)
    }

    @Test
    fun configurationChange_restoresFolder() {
        val drafts = drafts().folderContainer
        prepareFragment()
        clickOnItem(drafts().folder.name)
        configurationChange_restoresContainer_helper(drafts)
    }

    @Test
    fun configurationChange_restoresLabel() {
        val important = important().labelContainer
        prepareFragment()
        clickOnItem(important().label.name)
        configurationChange_restoresContainer_helper(important)
    }

    @Test
    fun configurationChange_restoresWithAttachesContainer() {
        val withAttachContainer = CustomContainer(CustomContainer.Type.WITH_ATTACHMENTS, false)
        prepareFragment()
        clickOnItem("With attachments")
        configurationChange_restoresContainer_helper(withAttachContainer)
    }

    @Test
    fun configurationChange_restoresUnreadContainer() {
        val unreadContainer = CustomContainer(CustomContainer.Type.UNREAD, false)
        prepareFragment()
        clickOnItem("Unread")
        configurationChange_restoresContainer_helper(unreadContainer)
    }

    @Ignore("fix it")
    @Test
    fun configurationChange_restoresCollapsedFolder() {
        val serverFolder = account.newChildFolder(serverInbox(), "whatever").build()
        account.addFolders(serverFolder)
        user.fetchContainers()
        prepareFragment()
        val childFolder = getLocalFolder(serverFolder).folderContainer
        clickOnItem("whatever") // select child folder
        collapse("Inbox")
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        triggerConfigurationChange()
        assertThat(containerChangeEvents).hasSize(1)
        val (newContainer) = containerChangeEvents[0]
        assertThat(newContainer).isEqualTo(childFolder)
    }

    @Test
    fun adapter_displayFoldersForTwoAccounts() {
        // user 1 part
        prepareFragment()
        var folderItems = allAdapterItems.filterIsInstance(
            ContainersAdapter.FolderItem::class.java
        )
        var adapterFids = folderItems.map { item: ContainersAdapter.FolderItem -> item.folder.fid }
        var serverFids = getServerFids(account)
        assertThat(adapterFids).hasSameElementsAs(serverFids)

        // user 2 part
        val account2 = getInstance().createAccountWrapper(LoginData("Second user", Accounts.LOGIN_TYPE, "AMMA_TOKEN_FOR_USER_2"))
        val user2 = User.create(account2.loginData)
        user2.initialLoad()
        sendAccountChangedEvent(user2.uid)
        val folderItems2 = allAdapterItems.filterIsInstance(
            ContainersAdapter.FolderItem::class.java
        )
        val adapterFids2 = folderItems2.map { item: ContainersAdapter.FolderItem -> item.folder.fid }
        val serverFids2 = getServerFids(account2)
        assertThat(adapterFids2).hasSameElementsAs(serverFids2)

        // user 1 again
        sendAccountChangedEvent(user.uid)
        folderItems = allAdapterItems.filterIsInstance(ContainersAdapter.FolderItem::class.java)
        adapterFids = folderItems.map { item: ContainersAdapter.FolderItem -> item.folder.fid }
        serverFids = getServerFids(account)
        assertThat(adapterFids).hasSameElementsAs(serverFids)
    }

    @Test
    fun fragment_shouldNotCrashOnNotificationIfNoLoadedDataOnResume() {
        createServerAndUser(Accounts.teamLoginData, true)
        val testScheduler = TestScheduler()
        RxJavaPlugins.setIoSchedulerHandler { testScheduler }
        val intent = Intent()
        intent.putExtra(Constants.FID_EXTRA, inboxFid())
        intent.putExtra(ContainerListFragment.UID_FOR_FOLDER_TO_SWITCH, user.uid)
        controller = FragmentController.of(ContainerListFragment(), TestActivity::class.java, intent)
        controller.create().start()
        controller.get()!!.onAccountChanged(Accounts.teamLoginData.uid)
        sendAccountChangedEvent(user.uid)
        controller.resume().visible()
        assertThat(controller.get()!!.viewBinding.containersList.isVisible).isFalse
        testScheduler.triggerActions() // call onDataLoaded()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(controller.get()!!.viewBinding.containersList.isVisible).isTrue
    }

    @Test
    fun fragment_shouldSelectInboxOfNewAccountIfFolderAccountWasChangedInBackground() {
        prepareFragment()
        val fragmentManager = controller.get().fragmentManager
        val state = fragmentManager!!.saveFragmentInstanceState(controller.get()!!)
        controller.stop().destroy()
        getInstance().createAccountWrapper(Accounts.teamLoginData)
        val user2 = User.create(Accounts.teamLoginData)
        user2.initialLoad()
        sendAccountChangedEvent(user2.uid)
        val testScheduler = TestScheduler()
        RxJavaPlugins.setIoSchedulerHandler { scheduler: Scheduler? -> testScheduler }
        val fragment = ContainerListFragment()
        fragment.setInitialSavedState(state)
        controller = FragmentController.of(fragment, TestActivity::class.java)
        controller.create().start().resume().visible()
        testScheduler.triggerActions() // call onDataLoaded()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(controller.get()!!.viewBinding.containersList.isVisible).isTrue
        assertThat(controller.get()!!.adapter.getSelectedItem()?.getEmailSource(false)).isEqualTo(inbox().folderContainer)
    }

    @Test
    @Throws(Exception::class)
    fun fragment_shouldChangeFolderFromFakeArchiveToReal() {
        val thread = account.newThread(account.newReadMessage(account.inboxFolder).mid("10000")).build()
        account.addThreads(thread)
        user.isThreaded = true
        user.fetchMessages(inbox())
        val task = ArchiveTask.create(
            IntegrationTestRunner.app(),
            listOf(10000L),
            user.uid
        )
        task.updateDatabase(IntegrationTestRunner.app())
        prepareFragment()
        val oldArchive = getFolderWithType(FolderType.ARCHIVE.serverType)
        controller.get()!!.switchToContainer(oldArchive.getEmailSource(false), false)
        assertThat(oldArchive.folder.fid).isEqualTo(ArchiveTask.FAKE_ARCHIVE_ID.toLong())
        account.addArchiveFolder()
        user.fetchContainers()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val newArchive = getFolderWithType(FolderType.ARCHIVE.serverType)
        assertThat(newArchive.folder.fid).isEqualTo(archive().folderId)
    }

    @Test
    fun fragment_shouldSelectFolder() {
        prepareFragment()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(controller.get()!!.adapter.getSelectedItem()?.getEmailSource(false)).isInstanceOf(FolderContainer::class.java)
    }

    @Test
    fun reopenSameFolder_shouldCallRefreshAndShouldNotCallChangeContainer() {
        prepareFragment()
        assertThat(containerChangeEvents.size).isEqualTo(1)
        assertThat(containerRefreshEventsCount).isEqualTo(0)
        clickOnItem("Inbox")
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(containerChangeEvents.size).isEqualTo(1)
        assertThat(containerRefreshEventsCount).isEqualTo(1)
    }

    @Test
    fun clickOnLogOut_currentAccountIsNotUsedInApp() {
        controller = FragmentController.of(ContainerListFragment(), TestActivity::class.java, Intent())
        controller.create().start()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val selectedUid = AccountModel.getSelectedUid(controller.get()!!.requireContext())
        Assert.assertTrue(isAccountUsedInApp(selectedUid))
        controller.get().onLogOutClicked()
        Assert.assertFalse(isAccountUsedInApp(selectedUid))
    }

    private fun isAccountUsedInApp(uid: Long): Boolean {
        return BaseMailApplication.getApplicationComponent(controller.get()!!.requireContext()).accountModel()
            .getAccountByUidSingle(uid)
            .blockingGet()
            .get()
            .isUsedInApp
    }

    private fun getFolderWithType(folderType: Int): ContainersAdapter.FolderItem {
        val folderItems = allAdapterItems.filterIsInstance(
            ContainersAdapter.FolderItem::class.java
        )
        return folderItems.first { item: ContainersAdapter.FolderItem -> item.folder.type == folderType }
    }

    private fun getServerFids(account: AccountWrapper): List<Long> {
        return getServerFids(account, true)
    }

    private fun getServerFids(account: AccountWrapper, hideOutgoing: Boolean): List<Long> {
        val fids = account.folders.foldersList.mapNotNull { (serverFid, _, _, type) ->
            if (!isTabContainer(
                    type
                )
            ) {
                return@mapNotNull serverFid.toLong()
            } else {
                return@mapNotNull null
            }
        }.toMutableList()
        if (hideOutgoing) {
            fids.remove(outgoingFid())
        }
        return fids
    }

    private fun triggerConfigurationChange() {
        val state = Bundle()
        controller.activityController.saveInstanceState(state)
        val newActivityController = Robolectric.buildActivity(
            TestActivity::class.java
        )
        newActivityController.create(state).start()
        controller = FragmentController.of(newActivityController.get().fragment!!)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private val containerChangeEvents: List<Pair<Container2?, Boolean>>
        get() = (controller.get()!!.activity as TestActivity?)!!.containerChangeEvents

    private val containerRefreshEventsCount: Int
        get() = (controller.get()!!.activity as TestActivity?)!!.containerRefreshEventsCount

    class TestActivity : TestFragmentActivity(), ActivityWithDrawer, ContainerListFragment.Callback {

        val containerChangeEvents: MutableList<Pair<Container2?, Boolean>> = ArrayList()

        var containerRefreshEventsCount = 0

        private var drawerOpened = false

        val fragment: ContainerListFragment?
            get() = supportFragmentManager.findFragmentById(ROBOLECTRIC_DEFAULT_FRAGMENT_ID) as ContainerListFragment?

        fun openDrawer() {
            drawerOpened = true
        }

        fun closeDrawer() {
            drawerOpened = false
        }

        override fun getDrawer(): ViewGroup {
            return Mockito.mock(ViewGroup::class.java)
        }

        override fun isDrawerOpened(): Boolean {
            return drawerOpened
        }

        override fun setDrawerLocked(locked: Boolean) {}

        override fun onMessageContainerChanged(newEmailSource: Container2?, automatic: Boolean) {
            containerChangeEvents.add(Pair(newEmailSource, automatic))
        }

        override fun onMessageContainerReset() {
            containerRefreshEventsCount++
        }

        override fun onSubscriptionsClick(pendingTab: Boolean) {}

        override fun onPurchaseClick(source: BuySubscriptionSource) {}
    }
}
