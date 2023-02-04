#include <balancer/kernel/http2/server/hpack/tests/common/hpack_test_common.h>
#include <balancer/kernel/http2/server/hpack/hpack_integers.h>

#include <library/cpp/testing/unittest/registar.h>

class THPackIntegersTest : public TTestBase {
public:
    UNIT_TEST_SUITE(THPackIntegersTest);
        UNIT_TEST(TestSuffixRead)
        UNIT_TEST(TestSuffixWrite)
        UNIT_TEST(TestRFC7541Examples);
        UNIT_TEST(TestValidExtremes);
    UNIT_TEST_SUITE_END();
private:

    template <size_t N>
    void AssertSuffixRead(const char (&suff)[N], ui32 val) {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        TInputData data{TStringBuf(suff, N - 1)};
        ui32 suffix;
        TryRethrowError(ReadIntSuffix(data.Region).AssignTo(suffix));
        UNIT_ASSERT_VALUES_EQUAL_C(suffix, val, data.ToString());
        UNIT_ASSERT_VALUES_EQUAL_C(data.Region.SizeConsumed(), N - 1, data.ToString());
    }

    template <size_t N>
    void AssertSuffixWrite(const char (&suff)[N], ui32 val, ui32 cons) {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        TOutputData data{N - 1};
        WriteIntSuffix(val, cons, data.Region);
        UNIT_ASSERT_VALUES_EQUAL_C(data.ToString(), ConvertToHex(TStringBuf(suff, N - 1)) + ".", val);
        UNIT_ASSERT_VALUES_EQUAL_C(data.Region.SizeConsumed(), cons, val);
    }

    void TestSuffixRead() {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        AssertSuffixRead("\x00", 0);
        AssertSuffixRead("\x80\x00", 0);
        AssertSuffixRead("\x80\x80\x00", 0);
        AssertSuffixRead("\x7F", 0x7f);
        AssertSuffixRead("\xFF\x7F", 0x3fff);
        AssertSuffixRead("\xFF\xFF\x7F", 0x1fffff);
        UNIT_ASSERT_EXCEPTION(AssertSuffixRead("", 0), TConnectionError);
        UNIT_ASSERT_EXCEPTION(AssertSuffixRead("\x80", 0), TConnectionError);
        UNIT_ASSERT_EXCEPTION(AssertSuffixRead("\x80\x80", 0), TConnectionError);
        UNIT_ASSERT_EXCEPTION(AssertSuffixRead("\x80\x80\x80", 0), TConnectionError);
        UNIT_ASSERT_EXCEPTION(AssertSuffixRead("\x80\x80\x80\x00", 0), TConnectionError);
    }

    void TestSuffixWrite() {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        AssertSuffixWrite("\x00", 0, 1);
        AssertSuffixWrite("\x7F", 0x7f, 1);
        AssertSuffixWrite("\xFF\x7F", 0x3fff, 2);
        AssertSuffixWrite("\xFF\xFF\x7F", 0x1fffff, 3);
    }

    template <ui8 Mask, size_t N>
    void AssertRead(const char (&suff)[N], ui32 val) {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        TInputData data{TStringBuf(suff, N - 1)};
        ui32 mask;
        TryRethrowError(ReadInt<Mask>(data.Region).AssignTo(mask));
        UNIT_ASSERT_VALUES_EQUAL_C(mask, val, data.ToString());
        UNIT_ASSERT_VALUES_EQUAL_C(data.Region.SizeConsumed(), N - 1, data.ToString());
    }

    template <ui8 Mask, ui8 Flags, size_t N>
    void AssertWrite(const char (&suff)[N], ui32 val) {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        TOutputData data{N - 1};
        WriteInt<Mask, Flags>(val, data.Region);
        UNIT_ASSERT_VALUES_EQUAL_C(data.ToString(), ConvertToHex(TStringBuf(suff, N - 1)) + ".", val);
        UNIT_ASSERT_VALUES_EQUAL_C(data.Region.SizeConsumed(), N - 1, val);
    }

    template <ui8 Mask, ui8 Flags, size_t N>
    void AssertReadWrite(const char (&suff)[N], ui32 val) {
        AssertRead<Mask>(suff, val);
        AssertWrite<Mask, Flags>(suff, val);
    }

    void TestRFC7541Examples() {
        // http://httpwg.org/specs/rfc7541.html#integer.representation.examples
        AssertReadWrite<0b1110'0000, 0b1000'0000>("\x8A", 10);
        AssertReadWrite<0b1110'0000, 0b0100'0000>("\x4A", 10);
        AssertReadWrite<0b1110'0000, 0b0010'0000>("\x2A", 10);
        AssertReadWrite<0b1110'0000, 0b0000'0000>("\x0A", 10);

        AssertReadWrite<0b1110'0000, 0b1000'0000>("\x9F\x9A\x0A", 1337);
        AssertReadWrite<0b1110'0000, 0b0100'0000>("\x5F\x9A\x0A", 1337);
        AssertReadWrite<0b1110'0000, 0b0010'0000>("\x3F\x9A\x0A", 1337);
        AssertReadWrite<0b1110'0000, 0b0000'0000>("\x1F\x9A\x0A", 1337);
    }

    void TestValidExtremes() {
        using namespace NSrvKernel::NHTTP2;
        AssertReadWrite<0b0000'0000, 0b0000'0000>("\x00", 0);
        AssertReadWrite<0b0000'0000, 0b0000'0000>("\xFF\xFF\xFF\x7F", IMPL_HPACK_INT_SUFFIX_LIMIT + 0xff);
        AssertReadWrite<0b1111'1110, 0b0000'0000>("\x00", 0);
        AssertReadWrite<0b1111'1110, 0b1111'1110>("\xFE", 0);
        AssertReadWrite<0b1111'1110, 0b1111'1110>("\xFF\xFF\xFF\x7F", IMPL_HPACK_INT_SUFFIX_LIMIT + 0x01);
        AssertReadWrite<0b1000'0000, 0b0000'0000>("\x00", 0);
        AssertReadWrite<0b1000'0000, 0b1000'0000>("\x80", 0);
        AssertReadWrite<0b1000'0000, 0b1000'0000>("\xFF\xFF\xFF\x7F", IMPL_HPACK_INT_SUFFIX_LIMIT + 0x7f);
    }
};

UNIT_TEST_SUITE_REGISTRATION(THPackIntegersTest);
