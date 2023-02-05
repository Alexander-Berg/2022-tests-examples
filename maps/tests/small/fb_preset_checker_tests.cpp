#include <library/cpp/testing/unittest/registar.h>
#include <maps/wikimap/mapspro/libs/acl_utils/include/feedback_preset_checker.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/task.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/task_filter.h>
#include <maps/wikimap/mapspro/libs/social/tests/helpers/fb_task_factory.h>

namespace maps::wiki::acl_utils::tests {

namespace sft = social::feedback::tests;

Y_UNIT_TEST_SUITE(fb_presets_checker_tests) {

Y_UNIT_TEST(test_match)
{
    {
        auto task = sft::FbTaskFactory().type(sf::Type::Subway).source("fbapi").create();
        sf::PresetEntries entriesMatch{
            .types = {{sf::Type::Address, sf::Type::Subway}}
        };
        sf::PresetEntries entriesNotMatch{
            .types = {{sf::Type::Address}}
        };
        UNIT_ASSERT(internal::match(task, entriesMatch));
        UNIT_ASSERT(!internal::match(task, entriesNotMatch));
    }

    {
        auto task = sft::FbTaskFactory().type(sf::Type::Subway).source("fbapi").create();
        sf::PresetEntries entriesMatch{
            .sources = {{"fbapi", "fbapi-samsara"}}
        };
        sf::PresetEntries entriesNotMatch{
            .sources = {{"fbapi-samsara"}}
        };
        UNIT_ASSERT(internal::match(task, entriesMatch));
        UNIT_ASSERT(!internal::match(task, entriesNotMatch));
    }

    {
        auto task = sft::FbTaskFactory().type(sf::Type::Subway)
            .source("fbapi").create();
        sf::PresetEntries entriesMatch{
            .workflows = {{sf::Workflow::Feedback, sf::Workflow::Task}}
        };
        sf::PresetEntries entriesNotMatch{
            .workflows = {{sf::Workflow::Task}}
        };
        UNIT_ASSERT(internal::match(task, entriesMatch));
        UNIT_ASSERT(!internal::match(task, entriesNotMatch));
    }
}

}

} // namespace maps::wiki::acl_utils::tests
