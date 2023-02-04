#include <maps/wikimap/feedback/api/src/libs/sync_queue/action_params.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/printers.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::sync_queue::tests {

namespace {

json::Value paramsToJson(const ActionParams& params)
{
    const auto jsonString = (
        maps::json::Builder() << params
    ).str();
    return json::Value::fromString(jsonString);
}

}  // unnamed namespace


Y_UNIT_TEST_SUITE(test_params)
{

Y_UNIT_TEST(ugc_contribution_params_json)
{
    auto jsonValue = paramsToJson(UgcContributionParams());
    UNIT_ASSERT_VALUES_EQUAL(jsonValue, json::Value::fromString("{}"));
}

Y_UNIT_TEST(ugc_assignment_done_params_json)
{
    auto jsonValue = paramsToJson(UgcAssignmentDoneParams());
    UNIT_ASSERT_VALUES_EQUAL(jsonValue, json::Value::fromString("{}"));
}

Y_UNIT_TEST(create_tracker_issue_params_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "queueKey": "NMAPS",
        "component": 540033,
        "summary": "issue-summary",
        "description": "Message for CreateTrackerIssue"
    })--");
    CreateTrackerIssueParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL("NMAPS", params.queueKey);
    UNIT_ASSERT_VALUES_EQUAL(540033, params.component);
    UNIT_ASSERT_VALUES_EQUAL("issue-summary", params.summary);
    UNIT_ASSERT_VALUES_EQUAL("Message for CreateTrackerIssue", params.description);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

Y_UNIT_TEST(sprav_sync_redirected_params_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "message": "Message for SpravSyncRedirectedParams"
    })--");
    SpravSyncRedirectedParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL("Message for SpravSyncRedirectedParams", params.message);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

Y_UNIT_TEST(sprav_create_params_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "cookie": "cookie for SpravCreateParams",
        "user_agent": "user_agent for SpravCreateParams",
        "fingerprint": "fingerprint for SpravCreateParams"
    })--");
    SpravCreateParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL("cookie for SpravCreateParams", params.httpData.cookie);
    UNIT_ASSERT_VALUES_EQUAL("user_agent for SpravCreateParams", params.httpData.userAgent);
    UNIT_ASSERT_VALUES_EQUAL("fingerprint for SpravCreateParams", params.httpData.fingerprint);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));

    const auto jsonValueNoFingerprint = json::Value::fromString(R"--({
        "cookie": "cookie for SpravCreateParams",
        "user_agent": "user_agent for SpravCreateParams"
    })--");
    SpravCreateParams paramsNoFingerprint(jsonValueNoFingerprint);

    UNIT_ASSERT(paramsNoFingerprint.httpData.fingerprint.empty());
    UNIT_ASSERT_VALUES_EQUAL(jsonValueNoFingerprint, paramsToJson(paramsNoFingerprint));
}

Y_UNIT_TEST(samsara_close_ticket_params_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "ticket_id": "12345",
        "resolution": "RESOLVED"
    })--");
    SamsaraCloseTicketParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL(ServiceObjectId{"12345"},            params.ticketId);
    UNIT_ASSERT_VALUES_EQUAL(samsara::TicketResolution::Resolved, params.resolution);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

Y_UNIT_TEST(samsara_link_to_tracker_params_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "samsara_status": "HOLD",
        "samsara_resolution": "WAIT_FOR_SERVICE",
        "samsara_queue": "content2L",
        "message": "note message"
    })--");
    SamsaraLinkToTrackerParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL(samsara::TicketStatus::Hold,               params.samsaraStatus);
    UNIT_ASSERT_VALUES_EQUAL(samsara::TicketResolution::WaitForService, params.samsaraResolution);
    UNIT_ASSERT_VALUES_EQUAL("content2L",                               params.samsaraQueue.value());
    UNIT_ASSERT_VALUES_EQUAL("note message",                            params.message);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

