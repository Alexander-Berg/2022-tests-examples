#include "helpers.h"

#include <library/cpp/testing/unittest/env.h>
#include <yandex/maps/shellcmd/logging_ostream.h>
#include <yandex/maps/shell_cmd.h>
#include <yandex/maps/wiki/revisionapi/revisionapi.h>
#include <yandex/maps/wiki/unittest/config.h>
#include <maps/libs/common/include/file_utils.h>

#include <boost/algorithm/string/replace.hpp>
#include <boost/filesystem.hpp>

namespace fs = boost::filesystem;
namespace revapi = maps::wiki::revisionapi;
namespace maps::wiki::misc::tests {

namespace {

const auto EDITOR_TOOL_PATH = BinaryPath(
    "maps/wikimap/mapspro/services/editor/src/bin/tool/wiki-editor-tool");

const std::string SERVICES_BASE_TEMPLATE = "/maps/wikimap/mapspro/cfg/services/services-base-template.xml";
const std::string CATEGORIES_DIR_PATH = "/maps/wikimap/mapspro/cfg/editor";
const std::string RENDERER_MAP_XML_DIR_PATH = "/maps/wikimap/mapspro/cfg/layers/mpskl";
const std::string RENDERER_LAYERS_PATH = "/maps/wikimap/mapspro/cfg/layers";

std::string
createTempServicesBaseXml()
{
    auto servicesBaseTemplate = maps::common::readFileToString(ArcadiaSourceRoot() + SERVICES_BASE_TEMPLATE);
    boost::replace_all(servicesBaseTemplate, "#CATEGORIES_DIR_PATH#", ArcadiaSourceRoot() + CATEGORIES_DIR_PATH);
    boost::replace_all(servicesBaseTemplate, "#RENDERER_MAP_XML_DIR_PATH#", ArcadiaSourceRoot() + RENDERER_MAP_XML_DIR_PATH);
    boost::replace_all(servicesBaseTemplate, "#RENDERER_LAYERS_PATH#", ArcadiaSourceRoot() + RENDERER_LAYERS_PATH);
    auto filepath = fs::temp_directory_path() / fs::unique_path();
    std::ofstream file(filepath.string());
    file << servicesBaseTemplate;
    return filepath.string();
}

} // unnamed namespace

std::string arcadiaDataPath(const std::string& filename)
{
    return SRC_("data/" + filename);
}

std::list<revision::DBID> importDataToRevision(pgpool3::Pool& pgPool, const std::string& filename)
{
    std::ifstream dataJson(filename);
    REQUIRE(!dataJson.fail(), "couldn't open file '" << filename << "'");
    revapi::RevisionAPI jsonLoader(pgPool, revapi::VerboseLevel::Full);
    auto commits = jsonLoader.importData(
        TEST_UID, revapi::IdMode::StartFromJsonId, dataJson);
    INFO() << "commits size = " << commits.size();
    ASSERT(!commits.empty());
    return commits;
}

void syncViewWithRevision(unittest::ArcadiaDbFixture& db)
{
    unittest::ConfigFileHolder config(db.database(), "", createTempServicesBaseXml());
    auto command = EDITOR_TOOL_PATH +
        " --config " + config.filepath() +
        " --log-level fatal" +
        " --branch 0" +
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

} // namespace maps::wiki::misc::tests
