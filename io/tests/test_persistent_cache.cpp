#include <yandex_io/modules/leds/led_capability/s3_downloader/cache/persistent_cache/persistent_cache.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/libs/base/utils.h>

#include <util/folder/path.h>

#include <fstream>

using namespace quasar;
using namespace YandexIO;
using namespace quasar::TestUtils;
using namespace std::literals;

namespace {

    class TestFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);

            testDir = JoinFsPaths(tryGetRamDrivePath(), "quasar-" + quasar::makeUUID());
            testDir.MkDirs();
            initCache();
        }

        void initCache() {
            cache = std::make_unique<PersistentLruCache>(testDir);
        }

        void putEntry(const std::string& filename) {
            std::ofstream file{cache->putEntry(filename)};
            file << "test";
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::TearDown(context);
            testDir.ForceDelete();
        }

        void checkHit(const std::string& filename, bool expectedHit) {
            const auto [file, hit] = cache->getEntry(filename);
            UNIT_ASSERT_VALUES_EQUAL(file, testDir / filename);
            UNIT_ASSERT(hit == expectedHit);
            UNIT_ASSERT(TFsPath(file).Exists() == expectedHit);
        }

    protected:
        TFsPath testDir;
        std::unique_ptr<PersistentLruCache> cache;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(PersistentLruCacheTest, TestFixture) {
    Y_UNIT_TEST(testGetEntry) {
        putEntry("test_1");
        checkHit("test_1", true);
        checkHit("test_2", false);
    }

    Y_UNIT_TEST(testOverflow) {
        cache->setSizeEntries(2);
        putEntry("test_1");
        checkHit("test_1", true);
        putEntry("test_2");
        checkHit("test_2", true);
        // trigger lru
        cache->getEntry("test_1");
        putEntry("test_3");
        checkHit("test_3", true);
        checkHit("test_2", false);
        checkHit("test_1", true);
    }

    Y_UNIT_TEST(testRestore) {
        putEntry("test_1");
        checkHit("test_1", true);
        // reinit Cache to emulate reboot
        initCache();
        checkHit("test_1", true);
    }
}
