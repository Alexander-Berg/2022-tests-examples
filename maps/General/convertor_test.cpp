#include <maps/libs/coverage/tools/common/xml2mms_convertor.h>

#include <maps/libs/coverage/layer_data.h>
#include <maps/libs/coverage/layer_metadata.h>
#include <maps/libs/coverage/region_data.h>
#include <maps/libs/coverage/region_geometry.h>
#include <maps/libs/coverage/test_tools/geom_test_types.h>
#include <maps/libs/coverage/test_tools/test_author.h>
#include <maps/libs/coverage/test_tools/io_std.h>

#include <yandex/maps/coverage5/builder.h>
#include <yandex/maps/coverage5/cmd_helper.h>

#include <yandex/maps/mms/holder2.h>
#include <yandex/maps/mms/copy.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/common/include/exception.h>

#include <maps/libs/xml/include/xml.h>

#include <library/cpp/testing/common/env.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/unit_test_log.hpp>
#include <boost/test/test_tools.hpp>
#include <boost/optional.hpp>

#include <vector>
#include <string>
#include <iostream>
#include <initializer_list>

using maps::coverage5::convert::XML2MMSConvertor;
using maps::Exception;

using maps::coverage5::test::TestAuthor;
using maps::coverage5::test::TestAuthors;

using namespace maps::coverage5;
using namespace maps::xml3;

typedef geom::StandaloneLinearRing LinearRing;
typedef geom::StandalonePolygon Polygon;
typedef geom::StandaloneMultiPolygon MultiPolygon;

static const std::string outDir(BuildRoot() + "/maps/libs/coverage/tools/cov2mms/ut/");

struct RegionDataTest {
    RegionId id;
    Zoom min;
    Zoom max;
    boost::optional<TestAuthors> authors;
    boost::optional<MultiPolygon> geometry;
};

struct LayerTest {
    std::string infile;
    std::string outfile;
    std::string name;
    bool isProvider;
    bool isReference;
    boost::optional<std::string> provider;
    std::string version;
    std::vector<RegionDataTest> regionsTests;
};

LayerTest g_providerLayerTest {
    "in/test_coverage_provider.xml", outDir + "test_coverage_provider.mms.1",
    "test", true, false, boost::none, "",
    {
        RegionDataTest {
            100,
            0, 12,
            boost::none,
            MultiPolygon {
                Polygon {
                    LinearRing { {-180., -85.}, {-180., 85.}, {180., 85.}, {180., -85.} }
                }
            }
        },
        RegionDataTest {
            200,
            12, 17,
            TestAuthors {
                TestAuthor {
                    {"dicentra"},
                    {"http://staff.yandex-team.ru/dicentra"},
                    {"dicentra@yandex-team.ru", "anastasia.petrushkina@gmail.com"}
                }
            },
            MultiPolygon {
                Polygon {
                    LinearRing { {-180., -85.}, {-180., 85.}, {180., 85.}, {180., -85.} },
                    LinearRing { {-90., -42.5}, {-90., 42.5}, {90., 42.5}, {90., -42.5} }
                }
            }
        }
    }
};

LayerTest g_nonProviderLayerTest {
    "in/test_coverage_non_provider.xml", outDir + "test_coverage_non_provider.mms.1",
    "test_non-prov", false, false, boost::none, "1.0",
    {
        RegionDataTest {
            1,
            0, 12,
            boost::none,
            MultiPolygon {
                Polygon {
                    LinearRing { {-180., -85.}, {-180., 85.}, {180., 85.}, {180., -85.} }
                }
            }
        },
        RegionDataTest {
            2,
            12, 17,
            TestAuthors {
                TestAuthor {
                    {"dicentra"},
                    {"http://staff.yandex-team.ru/dicentra"},
                    {"dicentra@yandex-team.ru", "anastasia.petrushkina@gmail.com"}
                }
            },
            MultiPolygon {
                Polygon {
                    LinearRing { {-180., -85.}, {-180., 85.}, {180., 85.}, {180., -85.} },
                    LinearRing { {-90., -42.5}, {-90., 42.5}, {90., 42.5}, {90., -42.5} }
                }
            }
        }
    }
};

