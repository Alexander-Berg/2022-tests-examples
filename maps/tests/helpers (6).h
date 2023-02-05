#pragma once

#include <yandex/maps/wiki/revision/branch.h>
#include <yandex/maps/wiki/revision/snapshot_id.h>
#include <maps/libs/pgpool/include/pgpool3.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <string>

struct SetLogLevelFixture : public NUnitTest::TBaseFixture
{
    SetLogLevelFixture() { maps::log8::setLevel(maps::log8::Level::FATAL); }
};

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

const TString EDITOR_CONFIG_PATH =
    ArcadiaSourceRoot() + "/maps/wikimap/mapspro/cfg/editor/editor.xml";

std::string dataPath(const std::string& filename);

struct SnapshotsPair {
    revision::Branch oldBranch;
    revision::SnapshotId oldSnapshotId;

    revision::Branch newBranch;
    revision::SnapshotId newSnapshotId;
};

class RevisionDB {
public:
    static pgpool3::Pool& pool();

    static void reset();
    static void clear();
    static void execSqlFile(const std::string& fname);
};

class ViewDB {
public:
    static pgpool3::Pool& pool();

    static void clear();
    static void syncView(revision::DBID branchId);
};

// create a pair of stable branches and load object revisions from
// @beforeJsonFile json into the first branch and from @afterJsonFile into
// the second.
SnapshotsPair loadData(
    const std::string& beforeJsonFile,
    const std::string& afterJsonFile,
    std::function<std::string(const std::string&)> loader = {});

class ResultsDB {
public:
    static pgpool3::Pool& pool();

    static void clear();
};

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
