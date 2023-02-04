#include "helpers.h"

#include <yandex/maps/wiki/validator/message.h>

#include <yandex/maps/wiki/revision/filters.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/snapshot.h>
#include <yandex/maps/wiki/revisionapi/revisionapi.h>
#include <yandex/maps/wiki/unittest/localdb.h>

#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/geolib/include/variant.h>

#include <library/cpp/testing/unittest/env.h>

#include <algorithm>
#include <fstream>
#include <vector>

namespace gl = maps::geolib3;
namespace rev = maps::wiki::revision;
namespace revapi = maps::wiki::revisionapi;

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

namespace {

const TString EDITOR_CONFIG_PATH =
    ArcadiaSourceRoot() + "/maps/wikimap/mapspro/cfg/editor/editor.xml";

inline std::string wkt2wkb(const std::string& wkt)
{
    auto geometryVariant = geolib3::WKT::read<geolib3::SimpleGeometryVariant>(wkt);
    return geolib3::WKB::toString(geometryVariant);
}

} // namespace

pgpool3::Pool& pool()
{
    static unittest::MapsproDbFixture fixture;
    return fixture.pool();
}

pgpool3::Pool& revisionPool()
{
    return pool();
}

pgpool3::Pool* revisionPgPool()
{
    return &pool();
}

pgpool3::Pool* validationPgPool()
{
    return &pool();
}

revision::RevisionsGateway
revGateway(pqxx::transaction_base& txn, DBID branchId)
{
    if (branchId == revision::TRUNK_BRANCH_ID) {
        return revision::RevisionsGateway(txn);
    }

    revision::BranchManager branchManager(txn);
    revision::Branch branch = branchManager.load(branchId);
    return revision::RevisionsGateway(txn, branch);
}

revision::Snapshot
headCommitSnapshot(pqxx::transaction_base& txn, DBID branchId)
{
    auto rg = revGateway(txn, branchId);
    return rg.snapshot(rg.headCommitId());
}

RevisionID createRoadElement(
        rev::RevisionsGateway& gateway, std::string wkt,
        const boost::optional<TId>& startJc,
        const boost::optional<TId>& endJc,
        rev::Attributes attributes)
{
    std::list<rev::RevisionsGateway::NewRevisionData> cmtData;

    rev::RevisionsGateway::NewRevisionData objData;
    objData.first = gateway.acquireObjectId();
    const TId elementId = objData.first.objectId();

    attributes["cat:rd_el"] = "1";
    objData.second.attributes = std::move(attributes);
    objData.second.geometry = wkt2wkb(std::move(wkt));
    cmtData.push_back(std::move(objData));

    if (startJc) {
        rev::RevisionsGateway::NewRevisionData startRel;
        startRel.first = gateway.acquireObjectId();
        startRel.second.attributes = rev::Attributes{
            {"rel:role", "start"},
            {"rel:master", "rd_el"},
            {"rel:slave", "rd_jc"}};
        startRel.second.relationData = rev::RelationData(elementId, *startJc);
        cmtData.push_back(std::move(startRel));
    }

    if (endJc) {
        rev::RevisionsGateway::NewRevisionData endRel;
        endRel.first = gateway.acquireObjectId();
        endRel.second.attributes = rev::Attributes{
            {"rel:role", "end"},
            {"rel:master", "rd_el"},
            {"rel:slave", "rd_jc"}};
        endRel.second.relationData = rev::RelationData(elementId, *endJc);
        cmtData.push_back(std::move(endRel));
    }

    const rev::Commit commit = gateway.createCommit(
        std::move(cmtData), TEST_UID,
        {{"description", "blah"}}
    );

    return {elementId, commit.id()};
}

RevisionID createJunction(rev::RevisionsGateway& gateway, std::string wkt)
{
    std::list<rev::RevisionsGateway::NewRevisionData> cmtData;

    rev::RevisionsGateway::NewRevisionData objData;
    objData.first = gateway.acquireObjectId();
    const TId junctionId = objData.first.objectId();

    objData.second.attributes = rev::Attributes{ {"cat:rd_jc", "1"} };
    objData.second.geometry = wkt2wkb(std::move(wkt));
    cmtData.push_back(std::move(objData));

    const rev::Commit commit = gateway.createCommit(
        std::move(cmtData), TEST_UID,
        {{"description", "blah"}}
    );

    return {junctionId, commit.id()};
}

RevisionID deleteObject(rev::RevisionsGateway& gateway, TId objectId)
{
    const DBID headCommitId = gateway.headCommitId();
    const RevisionIds revisionIds = gateway.snapshot(headCommitId).revisionIdsByFilter(
        {objectId}, revision::filters::ObjRevAttr::isNotDeleted()
    );

    REQUIRE(
        revisionIds.size() == 1,
        "Object with id " << objectId << " doesn't exist"
    );

    rev::ObjectData objectData;
    objectData.deleted = true;

    std::list<rev::RevisionsGateway::NewRevisionData> commitData {
        {revisionIds[0], objectData}
    };

    const rev::Commit commit = gateway.createCommit(
        std::move(commitData), TEST_UID,
        {{"description", "delete object"}}
    );

    return {objectId, commit.id()};
}


