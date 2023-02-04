#include <yandex/maps/wiki/common/pgpool3_helpers.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revisionapi/revisionapi.h>
#include <yandex/maps/wiki/unittest/localdb.h>

#include <maps/libs/common/include/exception.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/json/include/builder.h>
#include <maps/libs/pgpool/include/pgpool3.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/xml/include/xml.h>

#include <library/cpp/testing/common/env.h>

#include <boost/iostreams/device/back_inserter.hpp>
#include <boost/iostreams/filtering_stream.hpp>
#include <boost/test/unit_test.hpp>

#include <fstream>
#include <memory>
#include <unordered_map>

using namespace maps::json;
using namespace maps::pgpool3;
using namespace maps::xml3;
using namespace maps::wiki::common;
using namespace maps::wiki::unittest;

namespace rev = maps::wiki::revision;
namespace filters = rev::filters;
namespace revapi = maps::wiki::revisionapi;
namespace io = boost::iostreams;

namespace {

Pool& pool()
{
    maps::log8::setLevel(maps::log8::Level::WARNING);
    static MapsproDbFixture fixture;
    return fixture.pool();
}

void renewSchema(rev::DBID setSequenceObjectId = 0)
{
    auto writeTr = makeWriteableTransaction(pool().getMasterConnection());
    writeTr->exec("TRUNCATE TABLE revision.object_revision RESTART IDENTITY;");
    writeTr->exec("TRUNCATE TABLE revision.commit RESTART IDENTITY;");
    writeTr->exec("SELECT setval('revision.object_id_seq', 1, false);");

    if (setSequenceObjectId) {
        rev::RevisionsGateway(*writeTr).acquireObjectIds(setSequenceObjectId);
    }
    writeTr->commit();
}

std::list<rev::DBID> importWrapper(
    const std::string& path,
    const revapi::IdMode idMode,
    const size_t batchSize = 0,
    const size_t idOffset = 0)
{
    std::ifstream is(SRC_(path));
    return revapi::RevisionAPI(pool(), revapi::VerboseLevel::Brief)
        .importData(1, idMode, is, batchSize, idOffset);
}

template<typename ExportOption = revapi::FilterPtr>
std::string exportWrapper(
    const ExportOption& exportOption = revapi::FilterPtr{},
    revapi::RelationsExportFlags flags =
        revapi::RelationsExportFlags::MasterToSlave,
    rev::DBID commitId = 0,
    const revapi::Strings& invertDirectionRoles = revapi::Strings{},
    revapi::EmptyJsonPolicy policy = revapi::EmptyJsonPolicy::Export)
{
    auto readTr = makeReadOnlyTransaction(pool().getSlaveConnection());
    if (!commitId) {
        commitId = rev::RevisionsGateway(*readTr).headCommitId();
        BOOST_CHECK(commitId > 0);
    }

    revapi::ExportParams params(
        rev::BranchManager(*readTr).loadTrunk(),
        commitId,
        flags,
        invertDirectionRoles);
    params.setEmptyJsonPolicy(policy);

    std::ostringstream ss;

    revapi::RevisionAPI(pool()).exportData(
        params, revapi::SingleStreamWrapper(ss), exportOption);
    return ss.str();
}

size_t objectsCount(const Value& data)
{
    return data["objects"].size();
}

size_t geometryCount(const Value& data)
{
    unsigned res = 0;
    for (const auto& objKey: data["objects"].fields()) {
        res += data["objects"][objKey].hasField("geometry");
    }
    return res;
}

size_t relationCount(const Value& data)
{
    size_t res = 0;
    for (const auto& objKey: data["objects"].fields()) {
        res += data["objects"][objKey].hasField("relations")
            ? data["objects"][objKey]["relations"].size()
        : 0;
    }
    return res;
}

bool equal(const Value& a, const Value& b)
{
    return a["objects"].size() == b["objects"].size() &&
           relationCount(a) == relationCount(b) &&
       geometryCount(a) == geometryCount(b);
}

void doTestImport(const std::string& jsonFile)
{
    renewSchema();
    std::list<rev::DBID> commitIds = importWrapper(jsonFile, revapi::IdMode::IgnoreJsonId);

    BOOST_CHECK_EQUAL(commitIds.size(), 2);
    rev::DBID firstCommitId = *(commitIds.begin());
    BOOST_CHECK_EQUAL(firstCommitId, 1);

    rev::DBID secondCommitId = *(++commitIds.begin());
    BOOST_CHECK_EQUAL(secondCommitId, 2);


    auto readTr = makeReadOnlyTransaction(pool().getSlaveConnection());
    auto reader = rev::RevisionsGateway(*readTr).reader();
    //check first commit (only objects)
    {
        auto objectRevisions = reader.commitRevisions(firstCommitId);

        BOOST_CHECK_EQUAL(objectRevisions.size(), 15);

        size_t geomCount = 0;
        size_t relCount = 0;
        for (const auto& objectRevision: objectRevisions) {
            geomCount += static_cast<bool>(objectRevision.data().geometry);
            relCount += static_cast<bool>(objectRevision.data().relationData);
        }

        BOOST_CHECK_EQUAL( geomCount,
                           geometryCount(Value::fromFile(SRC_(jsonFile))) );
        BOOST_CHECK_EQUAL(relCount, 0);
    }
    //check second commit (only relations)
    {
        auto objectRevisions = reader.commitRevisions(secondCommitId);

        BOOST_CHECK_EQUAL( objectRevisions.size(),
                           relationCount(Value::fromFile(SRC_(jsonFile))) );
    }
}

} // namespace

