#include <balancer/kernel/http2/server/hpack/tests/common/hpack_test_common.h>
#include <balancer/kernel/http2/server/hpack/hpack_strings.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/string/hex.h>

class THPackStringsTest : public TTestBase {
public:
    UNIT_TEST_SUITE(THPackStringsTest);
        UNIT_TEST(TestRFC7541ExamplesHuffman);
        UNIT_TEST(TestHuffmanErrors);
        UNIT_TEST(TestEverySymbol);
        UNIT_TEST(TestFuzzerFound);
    UNIT_TEST_SUITE_END();

private:
    void DoTestHuffman(std::pair<TStringBuf, TStringBuf> test) {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        UNIT_ASSERT_VALUES_EQUAL_C(GetHuffmanEncodedStringSize(test.second), test.first.size(), ConvertToHex(test.first) << " " << test.second);
        {
            TOutputData data{test.second.size()};
            NSrvKernel::TryRethrowError(HuffmanDecodeString(test.first, data.Region));
            UNIT_ASSERT_VALUES_EQUAL_C(ConvertToHex(data.Data), ConvertToHex(test.second), test.second);
            UNIT_ASSERT_VALUES_EQUAL_C(data.Region.SizeConsumed(), test.second.size(), test.second);
        }
        {
            TOutputData data{test.first.size()};
            HuffmanEncodeString(test.second, data.Region);
            UNIT_ASSERT_VALUES_EQUAL_C(ConvertToHex(data.Data), ConvertToHex(test.first), test.second);
            UNIT_ASSERT_VALUES_EQUAL_C(data.Region.SizeConsumed(), test.first.size(), test.second);
        }
    }

    void TestRFC7541ExamplesHuffman() {
        for (const auto& test : RFC7541ExamplesHuffman) {
            DoTestHuffman(test);
        }
    }

    void TestHuffmanErrors() {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;

        {
            TOutputData dec{4};
            UNIT_ASSERT_EXCEPTION(TryRethrowError(HuffmanDecodeString(TStringBuf("\x00"sv), dec.Region)), TConnectionError);
        }

        for (const auto& test : RFC7541ExamplesHuffman) {
            {
                TOutputData dec{test.second.size() - 1};
                UNIT_ASSERT_EXCEPTION(TryRethrowError(HuffmanDecodeString(test.first, dec.Region)), TConnectionError);
            }
            {
                TOutputData dec{test.second.size()};
                UNIT_ASSERT_EXCEPTION(TryRethrowError(HuffmanDecodeString(test.first.SubStr(0, test.first.size() - 1), dec.Region)),
                                      TConnectionError);
            }
        }
    }

    void DoTestEncodeDecode(TStringBuf input, TStringBuf inputLower) {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        UNIT_ASSERT_VALUES_EQUAL_C(input.size(), inputLower.size(), ConvertToHex(input));

        {
            TOutputData encoded{GetHuffmanEncodedStringSize(input)};
            TOutputData encodedLowerCase{GetHuffmanEncodedStringSize(inputLower)};
            HuffmanEncodeString(input, encoded.Region);
            HuffmanEncodeStringLowerCase(input, encodedLowerCase.Region);
            UNIT_ASSERT_VALUES_EQUAL_C(encoded.Region.SizeAvailable(), 0, ConvertToHex(input));
            UNIT_ASSERT_VALUES_EQUAL_C(encodedLowerCase.Region.SizeAvailable(), 0, ConvertToHex(input));

            TOutputData decoded{input.size()};
            TOutputData decodedLowerCase{input.size()};
            NSrvKernel::TryRethrowError(HuffmanDecodeString(encoded.Data, decoded.Region));
            NSrvKernel::TryRethrowError(HuffmanDecodeString(encodedLowerCase.Data, decodedLowerCase.Region));
            UNIT_ASSERT_VALUES_EQUAL_C(decoded.Region.SizeAvailable(), 0, ConvertToHex(input));
            UNIT_ASSERT_VALUES_EQUAL_C(decodedLowerCase.Region.SizeAvailable(), 0, ConvertToHex(input));

            UNIT_ASSERT_VALUES_EQUAL_C(ConvertToHex(decoded.Data), ConvertToHex(input), input);
            UNIT_ASSERT_VALUES_EQUAL_C(ConvertToHex(decodedLowerCase.Data), ConvertToHex(inputLower), input);
        }

        {
            TOutputData copy{input.size()};
            CopyString(input, copy.Region);
            UNIT_ASSERT_VALUES_EQUAL(copy.Data, input);
            UNIT_ASSERT_VALUES_EQUAL(copy.Region.SizeConsumed(), input.size());

            TOutputData copyLower{inputLower.size()};
            CopyStringLowerCase(input, copyLower.Region);
            UNIT_ASSERT_VALUES_EQUAL(copyLower.Data, inputLower);
            UNIT_ASSERT_VALUES_EQUAL(copyLower.Region.SizeConsumed(), inputLower.size());
        }
    }

    static char ToLower(unsigned c) {
        return (char) (c >= 'A' && c <= 'Z' ? c - 'A' + 'a' : c);
    }

    void TestEverySymbol() {
        DoTestEncodeDecode("", "");

        for (unsigned i = 0; i < 256u; ++i) {
            {
                char arr1[]{char(i)};
                char arr1Lower[]{ToLower(i)};
                DoTestEncodeDecode({arr1, 1}, {arr1Lower, 1});
            }

            for (unsigned j = 0; j < 256u; ++j) {
                {
                    char arr2[2]{char(i), char(j)};
                    char arr2Lower[2]{ToLower(i), ToLower(j)};
                    DoTestEncodeDecode({arr2, 2}, {arr2Lower, 2});
                }
            }
        }

        for (unsigned i = 0; i < 256u; i += 3) {
            {
                char arr1[]{char(i)};
                char arr1Lower[]{ToLower(i)};
                DoTestEncodeDecode({arr1, 1}, {arr1Lower, 1});
            }

            for (unsigned j = 1; j < 256u; j += 3) {
                {
                    char arr2[2]{char(i), char(j)};
                    char arr2Lower[2]{ToLower(i), ToLower(j)};
                    DoTestEncodeDecode({arr2, 2}, {arr2Lower, 2});
                }

                for (unsigned k = 2; k < 256u; k += 3) {
                    char arr3[3]{char(i), char(j), char(k)};
                    char arr3Lower[3]{ToLower(i), ToLower(j), ToLower(k)};
                    DoTestEncodeDecode({arr3, 3}, {arr3Lower, 3});
                }
            }
        }
    }

    void TestFuzzerFound() {
        DoTestHuffman({HexDecode("929292929271ff00d66dc41fe5e5e52a7f7f"), "dmecmecha!0PiScaXC.fetZ"});
        DoTestEncodeDecode("dmecmecha!0PiScaXC.fetZ", "dmecmecha!0piscaxc.fetz");
    }

    const TVector<std::pair<TStringBuf, TStringBuf>> RFC7541ExamplesHuffman{
        {"\xf1\xe3\xc2\xe5\xf2\x3a\x6b\xa0\xab\x90\xf4\xff", "www.example.com"},
        {"\xa8\xeb\x10\x64\x9c\xbf",                         "no-cache"},
        {"\x25\xa8\x49\xe9\x5b\xa9\x7d\x7f",                 "custom-key"},
        {"\x25\xa8\x49\xe9\x5b\xb8\xe8\xb4\xbf",             "custom-value"}
    };
};


UNIT_TEST_SUITE_REGISTRATION(THPackStringsTest);
