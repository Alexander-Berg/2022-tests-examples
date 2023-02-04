#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/common/env.h>
#include <util/system/env.h>
#include <util/system/byteorder.h>
#include <util/folder/tempdir.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <yandex_io/libs/lmdb/lmdb_dbi.h>

#include <sys/stat.h> // mkdir
#include <sys/types.h>

#include <iostream>

using namespace quasar;

namespace {
    struct TestKey {
        std::uint64_t key;
        std::uint64_t type;
    };

    struct TestKeysConv {
        using KeyType = TestKey;

        static TestKey fromSv(const std::string_view& src) {
            TestKey result = lmdb::from_sv<TestKey>(src);
            result.key = InetToHost<std::uint64_t>(result.key); // be64toh(result.key);
            return result;
        }

        static TestKey toDbKey(const TestKey& src) {
            TestKey result = src;
            result.key = HostToInet<std::uint64_t>(src.key); // htobe64(src.key);
            return result;
        }
    };

    struct TempDirWrapper {
        TTempDir tempDir;

        TempDirWrapper(const std::string& path)
            : tempDir(TTempDir::NewTempDir(TString(path)))
        {
        }
    };

    struct Fixture: public NUnitTest::TBaseFixture {
        using Base = NUnitTest::TBaseFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            lmdbEnv = lmdb::env::create();
            lmdbEnv.set_mapsize(30 * 1024 * 1024); // enough for tests
            lmdbEnv.set_max_dbs(1);
            lmdbDir = std::make_unique<TempDirWrapper>(quasar::TestUtils::tryGetRamDrivePath());
            lmdbEnv.open(lmdbDir->tempDir.Name().c_str());
            dbi = LmdbDbi<TestKeysConv>(lmdbEnv, "test");
        }
        void TearDown(NUnitTest::TTestContext& context) override {
            lmdbEnv.close();
            Base::TearDown(context);
        }

        void fillDbi() {
            auto txn = lmdb::txn::begin(lmdbEnv);
            for (unsigned i = 0; i < 10; ++i) {
                TestKey key{
                    .key = i,
                    .type = i % 2,
                };
                dbi.put(txn, key, "payload");
            }
            txn.commit();
        }

        std::unique_ptr<TempDirWrapper> lmdbDir;
        lmdb::env lmdbEnv{nullptr};
        LmdbDbi<TestKeysConv> dbi;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(LmdbDbi, Fixture) {
    Y_UNIT_TEST(forEach) {
        fillDbi();
        auto txn = lmdb::txn::begin(lmdbEnv, nullptr, MDB_RDONLY);
        unsigned count = 0;
        dbi.forEach(txn, [&count](const TestKey& key, const std::string_view& value) {
            UNIT_ASSERT(value == "payload");
            ++count;
            if (key.key == 5) {
                return false;
            }
            return true;
        });
        UNIT_ASSERT_EQUAL(count, 6);
    }

    Y_UNIT_TEST(search) {
        fillDbi();
        auto txn = lmdb::txn::begin(lmdbEnv);
        auto cursor = dbi.openCursor(txn);
        {
            auto keyVal = cursor.toKeyVal(TestKey{.key = 4});
            UNIT_ASSERT_EQUAL(keyVal->key.key, 4);
            keyVal = cursor.nextKeyVal();
            UNIT_ASSERT_EQUAL(keyVal->key.key, 5);
            keyVal = cursor.prevKeyVal();
            UNIT_ASSERT_EQUAL(keyVal->key.key, 4);
            cursor.del();
        }
        auto keyVal = cursor.toKeyVal(TestKey{.key = 4});
        UNIT_ASSERT_EQUAL(keyVal->key.key, 5);
    }
}
