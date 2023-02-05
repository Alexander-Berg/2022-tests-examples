#include "helpers.h"

#include <yandex/maps/wiki/common/misc.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/commit_manager.h>
#include <yandex/maps/wiki/revision/snapshot_id.h>
#include <yandex/maps/wiki/revisionapi/revisionapi.h>
#include <yandex/maps/wiki/unittest/unittest.h>
#include <yandex/maps/wiki/unittest/config.h>
#include <yandex/maps/wiki/unittest/localdb.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/log8/include/log8.h>
#include <yandex/maps/shell_cmd.h>
#include <yandex/maps/shellcmd/logging_ostream.h>

#include <boost/algorithm/string/replace.hpp>
#include <boost/filesystem.hpp>

#include <memory>
#include <string>
#include <vector>
#include <fstream>
#include <cstring>
#include <stdexcept>

#include <signal.h>
#include <stdio.h>
#include <sys/wait.h>

namespace fs = boost::filesystem;

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {
namespace {

const std::string DB_LOCK_TAG = "diffalert-tests";
const std::string SERVICES_BASE_TEMPLATE = "/maps/wikimap/mapspro/cfg/services/services-base-template.xml";
const std::string CATEGORIES_DIR_PATH = "/maps/wikimap/mapspro/cfg/editor";
const std::string RENDERER_MAP_XML_DIR_PATH = "/maps/wikimap/mapspro/cfg/layers/mpskl";
const std::string RENDERER_LAYERS_PATH = "/maps/wikimap/mapspro/cfg/layers";

std::string
createTempServicesBaseXml()
{
    try {
        auto servicesBaseTemplate = maps::common::readFileToString(ArcadiaSourceRoot() + SERVICES_BASE_TEMPLATE);
        boost::replace_all(servicesBaseTemplate, "#CATEGORIES_DIR_PATH#", ArcadiaSourceRoot() + CATEGORIES_DIR_PATH);
        boost::replace_all(servicesBaseTemplate, "#RENDERER_MAP_XML_DIR_PATH#", ArcadiaSourceRoot() + RENDERER_MAP_XML_DIR_PATH);
        boost::replace_all(servicesBaseTemplate, "#RENDERER_LAYERS_PATH#", ArcadiaSourceRoot() + RENDERER_LAYERS_PATH);
        auto filepath = fs::temp_directory_path() / fs::unique_path();
        std::ofstream file(filepath.string());
        file << servicesBaseTemplate;
        return filepath.string();
    } catch (const std::exception& ex) {
        WARN() << ex.what();
    }
    return {};
}

class TestsSingleton
{
public:
    ~TestsSingleton()
    {
    }

    static TestsSingleton& instance()
    {
        static TestsSingleton instance_;
        return instance_;
    };

    std::string configPath() { return config.filepath(); }
    pgpool3::Pool& pool() { return db.pool(); }

private:
    TestsSingleton()
        : config(db.host(), db.port(), db.dbname(), db.user(), db.password(), "",
            createTempServicesBaseXml())
    {
    };
    unittest::MapsproDbFixture db;
    unittest::ConfigFileHolder config;
};

const auto EDITOR_TOOL_PATH = BinaryPath(
    "maps/wikimap/mapspro/services/editor/src/bin/tool/wiki-editor-tool");

const revision::DBID TESTS_USER = 777777;

const std::string VIEW_STABLE_SCHEMA_PATTERN = "vrevisions_stable_%";

const std::string RESULTS_SCHEMA_NAME = "diffalert";
const std::vector<std::string> RESULTS_TABLE_NAMES = {
    "messages",
    "message_attributes",
};

pgpool3::Pool& testDbPool()
{
    return TestsSingleton::instance().pool();
}

std::string servicesConfigPath()
{
    return TestsSingleton::instance().configPath();
}

} // namespace

std::string dataPath(const std::string& filename)
{
    return SRC_(filename);
}

pgpool3::Pool& RevisionDB::pool()
{ return testDbPool(); }

void RevisionDB::reset()
{
    auto txn = pool().masterWriteableTransaction();
    revision::RevisionsGateway(*txn).truncateAll();
    revision::RevisionsGateway(*txn).createDefaultBranches();
    txn->commit();
}

void RevisionDB::clear()
{
    auto txn = pool().masterWriteableTransaction();
    revision::RevisionsGateway(*txn).truncateAll();
    txn->commit();
}

void RevisionDB::execSqlFile(const std::string& fname)
{
    auto txn = pool().masterWriteableTransaction();
    txn->exec(maps::common::readFileToString(fname));
    txn->exec("SET search_path = public"); // Avoid conflicts with pg_dump
    txn->commit();
}

pgpool3::Pool& ViewDB::pool()
{ return testDbPool(); }

void ViewDB::clear()
{
    auto txn = pool().masterWriteableTransaction();
    for (const auto& row : txn->exec(
            "SELECT nspname FROM pg_namespace "
            "WHERE nspname LIKE '" + VIEW_STABLE_SCHEMA_PATTERN + "'")) {
        txn->exec("DROP SCHEMA " + row[0].as<std::string>() + " CASCADE");
    }
    txn->commit();
}

void ViewDB::syncView(revision::DBID branchId)
{
    auto command = EDITOR_TOOL_PATH +
        " --config " + servicesConfigPath() +
        " --log-level fatal" +
        " --branch " + std::to_string(branchId) +
        " --all-objects 1" +
        " --set-progress-state 1" +
        " --stages view";

    shell::stream::LoggingOutputStream loggedOut(
        [](const std::string& s){ INFO() << "shell.stdout: " << s; });
    shell::stream::LoggingOutputStream loggedErr(
        [](const std::string& s){ ERROR() << "shell.stderr: " << s; });
    shell::ShellCmd cmd(command, loggedOut, loggedErr);

    auto exitCode = cmd.run();
    REQUIRE(exitCode == 0, "View creation failed: " << command);
}

SnapshotsPair loadData(
        const std::string& beforeJsonFile, const std::string& afterJsonFile,
        std::function<std::string(const std::string&)> loader)
{
    if (!loader) {
        loader = maps::common::readFileToString;
    }

    RevisionDB::reset();
    ViewDB::clear();

    // RevisionAPI has the following limitations:
    // * it forbids importing objects with existing object_id-s
    // * it does not support importing into non-trunk branches
    // Therefore some manual interventions are necessary:
    // * Upload "before" revisions into trunk, approve them and merge
    // into stable branch.
    // * Upload "after" revisions with an offset to avoid duplicating
    // object_id's. Merge them into the second stable branch.
    // * Manually remove all created commits from trunk.
    // * Manually remove the object_id offset from "after" revisions.
    // After these steps object histories in "before" and "after"
    // branches become completely separate.

    auto getMaxObjectId = [](const std::string& fname) -> revision::DBID
    {
        auto json = json::Value::fromFile(fname);
        auto idStr = json["attributes"]["next_free_object_id"].toString();
        return std::stoul(idStr);
    };
    auto maxObjectId = std::max(
            getMaxObjectId(beforeJsonFile), getMaxObjectId(afterJsonFile));

    {
        // set sequence so that oids of relations in the first import
        // will not intersect with regular objects of both imports
        auto txn = RevisionDB::pool().masterWriteableTransaction();
        txn->exec("SELECT setval('revision.object_id_seq', "
                  + std::to_string(maxObjectId) + ")");
        txn->commit();
    }

    revisionapi::RevisionAPI rapi(RevisionDB::pool());

    std::stringstream beforeStream(loader(beforeJsonFile));
    rapi.importData(TESTS_USER, revisionapi::IdMode::PreserveId, beforeStream);

    std::unique_ptr<revision::Branch> beforeBranch;
    std::unique_ptr<revision::Branch> afterBranch;
    revision::DBID offset = 0;
    {
        auto txn = RevisionDB::pool().masterWriteableTransaction();

        revision::RevisionsGateway rg(*txn);
        auto headCommitId = rg.headCommitId();
        offset = rg.lastObjectId();
        // set sequence so that oids of relations of the second import
        // will not intersect with objects and relations of the first
        // import after subtracting offset from object_id-s
        txn->exec("SELECT setval('revision.object_id_seq', "
                  + std::to_string(2 * offset) + ")");

        revision::BranchManager bm(*txn);
        bm.createApproved(TESTS_USER, /*attributes=*/{});
        revision::CommitManager cm(*txn);
        cm.approveAll(headCommitId);
        beforeBranch.reset(
                new revision::Branch(bm.createStable(TESTS_USER, /*attributes=*/{})));
        cm.mergeApprovedToStable(headCommitId);

        txn->commit();
    }

    std::stringstream afterStream(loader(afterJsonFile));
    rapi.importData(
            TESTS_USER, revisionapi::IdMode::PreserveId, afterStream,
            /* batchLoadingSize= */0,
            offset);

    {
        auto txn = RevisionDB::pool().masterWriteableTransaction();

        auto headCommitId = revision::RevisionsGateway(*txn).headCommitId();

        revision::BranchManager bm(*txn);
        bm.loadStable().finish(*txn, TESTS_USER);
        revision::CommitManager cm(*txn);
        cm.approveAll(headCommitId);
        afterBranch.reset(
                new revision::Branch(bm.createStable(TESTS_USER, /*attributes=*/{})));
        cm.mergeApprovedToStable(headCommitId);

        txn->exec("UPDATE revision.commit SET trunk=false");

        std::stringstream updateObjs;
        updateObjs
            << "UPDATE revision.object_revision SET"
            << " object_id = object_id - " << offset
            << " WHERE object_id > " << offset;
        txn->exec(updateObjs.str());

        std::stringstream updateRels;
        updateRels
            << "UPDATE revision.object_revision SET"
            << " master_object_id = master_object_id - " << offset
            << ",slave_object_id = slave_object_id - " << offset
            << " WHERE master_object_id > " << offset;
        txn->exec(updateRels.str());

        txn->commit();
    }

    auto headSnapshotId = [](const revision::Branch& branch)
    {
        auto txn = RevisionDB::pool().masterWriteableTransaction();
        return revision::RevisionsGateway(*txn, branch).maxSnapshotId();
    };

    return SnapshotsPair{
            *beforeBranch, headSnapshotId(*beforeBranch),
            *afterBranch, headSnapshotId(*afterBranch)};
}


pgpool3::Pool& ResultsDB::pool()
{ return testDbPool(); }

void ResultsDB::clear()
{
    auto txn = pool().masterWriteableTransaction();
    for (const auto& tableName : RESULTS_TABLE_NAMES) {
        txn->exec(
            "TRUNCATE TABLE " +
            RESULTS_SCHEMA_NAME + "." + tableName + " CASCADE");
    }
    txn->commit();
}

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
