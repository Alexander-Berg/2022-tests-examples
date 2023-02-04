#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/common/include/exception.h>

#include <maps/libs/http/include/test_utils.h>

#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <maps/libs/geolib/include/conversion.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/factory/include/release.h>
#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/polygon_processing.h>

#include <fstream>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

const std::string RELEASES_XML_PATH
    = "maps/wikimap/mapspro/services/autocart/pipeline/libs/factory/tests/xml/releases.xml";

const std::set<Release> RELEASES{
    {493, 3515},
    {492, 3514},
    {491, 3502},
    {490, 3501}
};

const std::map<Release, std::string> RELEASE_TO_SHAPES_ZIP_PATH{
    {
        {493, 3515},
        "maps/wikimap/mapspro/services/autocart/pipeline/libs/factory/tests/shapes/3515.zip"
    },
    {
        {492, 3514},
        "maps/wikimap/mapspro/services/autocart/pipeline/libs/factory/tests/shapes/3514.zip"
    },
    {
        {491, 3502},
        "maps/wikimap/mapspro/services/autocart/pipeline/libs/factory/tests/shapes/3502.zip"
    },
    {
        {490, 3501},
        "maps/wikimap/mapspro/services/autocart/pipeline/libs/factory/tests/shapes/3501.zip"
    }
};

const std::map<Release, ReleaseAttrs> RELEASE_TO_ATTRS{
    {
        {493, 3515},
        {
            ReleaseGeometries{
                {
                    10, 20, // zmin, zmax
                    geolib3::MultiPolygon2({
                        geolib3::Polygon2({
                            {4113859.277215977665, -165329.832523494202},
                            {4121903.843046874274, -165342.165029119788},
                            {4122291.700101250783, -170484.866008121171},
                            {4105678.139713454060, -170945.970861040172},
                            {4105688.545692474581, -165346.935154066305}
                        })
                    })
                },
                {
                    10, 20, // zmin, zmax
                    geolib3::MultiPolygon2({
                        geolib3::Polygon2({
                            {4113685.649957533460, -159478.172365118982},
                            {4121460.053525480442, -159490.014425340487},
                            {4121917.068960102741, -165381.277301790047},
                            {4105689.468074643053, -165386.028584682994},
                            {4105685.209636577871, -159505.636104523990}
                        })
                    })
                },
                {
                    10, 20, // zmin, zmax
                    geolib3::MultiPolygon2({
                        geolib3::Polygon2({
                            {4121032.785096694715, -153708.899108067708},
                            {4121479.732774305623, -159525.201095931436},
                            {4105701.665128611028, -159550.746175086184},
                            {4105683.532485998236, -158519.900661103864},
                            {4105635.757188440301, -158470.828266413562},
                            {4105453.043509955518, -158462.714781132410},
                            {4105439.751951622777, -157200.240307558852},
                            {4105437.958561230917, -155428.295382389624},
                            {4114998.602070060559, -155574.538494411740},
                            {4114998.602070060559, -154810.168210437347},
                            {4119527.495991739444, -154638.184898793930},
                            {4119527.495991739444, -153799.344730732410},
                            {4121031.716707881540, -153708.984141820692}
                        })
                    })
                }
            },
            chrono::TimePoint::clock::now()
        }
    },
    {
        {492, 3514},
        {
            ReleaseGeometries{
                {
                    10, 19, // zmin, zmax
                    geolib3::MultiPolygon2({
                        geolib3::Polygon2({
                            {9457391.460459047928452, 7652899.356302580796182},
                            {9477668.776937454938889, 7652904.605607872828841},
                            {9477667.608796793967485, 7643093.806560727767646},
                            {9457341.197458518669009, 7642478.160858718678355}
                        })
                    })
                }
            },
            chrono::TimePoint::clock::now()
        }
    },
    {
        {491, 3502},
        {
            ReleaseGeometries{
                {
                    10, 19, // zmin, zmax
                    geolib3::MultiPolygon2({
                        geolib3::Polygon2({
                            {3920878.790472259279, 3695295.259103628341},
                            {3917954.559582004789, 3695255.439667525236},
                            {3917980.733905076049, 3698886.077091821469},
                            {3920851.302615751512, 3698853.830220517702},
                        })
                    })
                },
                {
                    10, 19, // zmin, zmax
                    geolib3::MultiPolygon2({
                        geolib3::Polygon2({
                            {3920886.127643956803, 3698831.801598361228},
                            {3919090.616433490068, 3698875.058056296781},
                            {3919089.299510280136, 3698875.074831156526},
                            {3917948.228341954760, 3698876.673195601441},
                            {3917948.228341954760, 3702051.037095791195},
                            {3920862.390045327134, 3701984.154696045443}
                        })
                    })
                }
            },
            chrono::TimePoint::clock::now()
        }
    },
    {
        {490, 3501},
        {
            ReleaseGeometries{
                {
                    10, 19, // zmin, zmax
                    geolib3::MultiPolygon2({
                        geolib3::Polygon2({
                            {4473907.320550509728, 6875223.287995749153},
                            {4473920.590375781991, 6875224.173633566126},
                            {4474164.417376193218, 6865551.509173016995},
                            {4458421.277206514962, 6865766.707207107916},
                            {4458201.132717987522, 6874993.632726231590},
                            {4473902.929040368646, 6875223.112694355659}
                        })
                    })
                }
            },
            chrono::TimePoint::clock::now()
        }
    }
};