BOOST_AUTO_TEST_CASE(test_duplicate_keys)
{
    renewSchema();
    BOOST_CHECK_THROW(
        importWrapper("duplicate_keys.json", revapi::IdMode::IgnoreJsonId),
        maps::Exception);
}

BOOST_AUTO_TEST_CASE(test_undefined_object)
{
    renewSchema();
    BOOST_CHECK_THROW(
        importWrapper("undefined_object.json", revapi::IdMode::IgnoreJsonId),
        maps::Exception);
}

BOOST_AUTO_TEST_CASE(test_bad_relation_both_slave_and_master)
{
    renewSchema();
    BOOST_CHECK_THROW(
        importWrapper("bad_rel_both_slave_and_master.json", revapi::IdMode::IgnoreJsonId),
        revapi::DataError);
}

BOOST_AUTO_TEST_CASE(test_bad_relation_neither_slave_nor_master)
{
    renewSchema();
    BOOST_CHECK_THROW(
        importWrapper("bad_rel_neither_slave_nor_master.json", revapi::IdMode::IgnoreJsonId),
        revapi::DataError);
}

BOOST_AUTO_TEST_CASE(test_import)
{
    doTestImport("simple_data.json");
}


BOOST_AUTO_TEST_CASE(test_import_slave_to_master)
{
    doTestImport("simple_data_rel_slave_to_master.json");
}

BOOST_AUTO_TEST_CASE(test_batching_import)
{
    const std::string json_file = "simple_data.json";
    const Value& sourceJson = Value::fromFile(SRC_(json_file));

    unsigned objCount = sourceJson["objects"].size();
    unsigned relCount = relationCount(sourceJson);

    for (size_t batchSize = 1; batchSize < 10; batchSize += 1) {
        renewSchema();
        auto commitIds = importWrapper(json_file, revapi::IdMode::IgnoreJsonId, batchSize);
        BOOST_CHECK_EQUAL((objCount + batchSize - 1) / batchSize +
                          (relCount + batchSize -1) / batchSize,
                          commitIds.size());

        const Value& generatedJson = Value::fromString(exportWrapper());
        BOOST_CHECK(equal(generatedJson, sourceJson));
    }
}

BOOST_AUTO_TEST_CASE(test_export)
{
    const std::string json_file = "simple_data.json";

    renewSchema();
    importWrapper(json_file, revapi::IdMode::IgnoreJsonId);
    const Value& generatedJson = Value::fromString(exportWrapper());
    const Value& sourceJson = Value::fromFile(SRC_(json_file));

    BOOST_CHECK(equal(generatedJson, sourceJson));
}

