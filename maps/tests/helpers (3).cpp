#include "helpers.h"

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <boost/test/unit_test.hpp>

#include <atomic>
#include <sstream>


namespace maps {
namespace wiki {
namespace revision {
namespace tests {

namespace {

std::atomic<DBID> objectIdSequence(10000);

} //anonymous namespace

ObjectRevision::ID createUniqueId()
{
    DBID objectId(objectIdSequence++);
    return RevisionID::createNewID(objectId);
}

ObjectRevision::ID createID(DBID objectId)
{
    return RevisionID::createNewID(objectId);
}

ObjectRevision::ID createID(DBID objectId, DBID commitId)
{
    return RevisionID(objectId, commitId);
}

namespace {

template <class T, class ID>
void checkValue(const std::string& name, const T& recv, const T& exp,
    const std::string& idName, const ID& id)
{
    BOOST_CHECK_MESSAGE(recv == exp,
        name << " check failed: expected " << exp << ", received " << recv <<
        ", " << idName << " " << id);
}

template <class T, class ID>
void requireValue(const std::string& name, const T& recv, const T& exp,
    const std::string& idName, const ID& id)
{
    BOOST_REQUIRE_MESSAGE(recv == exp,
        name << " check failed: expected " << exp << ", received " << recv <<
        ", " << idName << " " << id);
}

template <class ID>
void checkAttributes(const std::string& idName, const ID& id,
    const Attributes& recvAttrs,
    const Attributes& expAttrs)
{
    requireValue("Attributes count",
        recvAttrs.size(), expAttrs.size(), idName, id);
    for (const auto& recvAttrPair : recvAttrs) {
        auto expAttrIt = expAttrs.find(recvAttrPair.first);
        BOOST_REQUIRE_MESSAGE(expAttrIt != expAttrs.end(),
            "Attribute key " << recvAttrPair.first << " not found"
             ", " << idName << " " << id);
        const auto& recvAttrValue = recvAttrPair.second;
        const auto& expAttrValue = expAttrIt->second;
        checkValue("Attribute value", recvAttrValue, expAttrValue, idName, id);
    }
}

} // namespace

void checkRevisionAttributes(const ObjectRevision::ID& revId,
    const Attributes& recvAttrs,
    const Attributes& expAttrs)
{
    checkAttributes("revision", revId, recvAttrs, expAttrs);
}

void checkCommitAttributes(const DBID& commitId,
    const Attributes& recvAttrs,
    const Attributes& expAttrs)
{
    checkAttributes("commit", commitId, recvAttrs, expAttrs);
}

void checkAttributesOpt(
    const ObjectRevision::ID& revId,
    const std::optional<Attributes>& recvAttrs,
    const std::optional<Attributes>& expAttrs)
{
    requireValue("Attributes initialized",
        !recvAttrs, !expAttrs, "revision", revId);
    if (recvAttrs) {
        checkRevisionAttributes(revId, *recvAttrs, *expAttrs);
    }
}

void checkDescriptionOpt(
    const ObjectRevision::ID& revId,
    const std::optional<Description>& recvDesc,
    const std::optional<Description>& expDesc)
{
    requireValue("Description initialized",
        !recvDesc, !expDesc, "revision", revId);
    if (recvDesc) {
        checkValue("Description", *recvDesc, *expDesc, "revision", revId);
    }
}

void checkWkbOpt(
    const ObjectRevision::ID& revId,
    const std::optional<Wkb>& recvWkb,
    const std::optional<Wkb>& expWkb)
{
    requireValue("Geometry initialized",
        !recvWkb, !expWkb, "revision", revId);
    if (recvWkb) {
        checkValue("Geometry", *recvWkb, *expWkb, "revision", revId);
    }
}

void checkRelationDataOpt(
    const ObjectRevision::ID& revId,
    const std::optional<RelationData>& recvRData,
    const std::optional<RelationData>& expRData)
{
    requireValue("RelationData initialized",
        !recvRData,
        !expRData,
        "revision", revId);
    if (recvRData) {
        checkValue("Master object id",
            (*recvRData).masterObjectId(),
            (*expRData).masterObjectId(),
            "revision", revId);
        checkValue("Slave object id",
            (*recvRData).slaveObjectId(),
            (*expRData).slaveObjectId(),
            "revision", revId);
    }
}

void checkCommit(const Commit& recvCommit, const CommitData& expCommit)
{
    checkValue("Commit id",
        recvCommit.id(), expCommit.id, "commit", expCommit.id);
    checkValue("Commit stable_branch_id",
        recvCommit.stableBranchId(), expCommit.stableBranchId, "commit", expCommit.id);
    checkValue("Commit stable",
        recvCommit.inStable(), expCommit.inStable(), "commit", expCommit.id);
    checkValue("Commit trunk",
        recvCommit.inTrunk(), expCommit.inTrunk(), "commit", expCommit.id);
    checkValue("Commit created by",
        recvCommit.createdBy(), expCommit.createdBy, "commit", expCommit.id);
    checkValue("Commit state",
        recvCommit.state(), expCommit.state, "commit", expCommit.id);
    BOOST_CHECK(!recvCommit.createdAt().empty());
    checkCommitAttributes(
        recvCommit.id(), recvCommit.attributes(), expCommit.attributes());
}

void checkObjectData(
    const ObjectRevision::ID& revId,
    const ObjectData& recvObjectData,
    const ObjectData& expObjectData)
{
    checkAttributesOpt(
        revId, recvObjectData.attributes, expObjectData.attributes);
    checkDescriptionOpt(
        revId, recvObjectData.description, expObjectData.description);
    checkWkbOpt(
        revId, recvObjectData.geometry, expObjectData.geometry);
    checkRelationDataOpt(
        revId, recvObjectData.relationData, expObjectData.relationData);
    checkValue("Deleted",
        recvObjectData.deleted, expObjectData.deleted, "revision", revId);
}

void checkRevisionData(const ObjectRevision::ID& revId,
    const TestRevisionData& recvRevisionData,
    const TestRevisionData& expRevisionData)
{
    checkValue("Previous revision id",
        recvRevisionData.prevId, expRevisionData.prevId, "revision", revId);
    checkValue("Next trunk revision id",
        recvRevisionData.nextTrunkId, expRevisionData.nextTrunkId, "revision", revId);
    checkObjectData(revId, recvRevisionData.data, expRevisionData.data);
}

void checkRevision(const ObjectRevision& rev,
    const RevisionDataMap& correctData,
    const RevisionsGateway& gateway)
{
    auto it = correctData.find(rev.id());
    BOOST_REQUIRE_MESSAGE(it != correctData.end(),
        "RevisionData with id " << rev.id() << " not found "
        " via commitRevisions()");
    checkRevisionData(
        rev.id(),
        TestRevisionData(
            rev.prevId(),
            rev.nextTrunkId(),
            rev.data()),
        it->second);

    if (it->second.nextTrunkId.empty() && gateway.branchId() == TRUNK_BRANCH_ID) {
        auto srev = gateway.snapshot(BIG_ID).objectRevision(rev.id().objectId());
        BOOST_REQUIRE_MESSAGE(srev,
            "RevisionData with id " << rev.id() << " not found "
            " via objectRevision()");
        checkRevisionData(
            rev.id(),
            TestRevisionData(
                (*srev).prevId(),
                (*srev).nextTrunkId(),
                (*srev).data()),
            it->second);
    }
    ObjectRevision drev = gateway.reader().loadRevision(rev.id());
    checkRevisionData(
        rev.id(),
        TestRevisionData(
            drev.prevId(),
            drev.nextTrunkId(),
            drev.data()),
        it->second);
}

void checkRelations(const Revisions& revisions,
    const std::map<ObjectRevision::ID, RelationData>& correctRelations)
{
    BOOST_CHECK_EQUAL(revisions.size(), correctRelations.size());
    for (const auto& rev : revisions) {
        auto correctIt = correctRelations.find(rev.id());
        BOOST_REQUIRE_MESSAGE(correctIt != correctRelations.end(),
            "Relation for revision " << rev.id() << " not found");
        BOOST_REQUIRE_MESSAGE(rev.data().relationData,
            "Relation data for revision " << rev.id() << " is not initialized");
        checkValue("Slave object id",
            rev.data().relationData->slaveObjectId(),
            correctIt->second.slaveObjectId(),
            "revision", rev.id());
        checkValue("Master object id",
            rev.data().relationData->masterObjectId(),
            correctIt->second.masterObjectId(),
            "revision", rev.id());
    }
}

void checkAttributesDiff(const ObjectRevision::ID& revId,
    const std::optional<AttributesDiff>& received,
    const std::optional<AttributesDiff>& expected)
{
    requireValue("Attributes diff initialized",
        !received, !expected, "revision", revId);
    if (received) {
        requireValue("Attributes diff size",
            (*received).size(), (*expected).size(), "revision", revId);
        for (const auto& recvDiffPair : *received) {
            auto expDiffIt = (*expected).find(recvDiffPair.first);
            BOOST_REQUIRE_MESSAGE(expDiffIt != (*expected).end(),
                "Attribute with key " << recvDiffPair.first << " not found"
                ", revision id " << revId);
            checkValue("Attribute " + recvDiffPair.first + " old value",
                recvDiffPair.second.before,
                expDiffIt->second.before,
                "revision", revId);
            checkValue("Attribute " + recvDiffPair.first + " new value",
                recvDiffPair.second.after,
                expDiffIt->second.after,
                "revision", revId);
        }
    }
}

void checkDescriptionDiff(const ObjectRevision::ID& revId,
    const std::optional<DescriptionDiff>& received,
    const std::optional<DescriptionDiff>& expected)
{
    requireValue("Description diff initialized",
        !received, !expected, "revision", revId);
    if (received) {
        checkValue("Description old value",
            (*received).before, (*expected).before, "revision", revId);
        checkValue("Description new value",
            (*received).after, (*expected).after, "revision", revId);
    }
}

void checkGeometryDiff(const ObjectRevision::ID& revId,
    const std::optional<GeometryDiff>& received,
    const std::optional<GeometryDiff>& expected)
{
    requireValue("Geometry diff initialized",
        !received, !expected, "revision", revId);
    if (received) {
        checkValue("Geometry old value",
            (*received).before, (*expected).before, "revision", revId);
        checkValue("Geometry new value",
            (*received).after, (*expected).after, "revision", revId);
    }
}

void checkDeletedDiff(const ObjectRevision::ID& revId,
    const std::optional<DeletedDiff>& received,
    const std::optional<DeletedDiff>& expected)
{
    requireValue("Deleted diff initialized",
        !received, !expected, "revision", revId);
    if (received) {
        checkValue("Deleted old value",
            (*received).before, (*expected).before, "revision", revId);
        checkValue("Deleted new value",
            (*received).after, (*expected).after, "revision", revId);
    }
}

void checkRDataDiff(const ObjectRevision::ID& revId,
    const std::string& name,
    const std::optional<RelationData>& received,
    const std::optional<RelationData>& expected)
{
    requireValue(name + " initialized",
        !received, !expected, "revision", revId);
    if (received) {
        checkValue(name + "master object id value",
            (*received).masterObjectId(),
            (*expected).masterObjectId(),
            "revision", revId);
        checkValue(name + "slave object id value",
            (*received).slaveObjectId(),
            (*expected).slaveObjectId(),
            "revision", revId);
    }
}

void checkObjectDataDiff(const ObjectRevision::ID& revId,
    const ObjectRevisionDiff::Data& received,
    const ObjectRevisionDiff::Data& expected)
{
    checkAttributesDiff(revId, received.attributes, expected.attributes);
    checkDescriptionDiff(revId, received.description, expected.description);
    checkGeometryDiff(revId, received.geometry, expected.geometry);
    checkDeletedDiff(revId, received.deleted, expected.deleted);
    checkRDataDiff(revId, "Old RelationData",
        received.oldRelationData, expected.oldRelationData);
    checkRDataDiff(revId, "New RelationData",
        received.newRelationData, expected.newRelationData);
}

void checkObjectRevisionDiff(const ObjectRevision::ID& revId,
    const TestObjectRevisionDiff& received,
    const TestObjectRevisionDiff& expected)
{
    checkValue("Old revision id",
        received.oldId, expected.oldId, "revision", revId);
    checkValue("New revision id",
        received.newId, expected.newId, "revision", revId);
    checkObjectDataDiff(revId, received.data, expected.data);
}

void checkRevisionIds(RevisionIds receivedIds, RevisionIds expectedIds)
{
    auto toString = [](const RevisionIds& revisionIds) {
        std::ostringstream out;
        bool first = true;
        out << "[";
        for (const auto& id: revisionIds) {
            if (first) {
                first = false;
            } else {
                out << ",";
            }
            out << id;
        }
        out << "]";
        return out.str();
    };

    std::sort(receivedIds.begin(), receivedIds.end());
    std::sort(expectedIds.begin(), expectedIds.end());


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

    BOOST_CHECK_MESSAGE(
        receivedDiff.empty() && expectedDiff.empty(),
        "Not expected "
            << toString(receivedDiff) << ", "
            << "not received " << toString(expectedDiff) << ", but "
            << "expected " << toString(expectedIds)
    );
}

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps

