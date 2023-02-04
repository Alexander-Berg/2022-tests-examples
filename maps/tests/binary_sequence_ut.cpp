#include <maps/wikimap/mapspro/services/acl/lib/policy.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::aclsrv::ut {
namespace {
Sequence seqFromString(const std::string& text)
{
    Sequence result;
    result.reserve(text.length());
    for (const auto& c : text) {
        result.push_back(c != '0');
    }
    return result;
}
} // namespace

Y_UNIT_TEST_SUITE(binary_sequence) {

Y_UNIT_TEST(test_generate_sequences)
{
    {
        auto sequences = generateBinarySequences(1);
        UNIT_ASSERT_VALUES_EQUAL(sequences.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(sequences[0], seqFromString("1"));
    }
    {
        auto sequences = generateBinarySequences(4);
        UNIT_ASSERT_VALUES_EQUAL(sequences.size(), 15);
        UNIT_ASSERT_VALUES_EQUAL(sequences[0], seqFromString("1000"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[1], seqFromString("0100"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[2], seqFromString("0010"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[3], seqFromString("0001"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[4], seqFromString("1100"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[5], seqFromString("1010"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[6], seqFromString("1001"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[7], seqFromString("0110"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[8], seqFromString("0101"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[9], seqFromString("0011"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[10], seqFromString("1110"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[11], seqFromString("1101"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[12], seqFromString("1011"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[13], seqFromString("0111"));
        UNIT_ASSERT_VALUES_EQUAL(sequences[14], seqFromString("1111"));
    }
}

Y_UNIT_TEST(test_compare_sequences)
{
    UNIT_ASSERT(leftBinSeqContainsRight(seqFromString("0"), seqFromString("0")));
    UNIT_ASSERT(!leftBinSeqContainsRight(seqFromString("0"), seqFromString("1")));
    UNIT_ASSERT(leftBinSeqContainsRight(seqFromString("1"), seqFromString("0")));
    UNIT_ASSERT(leftBinSeqContainsRight(seqFromString("1"), seqFromString("1")));

    UNIT_ASSERT(leftBinSeqContainsRight(seqFromString("110"), seqFromString("010")));
    UNIT_ASSERT(!leftBinSeqContainsRight(seqFromString("110"), seqFromString("011")));
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::aclsrv::ut