BOOST_AUTO_TEST_CASE(test_import_ms_export_both)
{
    const std::string json_file = "simple_data.json";
    const std::string json_file_slave_to_master = "simple_data_rel_both.json";

    renewSchema();
    importWrapper(json_file, revapi::IdMode::IgnoreJsonId);
    const Value& generatedJson = Value::fromString(exportWrapper(
        revapi::FilterPtr(),
        revapi::RelationsExportFlags::MasterToSlave
            | revapi::RelationsExportFlags::SlaveToMaster));
    const Value& sourceJson = Value::fromFile(SRC_(json_file_slave_to_master));

    BOOST_CHECK(equal(generatedJson, sourceJson));
}

BOOST_AUTO_TEST_CASE(test_import_sm_export_both)
{
    const std::string importSourceFile = "simple_data_rel_slave_to_master.json";
    const std::string exportCanonicalFile = "simple_data_rel_both.json";

    renewSchema();
    importWrapper(importSourceFile, revapi::IdMode::IgnoreJsonId);
    const Value& exportedJson = Value::fromString(exportWrapper(
        revapi::FilterPtr{},
        revapi::RelationsExportFlags::MasterToSlave
            | revapi::RelationsExportFlags::SlaveToMaster));
    const Value& exportCanonicalJson = Value::fromFile(SRC_(exportCanonicalFile));

    BOOST_CHECK_EQUAL(exportedJson["objects"].size(), exportCanonicalJson["objects"].size());
    BOOST_CHECK_EQUAL(geometryCount(exportedJson), geometryCount(exportCanonicalJson));
    BOOST_CHECK_EQUAL(relationCount(exportedJson), relationCount(exportCanonicalJson));
}

BOOST_AUTO_TEST_CASE(test_chunked_export)
{
    const size_t NUMBER_OF_OBJECTS_IN_SINGLE_JSON = 2;
    std::unordered_map<size_t, std::string> chunkedJson;
    std::string onePieceJsonSource;
    std::string onePieceJsonFromChunked;

    revapi::GetStreamForChunkFunc callback =
        [&](size_t chunkNo) {
        return std::shared_ptr<std::ostream>(new io::filtering_ostream(io::back_inserter(chunkedJson[chunkNo])));
    };

    renewSchema();
    importWrapper("simple_data.json", revapi::IdMode::IgnoreJsonId);

    rev::DBID lastObjectId = 0;
    {
        auto readTr = makeReadOnlyTransaction(pool().getSlaveConnection());
        auto commitId = rev::RevisionsGateway(*readTr).headCommitId();
        BOOST_CHECK(commitId > 0);
        auto branch = rev::BranchManager(*readTr).loadTrunk();

        // chunked export
        {
            revapi::ExportParams params(
                branch,
                commitId,
                revapi::RelationsExportFlags::MasterToSlave);
            params.setWriteBatchSize(NUMBER_OF_OBJECTS_IN_SINGLE_JSON);

            revapi::RevisionAPI(pool()).exportData(params, callback);
        }

        // one-piece export
        {
            io::filtering_ostream ostream(io::back_inserter(onePieceJsonSource));
            revapi::ExportParams params(
                branch,
                commitId,
                revapi::RelationsExportFlags::MasterToSlave);

            revapi::RevisionAPI(pool()).exportData(
                params, revapi::SingleStreamWrapper(ostream));
            lastObjectId = rev::RevisionsGateway(*readTr).lastObjectId();
        }
    }

    renewSchema(lastObjectId);

    // import from chunks
    for (const auto& v: chunkedJson) {
        std::istringstream is(v.second);
        revapi::RevisionAPI(pool(), revapi::VerboseLevel::Full)
            .importData(1, revapi::IdMode::PreserveId, is, 0);
    }

    // export again
    auto readTr = makeReadOnlyTransaction(pool().getSlaveConnection());
    auto commitId = rev::RevisionsGateway(*readTr).headCommitId();
    BOOST_CHECK(commitId > 0);
    {
        io::filtering_ostream ostream(io::back_inserter(onePieceJsonFromChunked));
        revapi::ExportParams params(
            rev::BranchManager(*readTr).loadTrunk(),
            commitId,
            revapi::RelationsExportFlags::MasterToSlave);

        revapi::RevisionAPI(pool()).exportData(
            params, revapi::SingleStreamWrapper(ostream));
    }
    BOOST_CHECK(equal(
        Value::fromString(onePieceJsonSource),
        Value::fromString(onePieceJsonFromChunked)));
}

