package ru.yandex.yandexbus.inhouse.domain.route

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.maps.toolkit.datasync.binding.DataSyncManager
import ru.yandex.maps.toolkit.datasync.binding.Query
import ru.yandex.maps.toolkit.datasync.binding.datasync.concrete.route.Route
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.whenever

class RouteBookmarkRepositoryTest {

    private lateinit var sharedData: TestBookmarksSharedData
    private lateinit var repository: RouteBookmarkRepository

    @Before
    fun setUp() {
        sharedData = TestBookmarksSharedData()

        val dataSyncManager = Mockito.mock(DataSyncManager::class.java)
        whenever(dataSyncManager.query<Route, Query<Route>>(any())).thenReturn(sharedData)
        repository = RouteBookmarkRepository(dataSyncManager)
    }

    private fun initRoutes(vararg routes: Route) {
        sharedData.currentRoutes = routes.toList()
    }

    @Test
    fun `route without uri is not bookmarked`() {
        repository.isBookmarked(null)
            .test()
            .assertCompleted()
            .assertValue(false)
    }

    @Test
    fun `non existing in repository route is not bookmarked`() {
        repository.isBookmarked("non_existing_uri")
            .test()
            .assertValue(false)
    }

    @Test
    fun `route from repository is bookmarked`() {
        initRoutes(testRoute1, testRoute2)

        repository.isBookmarked(testRoute1.uri)
            .test()
            .assertValue(true)
    }

    @Test
    fun `add bookmark sends call to shared data`() {
        repository.addBookmark(testRoute1)
        Assert.assertEquals(listOf(testRoute1), sharedData.currentRoutes)
    }

    @Test
    fun `removal of non existing route completes`() {
        repository.removeBookmark("non_existing_uri")
            .test()
            .assertCompleted()
    }

    @Test
    fun `existing route is removed from shared data`() {
        initRoutes(testRoute1, testRoute2)

        repository.removeBookmark(testRoute1.uri)
            .test()
            .assertCompleted()

        Assert.assertEquals(listOf(testRoute2), sharedData.currentRoutes)
    }

    @Test
    fun `returns empty bookmarks for empty uris list`() {
        repository.getBookmarks(emptyList())
            .test()
            .assertCompleted()
            .assertValue(emptyList())
    }

    @Test
    fun `does not return bookmarks with non existing in repository uris`() {
        initRoutes(testRoute1, testRoute2)

        repository.getBookmarks(listOf("non_existing_uri"))
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `returns only bookmarks whose uris are in query`() {
        initRoutes(testRoute1, testRoute2)

        repository.getBookmarks(listOf(testRoute1.uri))
            .test()
            .assertValue(listOf(testRoute1))
    }

    companion object {
        private val testRoute1 = route("test_route_1", "Route title 1")
        private val testRoute2 = route("test_route_2", "Route title 2")

        fun route(uri: String, title: String): Route {
            return Route.builder().setUri(uri).setTitle(title).build()
        }
    }
}