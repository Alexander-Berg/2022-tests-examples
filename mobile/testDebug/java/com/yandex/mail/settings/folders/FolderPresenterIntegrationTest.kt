package com.yandex.mail.settings.folders

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.mail.R
import com.yandex.mail.container.AccountInfoContainer
import com.yandex.mail.entity.Folder
import com.yandex.mail.entity.NanoFoldersTree
import com.yandex.mail.model.AccountModel
import com.yandex.mail.model.FoldersModel
import com.yandex.mail.network.MailApi
import com.yandex.mail.network.response.FolderTaskJson
import com.yandex.mail.network.response.Status
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.storage.entities.EntitiesTestFactory
import com.yandex.mail.ui.presenters.configs.AccountPresenterConfig
import com.yandex.mail.util.BadStatusException
import com.yandex.mail.util.NanomailEntitiesTestUtils.createTestFolder
import com.yandex.mail.util.getSystemFoldersNames
import com.yandex.mail.widget.MailWidgetsModel
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.Single.error
import io.reactivex.Single.fromCallable
import io.reactivex.schedulers.Schedulers.trampoline
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.verify

@RunWith(IntegrationTestRunner::class)
class FolderPresenterIntegrationTest {

    private val folderView = mock<FolderView>()

    private val foldersModel = mock<FoldersModel>()

    private val foldersTree = mock<NanoFoldersTree>()

    private val accountModel = mock<AccountModel>()

    private val mailApi = mock<MailApi>()

    private val widgetsModel = mock<MailWidgetsModel>()

    private lateinit var folderPresenter: FolderPresenter

    private lateinit var presenterConfig: AccountPresenterConfig

    @Before
    fun setUp() {
        whenever(foldersModel.observeFoldersTree()).thenReturn(Flowable.fromIterable(listOf(foldersTree)))
        whenever(foldersTree.getSortedFolders()).thenReturn(
            listOf(createTestFolder(100L).copy(name = "Folder"))
        )

        whenever(mailApi.createFolder(anyString(), any())).thenReturn(mockResponse())
        whenever(mailApi.updateFolder(anyLong(), anyString(), any())).thenReturn(mockResponse())
        whenever(mailApi.deleteFolder(anyLong())).thenReturn(mockResponse())
        whenever(widgetsModel.getWidgetForFolders(anyLong(), anyCollection<Long>())).thenReturn(intArrayOf())

        presenterConfig = createAccountPresenterConfig()
        folderPresenter = createFolderPresenter()
    }

    @Test
    fun `createFolder should not add folder with system name`() {
        folderPresenter.onBindView(folderView)
        for (name in getSystemFoldersNames()) {
            folderPresenter.createRootFolder(name)

            verify(folderView).onFolderCreationError(R.string.folder_system_name_creation_error)
            verify(folderView, never()).onNetworkProblems()
            clearInvocations(folderView)
        }
    }

