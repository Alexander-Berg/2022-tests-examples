#include "story_module.h"

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <library/cpp/testing/unittest/registar.h>

#include <sstream>

using namespace quasar;

namespace {
    constexpr size_t MAX_BLOCKS = 2;

    Json::Value makeStoryArray(size_t block_id, size_t size) {
        Json::Value result;
        for (size_t i = 0; i != size; ++i) {
            Json::Value story;
            story["sound"] = std::to_string(block_id) + '_' + std::to_string(i);
            result.append(std::move(story));
        }
        return result;
    }

    Json::Value makeTestStoryModuleConfig(int block_page, size_t block_size) {
        Json::Value result;
        for (size_t i = 0; i != MAX_BLOCKS; ++i) {
            Json::Value block;
            block["blockSize"] = block_page;
            block["stories"] = makeStoryArray(i, block_size);
            result.append(std::move(block));
        }
        return result;
    }
} // namespace

Y_UNIT_TEST_SUITE(StoryModuleTest) {

    Y_UNIT_TEST(TestStoryModule) {
        constexpr size_t BLOCK_SIZE = 3;
        constexpr size_t BLOCK_PAGE = 2;
        auto config = makeTestStoryModuleConfig(BLOCK_PAGE, BLOCK_SIZE);
        YIO_LOG_INFO(config);
        auto storyModule = StoryModule("some_cache_file", config);
        YIO_LOG_INFO(storyModule.toJson());
        UNIT_ASSERT_EQUAL(storyModule.toJson(), config);
        auto items = storyModule.getAllDemoItems();
        UNIT_ASSERT_EQUAL(items.size(), BLOCK_SIZE* MAX_BLOCKS);
        for (size_t i = 0; i != items.size(); ++i) {
            UNIT_ASSERT_EQUAL(items[i].sound.url, std::to_string(i / BLOCK_SIZE) + '_' + std::to_string(i % BLOCK_SIZE));
        }
        items = storyModule.getNextDemoItems();
        for (size_t i = 0; i != items.size(); ++i) {
            UNIT_ASSERT_EQUAL(items[i].sound.url, std::to_string(i / BLOCK_PAGE) + '_' + std::to_string(i % BLOCK_PAGE));
        }
        items = storyModule.getNextDemoItems();
        for (size_t i = 0; i != items.size(); ++i) {
            UNIT_ASSERT_EQUAL(items[i].sound.url, std::to_string(i / BLOCK_PAGE) + '_' + std::to_string((BLOCK_PAGE + (i % BLOCK_PAGE)) % BLOCK_SIZE));
        }
    }

    Y_UNIT_TEST(TestUpdateModule) {
        constexpr size_t BLOCK_SIZE = 3;
        constexpr size_t BLOCK_PAGE = 2;
        auto config = makeTestStoryModuleConfig(BLOCK_PAGE, BLOCK_SIZE);
        auto storyModule = StoryModule("some_cache_file", Json::Value());
        UNIT_ASSERT(storyModule.getAllDemoItems().empty());
        storyModule.updateDemoItems(config);
        UNIT_ASSERT_EQUAL(storyModule.getAllDemoItems().size(), BLOCK_SIZE* MAX_BLOCKS);
        UNIT_ASSERT_EQUAL(storyModule.toJson(), config);
    }

};