BOOST_AUTO_TEST_CASE(test_processing_global_id)
{
    const std::string json_file = "global_ids.json";

    renewSchema();
    importWrapper(json_file, revapi::IdMode::StartFromJsonId);
    //check sequence
    auto writeTr = makeWriteableTransaction(pool().getMasterConnection());
    rev::RevisionID revId = rev::RevisionsGateway(*writeTr).acquireObjectId();
    writeTr->commit();

    BOOST_CHECK(revId.objectId() >= 3);
}

rev::DBID acquireIdsReturnLast(pqxx::transaction_base& tr, size_t count)
{
    std::list<rev::ObjectIdRange> ids = rev::RevisionsGateway(tr).acquireObjectIds(count);
    BOOST_CHECK_EQUAL(ids.size(), 1);
    return ids.front().second;
}

BOOST_AUTO_TEST_CASE(test_offset_id)
{
    // 15 objects, 16 relations
    const std::string jsonFile = "offset_id.json";
    const Value& sourceJson = Value::fromFile(SRC_(jsonFile));
    const size_t objects = objectsCount(sourceJson);
    const size_t relations = relationCount(sourceJson);
    BOOST_CHECK_EQUAL(objects, 15);
    BOOST_CHECK_EQUAL(relations, 16);

    renewSchema();

    // move sequence to 16
    auto writeTr = makeWriteableTransaction(pool().getMasterConnection());
    rev::DBID objectId = acquireIdsReturnLast(*writeTr, objects);
    writeTr->commit();
    BOOST_CHECK_EQUAL(objectId, 15);

    // object ids will occupy [1,15] interval
    // relation ids will occupy [16, 31] interval
    importWrapper(jsonFile, revapi::IdMode::PreserveId);

    // relation ids are allocated by 10 due to optimization in
    // librevision, so for 16 relations allocated 20 ids
    writeTr = makeWriteableTransaction(pool().getMasterConnection());
    objectId = acquireIdsReturnLast(*writeTr, 1);
    writeTr->commit();
    BOOST_CHECK_EQUAL(objectId, 16 + 20);

    // next batch of objects will occupy [32, 46]
    // and relations [47, 62]
    // so we move sequence to 47 (now it is at 37)
    writeTr = makeWriteableTransaction(pool().getMasterConnection());
    objectId = acquireIdsReturnLast(*writeTr, 10);
    writeTr->commit();
    BOOST_CHECK_EQUAL(objectId, 46);

    importWrapper(jsonFile, revapi::IdMode::PreserveId, 0, 31);

    writeTr = makeWriteableTransaction(pool().getMasterConnection());
    objectId = acquireIdsReturnLast(*writeTr, 1);
    writeTr->commit();
    BOOST_CHECK_EQUAL(objectId, 47 + 20);
}

BOOST_AUTO_TEST_CASE(test_filters)
{
    const std::string json_file = "categories.json";

    renewSchema();
    importWrapper(json_file, revapi::IdMode::IgnoreJsonId);

    auto filterAccum = std::make_shared<filters::ProxyFilterExpr>(filters::False());
    *filterAccum |= filters::Attr("cat:error").defined() && filters::Attr("cat:error") == "1";
    const Value& error = Value::fromString(exportWrapper(filterAccum));
    BOOST_CHECK_EQUAL(error["objects"].size(), 1);

    *filterAccum |= filters::Attr("cat:aoi").defined() && filters::Attr("cat:aoi") == "1";
    const Value& errorAndAoi = Value::fromString(exportWrapper(filterAccum));
    BOOST_CHECK_EQUAL(errorAndAoi["objects"].size(), 2);
}

BOOST_AUTO_TEST_CASE(test_filters_enchanced_attrs)
{
    const std::string json_file = "categories.json";

    renewSchema();
    importWrapper(json_file, revapi::IdMode::IgnoreJsonId);

    auto filterAccum = std::make_shared<filters::ProxyFilterExpr>(filters::False());
    *filterAccum |= filters::Attr("cat:error").defined();
    const Value& error = Value::fromString(exportWrapper(filterAccum));
    BOOST_CHECK_EQUAL(error["objects"].size(), 1);

    *filterAccum |= filters::Attr("cat:aoi").defined();
    const Value& errorAndAoi = Value::fromString(exportWrapper(filterAccum));
    BOOST_CHECK_EQUAL(errorAndAoi["objects"].size(), 2);

    filterAccum = std::make_shared<filters::ProxyFilterExpr>(
            filters::Attr::definedAny(revapi::Strings{"cat:aoi", "cat:error"}));
    const Value& errorAndAoi2 = Value::fromString(exportWrapper(filterAccum));
    BOOST_CHECK_EQUAL(errorAndAoi2["objects"].size(), 2);
}