http::MockResponse
makeMockResponseFromBinaryFile(const std::string& path)
{
    std::ifstream ifs(path, std::ios::binary);
    REQUIRE(ifs.is_open(), "Failed to open file: " + path);
    std::string blob(std::istreambuf_iterator<char>(ifs), {});
    ifs.close();
    return http::MockResponse(blob);
}

} // namespace

Y_UNIT_TEST_SUITE(proto_factory_service_client_should)
{

Y_UNIT_TEST(test_get_all_releses)
{
    http::MockHandle processMockHandle = http::addMock(
        ProtoFactoryServiceClient::FACTORY_URL + "/v1/releases/search",
        [&](const http::MockRequest&) {
            return makeMockResponseFromBinaryFile(SRC_("proto_responses/releases_search.pb"));
        }
    );

    ProtoFactoryServiceClient factoryClient;

    auto releases = factoryClient.getAllReleases();

    ASSERT_EQ(releases.size(), 5u);
    auto firstRelease = releases.begin();
    EXPECT_EQ(firstRelease->releaseId, 3504);
    EXPECT_EQ(firstRelease->issueId, 486);
}

Y_UNIT_TEST(test_get_relese_geometries)
{
    http::MockHandle processMockHandle = http::addMock(
        ProtoFactoryServiceClient::FACTORY_URL + "/v1/mosaics/search",
        [&](const http::MockRequest&) {
            return makeMockResponseFromBinaryFile(SRC_("proto_responses/mosaics_search.pb"));
        }
    );

    ProtoFactoryServiceClient factoryClient;

    auto geometries = factoryClient.loadReleaseGeometries(3504);

    ASSERT_EQ(geometries.size(), 2u);
    auto firstGeom = geometries.begin();
    EXPECT_EQ(firstGeom->zmin, 10);
    EXPECT_EQ(firstGeom->zmax, 17);
    EXPECT_NEAR(firstGeom->mercatorGeom.area(), 3538525880, 10);
}



} // Y_UNIT_TEST_SUITE(test_proto_factory_service_client)

