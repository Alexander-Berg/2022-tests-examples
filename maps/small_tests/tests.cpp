#include <maps/wikimap/feedback/api/src/samsara_importer/lib/ticket_data.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::samsara_importer::tests {

namespace {

const samsara::Queues DEFAULT_QUEUES {};

}  // unnamed namespace

Y_UNIT_TEST_SUITE(tests)
{

Y_UNIT_TEST(ticket_to_text)
{
    auto samsaraTicket = samsara::Ticket::fromJson(
        json::Value::fromFile(SRC_("data/samsara_ticket_with_articles.json")),
        DEFAULT_QUEUES);

    auto ticketText = TicketData{samsaraTicket}.feedbackText();

    auto expected = maps::common::readFileToString(SRC_("data/other_text1.txt"));
    UNIT_ASSERT_VALUES_EQUAL(expected, ticketText);
}

Y_UNIT_TEST(location_geo_from_ticket)
{
    {
        auto samsaraTicket = samsara::Ticket::fromJson(
            json::Value::fromFile(SRC_("data/samsara_ticket_with_articles.json")),
            DEFAULT_QUEUES);

        auto location = TicketData{samsaraTicket}.locationGeo();

        UNIT_ASSERT(location);
        UNIT_ASSERT_DOUBLES_EQUAL(location->x(), 30.55136123885173, 0.000001);
        UNIT_ASSERT_DOUBLES_EQUAL(location->y(), 60.14745921752886, 0.000001);
    }

    {
        auto samsaraTicket = samsara::Ticket::fromJson(
            json::Value::fromFile(SRC_("data/multi_location_in_middle.json")),
            DEFAULT_QUEUES);

        auto location = TicketData{samsaraTicket}.locationGeo();

        UNIT_ASSERT(location);
        UNIT_ASSERT_DOUBLES_EQUAL(location->x(), 30.55136123885173, 0.000001);
        UNIT_ASSERT_DOUBLES_EQUAL(location->y(), 60.14745921752886, 0.000001);
    }
}

Y_UNIT_TEST(is_confidential)
{
    UNIT_ASSERT(internal::isConfidential("email 123"));
    UNIT_ASSERT(internal::isConfidential("email: 123"));
    UNIT_ASSERT(internal::isConfidential("Email: 123"));

    UNIT_ASSERT(internal::isConfidential("email 123"));
    UNIT_ASSERT(internal::isConfidential("email: 123"));
    UNIT_ASSERT(internal::isConfidential("Email: 123"));

    UNIT_ASSERT(internal::isConfidential("LOGIN: yndx-cartographer-asessors-11"));
    UNIT_ASSERT(internal::isConfidential("почта: yndx-cartographer-asessors-13@yandex.ru"));
    UNIT_ASSERT(internal::isConfidential("ЛОГИН: yndx-cartographer-asessors-11"));
}

Y_UNIT_TEST(censor_confidential_lines)
{
    const std::string lines =
        "Логин: yndx-cartographer-asessors-11\n"
        "логин: yndx-cartographer-asessors-12\n"
        "ЛОГИН: yndx-cartographer-asessors-13\n"
        "text we do not want to remove1\n"
        "Имя: Просто Саша1\n"
        "имя: Просто Саша2\n"
        "ИМЯ: Просто Саша3\n"
        "text we do not want to remove2\n"
        "Почта: yndx-cartographer-asessors-11@yandex.ru\n"
        "ПОЧТА: yndx-cartographer-asessors-12@yandex.ru\n"
        "почта: yndx-cartographer-asessors-13@yandex.ru\n"
        "почта: \n"
        "yndx-cartographer-asessors-14@yandex.ru\n";
    UNIT_ASSERT_VALUES_EQUAL(
        internal::censorArticleBodyConfidentialLines(lines),
        "text we do not want to remove1\n"
        "text we do not want to remove2\n"
    );
}

} // tests suite

} // namespace maps::wiki::feedback::api::tests