BOOST_AUTO_TEST_CASE(test_filters_dangling_rels)
{
    const std::string inputFile = "dangling_rels.json";
    const std::string etalonFile = "dangling_rels_etalon.json";

    renewSchema(/*setSequenceObjectId = */100);
    importWrapper(inputFile, revapi::IdMode::PreserveId);

    const std::vector<rev::DBID> exportOids = {1, 8, 18, 24, 26};
    const Value& exportedJson = Value::fromString(exportWrapper(
        exportOids,
        revapi::RelationsExportFlags::MasterToSlave
            | revapi::RelationsExportFlags::SlaveToMaster
            | revapi::RelationsExportFlags::SkipDangling));
    const Value& etalonJson = Value::fromFile(SRC_(etalonFile));

    BOOST_CHECK_EQUAL(
        exportedJson["objects"].size(), etalonJson["objects"].size());
    BOOST_CHECK_EQUAL(
        geometryCount(exportedJson), geometryCount(etalonJson));
    BOOST_CHECK_EQUAL(
        relationCount(exportedJson), relationCount(etalonJson));
}

BOOST_AUTO_TEST_CASE(test_filters_dangling_rels_invert_start_roles)
{
    const std::string inputFile = "dangling_rels.json";
    const std::string etalonFile = "dangling_rels_invert_start_etalon.json";

    renewSchema(/*setSequenceObjectId = */100);
    importWrapper(inputFile, revapi::IdMode::PreserveId);

    const std::vector<rev::DBID> exportOids = {1, 8, 18, 24, 25};
    const Value& exportedJson = Value::fromString(exportWrapper(
        exportOids,
        revapi::RelationsExportFlags::SlaveToMaster
            | revapi::RelationsExportFlags::SkipDangling,
        0, {"start", "end"}));
    const Value& etalonJson = Value::fromFile(SRC_(etalonFile));

    BOOST_CHECK_EQUAL(
        exportedJson["objects"].size(), etalonJson["objects"].size());
    BOOST_CHECK_EQUAL(
        geometryCount(exportedJson), geometryCount(etalonJson));
    BOOST_CHECK_EQUAL(
        relationCount(exportedJson), relationCount(etalonJson));
}

BOOST_AUTO_TEST_CASE(test_empty_export)
{
    renewSchema();
    importWrapper("simple_data.json", revapi::IdMode::IgnoreJsonId);

    std::string exportResult = exportWrapper(
        std::make_shared<filters::ProxyFilterExpr>(filters::False()),
        revapi::RelationsExportFlags::MasterToSlave
            | revapi::RelationsExportFlags::SlaveToMaster);

    BOOST_REQUIRE(!exportResult.empty());
    BOOST_CHECK_NO_THROW(Value::fromString(exportResult));

    exportResult = exportWrapper(
        std::vector<rev::DBID>{},
        revapi::RelationsExportFlags::MasterToSlave
            | revapi::RelationsExportFlags::SlaveToMaster);

    BOOST_REQUIRE(!exportResult.empty());
    BOOST_CHECK_NO_THROW(Value::fromString(exportResult));
}

BOOST_AUTO_TEST_CASE(test_skip_empty_export)
{
    renewSchema();
    importWrapper("simple_data.json", revapi::IdMode::IgnoreJsonId);

    std::string exportResult = exportWrapper(
        std::make_shared<filters::ProxyFilterExpr>(filters::False()),
        revapi::RelationsExportFlags::MasterToSlave
            | revapi::RelationsExportFlags::SlaveToMaster,
        0 /*commitId*/,
        {} /*invertDirectionRoles*/,
        revapi::EmptyJsonPolicy::Skip);

    BOOST_REQUIRE(exportResult.empty());
}
