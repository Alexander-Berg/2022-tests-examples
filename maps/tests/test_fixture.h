#pragma once

#include <yandex/maps/log8.h>
#include <yandex/maps/wiki/common/extended_xml_doc.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/commit_manager.h>
#include <yandex/maps/wiki/revision/common.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/unittest/unittest.h>

#include <future>
#include <string>
#include <thread>
#include <vector>

const std::string POI_BUSINESS_ID_KEY = "poi:business_id";

struct TestLoggingSetup {
    TestLoggingSetup();
};

//! Helper class to create object revision data
class Data : public maps::wiki::revision::RevisionsGateway::NewRevisionData {
public:
    static Data createObj(maps::wiki::revision::DBID objectId,
                          const std::string& POI);
    static Data updatePOI(maps::wiki::revision::DBID commitId,
                          maps::wiki::revision::DBID objectId,
                          const std::string& POI);
    static Data deletePOI(maps::wiki::revision::DBID commitId,
                          maps::wiki::revision::DBID objectId);
    static Data deleteObj(maps::wiki::revision::DBID commitId,
                          maps::wiki::revision::DBID objectIdq);

    // create new object
    Data(maps::wiki::revision::DBID objectId,
         const maps::wiki::revision::Attributes& attrs);

private:
    // update object's attributes
    Data(const maps::wiki::revision::RevisionID& revisionId,
         const maps::wiki::revision::Attributes& attrs);

    // delete object's attribtues or object itself
    Data(const maps::wiki::revision::RevisionID& revisionId, bool deleteObj);
};
typedef std::vector<Data> DataCollection;

class AltayCommitNotifierFixture
    : public maps::wiki::unittest::DatabaseFixture {
public:
    AltayCommitNotifierFixture();

    //! Create approved commit
    //! \return new commit id
    maps::wiki::revision::DBID createCommit(const DataCollection& newRev);

    bool checkHTTPRequest(const std::string& request,
                          const std::string& method,
                          maps::wiki::revision::DBID mapsId,
                          const std::string& altayId) const;

    ~AltayCommitNotifierFixture() {}

private:
    //! Wait while there are no in-order commits in DB
    void pubsubBarrier();

    std::int64_t txidSnapshotXMin();

    std::int64_t txidSnapshotXMax();
};
