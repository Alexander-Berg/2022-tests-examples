#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include "ads/quality/apc_check/lib/apc_check.h"

class TApcCheckAATests : public TTestBase {
    UNIT_TEST_SUITE(TApcCheckAATests);
        UNIT_TEST(TestBCAA50);
        UNIT_TEST(TestITERAA50);
        UNIT_TEST(TestZ1AA50);
        UNIT_TEST(TestBCInvAA50);
        UNIT_TEST(TestBidsRatioAA50);
        UNIT_TEST(TestActionsRatioAA50);
        UNIT_TEST(TestBCAA10);
        UNIT_TEST(TestITERAA10);
        UNIT_TEST(TestZ1AA10);
        UNIT_TEST(TestBCInvAA10);
        UNIT_TEST(TestBidsRatioAA10);
        UNIT_TEST(TestActionsRatioAA10);
        UNIT_TEST(TestBCAASymmetry10);
        UNIT_TEST(TestITERAASymmetry20);
        UNIT_TEST(TestZ1AASymmetry10);
        UNIT_TEST(TestBCInvAASymmetry10);
        UNIT_TEST(TestBidsRatioAASymmetry10);
        UNIT_TEST(TestActionsRatioAASymmetry10);
        UNIT_TEST(TestBidsRatioZeroAlphaCpaAA10);
        UNIT_TEST(TestActionsRatioZeroAlphaCpaAA10);
    UNIT_TEST_SUITE_END();

private:
    void TestBCAA50();
    void TestITERAA50();
    void TestZ1AA50();
    void TestBCInvAA50();
    void TestBidsRatioAA50();
    void TestActionsRatioAA50();

    void TestBCAA10();
    void TestITERAA10();
    void TestZ1AA10();
    void TestBCInvAA10();
    void TestBidsRatioAA10();       // median not equals 1
    void TestActionsRatioAA10();    // median not equals 1

    void TestBCAASymmetry10();
    void TestITERAASymmetry20();
    void TestZ1AASymmetry10();
    void TestBCInvAASymmetry10();
    void TestBidsRatioAASymmetry10();
    void TestActionsRatioAASymmetry10();

    void TestBidsRatioZeroAlphaCpaAA10();
    void TestActionsRatioZeroAlphaCpaAA10();
    double TestAA(const NApcCheck::TMethod& method, double, const TString&, bool=false, int=100);
};

double TApcCheckAATests::TestAA(const NApcCheck::TMethod& method, double probability, const TString& testName, bool symmetry, int iters) {
    TVector<NApcCheck::TRoiGroup> groups;
    NApcCheck::ParseLogs("./2018071107", &groups);

    UNIT_ASSERT(!groups.empty());

    TVector<double> bidMults = NApcCheck::EvalAA(groups, method, probability, symmetry, iters);

    // check if large part of tries are divergent
    UNIT_ASSERT(bidMults.size() > 0.8 * iters);

    Sort(bidMults);

    size_t sz = bidMults.size();
    fprintf(stderr, "|| %lf | %lf | %lf | %s ||\n", bidMults[sz / 10], bidMults[sz / 2], bidMults[sz - sz / 10 - 1], testName.c_str());

    return bidMults[bidMults.size() / 2];
}

void TApcCheckAATests::TestBCAA50() {
    double median = TestAA(NApcCheck::ComputeBidMultBCLike, 0.5, "TestBCAA50");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestITERAA50() {
    double median = TestAA(NApcCheck::ComputeBidMultITER, 0.5, "TestITERAA50");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestZ1AA50() {
    double median = TestAA(NApcCheck::ComputeBidMultZ1, 0.5, "TestZ1AA50");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestBCInvAA50() {
    double median = TestAA(NApcCheck::ComputeBidMultBCLike, 0.5, "TestBCInvAA50");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestBidsRatioAA50() {
    double median = TestAA(NApcCheck::ComputeBidMultITER, 0.5, "TestBidsRatioAA50");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestActionsRatioAA50() {
    double median = TestAA(NApcCheck::ComputeBidMultZ1, 0.5, "TestActionsRatioAA50");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestBCAASymmetry10() {
    double median = TestAA(NApcCheck::ComputeBidMultBCLike, 0.1, "TestBCAASymmetry10", true);
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 3e-2);
}

void TApcCheckAATests::TestITERAASymmetry20() {
    double median = TestAA(NApcCheck::ComputeBidMultITER, 0.2, "TestITERAASymmetry20", true);
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 3e-2);
}

void TApcCheckAATests::TestZ1AASymmetry10() {
    double median = TestAA(NApcCheck::ComputeBidMultZ1, 0.1, "TestZ1AASymmetry10", true);
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 3e-2);
}

void TApcCheckAATests::TestBCInvAASymmetry10() {
    double median = TestAA(NApcCheck::ComputeBidMultBCLike, 0.5, "TestBCInvAASymmetry10");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestBidsRatioAASymmetry10() {
    double median = TestAA(NApcCheck::ComputeBidMultITER, 0.5, "TestBidsRatioAASymmetry10");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestActionsRatioAASymmetry10() {
    double median = TestAA(NApcCheck::ComputeBidMultZ1, 0.5, "TestActionsRatioAASymmetry10");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 1e-2);
}

void TApcCheckAATests::TestBCAA10() {
    double median = TestAA(NApcCheck::ComputeBidMultBCLike, 0.1, "TestBCAA10", false, 300);
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 2e-2);
}

void TApcCheckAATests::TestITERAA10() {
    double median = TestAA(NApcCheck::ComputeBidMultITER, 0.1, "TestITERAA10");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 2e-2);
}

void TApcCheckAATests::TestZ1AA10() {
    double median = TestAA(NApcCheck::ComputeBidMultZ1, 0.1, "TestZ1AA10");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 2e-2);
}

void TApcCheckAATests::TestBCInvAA10() {
    double median = TestAA(NApcCheck::ComputeBidMultBCLikeInversedReveight, 0.1, "TestBCInvAA10");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 2e-2);
}

void TApcCheckAATests::TestBidsRatioAA10() {
    double median = TestAA(NApcCheck::TBidsRatioCorrector(), 0.1, "TestBidsRatioAA10");
    UNIT_ASSERT(std::abs(median - 1) > 2e-2);
}

void TApcCheckAATests::TestActionsRatioAA10() {
    double median = TestAA(NApcCheck::ComputeBidMultActionsRatio, 0.1, "TestActionsRatioAA10");
    UNIT_ASSERT(std::abs(median - 1) > 2e-2);
}

void TApcCheckAATests::TestBidsRatioZeroAlphaCpaAA10() {
    NApcCheck::TCommandLineArguments::GetArgs().ALPHA_CPA = 0;
    double median = TestAA(NApcCheck::TBidsRatioCorrector(), 0.1, "TestBidsRatioZeroAlphaCpaAA10");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 2e-2);
}

void TApcCheckAATests::TestActionsRatioZeroAlphaCpaAA10() {
    NApcCheck::TCommandLineArguments::GetArgs().ALPHA_CPA = 0;
    double median = TestAA(NApcCheck::ComputeBidMultActionsRatio, 0.1, "TestActionsRatioZeroAlphaCpaAA10");
    UNIT_ASSERT_DOUBLES_EQUAL(median, 1, 2e-2);
}

UNIT_TEST_SUITE_REGISTRATION(TApcCheckAATests);