Y_UNIT_TEST_SUITE(factory_releases_tests)
{

Y_UNIT_TEST(shape_test)
{
    const Release release{493, 3515};

    http::MockHandle processMockHandle = http::addMock(
        XMLFactoryServiceClient::FACTORY_URL + "/boundary-exporter/?id=release:" + std::to_string(release.releaseId) + "&format=shape",
        [&](const http::MockRequest&) {
            std::ifstream ifs(BinaryPath(RELEASE_TO_SHAPES_ZIP_PATH.at(release)), std::ios::binary);
            REQUIRE(ifs.is_open(), "Failed to open file: " + RELEASE_TO_SHAPES_ZIP_PATH.at(release));
            std::string blob(std::istreambuf_iterator<char>(ifs), {});
            ifs.close();
            return http::MockResponse(blob);
        }
    );
    XMLFactoryServiceClient factoryClient;
    const ReleaseGeometries testGeometries = factoryClient.loadReleaseGeometries(release.releaseId);

    const ReleaseGeometries& gtGeometries = RELEASE_TO_ATTRS.at(release).geometries;
    EXPECT_TRUE(
        std::is_permutation(
            gtGeometries.begin(), gtGeometries.end(),
            testGeometries.begin(),
            [](const ReleaseGeometry& lhs, const ReleaseGeometry& rhs) {
                return lhs.zmin == rhs.zmin
                    && lhs.zmax == rhs.zmax
                    && geolib3::test_tools::approximateEqual(
                           lhs.mercatorGeom, rhs.mercatorGeom, geolib3::EPS);
            }
        )
    );
}

Y_UNIT_TEST(releases_list_test)
{
    http::MockHandle processMockHandle = http::addMock(
        XMLFactoryServiceClient::FACTORY_URL + "/satrep/GetTreeNodes?node_id=release-status:production/releases",
        [&](const http::MockRequest&) {
            std::ifstream ifs(BinaryPath(RELEASES_XML_PATH), std::ios::binary);
            REQUIRE(ifs.is_open(), "Failed to open file: " + RELEASES_XML_PATH);
            std::string blob(std::istreambuf_iterator<char>(ifs), {});
            ifs.close();
            return http::MockResponse(blob);
        }
    );

    XMLFactoryServiceClient factoryClient;

    Release release3502 = *std::find_if(
        RELEASES.begin(), RELEASES.end(),
        [&](const Release& release) {
            return release.releaseId == 3502;
        }
    );
    std::optional<Release> testRelease3502 = getReleaseByIssueId(factoryClient, 491);
    EXPECT_TRUE(testRelease3502.has_value());
    EXPECT_TRUE(testRelease3502.value() == release3502);

    std::set<Release> testReleases = factoryClient.getAllReleases();
    EXPECT_TRUE(RELEASES == testReleases);

    std::set<Release> nextReleases = getAllNextReleases(factoryClient, 3502);
    EXPECT_TRUE(nextReleases == std::set<Release>({{492, 3514}, {493, 3515}}));
}

Y_UNIT_TEST(all_releases_geometries_test)
{
    http::MockHandle xmlMockHandle = http::addMock(
        XMLFactoryServiceClient::FACTORY_URL + "/satrep/GetTreeNodes?node_id=release-status:production/releases",
        [&](const http::MockRequest&) {
            std::ifstream ifs(BinaryPath(RELEASES_XML_PATH), std::ios::binary);
            REQUIRE(ifs.is_open(), "Failed to open file: " + RELEASES_XML_PATH);
            std::string blob(std::istreambuf_iterator<char>(ifs), {});
            ifs.close();
            return http::MockResponse(blob);
        }
    );

    http::MockHandle zipMockHandle = http::addMock(
        XMLFactoryServiceClient::FACTORY_URL + "/boundary-exporter/",
        [&](const http::MockRequest& request) {
            std::string path = "";
            for (const Release& release : RELEASES) {
                if (request.url.params() == "id=release%3A" + std::to_string(release.releaseId) + "&format=shape") {
                    path = RELEASE_TO_SHAPES_ZIP_PATH.at(release);
                    break;
                }
            }
            REQUIRE(!path.empty(), "Failed mock request with params: " +request.url.params());
            std::ifstream ifs(BinaryPath(path), std::ios::binary);
            REQUIRE(ifs.is_open(), "Failed to open file: " + path);
            std::string blob(std::istreambuf_iterator<char>(ifs), {});
            ifs.close();
            return http::MockResponse(blob);
        }
    );

    XMLFactoryServiceClient factoryClient;

    std::map<Release, ReleaseGeometries> testReleaseToGeometries
        = loadAllReleasesGeometries(factoryClient);

    for (const auto& [release, gtAttrs] : RELEASE_TO_ATTRS) {
        auto testIt = testReleaseToGeometries.find(release);
        EXPECT_TRUE(testIt != testReleaseToGeometries.end());
        const ReleaseGeometries& testGeometries = testIt->second;
        EXPECT_TRUE(
            std::is_permutation(
                gtAttrs.geometries.begin(), gtAttrs.geometries.end(),
                testGeometries.begin(),
                [](const ReleaseGeometry& lhs, const ReleaseGeometry& rhs) {
                    return lhs.zmin == rhs.zmin
                        && lhs.zmax == rhs.zmax
                        && geolib3::test_tools::approximateEqual(
                               lhs.mercatorGeom, rhs.mercatorGeom, geolib3::EPS);
                }
            )
        );
    }
}

Y_UNIT_TEST(coverage_at_zoom_test)
{
    const int zoom = 20;
    std::vector<geolib3::MultiPolygon2> gtMultiPolygons;
    for (const ReleaseGeometry& geometry : RELEASE_TO_ATTRS.at({493, 3515}).geometries) {
        gtMultiPolygons.push_back(geometry.mercatorGeom);
    }
    const geolib3::MultiPolygon2 gtGeometry
        = mergeMultiPolygons(gtMultiPolygons);

    geolib3::MultiPolygon2 testGeometry
        = getReleasesCoverageAtZoom(RELEASE_TO_ATTRS, zoom);
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    gtGeometry, testGeometry, geolib3::EPS));
}

