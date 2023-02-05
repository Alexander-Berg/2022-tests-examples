#include "helpers.h"
#include <yandex/maps/wiki/diffalert/storage/results_viewer.h>
#include <yandex/maps/wiki/diffalert/storage/results_writer.h>
#include <yandex/maps/wiki/diffalert/storage/stored_message.h>

#include <maps/libs/introspection/include/comparison.h>

#include <algorithm>
#include <iostream>
#include <fstream>
#include <unordered_map>

namespace maps {
namespace wiki {
namespace diffalert {

auto introspect(const MessageStatisticsItem& item)
{
    return std::tie(item.majorPriority, item.categoryId, item.description, item.totalCount, item.inspectedCount);
}

namespace tests {

namespace {

const TaskId TEST_TASK_ID = 123;

const TUId TEST_UID_1 = 7777777;
const TUId TEST_UID_2 = 8888888;

const TId TEST_OBJECT_ID_1 = 1;
const TId TEST_OBJECT_ID_2 = 2;
const TId TEST_OBJECT_ID_3 = 3;

const std::string EMPTY_DESCRIPTION;
const std::string EMPTY_CATEGORY_ID;

} // namespace

Y_UNIT_TEST_SUITE_F(storage, SetLogLevelFixture) {

Y_UNIT_TEST(empty_result_view)
{
    ResultsDB::clear();

    auto txn = ResultsDB::pool().masterReadOnlyTransaction();
    ResultsViewer viewer(TEST_TASK_ID, *txn);
    UNIT_ASSERT_VALUES_EQUAL(viewer.taskId(), TEST_TASK_ID);
    UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount({}), 0);
    UNIT_ASSERT(viewer.messages({}, SortKind::BySize, 0, 10).empty());
}

Y_UNIT_TEST(empty_result_write)
{
    ResultsDB::clear();

    ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
    UNIT_ASSERT_VALUES_EQUAL(writer.taskId(), TEST_TASK_ID);
    // the writer isn't finished intentionally
}

Y_UNIT_TEST(smoke_test)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        UNIT_ASSERT_VALUES_EQUAL(writer.taskId(), TEST_TASK_ID);
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 1}, "test-description", Message::Scope::WholeObject),
            "test-category", "russian-name", HasOwnName::Yes, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }
    {
        auto txn = ResultsDB::pool().masterReadOnlyTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);
        UNIT_ASSERT_VALUES_EQUAL(viewer.taskId(), TEST_TASK_ID);
        UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount({}), 1);
        UNIT_ASSERT(viewer.messages({}, SortKind::BySize, 0, 0).empty());

        auto messages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount({}));
        UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 1);
        UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().major, 0);
        UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().minor, 1);
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].description(), "test-description");
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].categoryId(), "test-category");
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].objectLabel(), "russian-name");
        UNIT_ASSERT_VALUES_EQUAL(messages[0].postponed(), false);

        auto message = viewer.message(messages[0].id());
        UNIT_ASSERT(message.has_value());
        UNIT_ASSERT_VALUES_EQUAL(message->objectId(), 1);
        UNIT_ASSERT_VALUES_EQUAL(message->priority().major, 0);
        UNIT_ASSERT_VALUES_EQUAL(message->priority().minor, 1);
        UNIT_ASSERT_STRINGS_EQUAL(message->description(), "test-description");
        UNIT_ASSERT_STRINGS_EQUAL(message->categoryId(), "test-category");
        UNIT_ASSERT_STRINGS_EQUAL(message->objectLabel(), "russian-name");
        UNIT_ASSERT_VALUES_EQUAL(message->postponed(), false);
    }
}

