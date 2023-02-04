#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <maps/libs/common/include/exception.h>

#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/geolib/include/spatial_relation.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <yandex/maps/wiki/common/robot.h>

#include <maps/libs/http/include/test_utils.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/json/include/builder.h>

#include <maps/wikimap/mapspro/libs/editor_client/include/instance.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/nmaps/include/publish_nmaps_blds.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/storage/include/publication_results.h>

#include <maps/wikimap/mapspro/libs/editor_client/include/instance.h>

#include <maps/libs/common/include/file_utils.h>

#include <util/folder/tempdir.h>

using namespace testing;
using namespace std::literals::chrono_literals;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(publish_nmaps_blds_tests)
{

Y_UNIT_TEST(basic_test)
{
    const std::string READER_TEST_URL = "http://core-nmaps-editor.common.testing.maps.yandex.net";
    const std::string WRITER_TEST_URL = "http://core-nmaps-editor.common.testing.maps.yandex.net";
    const TTempDir STATE_DIR;
    const std::string STATE_PATH = maps::common::joinPath(STATE_DIR.Name(), "state.json");
    const size_t STATE_STEP = 1u;

    std::vector<Building> blds{
        // in the Caucasus mountains
        Building::fromGeodeticGeom(
            geolib3::Polygon2({
                {42.474172, 43.354393},
                {42.474333, 43.353791},
                {42.475245, 43.353806},
                {42.475169, 43.354409}
            })
        ),
        // in Moscow
        Building::fromGeodeticGeom(
            geolib3::Polygon2({
                {37.622649 ,55.757193},
                {37.622424, 55.755257},
                {37.626511, 55.755184},
                {37.626093, 55.757029}
            })
        )
    };

    auto saveMockHandle = http::addMock(
        WRITER_TEST_URL + "/objects",
        [&](const http::MockRequest& request) {
            json::Builder builder;
            builder << [&](json::ObjectBuilder b) {
                b["geoObjects"] = [&](json::ArrayBuilder b) {
                    b << [&](json::ObjectBuilder b) {
                        b["id"] = "1";
                        b["revisionId"] = "6:7";
                        b["categoryId"] = "bld";
                        b["state"] = "draft";
                        b["attrs"] = [](json::ObjectBuilder) {};
                        b["geometry"] = json::Value::fromString(request.body)["geometry"];
                    };
                };
                b["token"] = "token";
            };
            return http::MockResponse(builder.str());
        });

    auto lassoMockHandle = http::addMock(
        READER_TEST_URL + "/objects/query/lasso",
        [&](const http::MockRequest& request) {
            geolib3::Polygon2 polygon
                = geolib3::readGeojson<geolib3::Polygon2>(json::Value::fromString(request.body));
            if (geolib3::spatialRelation(polygon, blds[0].toGeodeticGeom(),
                                         geolib3::SpatialRelation::Intersects)) {
                json::Builder builder;
                builder << [&](json::ObjectBuilder b) {
                    b["geoObjects"] << [&](json::ArrayBuilder) {};
                };
                return http::MockResponse(builder.str());
            } else {
                json::Builder builder;
                builder << [&](json::ObjectBuilder b) {
                    b["geoObjects"] << [&](json::ArrayBuilder b) {
                        b << [&](json::ObjectBuilder b) {
                            b["id"] = "3";
                            b["revisionId"] = "4:5";
                            b["categoryId"] = "rd_el";
                        };
                    };
                };
                return http::MockResponse(builder.str());
            }
        });

    BaseResult baseResult1;
    baseResult1.id = 1;
    baseResult1.bld = blds[0];

    BaseResult baseResult2;
    baseResult2.id = 2;
    baseResult2.bld = blds[1];

    editor_client::Instance readerClient(READER_TEST_URL, common::WIKIMAPS_BLD_UID);
    editor_client::Instance writerClient(WRITER_TEST_URL, common::WIKIMAPS_BLD_UID);

    std::vector<PublicationResult> publicationResults
        = publishBuildingsToNMaps(
            {baseResult1, baseResult2},
            readerClient, writerClient,
            common::WIKIMAPS_BLD_UID,
            PublicationTimeRange::allDay(),
            STATE_PATH, STATE_STEP
        );

    // check published building
    EXPECT_EQ(publicationResults.size(), 2u);

    std::optional<PublicationResult> publishedResult;
    std::optional<PublicationResult> rejectedResult;
    for (const PublicationResult& publicationResult : publicationResults) {
        if (PublicationStatus::Published == publicationResult.status) {
            publishedResult = publicationResult;
        } else {
            rejectedResult = publicationResult;
        }
    }

    // check published results
    EXPECT_TRUE(publishedResult.has_value());
    EXPECT_EQ(publishedResult->id, baseResult1.id);
    EXPECT_EQ(publishedResult->bld.getId(), 1u);
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    publishedResult->bld.toGeodeticGeom(),
                    baseResult1.bld.toGeodeticGeom(), geolib3::EPS));

    // check rejected results
    EXPECT_TRUE(rejectedResult.has_value());
    EXPECT_EQ(rejectedResult->id, baseResult2.id);
    EXPECT_EQ(rejectedResult->status, PublicationStatus::Intersects);
    EXPECT_TRUE(!rejectedResult->bld.hasId());
    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    rejectedResult->bld.toGeodeticGeom(),
                    baseResult2.bld.toGeodeticGeom(), geolib3::EPS));
}