Y_UNIT_TEST(yt_node_test)
{
    const Release RELEASE{28071914, 11111918};

    Release testRelease = Release::fromYTNode(RELEASE.toYTNode());

    EXPECT_EQ(RELEASE.releaseId, testRelease.releaseId);
    EXPECT_EQ(RELEASE.issueId, testRelease.issueId);

    const ReleaseGeometry GEOMETRY{
        10, 18, // zmin, zmax
        geolib3::MultiPolygon2({
            geolib3::Polygon2({
                {0., 0.},
                {10000., 0.},
                {10000., 10000.},
                {0., 10000.}
            })
        })
    };

    ReleaseGeometry testGeometry = ReleaseGeometry::fromYTNode(GEOMETRY.toYTNode());

    EXPECT_EQ(GEOMETRY.zmin, testGeometry.zmin);
    EXPECT_EQ(GEOMETRY.zmax, testGeometry.zmax);
    EXPECT_TRUE(
        geolib3::test_tools::approximateEqual(
            GEOMETRY.mercatorGeom, testGeometry.mercatorGeom,
            geolib3::EPS
        )
    );

    const ReleasesCoverage COVERAGE{
        18,
        {134, 456},
        geolib3::MultiPolygon2({
            geolib3::Polygon2({
                {0., 0.},
                {100000., 0.},
                {100000., 100000.},
                {0., 100000.}
            })
        })
    };

    ReleasesCoverage testCoverage = ReleasesCoverage::fromYTNode(COVERAGE.toYTNode());

    EXPECT_EQ(COVERAGE.z, testCoverage.z);
    EXPECT_EQ(COVERAGE.lastRelease.releaseId, testCoverage.lastRelease.releaseId);
    EXPECT_EQ(COVERAGE.lastRelease.issueId, testCoverage.lastRelease.issueId);
    EXPECT_TRUE(
        geolib3::test_tools::approximateEqual(
            COVERAGE.mercatorGeom, testCoverage.mercatorGeom,
            geolib3::EPS
        )
    );
}

Y_UNIT_TEST(split_not_ready_releases_test)
{
    const std::map<Release, ReleaseAttrs> releaseToAttrs{
        {
            {493, 3515},
            {
                ReleaseGeometries{
                    {
                        10, 20, // zmin, zmax
                        geolib3::MultiPolygon2({
                            geolib3::Polygon2({{0, 0}, {0, 1}, {1, 1}, {1, 0}})
                        })
                    },
                },
                chrono::TimePoint::clock::now() - std::chrono::hours(24 * 7 * 3 + 1)
            }
        },
        {
            {494, 3516},
            {
                ReleaseGeometries{
                    {
                        10, 20, // zmin, zmax
                        geolib3::MultiPolygon2({
                            geolib3::Polygon2({{0, 0}, {0, 1}, {1, 1}, {1, 0}})
                        })
                    },
                },
                chrono::TimePoint::clock::now() - std::chrono::hours(24 * 7 * 3 - 1)
            }
        },
        {
            {495, 3517},
            {
                ReleaseGeometries{
                    {
                        10, 20, // zmin, zmax
                        geolib3::MultiPolygon2({
                            geolib3::Polygon2({{0, 0}, {0, 1}, {1, 1}, {1, 0}})
                        })
                    },
                },
                chrono::TimePoint::clock::now() - std::chrono::hours(24 * 7 * 3 + 3)
            }
        },
    };

    auto [newReleaseToAttrs, notReadyReleaseToAttrs]
        = splitNotReadyReleases(releaseToAttrs, 24 * 7 * 3 /* 3 weeks */);

    EXPECT_EQ(newReleaseToAttrs.size(), 2u);
    EXPECT_EQ(notReadyReleaseToAttrs.size(), 1u);
}

} // Y_UNIT_TEST_SUITE(factory_releases_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