Y_UNIT_TEST(message_ordering_test)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        UNIT_ASSERT_VALUES_EQUAL(writer.taskId(), TEST_TASK_ID);
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{3, 2}, "", Message::Scope::WholeObject),
            "", "", HasOwnName::No, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_2, Priority{0, 0}, "", Message::Scope::WholeObject),
            "", "", HasOwnName::No, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 1, 1.5}, "", Message::Scope::WholeObject),
            "", "", HasOwnName::No, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 1, 0.5}, "", Message::Scope::WholeObject),
            "", "", HasOwnName::No, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }
    {
        auto txn = ResultsDB::pool().masterReadOnlyTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);
        UNIT_ASSERT_VALUES_EQUAL(viewer.taskId(), TEST_TASK_ID);
        UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount({}), 4);

        auto messages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount({}));
        UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().major, 0);
        UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().minor, 0);

        UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().major, 0);
        UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().minor, 1);
        UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().sort, 0.5);

        UNIT_ASSERT_VALUES_EQUAL(messages[2].priority().major, 0);
        UNIT_ASSERT_VALUES_EQUAL(messages[2].priority().minor, 1);
        UNIT_ASSERT_VALUES_EQUAL(messages[2].priority().sort, 1.5);

        UNIT_ASSERT_VALUES_EQUAL(messages[3].priority().major, 3);
        UNIT_ASSERT_VALUES_EQUAL(messages[3].priority().minor, 2);
    }
}

Y_UNIT_TEST(message_ordering_by_name_test)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 0}, EMPTY_DESCRIPTION, Message::Scope::WholeObject),
            EMPTY_CATEGORY_ID, "Будапешт", HasOwnName::Yes, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0,0}, EMPTY_DESCRIPTION, Message::Scope::WholeObject),
            EMPTY_CATEGORY_ID, "виноградник", HasOwnName::No, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_2, Priority{0, 0}, EMPTY_DESCRIPTION, Message::Scope::WholeObject),
            EMPTY_CATEGORY_ID, "Александрия", HasOwnName::Yes, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 0}, EMPTY_DESCRIPTION, Message::Scope::WholeObject),
            EMPTY_CATEGORY_ID, "Варшава", HasOwnName::Yes, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0,0}, EMPTY_DESCRIPTION, Message::Scope::WholeObject),
            EMPTY_CATEGORY_ID, "акрополь", HasOwnName::No, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0,0}, EMPTY_DESCRIPTION, Message::Scope::WholeObject),
            EMPTY_CATEGORY_ID, "бульвар", HasOwnName::No, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }
    {
        auto txn = ResultsDB::pool().masterReadOnlyTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);

        UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount({}), 6);

        auto messages = viewer.messages({}, SortKind::ByName, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount({}));
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].objectLabel(), "Александрия");
        UNIT_ASSERT_STRINGS_EQUAL(messages[1].objectLabel(), "Будапешт");
        UNIT_ASSERT_STRINGS_EQUAL(messages[2].objectLabel(), "Варшава");
        UNIT_ASSERT_STRINGS_EQUAL(messages[3].objectLabel(), "акрополь");
        UNIT_ASSERT_STRINGS_EQUAL(messages[4].objectLabel(), "бульвар");
        UNIT_ASSERT_STRINGS_EQUAL(messages[5].objectLabel(), "виноградник");
    }
}

