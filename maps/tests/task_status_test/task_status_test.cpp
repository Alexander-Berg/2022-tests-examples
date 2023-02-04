#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/rtp/impl/task_status.h>
#include <maps/analyzer/libs/rtp/impl/internal_types.h>

#include "user_types.h"

#include <boost/test/auto_unit_test.hpp>
#include <boost/test/floating_point_comparison.hpp>
#include <boost/test/test_tools.hpp>

namespace rtp = maps::analyzer::rtp;
typedef rtp::impl::InternalTypes<UserTypes>::TaskStatus TaskStatus;
typedef rtp::impl::InternalTypes<UserTypes>::TaskPriorityQueue TaskPriorityQueue;
typedef UserTypes::ProcessedInfo ProcessedInfo;

BOOST_AUTO_TEST_CASE(empty_test) {
    TaskStatus status;
    BOOST_CHECK(status.processedInfo() == 0);
    BOOST_CHECK_EQUAL(status.inProgress(), true);
    BOOST_CHECK_THROW(status.queuePosition(), maps::Exception);
}

BOOST_AUTO_TEST_CASE(set_get_processed_info_test) {
    TaskStatus status;
    ProcessedInfo* info = new ProcessedInfo();
    status.setProcessedInfo(std::unique_ptr<ProcessedInfo>(info));
    BOOST_CHECK_EQUAL(status.processedInfo(), info);
    BOOST_CHECK_THROW(status.setProcessedInfo(
                        std::unique_ptr<ProcessedInfo>(new ProcessedInfo())),
                      maps::LogicError);
}

BOOST_AUTO_TEST_CASE(processing_test) {
    TaskStatus status;
    BOOST_CHECK(status.inProgress());
    TaskPriorityQueue queue;

    //set iterator first time
    status.setQueuePosition(queue.begin());
    BOOST_CHECK_EQUAL(status.inProgress(), false);
    BOOST_CHECK(status.queuePosition() == queue.begin());

    //start processing
    status.startProcessing();
    BOOST_CHECK_EQUAL(status.inProgress(), true);
    BOOST_CHECK_THROW(status.queuePosition(), maps::Exception);

    //end processing
    status.setQueuePosition(queue.begin());
    BOOST_CHECK_EQUAL(status.inProgress(), false);
    BOOST_CHECK(status.queuePosition() == queue.begin());
}

