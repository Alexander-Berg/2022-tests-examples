package com.example.bootcampapp.presentation.presenter

import com.example.bootcampapp.contract.StaffContract
import com.example.bootcampapp.data.network.dto.ContainerOfStaffMembersInfoDto
import com.example.bootcampapp.data.network.dto.StaffMemberInfoDto
import com.example.bootcampapp.domain.models.ContainerOfStaffMembersInfo
import com.example.bootcampapp.domain.models.StaffMemberInfo
import com.example.bootcampapp.domain.providers.StaffDownloadStatusProviderImpl
import com.example.bootcampapp.domain.usecases.CacheStaffMembersInfoUseCase
import com.example.bootcampapp.domain.usecases.GetMembersInfoFromDatabaseUseCase
import com.example.bootcampapp.domain.usecases.GetStaffMembersInfoFromNetworkUseCase
import com.example.bootcampapp.formatters.DomainModelToPresentationViewObjectsFormatter
import com.example.bootcampapp.presentation.providers.StaffDownloadStatusProvider
import com.example.bootcampapp.presentation.viewObjects.StaffMemberInfoViewObject
import com.example.bootcampapp.utils.IOError
import com.example.bootcampapp.utils.NetworkError
import com.example.bootcampapp.utils.Resource
import com.example.bootcampapp.utils.UnknownError
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class StaffPresenterImplTest {
    private var getStaffMembersInfoFromNetworkUseCase: GetStaffMembersInfoFromNetworkUseCase? = null
    private var getMembersInfoFromDatabaseUseCase: GetMembersInfoFromDatabaseUseCase? = null
    private var cacheStaffMembersInfoUseCase: CacheStaffMembersInfoUseCase? = null
    private var formatter: DomainModelToPresentationViewObjectsFormatter? = null
    private var viewState: StaffContract.StaffView? = null
    private var presenter: StaffPresenterImpl? = null
    private var staffDownloadStatusProvider: StaffDownloadStatusProvider? = StaffDownloadStatusProviderImpl()

    @Before
    fun setup() {
        getStaffMembersInfoFromNetworkUseCase = mock(GetStaffMembersInfoFromNetworkUseCase::class.java)
        getMembersInfoFromDatabaseUseCase = mock(GetMembersInfoFromDatabaseUseCase::class.java)
        cacheStaffMembersInfoUseCase = mock(CacheStaffMembersInfoUseCase::class.java)
        formatter = mock(DomainModelToPresentationViewObjectsFormatter::class.java)
        viewState = mock(StaffContract.StaffView::class.java)
        presenter = StaffPresenterImpl(
            getStaffMembersInfoFromNetworkUseCase!!,
            getMembersInfoFromDatabaseUseCase!!,
            cacheStaffMembersInfoUseCase!!,
            formatter!!,
            staffDownloadStatusProvider!!
        )
    }

    @After
    fun cleanup() {
        getStaffMembersInfoFromNetworkUseCase = null
        getMembersInfoFromDatabaseUseCase = null
        cacheStaffMembersInfoUseCase = null
        formatter = null
        viewState = null
        presenter = null
        staffDownloadStatusProvider = null
    }

    @Test
    fun `test StaffPresenterImpl - all use cases work properly the result is updated, cached and retrieved from cache at the end`() {
        whenever(getStaffMembersInfoFromNetworkUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnSuccess(containerOfStaffMembersInfoDto)))
        whenever(cacheStaffMembersInfoUseCase!!.execute(containerOfStaffMembersInfoDto))
            .thenReturn(Single.just(Resource.OnSuccess(Unit)))
        whenever(getMembersInfoFromDatabaseUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnSuccess(containerOfStaffMembersInfo)))
        whenever(formatter!!.map(staffMembersInfo)).thenReturn(staffMemberInfoViewObject)

        presenter!!.attachView(viewState)

        val order = Mockito.inOrder(
            viewState,
            getStaffMembersInfoFromNetworkUseCase,
            getMembersInfoFromDatabaseUseCase,
            cacheStaffMembersInfoUseCase
        )

        order.verify(viewState)!!.showProgress()
        order.verify(getStaffMembersInfoFromNetworkUseCase)!!.execute(currentPage)
        order.verify(cacheStaffMembersInfoUseCase)!!.execute(containerOfStaffMembersInfoDto)
        order.verify(getMembersInfoFromDatabaseUseCase)!!.execute(currentPage)
        order.verify(viewState)!!.hideProgress()
        order.verify(viewState)!!.addNewCards(listOfStaffMembersInfoViewObjects)
        order.verifyNoMoreInteractions()
        assertEquals(currentPage + 1, presenter!!.currentPage)
        assertEquals(false, presenter!!.isOver)
    }

    @Test
    fun `test StaffPresenterImpl - cache usecase throws an error the page is not changed and the result is unknown error`() {
        whenever(getStaffMembersInfoFromNetworkUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnSuccess(containerOfStaffMembersInfoDto)))
        whenever(cacheStaffMembersInfoUseCase!!.execute(containerOfStaffMembersInfoDto))
            .thenReturn(Single.just(Resource.OnError(UnknownError())))
        presenter!!.attachView(viewState)

        val order = Mockito.inOrder(
            viewState,
            getStaffMembersInfoFromNetworkUseCase,
            getMembersInfoFromDatabaseUseCase,
            cacheStaffMembersInfoUseCase
        )

        order.verify(viewState)!!.showProgress()
        order.verify(getStaffMembersInfoFromNetworkUseCase)!!.execute(currentPage)
        order.verify(cacheStaffMembersInfoUseCase)!!.execute(containerOfStaffMembersInfoDto)
        order.verify(viewState)!!.hideProgress()
        order.verifyNoMoreInteractions()
        assertEquals(currentPage, presenter!!.currentPage)
        assertEquals(false, presenter!!.isOver)
    }

    @Test
    fun `test StaffPresenterImpl - network usecase and staff usecase failed the page is not changed the result is IO Error`() {
        whenever(getStaffMembersInfoFromNetworkUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnError(NetworkError())))
        whenever(getMembersInfoFromDatabaseUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnError(IOError())))

        presenter!!.attachView(viewState)

        val order = Mockito.inOrder(
            viewState,
            getStaffMembersInfoFromNetworkUseCase,
            getMembersInfoFromDatabaseUseCase,
            cacheStaffMembersInfoUseCase
        )

        order.verify(viewState)!!.showProgress()
        order.verify(getStaffMembersInfoFromNetworkUseCase)!!.execute(currentPage)
        order.verify(viewState)!!.processError(NetworkError().message)
        order.verify(getMembersInfoFromDatabaseUseCase)!!.execute(currentPage)
        order.verify(viewState)!!.hideProgress()
        order.verifyNoMoreInteractions()
        assertEquals(currentPage, presenter!!.currentPage)
        assertEquals(false, presenter!!.isOver)
    }

    @Test
    fun `test StaffPresenterImpl - network usecase failed the page changes the result is list from cache`() {
        whenever(getStaffMembersInfoFromNetworkUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnError(NetworkError())))
        whenever(getMembersInfoFromDatabaseUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnSuccess(containerOfStaffMembersInfo)))
        whenever(formatter!!.map(staffMembersInfo)).thenReturn(staffMemberInfoViewObject)

        presenter!!.attachView(viewState)

        val order = Mockito.inOrder(
            viewState,
            getStaffMembersInfoFromNetworkUseCase,
            getMembersInfoFromDatabaseUseCase,
            cacheStaffMembersInfoUseCase
        )

        order.verify(viewState)!!.showProgress()
        order.verify(getStaffMembersInfoFromNetworkUseCase)!!.execute(currentPage)
        order.verify(viewState)!!.processError(NetworkError().message)
        order.verify(getMembersInfoFromDatabaseUseCase)!!.execute(currentPage)
        order.verify(viewState)!!.hideProgress()
        order.verify(viewState)!!.addNewCards(listOfStaffMembersInfoViewObjects)
        order.verifyNoMoreInteractions()
        assertEquals(currentPage + 1, presenter!!.currentPage)
        assertEquals(false, presenter!!.isOver)
    }

    @Test
    fun `test StaffPresenterImpl - when the last page is received isOver valuen is changed `() {
        whenever(getStaffMembersInfoFromNetworkUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnSuccess(containerOfStaffMembersInfoDtoWhenPagesAreOver)))
        whenever(cacheStaffMembersInfoUseCase!!.execute(containerOfStaffMembersInfoDtoWhenPagesAreOver))
            .thenReturn(Single.just(Resource.OnSuccess(Unit)))
        whenever(getMembersInfoFromDatabaseUseCase!!.execute(currentPage))
            .thenReturn(Single.just(Resource.OnSuccess(containerOfStaffMembersInfoWhenPagesAreOver)))
        whenever(formatter!!.map(staffMembersInfo)).thenReturn(staffMemberInfoViewObject)

        presenter!!.attachView(viewState)

        val order = Mockito.inOrder(
            viewState,
            getStaffMembersInfoFromNetworkUseCase,
            getMembersInfoFromDatabaseUseCase,
            cacheStaffMembersInfoUseCase
        )

        order.verify(viewState)!!.showProgress()
        order.verify(getStaffMembersInfoFromNetworkUseCase)!!.execute(currentPage)
        order.verify(cacheStaffMembersInfoUseCase)!!.execute(containerOfStaffMembersInfoDtoWhenPagesAreOver)
        order.verify(getMembersInfoFromDatabaseUseCase)!!.execute(currentPage)
        order.verify(viewState)!!.hideProgress()
        order.verify(viewState)!!.addNewCards(listOfStaffMembersInfoViewObjects)
        order.verifyNoMoreInteractions()
        assertEquals(totalPages + 1, presenter!!.currentPage)
        assertEquals(true, presenter!!.isOver)
    }

    @Test
    fun `test StaffPresenterImpl - when the last page has already been received no methods are invoked `() {
        staffDownloadStatusProvider!!.currentPage = totalPages + 1
        staffDownloadStatusProvider!!.isOver = true

        presenter!!.attachView(viewState)

        val order = Mockito.inOrder(
            viewState,
            getStaffMembersInfoFromNetworkUseCase,
            getMembersInfoFromDatabaseUseCase,
            cacheStaffMembersInfoUseCase
        )
        order.verifyNoMoreInteractions()
        assertEquals(totalPages + 1, presenter!!.currentPage)
        assertEquals(true, presenter!!.isOver)
    }

    companion object {
        private const val currentPage = 1
        private const val totalPages = 10
        private const val firstName = "first name"
        private const val lastName = "last name"
        private const val login = "login"
        private const val city = "San Andres"
        private const val photo = "http::/photo.pdf"
        private const val department = "boring department"
        private const val betterQualityPhoto = "http::/photo/460.pdf"
        private val staffMembersInfo = StaffMemberInfo(
            nameField = "$firstName $lastName",
            jobField = "$city, $department",
            loginField = login,
            photoUrl = betterQualityPhoto
        )
        private val staffMemberInfoViewObject = StaffMemberInfoViewObject(
            jobField = "$city, $department",
            nameField = "$firstName $lastName",
            loginField = login,
            photoUrl = betterQualityPhoto
        )
        private val listOfStaffMembersInfoDtoValue = listOf(
            StaffMemberInfoDto(
                firstName = firstName,
                lastName = lastName,
                login = login,
                photo = photo,
                department = department,
                city = city
            )
        )
        private val listOfStaffMembersInfoValue = listOf(
            staffMembersInfo
        )
        private val listOfStaffMembersInfoViewObjects = listOf(
            staffMemberInfoViewObject
        )
        private val containerOfStaffMembersInfo = ContainerOfStaffMembersInfo(
            currentPage = currentPage,
            pages = totalPages,
            listOfStaffMembersInfo = listOfStaffMembersInfoValue
        )
        private val containerOfStaffMembersInfoDto = ContainerOfStaffMembersInfoDto(
            currentPage = currentPage,
            pages = totalPages,
            listOfStaffMemberInfoDtos = listOfStaffMembersInfoDtoValue
        )
        private val containerOfStaffMembersInfoDtoWhenPagesAreOver = ContainerOfStaffMembersInfoDto(
            currentPage = totalPages,
            pages = totalPages,
            listOfStaffMemberInfoDtos = listOfStaffMembersInfoDtoValue
        )
        private val containerOfStaffMembersInfoWhenPagesAreOver = ContainerOfStaffMembersInfo(
            currentPage = totalPages,
            pages = totalPages,
            listOfStaffMembersInfo = listOfStaffMembersInfoValue
        )
    }
}