Y_UNIT_TEST(isolation_test)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 0}, "first-task", Message::Scope::WholeObject),
            "cat_a", "name-ru_a", HasOwnName::Yes, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }
    {
        ResultsWriter writer(TEST_TASK_ID + 1, ResultsDB::pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_2, Priority{2, 1}, "second-task", Message::Scope::WholeObject),
            "cat_b", "name-ru_b", HasOwnName::Yes, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 0}, "second-task", Message::Scope::WholeObject),
            "cat_a", "name-ru_a", HasOwnName::Yes, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }

    {
        auto txn = ResultsDB::pool().masterReadOnlyTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);
        UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount({}), 1);

        auto messages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount({}));
        UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 1);
        UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().minor, 0);
        UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().major, 0);
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].description(), "first-task");
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].categoryId(), "cat_a");
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].objectLabel(), "name-ru_a");
    }
    {
        auto txn = ResultsDB::pool().masterReadOnlyTransaction();
        ResultsViewer viewer(TEST_TASK_ID + 1, *txn);
        UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount({}), 2);

        auto messages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount({}));
        UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 1);
        UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().major, 0);
        UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().minor, 0);
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].description(), "second-task");
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].categoryId(), "cat_a");
        UNIT_ASSERT_STRINGS_EQUAL(messages[0].objectLabel(), "name-ru_a");

        UNIT_ASSERT_VALUES_EQUAL(messages[1].objectId(), 2);
        UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().major, 2);
        UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().minor, 1);
        UNIT_ASSERT_STRINGS_EQUAL(messages[1].description(), "second-task");
        UNIT_ASSERT_STRINGS_EQUAL(messages[1].categoryId(), "cat_b");
        UNIT_ASSERT_STRINGS_EQUAL(messages[1].objectLabel(), "name-ru_b");
    }
}


const Envelope ENVELOPE1(4184890.32, 4186229.65, 7503174.27, 7504368.11);
const Envelope ENVELOPE2(4165209.99, 4165713.27, 7462563.93, 7463255.95);

// intersects with envelope2
const std::string FILTER_GEOM_WKB = common::wkt2wkb(
        "POLYGON(("
        "4165209.99491551 7462563.92695491,"
        "4165209.99491551 7463255.9500668,"
        "4165713.27033339 7463255.9500668,"
        "4165713.27033339 7462563.92695491,"
        "4165209.99491551 7462563.92695491))");