    @Test
    fun `createFolder should not add folder with existing name`() {
        whenever(foldersTree.isNameExists(FOLDER_NAME1, null)).thenReturn(true)
        folderPresenter.onBindView(folderView)
        folderPresenter.createRootFolder(FOLDER_NAME1)

        verify(folderView).onFolderCreationError(R.string.folder_name_exists_error)
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `createFolder should not add subfolder with existing name`() {
        whenever(foldersTree.isNameExists(FOLDER_NAME1, 1L)).thenReturn(true)
        folderPresenter.onBindView(folderView)
        folderPresenter.createChildFolder(FOLDER_NAME1, 1L)

        verify(folderView).onFolderCreationError(R.string.folder_name_exists_error)
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `createFolder should handle api error`() {
        whenever(foldersTree.isNameExists(FOLDER_NAME1, null)).thenReturn(false)
        whenever(mailApi.createFolder(anyString(), any())).thenReturn(badStatus())
        folderPresenter.onBindView(folderView)
        folderPresenter.createRootFolder(FOLDER_NAME1)

        verify(folderView).onFolderCreationError(R.string.folders_settings_folder_creation_error_general)
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `createFolder should handle network error`() {
        whenever(foldersTree.isNameExists(FOLDER_NAME1, null)).thenReturn(false)
        whenever(mailApi.createFolder(anyString(), any())).thenReturn(Single.error(Exception()))
        folderPresenter.onBindView(folderView)
        folderPresenter.createRootFolder(FOLDER_NAME1)

        verify(folderView).onNetworkProblems()
    }

    @Test
    fun `createRootFolder should call api with null parent fid`() {
        folderPresenter.onBindView(folderView)
        folderPresenter.createRootFolder(FOLDER_NAME1)

        verify(mailApi).createFolder(FOLDER_NAME1, null)

        verify(folderView).onFinishFolderOperation()
        verify(folderView, never()).onFolderCreationError(anyInt())
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `createChildFolder should call api add with parent fid`() {
        folderPresenter.onBindView(folderView)
        folderPresenter.createChildFolder(FOLDER_NAME1, 0)

        verify(mailApi).createFolder(FOLDER_NAME1, 0)

        verify(folderView).onFinishFolderOperation()
        verify(folderView, never()).onFolderCreationError(anyInt())
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `updateFolder should not update folder name to system name`() {
        folderPresenter.onBindView(folderView)
        for (name in getSystemFoldersNames()) {
            folderPresenter.updateFolderNameOnly(0, name, null)

            verify(folderView).onFolderEditingError(R.string.folder_system_name_editing_error)
            verify(folderView, never()).onNetworkProblems()
            clearInvocations(folderView)
        }
    }

    @Test
    fun `updateFolder should not update folder name to existing name`() {
        whenever(foldersTree.isNameExists(FOLDER_NAME1, null)).thenReturn(true)
        folderPresenter.onBindView(folderView)
        folderPresenter.updateFolderNameOnly(0, FOLDER_NAME1, null)

        verify(folderView).onFolderEditingError(R.string.folder_name_exists_error)
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `updateFolder should not update subfolder name to existing name`() {
        whenever(foldersTree.isNameExists(FOLDER_NAME1, 1L)).thenReturn(true)
        folderPresenter.onBindView(folderView)
        folderPresenter.updateFolderParent(0, FOLDER_NAME1, 1L)

        verify(folderView).onFolderEditingError(R.string.folder_name_exists_error)
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `updateFolder should handle api error`() {
        whenever(foldersTree.isNameExists(FOLDER_NAME1, 0)).thenReturn(false)
        whenever(mailApi.updateFolder(anyLong(), anyString(), any())).thenReturn(badStatus())
        folderPresenter.onBindView(folderView)
        folderPresenter.updateFolderNameOnly(0, FOLDER_NAME1, null)

        verify(folderView).onFolderEditingError(R.string.folders_settings_folder_editing_error)
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `updateFolderNameOnly should call api with name updating only`() {
        folderPresenter.onBindView(folderView)

        folderPresenter.updateFolderNameOnly(0, FOLDER_NAME2, null)

        verify(mailApi).updateFolder(0, FOLDER_NAME2, null)
        verify(folderView, never()).onFolderEditingError(anyInt())
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `updateFolderParent should call api with new parent fid`() {
        folderPresenter.onBindView(folderView)
        folderPresenter.updateFolderParent(0, FOLDER_NAME1, 1)

        verify(mailApi).updateFolder(0, FOLDER_NAME1, "1")
        verify(folderView).onFinishFolderOperation()
        verify(folderView, never()).onFolderEditingError(anyInt())
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `updateFolderParent should call api with empty parent`() {
        folderPresenter.onBindView(folderView)
        folderPresenter.updateFolderParent(0, FOLDER_NAME1, null)

        verify(mailApi).updateFolder(0, FOLDER_NAME1, "")
        verify(folderView).onFinishFolderOperation()
        verify(folderView, never()).onFolderEditingError(anyInt())
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `deleteFolder should call api delete folder`() {
        folderPresenter.onBindView(folderView)

        folderPresenter.deleteFolder(0)

        verify(mailApi).deleteFolder(0)
        verify(folderView).onFinishFolderOperation()
        verify(folderView, never()).onFolderDeletionError()
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `deleteFolder should handle api error`() {
        folderPresenter.onBindView(folderView)
        whenever(mailApi.deleteFolder(anyLong())).thenReturn(badStatus())

        folderPresenter.deleteFolder(0)

        verify(folderView).onFolderDeletionError()
        verify(folderView, never()).onNetworkProblems()
    }

    @Test
    fun `loadFolderPath should load list of locale aware parents`() {
        val mockTree = mock<NanoFoldersTree>()
        val mockFolderParent1 = mock<Folder>()
        val mockFolderParent2 = mock<Folder>()
        val mockFolderChild = mock<Folder>()
        whenever(mockTree.getFullPath(mockFolderChild)).thenReturn(listOf(mockFolderParent1, mockFolderParent2))
        whenever(mockTree.getLocaleAwareName(mockFolderParent1)).thenReturn("Parent1")
        whenever(mockTree.getLocaleAwareName(mockFolderParent2)).thenReturn("Parent2")
        whenever(foldersModel.observeFoldersTree()).thenReturn(Flowable.fromIterable(listOf(mockTree)))

        folderPresenter.onBindView(folderView)
        folderPresenter.loadFolderPath(mockFolderChild)

        verify(folderView).onFolderPathLoaded(listOf("Parent1", "Parent2"))
    }

    @Test
    fun `loadUserInfo should load`() {
        val uid = 1L
        val accountInfo = AccountInfoContainer.fromAccountEntity(
            EntitiesTestFactory.buildAccountEntity(1, true)
        )
        whenever(accountModel.getAccountInfoWithMail(uid)).thenReturn(Single.fromCallable { accountInfo })

        folderPresenter.onBindView(folderView)
        folderPresenter.loadUserInfo(uid)

        verify(folderView).onUserInfoLoaded(accountInfo)
    }

    @Test
    fun `loadUserInfo should handle error`() {
        val uid = 1L
        whenever(accountModel.getAccountInfoWithMail(uid)).thenReturn(error(Exception()))

        folderPresenter.onBindView(folderView)
        folderPresenter.loadUserInfo(uid)

        verify(folderView).onUserInfoLoadingError()
    }

    @Test
    fun `loadFolderUiInfo should load info`() {
        val mockTree = mock<NanoFoldersTree>()
        val mockFolderParent = mock<Folder>()
        val mockFolderChild = mock<Folder>()
        whenever(mockTree.getLocaleAwareName(mockFolderChild)).thenReturn("Child")
        whenever(mockTree.getParent(mockFolderChild)).thenReturn(mockFolderParent)
        whenever(foldersModel.observeFoldersTree()).thenReturn(Flowable.fromIterable(listOf(mockTree)))

        folderPresenter.onBindView(folderView)
        folderPresenter.loadFolderUiInfo(mockFolderChild)

        verify(folderView).onFolderUiInfoLoaded("Child", mockFolderParent)
    }

    private fun createAccountPresenterConfig(): AccountPresenterConfig {
        return AccountPresenterConfig(
            trampoline(),
            trampoline(),
            1
        )
    }

    private fun createFolderPresenter(): FolderPresenter {
        return FolderPresenter(
            IntegrationTestRunner.app(),
            foldersModel,
            accountModel,
            presenterConfig,
            mailApi,
            widgetsModel
        )
    }

    private fun mockResponse(): Single<FolderTaskJson> = fromCallable { mock<FolderTaskJson>() }

    private fun badStatus(): Single<FolderTaskJson> = Single.error(BadStatusException(Status.create(0, "trace", "phrase", null)))

    companion object {

        private const val FOLDER_NAME1 = "folder1"

        private const val FOLDER_NAME2 = "folder2"
    }
}
