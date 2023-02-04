#include "tests/boost-tests/include/tools/map_tools.h"
#include "../../../../postgres/PostgresFeatureContainer.h"

#include "../../include/contexts.hpp"

#include <maps/renderer/libs/base/include/logger.h>
#include <yandex/maps/renderer5/core/ZOrderProperties.h>
#include <yandex/maps/renderer5/core/RenderingContextVariables.h>

#include <boost/test/unit_test.hpp>

namespace maps {
namespace renderer5 {
namespace postgres {

namespace {
renderer::base::ILoggerPtr gLogger(new renderer::base::ConsoleLogger());

class ZOrderPropertiesTest: public core::ZOrderProperties
{
public:
    void setType(core::FeatureZOrderType type)
    {
        ZOrderProperties::setType(type);
    }

    void setFlatZOrderColumnName(const std::wstring & columnName)
    {
        ZOrderProperties::setFlatZOrderColumnName(columnName);
    }

    void setLinearZOrderColumnNames(const std::wstring & columnNameBegin,
                                    const std::wstring & columnNameEnd)
    {
        ZOrderProperties::setLinearZOrderBeginColumnName(columnNameBegin);
        ZOrderProperties::setLinearZOrderEndColumnName(columnNameEnd);
    }
};

}

BOOST_AUTO_TEST_SUITE( getbb_pfc )

BOOST_FIXTURE_TEST_CASE( getbb, TransactionProviderContext<> )
{
    REN_LOG_PUSH(gLogger);

    PostgresFeatureContainer pfc(provider);
    pfc.setLayerId(1);
    pfc.setFeatureType(core::FeatureType::Polyline);
    pfc.setTableName(L"wiki_streets");
    pfc.setGeometryColumnName(L"the_geom");
    pfc.setIdColumnName(L"id");
    pfc.setFilteringExpression(L"state = 1");

    pfc.open();

    renderer::base::BoxD bb = pfc.getExtent();

    auto fit = pfc.findFeatures(bb, {});

    unsigned int num = 0;
    for (fit->reset(); fit->hasNext(); fit->next())
        ++num;

    BOOST_CHECK(num == 7);

    pfc.close();
}

BOOST_FIXTURE_TEST_CASE( renderclause, TransactionProviderContext<> )
{
    REN_LOG_PUSH(gLogger);

    PostgresFeatureContainer pfc(provider);
    pfc.setLayerId(1);
    pfc.setFeatureType(core::FeatureType::Point);
    pfc.setTableName(L"sample_points");
    pfc.setGeometryColumnName(L"the_geom");
    pfc.setIdColumnName(L"id");
    pfc.setFilteringExpression(L"id > %z%");

    core::ContextProviderPtr renderContext = core::createContextProvider();
    renderContext->setVariable(core::RenderingContextVariables::zoom, 1);
    pfc.setContextProvider(renderContext);

    BOOST_REQUIRE_NO_THROW(pfc.open());
    BOOST_REQUIRE(pfc.isOpened());

    const renderer::base::BoxD bb = pfc.getExtent();

    auto fit = pfc.findFeatures(bb, {});
    unsigned int num = 0;
    for (fit->reset(); fit->hasNext(); fit->next())
        ++num;

    BOOST_CHECK(num == 1);

    pfc.close();
}

BOOST_FIXTURE_TEST_CASE( zorder, TransactionProviderContext<> )
{
    REN_LOG_PUSH(gLogger);

    PostgresFeatureContainer pfc(provider);
    pfc.setLayerId(1);
    pfc.setFeatureType(core::FeatureType::Polyline);
    pfc.setTableName(L"wiki_streets");
    pfc.setGeometryColumnName(L"the_geom");
    pfc.setIdColumnName(L"id");
    pfc.setFilteringExpression(L"state = 1");

    pfc.open();

    ZOrderPropertiesTest testZOrder;

    core::FeatureZOrder fzo;

    core::FeatureIdType id = 3800139;
    renderer::feature::Feature ft(renderer::feature::FeatureType::Null);

    // check linear z order
    //
    {
        testZOrder.setType(core::FeatureZOrderType::Linear);
        testZOrder.setLinearZOrderColumnNames(L"zmin", L"zmax");
        pfc.setZOrderProperties(testZOrder);

        pfc.featureById(id, ft);
        fzo = ft.zOrder();

        BOOST_CHECK(fzo.type() == core::FeatureZOrderType::Linear);
        BOOST_CHECK(fzo.begin == 13);
        BOOST_CHECK(fzo.end == 23);
    }

    // check flat z order
    {
        testZOrder.setType(core::FeatureZOrderType::Flat);
        testZOrder.setFlatZOrderColumnName(L"zmin");
        pfc.setZOrderProperties(testZOrder);

        pfc.featureById(id, ft);
        fzo = ft.zOrder();

        BOOST_CHECK(fzo.type() == core::FeatureZOrderType::Flat);
        BOOST_CHECK(fzo.begin == 13);
    }

    renderer::base::BoxD bb = pfc.getExtent();

    // check linear z order with other type of query
    {
        testZOrder.setType(core::FeatureZOrderType::Linear);
        testZOrder.setLinearZOrderColumnNames(L"zmin", L"zmax");

        pfc.setZOrderProperties(testZOrder);
        pfc.featureById(id, ft);
        fzo = ft.zOrder();

        BOOST_CHECK(fzo.type() == core::FeatureZOrderType::Linear);
    }

    auto fit = pfc.findFeatures(bb, {});

    unsigned int num = 0;
    for (fit->reset(); fit->hasNext(); )
    {
        renderer::feature::Feature& feature = fit->next();

        core::FeatureZOrder fzo = feature.zOrder();
        BOOST_CHECK(fzo.type() == core::FeatureZOrderType::Linear);
        BOOST_CHECK(fzo.begin == 13);
        BOOST_CHECK(fzo.end == 23);
        ++num;
    }

    BOOST_CHECK(num > 0);

    pfc.close();
}

// void test::postgres::attach_pfc_container_test()
// {
//     PostgresTransactionProviderPtr transactionProvider(
//             new DefaultPostgresTransactionProvider(test::postgres::options));
//
//     REN_LOG_PUSH(logger_);
//
//     PostgresFeatureContainer pfc(transactionProvider, 1, core::FeatureType::Polyline,
//         L"wiki_streets", L"the_geom", L"id", L"", false, true);
//
//     pfc.open();
//
//     pfc.close();
// }

BOOST_AUTO_TEST_SUITE_END()

} // namespace postgres
} // namespace renderer5
} // namespace maps
