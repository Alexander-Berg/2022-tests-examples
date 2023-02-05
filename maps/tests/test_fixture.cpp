#include "test_fixture.h"

#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/asio.hpp>
#include <boost/format.hpp>
#include <boost/test/detail/unit_test_parameters.hpp>
#include <boost/test/unit_test.hpp>

#include <cctype>
#include <chrono>

namespace asio = boost::asio;
using namespace maps::wiki;

namespace {
const std::string CONFIG_PATH = "tests/services.xml";
const maps::wiki::revision::UserID TEST_UID = 13;
const std::string TAG = "altay-commit-notifier";

const maps::wiki::revision::Attributes COMMIT_ATTRS
    = {{"description", "test"}};
//! Workaround absense of the approved branch in DB after initialization
const char* INSERT_APPROVED_BRANCH = "INSERT INTO revision.branch (id, type, "
                                     "state, created_by) VALUES (1, "
                                     "'approved', 'normal', 1)";
}

TestLoggingSetup::TestLoggingSetup()
{
    using namespace boost::unit_test;
    if (log_test_units < runtime_config::log_level())
        unit_test_log.set_threshold_level(log_test_units);

    maps::log8::setBackend(maps::log8::toSyslog(TAG));
}

Data Data::createObj(revision::DBID objectId, std::string const& POI)
{
    if (POI.size())
        return {objectId, {{POI_BUSINESS_ID_KEY, POI}}};
    else
        return {objectId, {{"cat:poi", "1"}}};
}

Data Data::updatePOI(revision::DBID commitId,
                     revision::DBID objectId,
                     const std::string& POI)
{
    return {{objectId, commitId}, {{POI_BUSINESS_ID_KEY, POI}}};
}

Data Data::deletePOI(revision::DBID commitId, revision::DBID objectId)
{
    return {{objectId, commitId}, {{"cat:poi", "1"}}};
}

Data Data::deleteObj(revision::DBID commitId, revision::DBID objectId)
{
    return {{objectId, commitId}, true};
}

Data::Data(revision::DBID objectId, const revision::Attributes& attrs)
    : revision::RevisionsGateway::NewRevisionData(
          revision::RevisionID::createNewID(objectId),
          revision::ObjectData(attrs, boost::none, boost::none, boost::none))
{
}

Data::Data(const revision::RevisionID& revisionId,
           const revision::Attributes& attrs)
    : revision::RevisionsGateway::NewRevisionData(
          revisionId,
          revision::ObjectData(attrs, boost::none, boost::none, boost::none))
{
}

Data::Data(const revision::RevisionID& prevId, bool deleteObj)
    : revision::RevisionsGateway::NewRevisionData(
          prevId,
          revision::ObjectData(
              boost::none, boost::none, boost::none, boost::none, deleteObj))
{
}

AltayCommitNotifierFixture::AltayCommitNotifierFixture()
    : unittest::DatabaseFixture(CONFIG_PATH, TAG)
{
    auto txn = pool().masterWriteableTransaction();
    txn->exec(INSERT_APPROVED_BRANCH);
    txn->commit();
}

revision::DBID
AltayCommitNotifierFixture::createCommit(const DataCollection& newRev)
{
    auto txn = pool().masterWriteableTransaction();
    auto branch
        = revision::BranchManager(*txn).load(revision::TRUNK_BRANCH_ID);
    revision::RevisionsGateway gateway(*txn, branch);
    revision::Commit commit
        = gateway.createCommit(newRev, TEST_UID, COMMIT_ATTRS);
    revision::CommitManager(*txn).approve({commit.id()});
    txn->commit();
    pubsubBarrier();
    return commit.id();
}

void AltayCommitNotifierFixture::pubsubBarrier()
{
    const std::int64_t currentMaxTxnId = txidSnapshotXMax();
    while (txidSnapshotXMin() <= currentMaxTxnId) {
        BOOST_TEST_MESSAGE("waiting in pubsub barrier");
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
}

std::int64_t AltayCommitNotifierFixture::txidSnapshotXMin()
{
    auto txn = pool().masterReadOnlyTransaction();
    auto res
        = txn->exec("SELECT txid_snapshot_xmin(txid_current_snapshot())");
    return res[0][0].as<std::int64_t>();
}

std::int64_t AltayCommitNotifierFixture::txidSnapshotXMax()
{
    auto txn = pool().masterReadOnlyTransaction();
    auto res
        = txn->exec("SELECT txid_snapshot_xmax(txid_current_snapshot())");
    return res[0][0].as<std::int64_t>();
}

bool AltayCommitNotifierFixture::checkHTTPRequest(
    const std::string& request,
    const std::string& method,
    revision::DBID mapsId,
    const std::string& altayId) const
{
    std::string pattern = (boost::format("%1% /v1.0.0/signal/%2%/"
                                         "provider_link?provider_name=nyak_"
                                         "original_id_handle&original_id=%3%")
                           % method % altayId % mapsId).str();
    return request.find(pattern) != std::string::npos;
}