Y_UNIT_TEST(filters_test)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 0}, "junction-high", Message::Scope::WholeObject),
            "rd_jc", "name-ru", HasOwnName::Yes, ENVELOPE1);
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{1, 1}, "junction-mid", Message::Scope::WholeObject),
            "rd_jc", "name-ru", HasOwnName::Yes, ENVELOPE1);
        data.emplace_back(
            Message(TEST_OBJECT_ID_2, Priority{1, 0}, "center-mid", Message::Scope::WholeObject),
            "ad_cnt", "name-ru", HasOwnName::Yes, ENVELOPE2);
        data.emplace_back(
            Message(TEST_OBJECT_ID_2, Priority{2, 0}, "center-low", Message::Scope::WholeObject),
            "ad_cnt", "name-ru", HasOwnName::Yes, ENVELOPE2);
        writer.put(std::move(data));
        writer.finish();
    }
    {
        auto txn = ResultsDB::pool().masterWriteableTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);

        StoredMessagesFilter filter;
        filter.categoryIds.emplace_back("rd_jc");
        auto messages = viewer.messages({filter}, SortKind::BySize, 0, 10);
        StoredMessage::markAsInspected(txn.get(), {messages[0].id()}, TEST_UID_1);

        filter.categoryIds.clear();
        filter.categoryIds.emplace_back("ad_cnt");
        messages = viewer.messages({filter}, SortKind::BySize, 0, 10);
        StoredMessage::markAsInspected(txn.get(), {messages[0].id()}, TEST_UID_2);

        txn->commit();
    }

    {
        auto txn = ResultsDB::pool().masterReadOnlyTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);
        UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount({}), 4);

        { // By priority
            StoredMessagesFilter filter;
            filter.majorPriority = 1;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 2);
            auto messages = viewer.messages(filter, SortKind::BySize, 0, 10);
            UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount(filter));
            UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 2);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().major, 1);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().minor, 0);
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].description(), "center-mid");
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].categoryId(), "ad_cnt");
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].objectLabel(), "name-ru");

            UNIT_ASSERT_VALUES_EQUAL(messages[1].objectId(), 1);
            UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().major, 1);
            UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().minor, 1);
            UNIT_ASSERT_STRINGS_EQUAL(messages[1].description(), "junction-mid");
            UNIT_ASSERT_STRINGS_EQUAL(messages[1].categoryId(), "rd_jc");
            UNIT_ASSERT_STRINGS_EQUAL(messages[1].objectLabel(), "name-ru");
        }
        { // By category
            StoredMessagesFilter filter;
            filter.categoryIds.emplace_back("ad_cnt");
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 2);
            auto messages = viewer.messages(filter, SortKind::BySize, 0, 10);
            UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount(filter));
            UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 2);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().major, 1);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().minor, 0);
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].description(), "center-mid");
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].categoryId(), "ad_cnt");
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].objectLabel(), "name-ru");

            UNIT_ASSERT_VALUES_EQUAL(messages[1].objectId(), 2);
            UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().major, 2);
            UNIT_ASSERT_VALUES_EQUAL(messages[1].priority().minor, 0);
            UNIT_ASSERT_STRINGS_EQUAL(messages[1].description(), "center-low");
            UNIT_ASSERT_STRINGS_EQUAL(messages[1].categoryId(), "ad_cnt");
            UNIT_ASSERT_STRINGS_EQUAL(messages[1].objectLabel(), "name-ru");
        }
        { // By multiple categoryIds
            StoredMessagesFilter filter;
            filter.categoryIds.emplace_back("rd_jc");
            filter.categoryIds.emplace_back("ad_cnt");
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 4);
            UNIT_ASSERT_VALUES_EQUAL(
                viewer.messages(filter, SortKind::BySize, 0, 10).size(),
                viewer.messageCount(filter));
        }
        { // By category + priority
            StoredMessagesFilter filter;
            filter.categoryIds.emplace_back("rd_jc");
            filter.majorPriority = 1;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 1);
            auto messages = viewer.messages(filter, SortKind::BySize, 0, 10);
            UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount(filter));
            UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 1);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().minor, 1);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().major, 1);
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].description(), "junction-mid");
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].categoryId(), "rd_jc");
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].objectLabel(), "name-ru");
        }
        { // By description
            StoredMessagesFilter filter;
            filter.description = "junction-high";
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 1);
            auto messages = viewer.messages(filter, SortKind::BySize, 0, 10);
            UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount(filter));
            UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 1);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().minor, 0);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].priority().major, 0);
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].description(), "junction-high");
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].categoryId(), "rd_jc");
            UNIT_ASSERT_STRINGS_EQUAL(messages[0].objectLabel(), "name-ru");
        }
        { // By geometry
            StoredMessagesFilter filter;
            filter.geomWkb = FILTER_GEOM_WKB;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 2);
            auto messages = viewer.messages(filter, SortKind::BySize, 0, 10);
            UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount(filter));
            UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 2);
            UNIT_ASSERT_VALUES_EQUAL(messages[1].objectId(), 2);
        }
        {  // By postponed false
            StoredMessagesFilter filter;
            filter.postponed = false;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 4);
            auto messages = viewer.messages(filter, SortKind::BySize, 0, 10);
            UNIT_ASSERT_VALUES_EQUAL(messages.size(), viewer.messageCount(filter));
        }
        {  // By postponed true
            StoredMessagesFilter filter;
            filter.postponed = true;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 0);
            UNIT_ASSERT(viewer.messages(filter, SortKind::BySize, 0, 10).empty());
        }
        {  // By inspected user
            StoredMessagesFilter filter;
            filter.excludeInspectedBy = TEST_UID_1;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 2);
            auto messages = viewer.messages(filter, SortKind::BySize, 0, 10);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 2);
            UNIT_ASSERT_VALUES_EQUAL(messages[1].objectId(), 2);

            filter.excludeInspectedBy = TEST_UID_2;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 2);
            messages = viewer.messages(filter, SortKind::BySize, 0, 10);
            UNIT_ASSERT_VALUES_EQUAL(messages[0].objectId(), 1);
            UNIT_ASSERT_VALUES_EQUAL(messages[1].objectId(), 1);
        }
        {  // Nonexistent priority
            StoredMessagesFilter filter;
            filter.majorPriority = -1;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 0);
            UNIT_ASSERT(viewer.messages(filter, SortKind::BySize, 0, 10).empty());
        }
        { // Nonexistent category
            StoredMessagesFilter filter;
            filter.categoryIds.emplace_back("nonexistent");
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 0);
            UNIT_ASSERT(viewer.messages(filter, SortKind::BySize, 0, 10).empty());
        }
        { // Nonexistent description
            StoredMessagesFilter filter;
            filter.description = "nonexistent";
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 0);
            UNIT_ASSERT(viewer.messages(filter, SortKind::BySize, 0, 10).empty());
        }
        { // Filter invalidation
            StoredMessagesFilter filter;
            filter.majorPriority = 1;
            filter.categoryIds.emplace_back("rd_jc");
            filter.description = "junction-mid";
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 1);

            filter.majorPriority = 2;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 0);
            filter.majorPriority = 1;
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 1);

            filter.categoryIds[0] = "ad_cnt";
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 0);
            filter.categoryIds[0] = "rd_jc";
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 1);

            filter.description = "center-mid";
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 0);
            filter.description = "junction-mid";
            UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount(filter), 1);
        }
    }
}

