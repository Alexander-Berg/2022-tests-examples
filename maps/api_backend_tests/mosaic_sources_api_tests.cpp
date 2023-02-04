#include <maps/factory/services/sputnica_back/tests/common/common.h>
#include <maps/factory/services/sputnica_back/tests/api_backend_tests/fixture.h>
#include <maps/factory/services/sputnica_back/tests/common/test_data.h>
#include <maps/factory/services/sputnica_back/lib/yacare_helpers.h>

#include <maps/factory/libs/db/mosaic_source.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_status_event.h>
#include <maps/factory/libs/db/mosaic_source_status_event_gateway.h>
#include <maps/factory/libs/db/mosaic_source_pipeline.h>
#include <maps/factory/libs/sproto_helpers/mosaic_source.h>

#include <maps/libs/common/include/exception.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/geolib/include/polygon.h>
#include <yandex/maps/geolib3/sproto.h>
#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <boost/lexical_cast.hpp>

namespace maps::factory::sputnica::tests {

Y_UNIT_TEST_SUITE_F(mosaic_sources_api_tests, Fixture) {

Y_UNIT_TEST(test_mosaic_source_api)
{
    int64_t orderId;
    int64_t aoiId;
    int64_t mosaicSourceId;

    //creating new order, and aoi bound to it
    {
        pqxx::connection conn(postgres().connectionString());
        pqxx::work txn(conn);

        auto mosaicSource = makeNewMosaicSource(txn);
        orderId = *mosaicSource.orderId();
        aoiId = *mosaicSource.aoiId();
        mosaicSourceId = mosaicSource.id();
        txn.commit();
    }

    //searching mosaic sources by filter
    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/mosaic-sources/search")
                .addParam("filter", "order:" + std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoMosaicSources = boost::lexical_cast<sproto_helpers::smosaics::MosaicSources>(resp.body);
        ASSERT_EQ(sprotoMosaicSources.mosaicSources().size(), 1u);
        const auto& firstSource = sprotoMosaicSources.mosaicSources()[0];
        EXPECT_EQ(firstSource.id(), std::to_string(mosaicSourceId));
        EXPECT_EQ(firstSource.name(), MOSAIC_NAME);
        EXPECT_EQ(*firstSource.status(), sproto_helpers::smosaics::MosaicSource::Status::NEW);
        EXPECT_EQ(firstSource.satellite(), MOSAIC_SATELLITE);
        EXPECT_EQ(
            geolib3::sproto::decode(firstSource.boundingBox()),
            MOSAIC_SOURCE_GEODETIC_BOUNDING_BOX
        );
    }

    //getting mosaic source with its status history by mosaicSourceId
    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/mosaic-sources/get")
                .addParam("mosaicSourceId", std::to_string(mosaicSourceId))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoHeavySource = boost::lexical_cast<sproto_helpers::smosaics::HeavyMosaicSource>(resp.body);
        ASSERT_EQ(sprotoHeavySource.mosaicSource().id(), std::to_string(mosaicSourceId));
        ASSERT_EQ(sprotoHeavySource.statusEvents().size(), 1u);
        EXPECT_EQ(
            sproto_helpers::convertFromSproto(*sprotoHeavySource.statusEvents()[0].status()),
            db::MosaicSourceStatus::New
        );
        EXPECT_EQ(
            sprotoHeavySource.statusEvents()[0].createdBy(),
            std::to_string(db::ROBOT_USER_ID)
        );

        EXPECT_EQ(sprotoHeavySource.mosaicSource().geometry().size(), 1u);
        ASSERT_EQ(sprotoHeavySource.mosaicSource().metadata().size(), 3u);
        EXPECT_EQ(sprotoHeavySource.mosaicSource().metadata()[0].key(), "SUNELEVATION");
        EXPECT_EQ(sprotoHeavySource.mosaicSource().metadata()[0].value(), "31337");
    }

    {
        /*
         * The entire MosaicSourceStatus automata is not testable,
         * so only the following status transition will be tested:
         *   New -> Rejected -> RejectedDeclined -> Accepted
         */
        sproto_helpers::smosaics::MosaicSourceStatusMessage statusMessage;
        http::MockRequest rq(
            http::PATCH,
            http::URL("http://localhost/mosaic-sources/set-status")
                .addParam("mosaicSourceId", std::to_string(mosaicSourceId))
        );
        setAuthHeaderFor(db::Role::Supplier, rq);
        //unallowed status transition
        statusMessage.status() = sproto_helpers::smosaics::MosaicSource::Status::EXTEND_BOUNDARY_DECLINED;
        rq.body = boost::lexical_cast<std::string>(statusMessage);
        EXPECT_EQ(
            yacare::performTestRequest(rq).status,
            403
        );

        setAuthHeaderFor(db::Role::Customer, rq);
        //rejection reason was not set
        statusMessage.status() = sproto_helpers::smosaics::MosaicSource::Status::REJECTED;
        rq.body = boost::lexical_cast<std::string>(statusMessage);
        EXPECT_EQ(
            yacare::performTestRequest(rq).status,
            400
        );

        statusMessage.rejectionReason() = "Let it snow, let it snow, let it snow!";
        rq.body = boost::lexical_cast<std::string>(statusMessage);

        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        auto sprotoHeavySource = boost::lexical_cast<sproto_helpers::smosaics::HeavyMosaicSource>(resp.body);
        //statusEvents are: [parsed, new, rejected]
        EXPECT_EQ(sprotoHeavySource.statusEvents().size(), 2u);

        setAuthHeaderFor(db::Role::Supplier, rq);
        statusMessage.status() = sproto_helpers::smosaics::MosaicSource::Status::REJECTED_DECLINED;
        rq.body = boost::lexical_cast<std::string>(statusMessage);
        resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        //statusEvents are [parsed, new, rejected, rejected_declined]

        setAuthHeaderFor(db::Role::Customer, rq);
        statusMessage.status() = sproto_helpers::smosaics::MosaicSource::Status::ACCEPTED;
        rq.body = boost::lexical_cast<std::string>(statusMessage);
        resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        sprotoHeavySource = boost::lexical_cast<sproto_helpers::smosaics::HeavyMosaicSource>(resp.body);
        EXPECT_EQ(
            *sprotoHeavySource.mosaicSource().status(),
            sproto_helpers::smosaics::MosaicSource::Status::ACCEPTED
        );
        //statusEvents are [parsed, new, rejected, rejected_declined, accepted, ready]
        const auto& statusEvents = sprotoHeavySource.statusEvents();
        ASSERT_EQ(statusEvents.size(), 4u);
        EXPECT_EQ(*statusEvents[0].status(), sproto_helpers::smosaics::MosaicSource::Status::NEW);
        EXPECT_EQ(*statusEvents[1].status(), sproto_helpers::smosaics::MosaicSource::Status::REJECTED);
        EXPECT_EQ(*statusEvents[2].status(),
            sproto_helpers::smosaics::MosaicSource::Status::REJECTED_DECLINED);
        EXPECT_EQ(*statusEvents[3].status(), sproto_helpers::smosaics::MosaicSource::Status::ACCEPTED);
    }
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::factory::sputnica::tests
