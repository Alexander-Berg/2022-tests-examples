#pragma once

#include <maps/wikimap/mapspro/libs/revision/commit_data.h>
#include <maps/wikimap/mapspro/libs/revision/revision_data.h>
#include <maps/wikimap/mapspro/libs/revision/revisions_gateway_impl.h>

#include <yandex/maps/wiki/revision/common.h>
#include <yandex/maps/wiki/revision/revisionid.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

const std::string STR_DESCRIPTION = "description";

const auto NO_GEOM = std::nullopt;
const auto NO_ATTR = std::nullopt;
const auto NO_DESC = std::nullopt;
const auto NO_RDATA = std::nullopt;

const auto NO_GEOM_DIFF = std::nullopt;
const auto NO_ATTR_DIFF = std::nullopt;
const auto NO_DESC_DIFF = std::nullopt;
const auto NO_DEL_DIFF = std::nullopt;

const DBID BIG_ID = 100500;
const ObjectRevision::ID NO_ID = RevisionID();

const bool DELETED = true;

//helper functions for ObjectRevision::ID creation
//will create ObjectRevision with auto-incrementing objectId leaving commitId empty
ObjectRevision::ID createUniqueId();

//will create ObjectRevision with given objectId leaving commitId empty
ObjectRevision::ID createID(DBID objectId);
//will create ObjectRevision with specific objectId and commitId
ObjectRevision::ID createID(DBID objectId, DBID commitId);


struct TestCommitData : public CommitData {
    TestCommitData(
            const Attributes& attributes,
            CommitState state,
            DBID id,
            UserID createdBy,
            bool trunk,
            DBID stableCommitId)
        //FIXME: This code always uses 0 as approveOrder value
        //       Approve order is not used for comparing test data for correctness
        //       Please, spent some time specifying correct approveOrders here
        : CommitData(id, 0, state, createdBy, attributes, "", stableCommitId, trunk)
    {
    }
};

struct TestRevisionData {
    TestRevisionData(
            ObjectRevision::ID prevId,
            ObjectRevision::ID nextTrunkId,
            const ObjectData& data)
        : prevId(prevId)
        , nextTrunkId(nextTrunkId)
        , data(data)
    {}

    ObjectRevision::ID prevId;
    ObjectRevision::ID nextTrunkId;
    ObjectRevision::ID nextStableId;
    bool hasNextApproved;
    ObjectData data;
};

struct TestData {
    TestData() {}
    explicit TestData(
            const Wkb& geom,
            const std::optional<Attributes>& attrs = std::nullopt,
            const std::optional<Description>& desc = std::nullopt)
        : data(attrs, desc, geom, std::nullopt, false)
    {}
    explicit TestData(
            const RelationData& rdata,
            const std::optional<Attributes>& attrs = std::nullopt,
            const std::optional<Description>& desc = std::nullopt)
        : data(attrs, desc, std::nullopt, rdata, false)
    {}
    explicit TestData(
            const std::optional<Attributes>& attrs,
            const std::optional<Description>& desc = std::nullopt,
            const std::optional<Wkb>& geom = std::nullopt,
            const std::optional<RelationData>& rdata = std::nullopt)
        : data(attrs, desc, geom, rdata, false)
    {}
    explicit TestData(
            bool deleted,
            const std::optional<Attributes>& attrs = std::nullopt,
            const std::optional<Description>& desc = std::nullopt,
            const std::optional<Wkb>& geom = std::nullopt,
            const std::optional<RelationData>& rdata = std::nullopt)
        : data(attrs, desc, geom, rdata, deleted)
    {}

    operator ObjectData() const
    {
        return data;
    }

    ObjectData data;
};

typedef std::map<ObjectRevision::ID, TestRevisionData> RevisionDataMap;


void checkAttributes(const ObjectRevision::ID& revId,
    const Attributes& recvAttrs,
    const Attributes& expAttrs);

void checkAttributesOpt(const ObjectRevision::ID& revId,
    const std::optional<Attributes>& recvAttrs,
    const std::optional<Attributes>& expAttrs);

void checkDescriptionOpt(const ObjectRevision::ID& revId,
    const std::optional<Description>& recvDesc,
    const std::optional<Description>& expDesc);

void checkWkbOpt(const ObjectRevision::ID& revId,
    const std::optional<Wkb>& recvWkb,
    const std::optional<Wkb>& expWkb);

void checkRelationDataOpt(const ObjectRevision::ID& revId,
    const std::optional<RelationData>& recvRData,
    const std::optional<RelationData>& expRData);

void checkCommit(const Commit& recvCommit, const CommitData& expCommit);

void checkObjectData(const ObjectRevision::ID& revId,
    const ObjectData& recvObjectData,
    const ObjectData& expObjectData);

void checkRevisionData(const ObjectRevision::ID& revId,
    const TestRevisionData& recvRevisionData,
    const TestRevisionData& expRevisionData);

void checkRevision(const ObjectRevision& rev,
    const RevisionDataMap& correctData,
    const RevisionsGateway& gateway);

void checkRelations(const Revisions& revisions,
    const std::map<ObjectRevision::ID, RelationData>& correctRelations);

struct TestObjectRevisionDiff {
    TestObjectRevisionDiff(
            const RevisionID& oldId,
            const RevisionID& newId,
            const ObjectRevisionDiff::Data& data = ObjectRevisionDiff::Data())
        : oldId(oldId)
        , newId(newId)
        , data(data)
    {}
    TestObjectRevisionDiff(
            const RevisionID& oldId,
            const RevisionID& newId,
            const GeometryDiff& geomDiff)
        : oldId(oldId)
        , newId(newId)
    {
        data.geometry = geomDiff;
    }
    TestObjectRevisionDiff(
            const RevisionID& oldId,
            const RevisionID& newId,
            const DeletedDiff& deletedDiff,
            const std::optional<Attributes>& oldAttrs,
            const std::optional<RelationData>& oldData)
        : oldId(oldId)
        , newId(newId)
    {
        data.deleted = deletedDiff;
        if (oldAttrs && !oldAttrs->empty()) {
            AttributesDiff adiff;
            for (const auto& p : *oldAttrs) {
                adiff.insert({p.first, {p.second, ""}});
            }
            data.attributes = adiff;
        }
        data.oldRelationData = oldData;
    }
    TestObjectRevisionDiff(
            const RevisionID& oldId,
            const RevisionID& newId,
            const std::optional<RelationData>& newData)
        : oldId(oldId)
        , newId(newId)
    {
        data.newRelationData = newData;
    }

    TestObjectRevisionDiff(
            const RevisionID& oldId,
            const RevisionID& newId,
            const std::optional<RelationData>& newData,
            const std::optional<Attributes>& newAttrs)
        : oldId(oldId)
        , newId(newId)
    {
        data.newRelationData = newData;
        if (newAttrs && !newAttrs->empty()) {
            AttributesDiff adiff;
            for (const auto& p : *newAttrs) {
                adiff.insert({p.first, {"", p.second}});
            }
            data.attributes = adiff;
        }
    }

    RevisionID oldId;
    RevisionID newId;
    ObjectRevisionDiff::Data data;
};

void checkObjectDataDiff(const ObjectRevision::ID& revId,
    const ObjectRevisionDiff::Data& received,
    const ObjectRevisionDiff::Data& expected);

void checkObjectRevisionDiff(const ObjectRevision::ID& revId,
    const TestObjectRevisionDiff& received,
    const TestObjectRevisionDiff& expected);

void checkRevisionIds(RevisionIds receivedIds, RevisionIds expectedIds);

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