Y_UNIT_TEST(stats_test)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 0}, "junction-high", Message::Scope::WholeObject),
            "rd_jc", "name-ru", HasOwnName::Yes, ENVELOPE1);
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{1, 1}, "junction-mid", Message::Scope::WholeObject),
            "rd_jc", "name-ru", HasOwnName::Yes, ENVELOPE1);
        data.emplace_back(
            Message(TEST_OBJECT_ID_2, Priority{1, 0}, "center-mid", Message::Scope::WholeObject),
            "ad_cnt", "name-ru", HasOwnName::Yes, ENVELOPE2);
        data.emplace_back(
            Message(TEST_OBJECT_ID_2, Priority{2, 0}, "center-low", Message::Scope::WholeObject),
            "ad_cnt", "name-ru", HasOwnName::Yes, ENVELOPE2);
        data.emplace_back(
            Message(TEST_OBJECT_ID_2, Priority{2, 1}, "center-low", Message::Scope::WholeObject),
            "ad_cnt", "name-ru", HasOwnName::Yes, ENVELOPE2);
        data.emplace_back(
            Message(TEST_OBJECT_ID_3, Priority{0, 0}, "junction-high", Message::Scope::WholeObject),
            "rd_jc", "name-ru", HasOwnName::Yes, ENVELOPE1);
        writer.put(std::move(data));
        writer.finish();
    }

    {
        auto txn = ResultsDB::pool().masterReadOnlyTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);
        UNIT_ASSERT_VALUES_EQUAL(viewer.messageCount({}), 6);

        const std::vector<MessageStatisticsItem> expected = {
            {3, 0, "rd_jc", "junction-high", 2, 0},
            {3, 1, "ad_cnt", "center-mid", 1, 0},
            {3, 1, "rd_jc", "junction-mid", 1, 0},
            {3, 2, "ad_cnt", "center-low", 2, 0},
        };

