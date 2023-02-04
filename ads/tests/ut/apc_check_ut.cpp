#include "ads/quality/apc_check/lib/apc_check.h"

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

typedef std::function<NApcCheck::TBidMultAnswer (const NApcCheck::TRoiGroupZCompute&, ssize_t, int)> TMethod;
typedef TMersenne<ui32> TMRng;

class TApcCheckTests : public TTestBase {
    UNIT_TEST_SUITE(TApcCheckTests);
        UNIT_TEST(TestBCOldManual1);
        UNIT_TEST(TestZ1OldManual1);
        UNIT_TEST(TestITEROldManual1);
        UNIT_TEST(TestITERManual6);
        UNIT_TEST(TestITERManual6NoHeader);
    UNIT_TEST_SUITE_END();

private:
    void TestManualMultOnly(const TMethod& method, const TString&, double, double=1e-3, bool noHeader=false);
    void TestBCOldManual1();
    void TestZ1OldManual1();
    void TestITEROldManual1();
    void TestITERManual6();
    void TestITERManual6NoHeader();
};

#include <util/generic/xrange.h>
#include <util/stream/file.h>
void TApcCheckTests::TestManualMultOnly(const TMethod& method, const TString& filename,
                                        double expectedAns, double precision,
                                        bool noHeader) {
    NApcCheck::TCommandLineArguments::GetArgs().NOBOOTSTRAP = true;
    NApcCheck::TRoiGroupZCompute data;

    if (noHeader) {
        data = NApcCheck::ReadTableFromFile(ArcadiaSourceRoot() + "/ads/quality/apc_check/test-logs/" + filename,
                                            "1", NApcCheck::TLogsMeta(0, 1, 3, 2, -1, 4)); // GID Apc IsExp Bid CPA Weight
    } else {
        data = NApcCheck::ReadTableFromFile(ArcadiaSourceRoot() + "/ads/quality/apc_check/test-logs/" + filename);
    }
    auto ans = method(data, 100, 0);

    UNIT_ASSERT(!ans.isBadResult);
    UNIT_ASSERT(abs(ans.bidMult - expectedAns) < precision);
}

void TApcCheckTests::TestZ1OldManual1() {
    TestManualMultOnly(NApcCheck::ComputeBidMultZ1, "log1", 0.9247591206);
}

void TApcCheckTests::TestITEROldManual1() {
    TestManualMultOnly(NApcCheck::ComputeBidMultITER, "log1", 0.9192432674);
}

void TApcCheckTests::TestBCOldManual1() {
    TestManualMultOnly(NApcCheck::ComputeBidMultBCLike, "log1", 0.9198200323);
}

void TApcCheckTests::TestITERManual6() {
    TestManualMultOnly(NApcCheck::ComputeBidMultITER, "log6", 1.59551);
}

void TApcCheckTests::TestITERManual6NoHeader() {
    TestManualMultOnly(NApcCheck::ComputeBidMultITER, "log6_no_header", 1.59551, 1e-3, true);
}


UNIT_TEST_SUITE_REGISTRATION(TApcCheckTests);
