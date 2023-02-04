#define BOOST_TEST_ALTERNATIVE_INIT_API

#include <yandex/maps/wiki/routing/common.h>
#include <yandex/maps/wiki/routing/exception.h>
#include <yandex/maps/wiki/routing/route.h>

#include "common.h"
#include "io.h"
#include "json.h"

#include <boost/test/unit_test.hpp>

#include <iterator>
#include <map>
#include <string>
#include <vector>

namespace bt = boost::unit_test;

namespace maps {
namespace wiki {
namespace routing {
namespace tests {

constexpr double FLOAT_COMPARE_EPSILON = 1e6;

template<typename T>
bool equal(const T& lhs, const T& rhs) { return lhs == rhs; }

template<>
bool equal(const double& output, const double& expected)
{
    return std::abs(output - expected) < FLOAT_COMPARE_EPSILON;
}

template<>
bool equal(const Point& output, const Point& expected)
{
    return equal(output.x(), expected.x()) && equal(output.y(), expected.y());
}

template<>
bool equal(const StopSnap& output, const StopSnap& expected)
{
    return equal(output.stopId(), expected.stopId())
        && equal(output.locationOnElement(), expected.locationOnElement())
        && equal(output.point(), expected.point());
}

template<>
bool equal(const TracePoint& output, const TracePoint& expected)
{
    if (output.stopSnap()) {
        if (!expected.stopSnap() || !equal(*output.stopSnap(), *expected.stopSnap())) {
            return false;
        }
    } else if (output.stopSnap()) {
        return false;
    }

    return equal(output.directedElementId(), expected.directedElementId());
}

bool equal(const Stop& output, const Stop& expected)
{
    return equal(output.id(), expected.id()) && equal(output.geom(), expected.geom());
}

template<>
bool equal(const AmbiguousPathError& output, const AmbiguousPathError& expected)
{
    return equal(output.fromStopId(), expected.fromStopId())
        && equal(output.toStopId(), expected.toStopId())
        && equal(output.elementId(), expected.elementId());
}

template<>
bool equal(const NoPathError& output, const NoPathError& expected)
{
    return equal(output.fromStopId(), expected.fromStopId())
        && equal(output.toStopId(), expected.toStopId());
}

template<typename TCollection>
bool equalCollection(const TCollection& output, const TCollection& expected)
{
    return equal(output.size(), expected.size())
        && std::equal(
            output.begin(), output.end(),
            expected.begin(),
            equal<typename TCollection::value_type>
        );
}


void checkErrorType(const std::string& type, const json::Value& errorJson)
{
    const std::string expectedErrorType = errorJson["type"].toString();
    BOOST_REQUIRE_MESSAGE(
        expectedErrorType == type,
        "Catched " << type << ", but expected " << expectedErrorType
    );
}

template<typename T>
void checkErrorField(const T& output, const json::Value& errorJson, const std::string& field) {
    const T expected = fromJson<T>(errorJson[field]);
    BOOST_REQUIRE_MESSAGE(
        equal(output, expected),
        "Expected " << field << " " << print(expected)
            << ", but catched " << print(output)
    );
}

struct DiffSet {
    Ids unexpected;
    Ids missed;
};

DiffSet diffSet(const IdSet& output, const IdSet& expected)
{
    Ids unexpected;
    std::set_difference(
        output.begin(), output.end(),
        expected.begin(), expected.end(),
        std::back_inserter(unexpected)
    );

    Ids missed;
    std::set_difference(
        expected.begin(), expected.end(),
        output.begin(), output.end(),
        std::back_inserter(missed)
    );

    return {
        std::move(unexpected),
        std::move(missed)
    };
}

template<>
void checkErrorField(const IdSet& output, const json::Value& errorJson, const std::string& field) {
    const auto expected = fromJson<IdSet>(errorJson[field]);

    const auto idDiffSet = diffSet(output, expected);
    BOOST_REQUIRE_MESSAGE(
        idDiffSet.unexpected.empty() && idDiffSet.missed.empty(),
        "Unexpected " << field << printCollection(idDiffSet.unexpected)
            << ", missed ids: " << printCollection(idDiffSet.missed)
    );
}

void checkTrace(const Trace& output, const Trace& expected)
{
    BOOST_REQUIRE_MESSAGE(
        equalCollection(output, expected),
        "Output trace differs from the expected trace "
            << "expected " << printCollection(expected)
            << ", but returned " << printCollection(output)
    );
}

void checkSet(const IdSet& output, const IdSet& expected, const std::string& name)
{
    const auto diff = diffSet(output, expected);

    BOOST_REQUIRE_MESSAGE(
        diff.unexpected.empty() && diff.missed.empty(),
        "Unexpected " << name << " " << printCollection(diff.unexpected)
            << ", missed " << name << " " << printCollection(diff.missed)
    );
}

void checkAmbiguousPathErrors(
        const AmbiguousPathErrors& output,
        const AmbiguousPathErrors& expected,
        const std::string& type)
{
    BOOST_REQUIRE_MESSAGE(
        equalCollection(output, expected),
        "Output " <<  type << " ambiguous path errors differ from the expected, "
            << "expected " << printCollection(expected)
            << ", but returned " << printCollection(output)
    );
}

void checkNoPathErrors(const NoPathErrors& output, const NoPathErrors& expected)
{
    BOOST_REQUIRE_MESSAGE(
        equalCollection(output, expected),
        "Output no path errors differ from the expected, "
            << "expected " << printCollection(expected)
            << ", but returned " << printCollection(output)
    );
}

void check(const json::Value& test)
{
    auto require = [&test](const std::string& field) {
        const json::Value& value = test[field];
        REQUIRE(value.exists(), "field " << field << " required");
        return value;
    };

    const auto& input = require("input");
    try {
        const auto output = restore(
            fromJson<Elements>(input["elements"], {}),
            fromJson<Stops>(input["stops"], {}),
            fromJson<Conditions>(input["conditions"], {}),
            input["stopSnapRadius"].as<double>()
        );

        const auto expected = fromJson<RestoreResult>(require("expected"));

        checkTrace(output.trace(), expected.trace());
        checkSet(
            output.unusedElementIds(),
            expected.unusedElementIds(),
            "unused element ids"
        );
        checkAmbiguousPathErrors(
            output.forwardAmbiguousPathErrors(),
            expected.forwardAmbiguousPathErrors(),
            "forward"
        );
        checkAmbiguousPathErrors(
            output.backwardAmbiguousPathErrors(),
            expected.backwardAmbiguousPathErrors(),
            "backward"
        );
        checkNoPathErrors(
            output.noPathErrors(),
            expected.noPathErrors()
        );
    } catch (const ImpossibleSnapStopError& error) {
        const auto& errorJson = test["error"];
        BOOST_REQUIRE_MESSAGE(errorJson.exists(), "Unexpected error " << print(error));

        checkErrorType("ImpossibleSnapStopError", errorJson);

        checkErrorField(error.stopIds(), errorJson, "stopIds");
    }
}

} // namespace tests
} // namespace routing
} // namespace wiki
} // namespace maps

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    maps::wiki::routing::tests::initTestSuite("restore",
        maps::wiki::routing::tests::check);
    return nullptr;
}

#ifdef YANDEX_MAPS_BUILD
bool init_unit_test_suite()
{
    init_unit_test_suite(0, NULL);
    return true;
}

int main(int argc, char* argv[])
{
    return bt::unit_test_main(&init_unit_test_suite, argc, argv);
}
#endif //YANDEX_MAPS_BUILD