#define COMPARE_COLLECTIONS(it1, it2, it3, it4)              \
            std::vector<MessageStatisticsItem> v1(it1, it2); \
            std::vector<MessageStatisticsItem> v2(it3, it4); \
            UNIT_ASSERT_EQUAL(v1, v2);                       \

        { // Full
            auto stats = viewer.statistics({});
            std::sort(stats.begin(), stats.end());
            UNIT_ASSERT_EQUAL(stats, expected);
        }
        { // By priority
            StoredMessagesFilter filter;
            filter.majorPriority = 1;
            auto stats = viewer.statistics(filter);
            std::sort(stats.begin(), stats.end());
            COMPARE_COLLECTIONS(
                stats.cbegin(), stats.cend(),
                expected.cbegin() + 1, expected.cbegin() + 3);
        }
        { // By category
            StoredMessagesFilter filter;
            filter.categoryIds.emplace_back("ad_cnt");
            auto stats = viewer.statistics(filter);
            std::sort(stats.begin(), stats.end());
            decltype(expected) adExpected = {expected[1], expected[3]};
            UNIT_ASSERT_EQUAL(stats, adExpected);
        }
        { // By multiple categoryIds
            StoredMessagesFilter filter;
            filter.categoryIds.emplace_back("rd_jc");
            filter.categoryIds.emplace_back("ad_cnt");
            auto stats = viewer.statistics(filter);
            std::sort(stats.begin(), stats.end());
            UNIT_ASSERT_EQUAL(stats, expected);
        }
        { // By category + priority
            StoredMessagesFilter filter;
            filter.categoryIds.emplace_back("rd_jc");
            filter.majorPriority = 1;
            auto stats = viewer.statistics(filter);
            std::sort(stats.begin(), stats.end());
            COMPARE_COLLECTIONS(
                stats.cbegin(), stats.cend(),
                expected.cbegin() + 2, expected.cbegin() + 3);
        }
        { // By description
            StoredMessagesFilter filter;
            filter.description = "junction-high";
            auto stats = viewer.statistics(filter);
            std::sort(stats.begin(), stats.end());
            COMPARE_COLLECTIONS(
                stats.cbegin(), stats.cend(),
                expected.cbegin(), expected.cbegin() + 1);
        }
        { // By geometry
            StoredMessagesFilter filter;
            filter.geomWkb = FILTER_GEOM_WKB;
            auto stats = viewer.statistics(filter);
            std::sort(stats.begin(), stats.end());
            const std::vector<MessageStatisticsItem> expected = {
                {3, 1, "ad_cnt", "center-mid", 1, 0},
                {3, 2, "ad_cnt", "center-low", 2, 0},
            };
            UNIT_ASSERT_EQUAL(stats, expected);
        }
        { // By postponed false
            StoredMessagesFilter filter;
            filter.postponed = false;
            auto stats = viewer.statistics(filter);
            std::sort(stats.begin(), stats.end());
            UNIT_ASSERT_EQUAL(stats, expected);
        }
        { // By postponed true
            StoredMessagesFilter filter;
            filter.postponed = true;
            UNIT_ASSERT(viewer.statistics(filter).empty());
        }
        {  // Nonexistent priority
            StoredMessagesFilter filter;
            filter.majorPriority = -1;
            UNIT_ASSERT(viewer.statistics(filter).empty());
        }
        { // Nonexistent category
            StoredMessagesFilter filter;
            filter.categoryIds.emplace_back("nonexistent");
            UNIT_ASSERT(viewer.statistics(filter).empty());
        }
        { // Nonexistent description
            StoredMessagesFilter filter;
            filter.description = "nonexistent";
            UNIT_ASSERT(viewer.statistics(filter).empty());
        }
    }
}

Y_UNIT_TEST(test_inspected_mark)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 1}, "test-description", Message::Scope::WholeObject),
            "test-category", "name-ru", HasOwnName::Yes, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }

    {
        auto txn = ResultsDB::pool().masterWriteableTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);

        auto messages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);

        UNIT_ASSERT_VALUES_EQUAL(messages[0].inspectedBy(), 0);
        UNIT_ASSERT(messages[0].inspectedAt().empty());

        StoredMessageId nonexistentId = messages[0].id() + 1;
        UNIT_ASSERT(StoredMessage::markAsInspected(
                            txn.get(), {nonexistentId}, TEST_UID_1).empty());

        auto markedMessages = StoredMessage::markAsInspected(
                txn.get(), {messages[0].id()}, TEST_UID_1);
        UNIT_ASSERT_VALUES_EQUAL(markedMessages.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(markedMessages[0].id(), messages[0].id());
        UNIT_ASSERT_VALUES_EQUAL(markedMessages[0].inspectedBy(), TEST_UID_1);
        UNIT_ASSERT(!markedMessages[0].inspectedAt().empty());

        auto loadedAgainMessages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages[0].id(), messages[0].id());
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages[0].inspectedBy(), TEST_UID_1);
        UNIT_ASSERT(!loadedAgainMessages[0].inspectedAt().empty());
    }
}