LayerTest g_referenceLayerTest {
    "in/test_coverage_reference.xml", outDir + "test_coverage_reference.mms.1",
    "test_ref", false, true, std::string{"test"}, "1.0",
    {
        RegionDataTest {
            100,
            0, 23,
            boost::none,
            boost::none
        },
        RegionDataTest {
            200,
            12, 17,
            TestAuthors {
                TestAuthor {
                    {"dicentra"},
                    {"http://staff.yandex-team.ru/dicentra"},
                    {"dicentra@yandex-team.ru", "anastasia.petrushkina@gmail.com"}
                }
            },
            boost::none
        }
    }
};

struct SeveralLayersTestData {
    std::string infile;
    std::vector<std::string> layers;
    std::vector<std::string> outfiles;
}
    g_severalLayersTest
{
    "in/test_several_layers.xml",
    std::vector<std::string> {
        "test",
        "test_non-prov",
        "test_ref"
    },
    std::vector<std::string> {
        outDir + "test.mms.1",
        outDir + "test_non-prov.mms.1",
        outDir + "test_ref.mms.1"
    }
};

std::string g_incorrectCoverageTests[]
{
    "in/test_coverage_duplicate_ID.xml",
    "in/test_coverage_missing_ID.xml",
    "in/test_coverage_ambiguous_ID.xml",
    "in/test_coverage_ref_missing_provider.xml",
    "in/test_coverage_ref_missing_ID.xml"
};

void checkGeometry(const MultiPolygon& expected,
    const geom::MultiPolygon<mms::Mmapped>& received)
{
    MultiPolygon recCopy;
    mms::copy(received, recCopy);
    test::TestMultiPolygon expTest, recTest;
    expTest = test::convertToTest(expected);
    recTest = test::convertToTest(recCopy);
    BOOST_CHECK_MESSAGE(test::CutTestGeometryCheck<test::TestMultiPolygon>()(
        expTest, recTest),
        "Geometry check failed" <<
        "\n<<<<< Expected result >>>>>\n" << expTest <<
        "\n>>>>> Received result <<<<<\n" << recTest << "\n");
}

void testLayer(const LayerTest& test,
    const boost::optional<std::string>& name = boost::none,
    const boost::optional<std::string>& version = boost::none)
{
    XML2MMSConvertor conv(CmdHelper({"test", "-o", test.outfile}));
    conv.convertCoverage(test.infile, name, version);

    mms::Holder2<LayerDataSecond<mms::Mmapped>> layerHolder(test.outfile);
    const LayerDataSecond<mms::Mmapped>* layerData = layerHolder.get();

    // check layer metadata
    const LayerMetaDataSecond<mms::Mmapped>& layerMeta = layerData->metaData();
    BOOST_CHECK((!name && layerMeta.name() == test.name) ||
        (name && layerMeta.name() == *name));
    BOOST_CHECK(layerMeta.isProvider() == test.isProvider);
    BOOST_CHECK(layerMeta.isReference() == test.isReference);

    if (layerMeta.name() == "test") {
        BOOST_CHECK(layerMeta.metadata());
        std::string metadata = layerMeta.metadata()->c_str();
        BOOST_CHECK(metadata.find("<scaled>") != std::string::npos);
        Doc metaDoc(metadata, Doc::String);
        //checking namespace (nodeNamespace returns dereferences namespace)
        BOOST_CHECK_EQUAL(metaDoc.root().nodeNamespace(), "http://maps.yandex.ru/coverage/2.x");
    }

    BOOST_CHECK(
        (!test.isReference && !layerMeta.provider() && !test.provider) ||
        (test.isReference && layerMeta.provider() && test.provider &&
        *test.provider == (*layerMeta.provider()).c_str()));

    BOOST_CHECK((!version && layerMeta.version() == test.version) ||
        (version && layerMeta.version() == *version));

    const RegionsData<mms::Mmapped>* regionsData = layerData->regionsData();

    // check regions data
    for (auto regionTest: test.regionsTests) {
        const RegionData<mms::Mmapped>& regionMeta =
            *(regionsData->data(regionTest.id));
        if (test.isReference || test.isProvider) {
            BOOST_REQUIRE(regionMeta.id());
            BOOST_CHECK_EQUAL(*regionMeta.id(), regionTest.id);
        } else {
            BOOST_CHECK(!regionMeta.id());
        }
        BOOST_CHECK_EQUAL(regionMeta.zoomMin(), regionTest.min);
        BOOST_CHECK_EQUAL(regionMeta.zoomMax(), regionTest.max);
        if (regionTest.authors) {
            checkAuthors(*regionTest.authors, regionMeta.authors());
        }
        if (regionTest.geometry) {
            checkGeometry(*regionTest.geometry,
                *layerData->geometry()->geometry(regionTest.id)->geoms());
        }

        if (layerMeta.name() == "test") {
            std::string metadata = regionMeta.metaData().c_str();
            Doc metaDoc(metadata, Doc::String);
            //checking namespace (nodeNamespace returns dereferences namespace)
            BOOST_CHECK_EQUAL(metaDoc.root().nodeNamespace(), "http://maps.yandex.ru/coverage/2.x");
        }
    }
}

