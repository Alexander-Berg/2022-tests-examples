#include <yandex/maps/wiki/http/fbapi/gateway.h>
#include <yandex/maps/wiki/http/fbapi/exception.h>
#include <maps/libs/geolib/include/conversion.h>
#include <boost/test/unit_test.hpp>

namespace maps::wiki::tests {

BOOST_AUTO_TEST_CASE(test_parse_parking_opt)
{
    auto value = json::Value::fromFile("fbapi_parking_original_task_opt.json");
    fbapi::OriginalTask originalTask(value);
    auto answer = originalTask.answerContext()->as<std::optional<fbapi::Parking>>();

    BOOST_CHECK(answer);
    BOOST_CHECK_EQUAL(answer->entrances().size(), 0);
}

BOOST_AUTO_TEST_CASE(test_parse_parking)
{
    auto value = json::Value::fromFile("fbapi_parking_original_task.json");
    fbapi::OriginalTask originalTask(value);
    auto answer = originalTask.answerContext()->as<std::optional<fbapi::Parking>>();

    BOOST_CHECK(answer);
    {
        BOOST_CHECK(answer->center());
        const auto center = answer->center().value();
        BOOST_CHECK_CLOSE(center.x(), 4172975, 0.001);
        BOOST_CHECK_CLOSE(center.y(), 7470966, 0.001);
    }
    {
        const auto entrances = answer->entrances();
        BOOST_CHECK_EQUAL(entrances.size(), 1);
        BOOST_CHECK_CLOSE(entrances[0].x(), 4284295, 0.001);
        BOOST_CHECK_CLOSE(entrances[0].y(), 7670867, 0.001);
    }
}

BOOST_AUTO_TEST_CASE(test_parse_route_opt)
{
    auto value = json::Value::fromFile("fbapi_route_original_task_opt.json");
    fbapi::OriginalTask originalTask(value);
    auto question = originalTask.questionContext()->as<fbapi::RouteQuestionContext>();
    auto answer = originalTask.answerContext()->as<fbapi::RouteAnswerContext>();

    BOOST_CHECK_EQUAL(question.viaPoints().size(), 0);
    BOOST_CHECK_EQUAL(answer.errors().size(), 0);
    BOOST_CHECK_EQUAL(answer.segments().size(), 0);
}

BOOST_AUTO_TEST_CASE(test_parse_route_answer)
{
    auto value = json::Value::fromFile("fbapi_route_original_task.json");
    fbapi::OriginalTask originalTask(value);
    auto answer = originalTask.answerContext()->as<fbapi::RouteAnswerContext>();

    {
        auto errors = answer.errors();
        BOOST_CHECK_EQUAL(errors.size(), 2);
        {
            const auto& error = errors[0];
            BOOST_CHECK_EQUAL(error.type(), fbapi::RouteErrorType::Barrier);
            BOOST_CHECK_CLOSE(error.point().x(), 4184302, 0.001);
            BOOST_CHECK_CLOSE(error.point().y(), 7670130, 0.001);
        }
        {
            const auto& error = errors[1];
            BOOST_CHECK_EQUAL(error.type(), fbapi::RouteErrorType::ProhibitingSign);
            const auto context = error.prohibitingSignContext();
            BOOST_CHECK(context);
            BOOST_CHECK(context->vehicleRestrictions());
            const auto vehicleRestrictions = context->vehicleRestrictions().value();
            BOOST_CHECK(!vehicleRestrictions.weight().has_value());
            BOOST_CHECK(!vehicleRestrictions.maxWeight().has_value());
            BOOST_CHECK_EQUAL(vehicleRestrictions.payload().value(), 12);
            BOOST_CHECK(!vehicleRestrictions.axleWeight().has_value());
            BOOST_CHECK(!vehicleRestrictions.height().has_value());
            BOOST_CHECK(!vehicleRestrictions.length().has_value());
            BOOST_CHECK(!vehicleRestrictions.width().has_value());
            BOOST_CHECK(!vehicleRestrictions.ecoClass().has_value());
            BOOST_CHECK_EQUAL(vehicleRestrictions.hasTrailer().value(), false);
        }
    }
    {
        auto segments = answer.segments();
        BOOST_CHECK_EQUAL(segments.size(), 1);
        const auto& segment = segments[0];
        BOOST_CHECK_EQUAL(segment.travelMode(), fbapi::TravelMode::Masstransit);
        auto segmentPoints = segment.points();
        BOOST_CHECK_EQUAL(segmentPoints.size(), 3);
        const auto& segmentPoint = segmentPoints[2];
        BOOST_CHECK_CLOSE(segmentPoint.x(), 4184971, 0.001);
        BOOST_CHECK_CLOSE(segmentPoint.y(), 7469703, 0.001);
    }
}

BOOST_AUTO_TEST_CASE(test_parse_route_question)
{
    auto value = json::Value::fromFile("fbapi_route_original_task.json");
    fbapi::OriginalTask originalTask(value);
    auto question = originalTask.questionContext()->as<fbapi::RouteQuestionContext>();

    BOOST_CHECK_EQUAL(question.travelMode(), fbapi::TravelMode::Masstransit);
    BOOST_CHECK_EQUAL(question.trafficJams(), true);
    BOOST_CHECK(question.encodedPoints());
    BOOST_CHECK_EQUAL(*question.encodedPoints(), "wNinAICxTwFAQg8AQEIPALgLAAAKAAAA");
    {
        auto points = question.wayPoints();
        BOOST_CHECK_EQUAL(points.size(), 2);
        BOOST_CHECK_CLOSE(points[1].x(), 4184293, 0.001);
        BOOST_CHECK_CLOSE(points[1].y(), 7470271, 0.001);
    }
    {
        auto points = question.viaPoints();
        BOOST_CHECK_EQUAL(points.size(), 1);
        BOOST_CHECK_CLOSE(points[0].x(), 3516376, 0.001);
        BOOST_CHECK_CLOSE(points[0].y(), 7275466, 0.001);
    }
    {
        auto segments = question.segments();
        BOOST_CHECK_EQUAL(segments.size(), 1);
        const auto& segment = segments.at(0);
        BOOST_CHECK_EQUAL(segment.travelMode(), fbapi::TravelMode::Auto);
        BOOST_CHECK_CLOSE(segment.from().x(), 5298219, 0.001);
        BOOST_CHECK_CLOSE(segment.from().y(), 9762845, 0.001);
        BOOST_CHECK_CLOSE(segment.to().x(), 5297407, 0.001);
        BOOST_CHECK_CLOSE(segment.to().y(), 9765521, 0.001);
    }
}

BOOST_AUTO_TEST_CASE(test_parse_task)
{
    auto value = json::Value::fromFile("fbapi_tasks.json");
    fbapi::Tasks tasks(value);

    BOOST_CHECK_EQUAL(tasks.tasks().size(), 2);

    const auto task = tasks.tasks().at(0);
    BOOST_CHECK_EQUAL(task.id(), "7adadc6da15e42a0a7cbf3264418773e");
    BOOST_CHECK_EQUAL(task.service(), fbapi::Service::Nmaps);
    BOOST_CHECK_EQUAL(chrono::formatIsoDateTime(task.createdAt()), "2017-06-22T19:54:24Z");
    BOOST_CHECK_EQUAL(task.attachedPhotos().size(), 2);
    BOOST_CHECK_EQUAL(task.attachedPhotos().at(0), "http://ya.ru/photo1");

    const auto originalTask = task.originalTask();
    BOOST_CHECK_EQUAL(originalTask.formId(), "toponym_form");
    BOOST_CHECK(originalTask.questionContext());
    BOOST_CHECK(originalTask.formPoint() ==
        geolib3::convertGeodeticToMercator(geolib3::Point2(37.589033, 55.734209))
    );

    auto toponymQuestion = originalTask.questionContext()->as<fbapi::Toponym>();
    BOOST_CHECK(!toponymQuestion.center());
    BOOST_CHECK_EQUAL(toponymQuestion.addressComponents().size(), 1);

    const auto addressComponent = toponymQuestion.addressComponents().at(0);
    BOOST_CHECK_EQUAL(addressComponent.kind(), fbapi::ToponymAddressComponentKind::Street);
    BOOST_CHECK(addressComponent.name());
    BOOST_CHECK_EQUAL(*addressComponent.name(), "Студеный проезд");

    BOOST_CHECK(originalTask.answerContext());
    auto toponymAnswer = originalTask.answerContext()->as<fbapi::Toponym>();
    BOOST_CHECK(toponymAnswer.name());
    BOOST_CHECK(*toponymAnswer.name() == "OptionalName");

    const auto metadata = originalTask.metadata();
    BOOST_CHECK_EQUAL(metadata.clientId(), "mobile_maps_android");
    BOOST_CHECK_EQUAL(metadata.version(), "1.0");
    BOOST_CHECK(metadata.ip());
    BOOST_CHECK_EQUAL(*metadata.ip(), "ip");
    BOOST_CHECK(metadata.fingerprint());
    BOOST_CHECK_EQUAL(*metadata.fingerprint(), "fingerprint");
    BOOST_CHECK(metadata.uid() == std::nullopt);

    const auto formContext = originalTask.formContext();
    BOOST_ASSERT(formContext);
    const auto searchText = formContext->searchText();
    BOOST_ASSERT(searchText);
    BOOST_CHECK_EQUAL(*searchText, "User search request");
    BOOST_ASSERT(formContext->fromPush());

    const auto task2 = tasks.tasks().at(1);
    const auto originalTask2 = task2.originalTask();
    const auto formContext2 = originalTask2.formContext();
    BOOST_ASSERT(formContext2);
    BOOST_ASSERT(!formContext2->fromPush());
}

BOOST_AUTO_TEST_CASE(test_parse_application_versions)
{
    auto checkApplicationVersion = [](const fbapi::Task& task, const fbapi::Version& expectedVersion){
        const auto originalTask = task.originalTask();
        const auto metadata = originalTask.metadata();

        BOOST_CHECK(metadata.applicationVersion());

        auto version = metadata.applicationVersion().value();
        BOOST_CHECK_EQUAL(version.getMajor(), expectedVersion.getMajor());
        BOOST_CHECK_EQUAL(version.getMinor(), expectedVersion.getMinor());
        BOOST_CHECK_EQUAL(version.getPatch(), expectedVersion.getPatch());
    };

    auto value = json::Value::fromFile("fbapi_tasks.json");
    fbapi::Tasks tasks(value);

    BOOST_CHECK_EQUAL(tasks.tasks().size(), 2);
    checkApplicationVersion(tasks.tasks().at(0), fbapi::Version(8, 1, 0));
    checkApplicationVersion(tasks.tasks().at(1), fbapi::Version(12, 1, 9));
}


BOOST_AUTO_TEST_CASE(test_lazy_parsing)
{
    auto value = json::Value::fromString("{}");
    fbapi::Task task(value);
    BOOST_CHECK_THROW(task.id(), fbapi::FeedbackError);
}

BOOST_AUTO_TEST_CASE(test_history_parsing)
{
    fbapi::Tasks tasks(json::Value::fromFile("fbapi_tasks.json"));
    const auto history = tasks.tasks().at(1).sortedHistory();

    BOOST_CHECK_EQUAL(history.size(), 2);

    const fbapi::TaskChange& firstChange = history.at(0);

    BOOST_CHECK(firstChange.message() == std::nullopt);
    BOOST_CHECK(firstChange.service() == std::nullopt);
    BOOST_CHECK_EQUAL(firstChange.status(), fbapi::TaskStatus::InProgress);
    BOOST_CHECK_EQUAL(chrono::formatIsoDateTime(firstChange.createdAt()), "2017-06-24T22:54:24Z");

    const fbapi::TaskChange& secondChange = history.at(1);

    BOOST_CHECK(secondChange.message() == "razobralis");
    BOOST_CHECK(secondChange.service() == fbapi::Service::Nmaps);
    BOOST_CHECK_EQUAL(secondChange.status(), fbapi::TaskStatus::New);
    BOOST_CHECK_EQUAL(chrono::formatIsoDateTime(secondChange.createdAt()), "2017-06-25T22:54:24Z");
}

BOOST_AUTO_TEST_CASE(test_parse_fence)
{
    auto value = json::Value::fromFile("fbapi_fence_original_task.json");
    fbapi::OriginalTask originalTask(value);
    auto answer = originalTask.answerContext()->as<fbapi::Fence>();

    {
        BOOST_CHECK(answer.center());
        const auto center = answer.center().value();
        BOOST_CHECK_CLOSE(center.x(), 3616378, 0.001);
        BOOST_CHECK_CLOSE(center.y(), 6719203, 0.001);
    }
    {
        const auto points = answer.points();
        BOOST_CHECK_EQUAL(points.size(), 2);
        BOOST_CHECK_CLOSE(points[0].x(), 3627730, 0.001);
        BOOST_CHECK_CLOSE(points[0].y(), 6718578, 0.001);
        BOOST_CHECK_CLOSE(points[1].x(), 3627874, 0.001);
        BOOST_CHECK_CLOSE(points[1].y(), 6718636, 0.001);
    }
    {
        const auto gates = answer.gates();
        BOOST_CHECK_EQUAL(gates.size(), 1);
        BOOST_CHECK_CLOSE(gates[0].center().x(), 3630445, 0.001);
        BOOST_CHECK_CLOSE(gates[0].center().y(), 6621110, 0.001);
    }
}

} //namespace maps::wiki::tests
