package ru.auto.api.model.events

import org.scalatest.OptionValues._
import ru.auto.api.{BaseSpec, StatEvents}

class SearchShowEventSpec extends BaseSpec {

  val emptyMessage: StatEvents.SearchShowEvent = StatEvents.SearchShowEvent.newBuilder().build()

  val sampleQueryWithSorting =
    "category=cars&context=listing&geo_radius=200&page_size=1&page=1&rid=213&sort=autoru_shuffle-asc&state_group=ALL"

  "Search show event" should {
    "have proper component" in {
      val event = SearchShowEvent(emptyMessage)
      event.component shouldBe "search.show"
    }

    "handle empty message" in {
      val event = SearchShowEvent(emptyMessage)
      noException should be thrownBy event.props
    }
  }

  "should parse sorting from a query" in {
    val message = StatEvents.SearchShowEvent
      .newBuilder()
      .setQuery(sampleQueryWithSorting)
      .build()
    val event = SearchShowEvent(message)

    event.props.get("sorting_name").value shouldBe "autoru_shuffle"
    event.props.get("sorting_desc").value shouldBe "false"
  }
}