Y_UNIT_TEST(test_inspected_mark_same_object)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 1}, "test-description", Message::Scope::WholeObject),
            "test-category", "name-ru", HasOwnName::Yes, Envelope());
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 1}, "test-description-another", Message::Scope::WholeObject),
            "test-category", "name-ru", HasOwnName::Yes, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }

    {
        auto txn = ResultsDB::pool().masterWriteableTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);

        auto messages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), 2);

        UNIT_ASSERT_VALUES_EQUAL(messages[0].inspectedBy(), 0);
        UNIT_ASSERT(messages[0].inspectedAt().empty());
        UNIT_ASSERT_VALUES_EQUAL(messages[1].inspectedBy(), 0);
        UNIT_ASSERT(messages[1].inspectedAt().empty());

        auto markedMessages = StoredMessage::markAsInspected(
                txn.get(), {messages[0].id()}, TEST_UID_1);
        UNIT_ASSERT_VALUES_EQUAL(markedMessages.size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(markedMessages[0].inspectedBy(), TEST_UID_1);
        UNIT_ASSERT(!markedMessages[0].inspectedAt().empty());
        UNIT_ASSERT_VALUES_EQUAL(markedMessages[1].inspectedBy(), TEST_UID_1);
        UNIT_ASSERT(!markedMessages[1].inspectedAt().empty());

        auto loadedAgainMessages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages.size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages[0].inspectedBy(), TEST_UID_1);
        UNIT_ASSERT(!loadedAgainMessages[0].inspectedAt().empty());
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages[1].inspectedBy(), TEST_UID_1);
        UNIT_ASSERT(!loadedAgainMessages[1].inspectedAt().empty());
    }
}

Y_UNIT_TEST(test_postponed)
{
    ResultsDB::clear();

    {
        ResultsWriter writer(TEST_TASK_ID, ResultsDB::pool());
        std::list<StoredMessage> data;
        data.emplace_back(
            Message(TEST_OBJECT_ID_1, Priority{0, 1}, "test-description", Message::Scope::WholeObject),
            "test-category", "name-ru", HasOwnName::Yes, Envelope());
        writer.put(std::move(data));
        writer.finish();
    }

    {
        auto txn = ResultsDB::pool().masterWriteableTransaction();
        ResultsViewer viewer(TEST_TASK_ID, *txn);

        auto messages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(messages.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(messages[0].postponed(), false);

        StoredMessageId nonexistentId = messages[0].id() + 1;
        UNIT_CHECK_GENERATED_EXCEPTION(
            StoredMessage::postpone(txn.get(), nonexistentId, PostponeAction::Postpone),
            maps::LogicError);

        auto postponedMessage = StoredMessage::postpone(
                txn.get(), messages[0].id(), PostponeAction::Postpone);
        UNIT_ASSERT_VALUES_EQUAL(postponedMessage.id(), messages[0].id());
        UNIT_ASSERT_VALUES_EQUAL(postponedMessage.postponed(), true);

        auto loadedAgainMessages = viewer.messages({}, SortKind::BySize, 0, 10);
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages[0].id(), messages[0].id());
        UNIT_ASSERT_VALUES_EQUAL(loadedAgainMessages[0].postponed(), true);

        auto returnedMessage = StoredMessage::postpone(
                txn.get(), messages[0].id(), PostponeAction::Return);
        UNIT_ASSERT_VALUES_EQUAL(returnedMessage.id(), messages[0].id());
        UNIT_ASSERT_VALUES_EQUAL(returnedMessage.postponed(), false);
    }
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
