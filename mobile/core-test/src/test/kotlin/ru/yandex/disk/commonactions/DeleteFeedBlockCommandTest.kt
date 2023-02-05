package ru.yandex.disk.commonactions

import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import ru.yandex.disk.FeedDatabaseRule
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.feed.BetterCollection
import ru.yandex.disk.feed.DiskDataSyncException
import ru.yandex.disk.feed.DiskDataSyncManager
import ru.yandex.disk.feed.MockCollectionBuilder
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.util.Signal
import rx.schedulers.Schedulers
import rx.subjects.TestSubject
import rx.subjects.TestSubject.create
import java.util.concurrent.TimeUnit

class DeleteFeedBlockCommandTest : AndroidTestCase2() {
    @Rule
    @JvmField
    val rule = FeedDatabaseRule()

    private val commandLogger = CommandLogger()
    private val commandStarter = mock(CommandStarter::class.java)!!
    private val mockFeedDataSyncManager = mock(DiskDataSyncManager::class.java)!!
    private val eventLogger = EventLogger()
    private val testScheduler = Schedulers.test()!!

    private lateinit var command: DeleteFeedBlockCommand

    private val blockId = 1L
    private val collectionId = "index"
    private val collectionBuilder = MockCollectionBuilder().setId(collectionId)!!
    private val remoteBlockId = "20"

    private val remoteCollectionSubject: TestSubject<BetterCollection> = create(testScheduler)

    @Before
    fun setup() {
        command = DeleteFeedBlockCommand(
                rule.feedDatabase,
                eventLogger,
                mockFeedDataSyncManager,
                testScheduler,
                commandStarter)
    }

    @Test
    fun `should delete from FeedDatabase`() {
        insertBlockToDatabases()

        executeCommand()

        rule.feedDatabase.queryFeedBlock(blockId).use { block ->
            assertThat(block.isEmpty, equalTo(true))
        }
    }

    @Test
    fun `should start dataSync update after actual delete`() {
        insertBlockToDatabases()

        executeCommand()
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)

        val inOrder = inOrder(collectionBuilder.mockEditor, mockFeedDataSyncManager)
        inOrder.verify(collectionBuilder.mockEditor).removeRecord(remoteBlockId)
        inOrder.verify(mockFeedDataSyncManager).requestRemoteCollection(collectionId)
        assertThat(remoteCollectionSubject.hasObservers(), `is`(true))
    }

    @Test
    fun `exception during dataSync update should not lead to error event`() {
        insertBlockToDatabases()
        remoteCollectionSubject.onError(DiskDataSyncException("test"))

        executeCommand()
        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)

        assertThat(eventLogger.last, instanceOf(DiskEvents.FeedBlockDeleted::class.java))
    }

    private fun executeCommand() {
        command.execute(DeleteFeedBlockCommandRequest(blockId, Signal(), Signal()))
    }

    private fun insertBlockToDatabases() {
        rule.insertBlockWithContentItem(blockId)
        collectionBuilder.addContentBlock(remoteBlockId)
        `when`(mockFeedDataSyncManager.requestAllLocalCollections())
                .thenReturn(collectionBuilder.buildObservable())
        `when`(mockFeedDataSyncManager.requestLocalCollection(collectionId))
                .thenReturn(collectionBuilder.buildObservable())
        `when`(mockFeedDataSyncManager.requestRemoteCollection(collectionId))
                .thenReturn(remoteCollectionSubject)
    }
}
