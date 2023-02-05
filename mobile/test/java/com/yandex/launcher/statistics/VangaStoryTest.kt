package com.yandex.launcher.statistics

import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

const val TEST_JSON = """{"placement":{"all_apps":{"header":{"position":"1:2"}}},"packageName":"test","className":"test2"}"""

class VangaStoryTest : BaseRobolectricTest() {

    private val testVangaInfo = VangaStory.VangaInfo("test", "test2", 1, 2)
    private val testViewEvent = createStoryEvent(StoryEvent.Events.EVENT_VANGA_VIEW, arrayListOf(testVangaInfo))
    private val testLaunchEvent = createStoryEvent(StoryEvent.Events.EVENT_VANGA_LAUNCH, testVangaInfo)

    private val mockStoryManager = mock<StoryManager>()
    private val story = VangaStory()

    override fun setUp() {
        super.setUp()
        story.storyManager = mockStoryManager
    }

    @Test
    fun `on execute routine, events should be reported`() {
        verifyNoInteractions(mockStoryManager)

        story.onEvent(testViewEvent)

        verify(mockStoryManager).sendStatboxJson(VANGA_VIEW_EVENT_NAME, TEST_JSON)
    }

    @Test
    fun `on launch, correct json reported`() {
        verifyNoInteractions(mockStoryManager)

        story.onEvent(testLaunchEvent)

        verify(mockStoryManager).sendStatboxJson(VANGA_LAUNCH_EVENT_NAME, TEST_JSON)
    }

    private fun createStoryEvent(type: Int, data: Any): StoryEvent {
        val storyEvent = ReflectionHelpers.callConstructor(StoryEvent::class.java)
        ReflectionHelpers.setField(storyEvent, "type", type)
        ReflectionHelpers.setField(storyEvent, "param1", data)
        return storyEvent
    }
}