DBID createStubCommit(revision::RevisionsGateway& gateway)
{
    std::list<rev::RevisionsGateway::NewRevisionData> cmtData(1);
    cmtData.back().first = gateway.acquireObjectId();

    const rev::Commit commit = gateway.createCommit(
        cmtData, TEST_UID, { {"description", "stub commit"} }
    );

    return commit.id();
}


DBID loadJson(pgpool3::Pool& pgPool, const std::string& filename)
{
    std::ifstream dataJson(filename);
    REQUIRE(!dataJson.fail(), "couldn't open file '" << filename << "'");
    revapi::RevisionAPI jsonLoader(pgPool, revapi::VerboseLevel::Full);
    auto commits = jsonLoader.importData(
            TEST_UID, revapi::IdMode::StartFromJsonId, dataJson);
    ASSERT(!commits.empty());
    return *std::max_element(commits.begin(), commits.end());
}

ValidatorMixin::ValidatorMixin()
    : aoi(gl::WKT::read<gl::Polygon2>("POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))"))
    , validatorConfig(static_cast<std::string>(EDITOR_CONFIG_PATH))
    , validator(validatorConfig)
{
    validator.initModules();
}

EditorConfigValidatorMixin::EditorConfigValidatorMixin()
{
    validator.enableCardinalityCheck();
}

DbMixin::DbMixin()
{
    {
        auto revisionTxn = revisionPool().masterWriteableTransaction();
        auto rg = revGateway(*revisionTxn, rev::TRUNK_BRANCH_ID);
        rg.truncateAll();
        rg.createDefaultBranches();
        revisionTxn->commit();
    }

    {
        auto validationTxn = validationPgPool()->masterWriteableTransaction();
        validationTxn->exec(
                "TRUNCATE TABLE validation.startrek_issue;"
                "TRUNCATE TABLE validation.task_message CASCADE;"
                "TRUNCATE TABLE validation.task_message_stats;"
                "TRUNCATE TABLE validation.exclusion, validation.exclusion_view;");
        validationTxn->commit();
    }

    {
        auto revisionTxn = revisionPool().slaveTransaction();
        startCommitId = revGateway(*revisionTxn).headCommitId();
    }
}

void checkMessage(const Message& message, const std::string& description) {
    UNIT_ASSERT_STRINGS_EQUAL_C(
        message.attributes().description,
        description,
        "Expected message '" << description
            << "', but reported '" << message.attributes().description << "'"
    );
}

std::string revisionIdsToString(const std::vector<RevisionID>& revisionIds)
{
    std::stringstream out;
    out << "[";
    bool first = true;
    for (const auto& id : revisionIds) {
        if (first) {
            first = false;
            out << id;
        } else {
            out << ", " << id;
        }
    }
    out << "]";
    return out.str();
}

// revision ids must be sorted
void checkRevisionIds(RevisionIds receivedIds, RevisionIds expectedIds)
{
    RevisionIds receivedDiff;
    std::set_difference(
        receivedIds.begin(), receivedIds.end(),
        expectedIds.begin(), expectedIds.end(),
        std::back_inserter(receivedDiff)
    );

    RevisionIds expectedDiff;
    std::set_difference(
        expectedIds.begin(), expectedIds.end(),
        receivedIds.begin(), receivedIds.end(),
        std::back_inserter(expectedDiff)
    );

    UNIT_ASSERT_C(
        receivedDiff.empty() && expectedDiff.empty(),
        "Not expected "
            << revisionIdsToString(receivedDiff) << ", "
            << "not received " << revisionIdsToString(expectedDiff) << ", but "
            << "expected " << revisionIdsToString(expectedIds)
    );
}


void checkMessage(Message message,
    const std::string& description,
    RevisionIds expectedRevisionIds)
{
    checkMessage(message, description);
    std::sort(expectedRevisionIds.begin(), expectedRevisionIds.end());
    checkRevisionIds(
        message.revisionIds(),
        std::move(expectedRevisionIds)
    );
}

void checkMessages(const Messages& messages,
    std::vector<MessageTestData> expectedData)
{
    std::multimap<std::string, RevisionIds> descriptionToRevisionIds;
    for (const auto& message: messages) {
        descriptionToRevisionIds.insert(
            std::make_pair(message.attributes().description, message.revisionIds())
        );
    }

    for (auto& data: expectedData) {
        const auto range = descriptionToRevisionIds.equal_range(data.description);
        typedef std::pair<std::string, RevisionIds> DescriptionAndRevisionIds;

        std::sort(data.revisionIds.begin(), data.revisionIds.end());
        auto it = std::find_if(
            range.first, range.second,
            [&data](const DescriptionAndRevisionIds& pair) -> bool {
                return pair.second == data.revisionIds;
            }
        );
        if (it == descriptionToRevisionIds.end()) {
            UNIT_FAIL(
                "Lost message: " << data.description << ", "
                << revisionIdsToString(data.revisionIds)
            );
        } else {
            descriptionToRevisionIds.erase(it);
        }
    }

    for (const auto& pair: descriptionToRevisionIds) {
        UNIT_FAIL(
            "Unexpected message: " << pair.first << ", "
            << revisionIdsToString(pair.second)
        );
    }
}

std::string dataPath(const std::string& filename)
{
    return ArcadiaSourceRoot() + "/maps/wikimap/mapspro/libs/validator/tests/data/" + filename;
}

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
