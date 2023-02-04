#include "tests/boost-tests/include/tools/map_tools.h"

#include "postgres/PostgresFeatureContainer.h"
#include "core/DynamicGeometryFeatureContainer.h"
#include "core/DynamicMapPathResolver.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer/io/io.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::postgres;
using namespace maps::renderer5::core;

namespace {
}

#define CHECK_EQUAL(container1, container2, accessor) \
    BOOST_CHECK(container1->accessor == container2->accessor)

#define CHECK_NO_EQUAL(container1, container2, accessor) \
    BOOST_CHECK(container1->accessor != container2->accessor)

#define CHECK_GEOMETRY_CONTAINERS(container1, container2, method) \
    method(container1, container2, featureType()); \
    method(container1, container2, idColumnName()); \
    method(container1, container2, filteringExpression()); \
    method(container1, container2, supplementaryPriorityExpression()); \
    method(container1, container2, zMinExpression()); \
    method(container1, container2, zMaxExpression()); \
    method(container1, container2, textExpression()); \
    method(container1, container2, textAlternativeExpression());

#define CHECK_PFC_CONTAINERS(container1, container2, method) \
    CHECK_GEOMETRY_CONTAINERS(container1, container2, method) \
    method(container1, container2, tableName()); \
    method(container1, container2, geometryColumnName()); \
    method(container1, container2, geometryExpression()); \
    method(container1, container2, meshColumnName()); \
    method(container1, container2, heightColumnName());

#define CHECK_DGFC_CONTAINERS(container1, container2, method) \
    CHECK_GEOMETRY_CONTAINERS(container1, container2, method)

BOOST_AUTO_TEST_SUITE(cloning)

BOOST_AUTO_TEST_CASE(pfc)
{
    auto gui = test::map::createTestMapGui();
    PostgresTransactionProviderPtr provider;
    auto container = std::make_shared<PostgresFeatureContainer>(provider);
    auto emptyContainer = std::make_shared<PostgresFeatureContainer>(provider);

    CHECK_PFC_CONTAINERS(container, emptyContainer, CHECK_EQUAL);

    container->setLayerId(1);
    container->setFeatureType(core::FeatureType::Polyline);
    container->setTableName(L"wiki_streets");
    container->setGeometryColumnName(L"the_geom");
    container->setIdColumnName(L"id");
    container->setFilteringExpression(L"state = 1");
    container->setSupplementaryPriorityExpression(L"10");
    container->setMeshColumnName(L"mesh");
    container->setHeightColumnName(L"height");
    container->setZMinExpression(L"zmin");
    container->setZMaxExpression(L"zmax");
    container->setTextExpression(L"text");
    container->setTextAlternativeExpression(L"alt_text");

    CHECK_PFC_CONTAINERS(container, emptyContainer, CHECK_NO_EQUAL);

    auto clone = std::unique_ptr<PostgresFeatureContainer>(
        static_cast<PostgresFeatureContainer*>(
            container->clone(gui->map().env(), 1).release()));

    CHECK_PFC_CONTAINERS(container, clone, CHECK_EQUAL);
}

BOOST_AUTO_TEST_CASE(dgfc)
{
    auto gui = test::map::createTestMapGui();
    auto progressStub = test::map::createProgressStub();

    auto container = std::make_shared<DynamicGeometryFeatureContainer>(
        gui->pathResolver(), *progressStub,
        io::path::absolute("tests/manual-tests/data/roads.MIF"),
        core::FeatureType::Polyline, L"id");
    auto emptyContainer = std::make_shared<DynamicGeometryFeatureContainer>(
        gui->pathResolver(), *progressStub);

    container->setLayerId(1);
    container->setFilteringExpression(L"state = 1");
    container->setSupplementaryPriorityExpression(L"10");
    container->setZMinExpression(L"zmin");
    container->setZMaxExpression(L"zmax");
    container->setTextExpression(L"text");
    container->setTextAlternativeExpression(L"alt_text");

    CHECK_DGFC_CONTAINERS(container, emptyContainer, CHECK_NO_EQUAL);

    auto clone = std::unique_ptr<DynamicGeometryFeatureContainer>(
        static_cast<DynamicGeometryFeatureContainer*>(
            container->clone(gui->map().env(), 1).release()));

    CHECK_DGFC_CONTAINERS(container, clone, CHECK_EQUAL);
}

BOOST_AUTO_TEST_SUITE_END()
