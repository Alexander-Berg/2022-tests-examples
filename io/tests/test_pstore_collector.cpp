#include <yandex_io/libs/pstore_collector/pstore_collector.h>

#include <yandex_io/libs/base/utils.h>

#include <yandex_io/tests/testlib/unittest_helper/telemetry_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/stream/output.h>

#include <fstream>
#include <future>
#include <map>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(PstoreCollectorTests, TelemetryTestFixture) {
    Y_UNIT_TEST(testCollect) {
        const TFsPath pstorePath = "pstore";
        const std::unordered_map<std::string, std::string> files = {
            {"console-ramoops-0", "oops"},
            {"dmesg-ramoops-0", "aaaaaa"},
            {"dmesg-ramoops-1", "Kernel panic - not syncing"}};

        pstorePath.MkDirs();

        for (const auto& it : files) {
            auto file = it.first;
            auto content = it.second;

            if (file == "dmesg-ramoops-1") {
                file = file + ".enc.z";
                content = gzipCompress(content);
            }

            std::ofstream f(JoinFsPaths(pstorePath, file));
            f << content;
            f.close();
        }

        std::promise<std::unordered_map<std::string, std::string>> filesPromise;

        setKeyValueListener([&filesPromise](const std::string& event, const std::unordered_map<std::string, std::string>& keyValues, YandexIO::ITelemetry::Flags /*flags*/) {
            if (event == "pstore") {
                filesPromise.set_value(keyValues);
            }
        });

        PstoreCollector(pstorePath.GetPath(), getDeviceForTests()).collect();

        auto filesFuture = filesPromise.get_future();
        UNIT_ASSERT(files == filesFuture.get());
    }
}