Y_UNIT_TEST(unavailable_nmaps_test)
{
    const std::string READER_TEST_URL = "http://core-nmaps-editor.common.testing.maps.yandex.net";
    const std::string WRITER_TEST_URL = "http://core-nmaps-editor.common.testing.maps.yandex.net";

    const TTempDir STATE_DIR;
    const std::string STATE_PATH = maps::common::joinPath(STATE_DIR.Name(), "state.json");
    const size_t STATE_STEP = 1u;

    auto mockHandle = http::addMock(
        READER_TEST_URL + "/objects/query/lasso",
        [](const http::MockRequest&) {
            return http::MockResponse::withStatus(500);
        }
    );

    // in the Caucasus mountains
    Building bld = Building::fromGeodeticGeom(
        geolib3::Polygon2({
            {42.474172, 43.354393},
            {42.474333, 43.353791},
            {42.475245, 43.353806},
            {42.475169, 43.354409}
        })
    );

    BaseResult baseResult;
    baseResult.id = 1;
    baseResult.bld = bld;

    editor_client::Instance readerClient(READER_TEST_URL, common::WIKIMAPS_BLD_UID);
    editor_client::Instance writerClient(WRITER_TEST_URL, common::WIKIMAPS_BLD_UID);

    std::vector<PublicationResult> publicationResults
        = publishBuildingsToNMaps(
            {baseResult},
            readerClient, writerClient,
            common::WIKIMAPS_BLD_UID,
            PublicationTimeRange::allDay(),
            STATE_PATH, STATE_STEP
        );

    EXPECT_EQ(publicationResults.size(), 1u);

    EXPECT_EQ(publicationResults.front().id, baseResult.id);
    EXPECT_EQ(publicationResults.front().status, PublicationStatus::Error);
    EXPECT_TRUE(
        geolib3::test_tools::approximateEqual(
            publicationResults.front().bld.toGeodeticGeom(),
            bld.toGeodeticGeom(),
            geolib3::EPS
        )
    );
}

Y_UNIT_TEST(time_range_test)
{
    const int START_HOUR = 18;
    const int END_HOUR = 2;
    const PublicationTimeRange TIME_RANGE{START_HOUR, END_HOUR};
    const std::string DATE_FORMAT = "%Y-%m-%d %H:%M:%S";

    EXPECT_TRUE(
        TIME_RANGE.checkIsInRange(
            chrono::parseIntegralDateTime("2029-01-23 18:15:45", DATE_FORMAT)
        )
    );
    EXPECT_TRUE(
        TIME_RANGE.checkIsInRange(
            chrono::parseIntegralDateTime("2019-01-23 19:15:45", DATE_FORMAT)
        )
    );
    EXPECT_TRUE(
        TIME_RANGE.checkIsInRange(
            chrono::parseIntegralDateTime("2019-01-23 23:15:45", DATE_FORMAT)
        )
    );
    EXPECT_TRUE(
        TIME_RANGE.checkIsInRange(
            chrono::parseIntegralDateTime("2019-01-23 01:15:45", DATE_FORMAT)
        )
    );

    EXPECT_FALSE(
        TIME_RANGE.checkIsInRange(
            chrono::parseIntegralDateTime("2019-01-23 03:15:45", DATE_FORMAT)
        )
    );
    EXPECT_FALSE(
        TIME_RANGE.checkIsInRange(
            chrono::parseIntegralDateTime("2019-07-23 03:15:45", DATE_FORMAT)
        )
    );
    EXPECT_FALSE(
        TIME_RANGE.checkIsInRange(
            chrono::parseIntegralDateTime("2019-09-23 16:15:45", DATE_FORMAT)
        )
    );


    EXPECT_EQ(
        TIME_RANGE.getMinutesToStart(
            chrono::parseIntegralDateTime("2019-09-23 20:26:45", DATE_FORMAT)
        ),
        0min
    );
    EXPECT_EQ(
        TIME_RANGE.getMinutesToStart(
            chrono::parseIntegralDateTime("2019-09-23 01:29:45", DATE_FORMAT)
        ),
        0min
    );
    EXPECT_EQ(
        TIME_RANGE.getMinutesToStart(
            chrono::parseIntegralDateTime("2019-09-23 16:15:45", DATE_FORMAT)
        ),
        1h + 45min
    );
    EXPECT_EQ(
        TIME_RANGE.getMinutesToStart(
            chrono::parseIntegralDateTime("2019-09-23 02:46:45", DATE_FORMAT)
        ),
        15h + 14min
    );
}

} // Y_UNIT_TEST_SUITE(publish_nmaps_blds_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
