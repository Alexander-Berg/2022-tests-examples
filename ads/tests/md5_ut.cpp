#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/digest/md5/md5.h>

class TMd5Tests : public TTestBase {
    UNIT_TEST_SUITE(TMd5Tests)
        UNIT_TEST(TestIntHash);
    UNIT_TEST_SUITE_END();

private:
    void TestIntHash();
    void TestEndHalfMix();
};

void TMd5Tests::TestIntHash() {
    auto md5 = MD5{};
    ui64 v1 = 1ull;
    md5.Update(&v1, sizeof(v1));
    char r[33];
    TString rs(md5.End(r));

    // bytes = int(1).to_bytes(8, 'little')
    // m = md5()
    // m.update(bytes)
    // m.hexdigest()
    // '33cdeccccebe80329f1fdbee7f5874cb'
    UNIT_ASSERT_EQUAL(rs,"33cdeccccebe80329f1fdbee7f5874cb");
}

UNIT_TEST_SUITE_REGISTRATION(TMd5Tests)