BOOST_AUTO_TEST_CASE(test_geometry_provider_coverage)
{
    testLayer(g_providerLayerTest);
    testLayer(g_providerLayerTest,
        boost::optional<std::string>(g_providerLayerTest.name));
    testLayer(g_providerLayerTest,
        boost::optional<std::string>(g_providerLayerTest.name),
        boost::optional<std::string>("TEST_VERSION"));
}

BOOST_AUTO_TEST_CASE(test_non_provider_coverage)
{
    testLayer(g_nonProviderLayerTest);
    testLayer(g_nonProviderLayerTest,
        boost::optional<std::string>(g_nonProviderLayerTest.name));
    testLayer(g_nonProviderLayerTest,
        boost::optional<std::string>(g_nonProviderLayerTest.name),
        boost::optional<std::string>("TEST_VERSION"));
}

BOOST_AUTO_TEST_CASE(reference_coverage_test)
{
    testLayer(g_referenceLayerTest);
    testLayer(g_referenceLayerTest,
        boost::optional<std::string>(g_referenceLayerTest.name));
    testLayer(g_referenceLayerTest,
        boost::optional<std::string>(g_referenceLayerTest.name),
        boost::optional<std::string>("TEST_VERSION"));
}

BOOST_AUTO_TEST_CASE(test_several_layers)
{
    for (size_t i = 0; i < g_severalLayersTest.layers.size(); ++i) {
        XML2MMSConvertor conv(CmdHelper(
            {"test", "-o",  g_severalLayersTest.outfiles[i]}));

        BOOST_CHECK_THROW(
            conv.convertCoverage(g_severalLayersTest.infile),
            maps::Exception);
        BOOST_CHECK_NO_THROW(
            conv.convertCoverage(g_severalLayersTest.infile,
                g_severalLayersTest.layers[i]));
    }
}

BOOST_AUTO_TEST_CASE(test_incorrect_coverage)
{
    for (auto incorrectCov: g_incorrectCoverageTests) {
        XML2MMSConvertor conv(CmdHelper({"test", "-o",  "fake"}));

        BOOST_CHECK_THROW(conv.convertCoverage(incorrectCov),
            maps::Exception);
    }
}

BOOST_AUTO_TEST_CASE(test_one_layer_from_several)
{
    std::vector<LayerTest> tests;
    tests.push_back(g_providerLayerTest);
    tests.push_back(g_nonProviderLayerTest);
    tests.push_back(g_referenceLayerTest);
    for (auto test: tests) {
        test.infile = g_severalLayersTest.infile;
        testLayer(test, boost::optional<std::string>(test.name));
        testLayer(test, boost::optional<std::string>(test.name),
            boost::optional<std::string>("TEST_VERSION"));
    }
}