Y_UNIT_TEST(samsara_update_ticket_params_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "samsara_queue": "NMAPS",
        "force_samsara_queue": true,
        "ticket_note_message": "note message",
        "keep_integration": true
    })--");
    SamsaraUpdateTicketParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL("NMAPS",        params.samsaraQueue.value());
    UNIT_ASSERT_VALUES_EQUAL(true,           params.forceSamsaraQueue);
    UNIT_ASSERT_VALUES_EQUAL("note message", params.ticketNoteMessage.value_or("empty"));
    UNIT_ASSERT_VALUES_EQUAL(true,           params.keepIntegration);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

Y_UNIT_TEST(samsara_add_note_params_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "ticket_id": "12345",
        "message": "note message"
    })--");
    SamsaraAddNoteParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL(ServiceObjectId{"12345"}, params.ticketId);
    UNIT_ASSERT_VALUES_EQUAL("note message",           params.message);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

Y_UNIT_TEST(sup_send_push_params_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "push_type": "toponym_rejected_prohibited_by_rules",
        "locale": "en_RU",
        "client_id": "desktop-maps",
        "receiver_uid": 220317986,
        "contribution_id": "fb:1a1da2cf-8c80-c2cf-1ac4-82e547a014c0"
    })--");
    SupSendPushParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL(PushType::ToponymRejectedProhibitedByRules, params.pushType);
    UNIT_ASSERT_VALUES_EQUAL("en_RU", params.locale);
    UNIT_ASSERT_VALUES_EQUAL("desktop-maps", params.clientId);
    UNIT_ASSERT_VALUES_EQUAL(220317986, params.receiverUid.value());
    UNIT_ASSERT_VALUES_EQUAL("fb:1a1da2cf-8c80-c2cf-1ac4-82e547a014c0", params.contributionId);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

Y_UNIT_TEST(sender_send_email_params_toponym_json)
{
    const double PRECISION = 0.000001;

    const auto jsonValue = json::Value::fromString(R"--({
        "mail_type": "toponym_published",
        "locale": "ru_RU",
        "email": "sample@yandex.ru",
        "form_point": {
           "lat": 59.98733,
           "lon": 30.299456
        },
        "comment": "Комментарий"
    })--");
    SenderSendEmailParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL(MailNotificationType::ToponymPublished, params.mailType);
    UNIT_ASSERT_VALUES_EQUAL("ru_RU", params.locale);
    UNIT_ASSERT_VALUES_EQUAL("sample@yandex.ru", params.email);

    UNIT_ASSERT(params.comment);
    UNIT_ASSERT(!params.routeEncodedPoints);

    UNIT_ASSERT_VALUES_EQUAL("Комментарий", *params.comment);
    UNIT_ASSERT(std::abs(params.formPoint.x() - 30.299456) < PRECISION);
    UNIT_ASSERT(std::abs(params.formPoint.y() - 59.98733) < PRECISION);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

Y_UNIT_TEST(sender_send_email_params_route_json)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "mail_type": "route_new",
        "locale": "en_EN",
        "email": "sample@yandex.ru",
        "form_point": {
           "lat": 59.98733,
           "lon": 30.299456
        },
        "route_encoded_points": "encoded_route"
    })--");
    SenderSendEmailParams params(jsonValue);

    UNIT_ASSERT_VALUES_EQUAL(MailNotificationType::RouteNew, params.mailType);
    UNIT_ASSERT_VALUES_EQUAL("en_EN", params.locale);
    UNIT_ASSERT_VALUES_EQUAL("sample@yandex.ru", params.email);

    UNIT_ASSERT(!params.comment);
    UNIT_ASSERT(params.routeEncodedPoints);

    UNIT_ASSERT_VALUES_EQUAL("encoded_route", *params.routeEncodedPoints);

    UNIT_ASSERT_VALUES_EQUAL(jsonValue, paramsToJson(params));
}

} // test_samsara suite

} // namespace maps::wiki::feedback::api::sync_queue::tests
