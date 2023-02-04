#include "json_compare.h"

#include <maps/libs/json/include/value.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/spatial_relation.h>
#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/geolib/include/variant.h>

#include <boost/test/unit_test.hpp>

#include <map>
#include <string>

namespace maps {
namespace wiki {
namespace groupedit {
namespace tests {

namespace {

const std::string DELETED_FLAG = "deleted";
const std::string ATTRIBUTES_FIELD = "attributes";
const std::string GEOMETRY_FIELD = "geometry";
const std::string RELATIONS_FIELD = "relations";
const std::string SLAVE_FIELD = "slave";

void compareAttributes(
        const std::string& what,
        const json::Value& attrs,
        const json::Value& expected)
{
    for (const std::string& attrName : expected.fields()) {
        if (!attrs.hasField(attrName)) {
            BOOST_ERROR(what << " missing attribute " << attrName);
            continue;
        }

        const auto& expectedValue = expected[attrName].as<std::string>();
        const auto& currentValue = attrs[attrName].as<std::string>();
        BOOST_CHECK_MESSAGE(
            currentValue == expectedValue,
            what << " has wrong attribute " << attrName
                << " value: '" << currentValue
                << "', expected: '" << expectedValue << "'");
    }

    for (const std::string& attrName : attrs.fields()) {
        if (!expected.hasField(attrName)) {
            BOOST_ERROR(what << " has extra attribute " << attrName);
        }
    }
}

void compareGeometries(
        const std::string& objectId,
        const json::Value& geomValue,
        const json::Value& expectedValue)
{
    auto geometryVariant = geolib3::readGeojson<geolib3::SimpleGeometryVariant>(geomValue);
    auto expectedGeometryVariant = geolib3::readGeojson<geolib3::SimpleGeometryVariant>(expectedValue);

    BOOST_REQUIRE_EQUAL(geometryVariant.geometryType(), expectedGeometryVariant.geometryType());

    geometryVariant.visit([&](const auto& geometry) {
        using Geometry = std::decay_t<decltype(geometry)>;
        const auto& expectedGeometry = expectedGeometryVariant.get<Geometry>();

        BOOST_CHECK_MESSAGE(
            geolib3::spatialRelation(geometry, expectedGeometry, geolib3::Equals),
            "Object " << objectId << " has wrong geometry");
    });
}

void compareRelations(
        const std::string& objectId,
        const json::Value& relations,
        const json::Value& expected)
{
    std::map<std::string, json::Value> relationsBySlave;

    for (const json::Value& rel : expected) {
        auto slave = rel[SLAVE_FIELD].as<std::string>();
        BOOST_REQUIRE_MESSAGE(
            relationsBySlave.insert(
                {std::move(slave), rel[ATTRIBUTES_FIELD]}).second,
            "Duplicate relations for slave id " << slave
                << " in diff for object " << objectId);
    }

    for (const json::Value& rel : relations) {
        const auto& slave = rel[SLAVE_FIELD].as<std::string>();

        if (relationsBySlave.count(slave) == 0) {
            BOOST_ERROR("Extra relation between objects "
                << objectId << " and " << slave);
            continue;
        }

        compareAttributes(
            std::string("Relation between objects ")
                + objectId + " and " + slave,
            rel[ATTRIBUTES_FIELD],
            relationsBySlave.at(slave));

        relationsBySlave.erase(slave);
    }

    for (const auto& slaveRelPair : relationsBySlave) {
        BOOST_ERROR("Missing relation between objects "
            << objectId << " and " << slaveRelPair.first);
    }
}

} // namespace

void checkExpectedJson(
        const json::Value& objects,
        const json::Value& expected)
{
    for (const std::string& objectId : expected.fields()) {
        const auto& objectExpected = expected[objectId];

        if (objectExpected.isString()
                && objectExpected.as<std::string>() == DELETED_FLAG) {
            BOOST_CHECK_MESSAGE(!objects.hasField(objectId),
                "Object " << objectId << " has not beed deleted");
            continue;
        }

        if (!objects.hasField(objectId)) {
            BOOST_ERROR("Object " << objectId << " missing");
            continue;
        }

        const auto& object = objects[objectId];

        for (const std::string& field : objectExpected.fields()) {
            if (!object.hasField(field)) {
                BOOST_ERROR("Missing field '" << field << "' for object " << objectId);
                continue;
            }
            if (field == ATTRIBUTES_FIELD) {
                compareAttributes(std::string("Object ") + objectId,
                    object[field], objectExpected[field]);
            } else if (field == GEOMETRY_FIELD) {
                compareGeometries(objectId, object[field], objectExpected[field]);
            } else if (field == RELATIONS_FIELD) {
                compareRelations(objectId, object[field], objectExpected[field]);
            } else {
                BOOST_FAIL("Unknown field '" << field << "' in expected data"
                        << " for object " << objectId);
            }
        }
    }
}

} // namespace tests
} // namespace groupedit
} // namespace wiki
} // namespace maps
