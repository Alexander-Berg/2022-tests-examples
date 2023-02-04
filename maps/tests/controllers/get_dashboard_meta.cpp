#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>

#include <algorithm>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_dashboard_meta)
{
WIKI_FIXTURE_TEST_CASE(should_get_dashboard_meta, EditorTestFixture)
{
    auto result = performAndValidateJsonGetRequest<SocialModerationDashboardMeta>(TESTS_USER);

    // Category groups
    auto catGroups = result["categoryGroups"];
    WIKI_TEST_REQUIRE(!catGroups.empty());
    WIKI_TEST_REQUIRE(catGroups.isArray());
    for (auto& catGroup: catGroups) {
        UNIT_ASSERT(catGroup.as<std::string>().ends_with("_group"));
        UNIT_ASSERT_VALUES_UNEQUAL(catGroup.as<std::string>(), CATEGORY_GROUP_SERVICE);
    }

    // Event types
    auto eventTypes = result["eventTypes"];
    WIKI_TEST_REQUIRE(!eventTypes.empty());
    WIKI_TEST_REQUIRE(eventTypes.isArray());
    UNIT_ASSERT_EQUAL(eventTypes.size(), enum_io::enumerateValues<social::EventType>().size() - 1); // all except ClosedFeedback
    for (auto& eventType: eventTypes) {
        UNIT_ASSERT(enum_io::tryFromString<social::EventType>(eventType.as<std::string>()));
        UNIT_ASSERT_UNEQUAL(
            enum_io::fromString<social::EventType>(eventType.as<std::string>()),
            social::EventType::ClosedFeedback);
    }
}
} //Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
