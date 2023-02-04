#include "app_fixture.h"

#include <maps/libs/common/include/file_utils.h>

namespace maps::renderer::cartograph::test_util {

const std::string TESTS_DATA_PATH =
    common::joinPath(ArcadiaSourceRoot(), "maps/renderer/cartograph/tests/data");

AppFixture::AppFixture()
{
    sandboxClient = std::make_shared<test_util::MockSandboxClient>();
    stylerepoClient = std::make_shared<test_util::MockStylerepoClient>();
    userDataSetFileStorage = std::make_shared<test_util::MockFileStorage>();
    exportFileStorage = std::make_shared<test_util::MockFileStorage>();
    ::testing::Mock::AllowLeak(sandboxClient.get());
    ::testing::Mock::AllowLeak(stylerepoClient.get());
    ::testing::Mock::AllowLeak(userDataSetFileStorage.get());
    ::testing::Mock::AllowLeak(exportFileStorage.get());

    config.dataPath = TESTS_DATA_PATH;
}

std::unique_ptr<Application> AppFixture::createApp()
{
    AppDependencies depends;
    depends.db = pgPool;
    depends.sandboxClient = sandboxClient;
    depends.stylerepoClient = stylerepoClient;
    depends.userDataSetsFileStorage = userDataSetFileStorage;
    depends.exportFileStorage = exportFileStorage;
    return std::make_unique<Application>(config, depends);
}

} // namespace maps::renderer::cartograph::test_util
