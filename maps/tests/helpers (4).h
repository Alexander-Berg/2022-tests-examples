#pragma once

#include <yandex/maps/wiki/validator/validator.h>

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/snapshot.h>

#include <maps/libs/pgpool/include/pgpool3.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/registar.h>

#include <boost/optional.hpp>

#include <list>
#include <string>
#include <utility>
#include <vector>

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

typedef std::vector<RevisionID> RevisionIds;

const revision::UserID TEST_UID = 111;

// re-initialize revision and validation schemas respectively and create
// PgPool when first called.
pgpool3::Pool& revisionPool();

pgpool3::Pool* revisionPgPool();
pgpool3::Pool* validationPgPool();

revision::RevisionsGateway
revGateway(
        pqxx::transaction_base& txn,
        DBID branchId = revision::TRUNK_BRANCH_ID);

revision::Snapshot
headCommitSnapshot(
        pqxx::transaction_base& txn,
        DBID branchId = revision::TRUNK_BRANCH_ID);

RevisionID createRoadElement(
        revision::RevisionsGateway& gateway, std::string wkt,
        const boost::optional<TId>& startJc = boost::none,
        const boost::optional<TId>& endJc = boost::none,
        revision::Attributes attributes = revision::Attributes());

RevisionID createJunction(revision::RevisionsGateway& gateway, std::string wkt);

RevisionID deleteObject(revision::RevisionsGateway& gateway, TId objectId);

DBID createStubCommit(revision::RevisionsGateway& gateway);

// returns max commit id
DBID loadJson(pgpool3::Pool& pgPool, const std::string& filename);

template <maps::log8::Level level>
struct SetLogLevelFixtureT
{
    SetLogLevelFixtureT()
    { maps::log8::setLevel(level); }
};

using SetLogLevelFixture = SetLogLevelFixtureT<maps::log8::Level::FATAL>;

struct ValidatorMixin: public SetLogLevelFixture, public NUnitTest::TBaseFixture
{
    ValidatorMixin();

    geolib3::Polygon2 aoi;
    ValidatorConfig validatorConfig;
    Validator validator;
};

struct EditorConfigValidatorMixin: public ValidatorMixin
{
    EditorConfigValidatorMixin();
};

using ValidatorFixture = ValidatorMixin;

struct DbMixin
{
    DbMixin();

    DBID startCommitId;
};

template <typename ...Mixins>
struct CompositeFixture: public Mixins... {
};

using DbFixture = CompositeFixture<ValidatorMixin, DbMixin>;

std::string revisionIdsToString(const std::vector<RevisionID>& revisionIds);

void checkMessage(const Message& message, const std::string& description);

void checkMessage(Message message, const std::string& description, RevisionIds expectedRevisionIds);

struct MessageTestData {
    std::string description;
    RevisionIds revisionIds;
};

void checkMessages(const Messages& messages, std::vector<MessageTestData> expectedData);

std::string dataPath(const std::string& filename);

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
