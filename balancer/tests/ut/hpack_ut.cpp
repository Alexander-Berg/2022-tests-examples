#define HPACK_UT_MODE
#include <balancer/kernel/http2/server/hpack/tests/common/hpack_test_common.h>

#include <library/cpp/scheme/ut_utils/scheme_ut_utils.h>
#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/hex.h>
#include <util/string/split.h>
#include <util/string/subst.h>

namespace NSrvKernel::NHTTP2 {
    namespace NUt {
        TString NormalizeHex(TStringBuf str) {
            TString tmp = TString{str};
            SubstGlobal(tmp, TStringBuf(" "), TStringBuf());
            tmp.to_upper();
            return tmp;
        }

        struct THeaderFieldExample {
            TString Name;
            TString Value;
            ui32 Indexing = 0;

            THeaderFieldExample(TStringBuf nameValue, ui32 indexing = 0)
                : Indexing(indexing)
            {
                Split(nameValue, ": ", Name, Value);
            }

            THeaderFieldExample(TStringBuf name, TStringBuf value, ui32 indexing = 0)
                : Name(name)
                , Value(value)
                , Indexing(indexing)
            {}

            template <size_t N>
            THeaderFieldExample(const char (&nameValue)[N], ui32 indexing = 0)
                : THeaderFieldExample(TStringBuf(nameValue, N - 1), indexing)
            {}

            template <size_t N, size_t M>
            THeaderFieldExample(const char (&name)[N], const char (&value)[M], ui32 indexing = 0)
                : THeaderFieldExample(TStringBuf(name, N - 1), TStringBuf(value, M - 1), indexing)
            {}
        };

        enum class EDecodeStatus {
            OK, CompressionError, ProtocolError
        };

        struct THeaderListExample {
            TString Encoded;
            TString Reencoded;
            TVector<THeaderFieldExample> Headers;
            TVector<THeaderFieldExample> Table;
            size_t TableSize = 0;
            EDecodeStatus Status = EDecodeStatus::OK;

            explicit THeaderListExample(EDecodeStatus status = EDecodeStatus::OK)
                : Status(status)
            {}

            THeaderListExample& SetEncoded(TStringBuf encoded) {
                Encoded = HexDecode(NormalizeHex(encoded));
                return *this;
            }

            THeaderListExample& SetReencoded(TStringBuf reencoded) {
                Reencoded = HexDecode(NormalizeHex(reencoded));
                return *this;
            }

            THeaderListExample& SetHeaders(TVector<THeaderFieldExample> headers) {
                Headers = headers;
                return *this;
            }

            THeaderListExample& SetTable(TVector<THeaderFieldExample> table, size_t sz) {
                Table = table;
                TableSize = sz;
                return *this;
            }
        };

        struct THPackSessionExample {
            THPackSettings Settings;
            TVector<THeaderListExample> HeaderLists;

            THPackSessionExample(size_t maxTableSize, bool enableHuffman, TVector<THeaderListExample> tests, size_t maxFrameSize)
                : HeaderLists(tests)
            {
                Settings.SetMaxHeaderTableSize(maxTableSize);
                Settings.Encoder.EnableHuffman = enableHuffman;
                Settings.Encoder.MaxFrameSize = maxFrameSize;
                Settings.Encoder.DefaultHeaderRule = EHPackHeaderRule::NeverIndex;
            }

            bool IsOk() const {
                for (const auto& example : HeaderLists) {
                    if (EDecodeStatus::OK != example.Status) {
                        return false;
                    }
                }
                return true;
            }
        };

        NSc::TValue GenerateHeadersTest(const TVector<THeaderFieldExample>& headers, bool withIndexing) {
            NSc::TValue res;
            for (const auto& header : headers) {
                auto& h = res.Push();
                h["name"] = ConvertToHex(header.Name);
                h["value"] = ConvertToHex(header.Value);
                if (withIndexing) {
                    h["indexing"] = header.Indexing;
                }
            }
            return res;
        }

        NSc::TValue GenerateDecoderTest(const TVector<THeaderListExample>& tests) {
            NSc::TValue res;
            for (const auto& test : tests) {
                res["input"].Push() = ConvertToHex(test.Encoded);
                auto& output = res["output"].Push();
                switch (test.Status) {
                case EDecodeStatus::OK:
                    output["headers"] = GenerateHeadersTest(test.Headers, true);
                    output["table"] = GenerateHeadersTest(test.Table, false);
                    output["table_size"] = test.TableSize;
                    break;
                case EDecodeStatus::CompressionError:
                    output = "COMPRESSION_ERROR";
                    return res;
                case EDecodeStatus::ProtocolError:
                    output = "PROTOCOL_ERROR";
                    return res;
                }
            }
            return res;
        }

        NSc::TValue GenerateEncoderTest(const TVector<THeaderListExample>& tests) {
            NSc::TValue res;
            for (const auto& test : tests) {
                res["output"].Push() = ConvertToHex(test.Reencoded ? test.Reencoded : test.Encoded);
                res["input"].Push() = GenerateHeadersTest(test.Headers, true);
            }
            return res;
        }

        NSc::TValue ConvertDecoderOutputToEncoderInput(const NSc::TValue& decOut) {
            NSc::TValue inp;
            for (const auto& dec : decOut.GetArray()) {
                if (dec.IsDict()) {
                    inp.Push() = dec["headers"];
                } else {
                    inp.Push() = dec;
                }
            }
            return inp;
        }
    }
}

using namespace NSrvKernel::NHTTP2;

class THPackTest : public TTestBase {
UNIT_TEST_SUITE(THPackTest);
    UNIT_TEST(TestRFC7541RequestExampleNoHuffman);
    UNIT_TEST(TestRFC7541RequestExampleHuffman);
    UNIT_TEST(TestRFC7541ResponseExampleNoHuffman);
    UNIT_TEST(TestRFC7541ResponseExampleHuffman);
    UNIT_TEST(TestFuzzerDetected)
UNIT_TEST_SUITE_END();
private:
    void DoTestDecoder(const NSc::TValue& input, const NSc::TValue& output) {
        using namespace NSrvKernel::NHTTP2;
        UNIT_ASSERT_JSON_EQ_JSON(
            NUt::TestHPackDecoder(input, NUt::THPackSettings(), NUt::ETestMode::CPP),
            output
        );
    }

    TString ToLowerAscii(TStringBuf str) {
        TString result;
        result.reserve(str.size());
        for (size_t i = 0, sz = str.size(); i < sz; ++i) {
            result.append(AsciiToLower(str[i]));
        }
        return result;
    }

    NSc::TValue NamesToLowerHex(const NSc::TValue& test) {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        NSc::TValue result;
        if (test.IsArray()) {
            for (const auto& value : test.GetArray()) {
                result.Push() = NamesToLowerHex(value);
            }
        } else if (test.IsDict()) {
            for (const auto key : test.DictKeys()) {
                const auto& value = test[key];
                if ("name" == key && value.IsString()) {
                    result[key] = ConvertToHex(ToLowerAscii(HexDecode(value)));
                } else {
                    result[key] = NamesToLowerHex(value);
                }
            }
        } else {
            result = test;
        }
        return result;
    }

    void DoTestExample(const NUt::THPackSessionExample& example) {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;
        auto decTest = GenerateDecoderTest(example.HeaderLists);
        auto encTest = GenerateEncoderTest(example.HeaderLists);

        UNIT_ASSERT_JSON_EQ_JSON(
            NamesToLowerHex(
                TestHPackDecoder(decTest["input"], example.Settings, NUt::ETestMode::CPP)
            ),
            decTest["output"]
        );

        if (example.IsOk()) {
            auto encoded = TestHPackEncoder(encTest["input"], example.Settings);

            UNIT_ASSERT_JSON_EQ_JSON(encoded, encTest["output"]);

            UNIT_ASSERT_JSON_EQ_JSON(
                ConvertDecoderOutputToEncoderInput(
                    TestHPackDecoder(encTest["output"], example.Settings, NUt::ETestMode::CPP)
                ),
                encTest["input"]
            );
        }
    }

    void TestRFC7541RequestExampleNoHuffman() {
        DoTestExample(RequestsNoHuffman);
    }

    void TestRFC7541RequestExampleHuffman() {
        DoTestExample(Requests);
    }

    void TestRFC7541ResponseExampleNoHuffman() {
        DoTestExample(ResponsesNoHuffman);
    }

    void TestRFC7541ResponseExampleHuffman() {
        DoTestExample(Responses);
    }

    void TestFuzzerDetected() {
        using namespace NSrvKernel::NHTTP2;
        using namespace NUt;

        for (const auto& example : FuzzerFoundRaw) {
            TFuzzerData data;
            ParseFuzzerInput(data, Base64Decode(example));
            Cerr << data.ToString() << Endl;
            TestHPackOnFuzzerInput(data);
        }

        for (const auto& example : FuzzerFound) {
            DoTestExample(example);
        }
    }

    const TVector<TString> FuzzerFoundRaw{/*"IQcCIQCmpqampqarpg=="*/};

    const TVector<NUt::THPackSessionExample> FuzzerFound{
        {217, true,
            {NUt::THeaderListExample()
                .SetEncoded("4400")
                .SetHeaders({{":path: "}})
                .SetTable({{":path: "}}, 37)}
        , RFC_MAX_FRAME_SIZE_MIN},
        {109, true,
            {NUt::THeaderListExample(NUt::EDecodeStatus::CompressionError)
                .SetEncoded("6d 00 bf")}
        , RFC_MAX_FRAME_SIZE_MIN},
        {217, true,
            {NUt::THeaderListExample()
                .SetEncoded("11004400")
                .SetHeaders({{":authority: ", 2}, {":path: "}})
                .SetTable({{":path: "}}, 37)}
        , RFC_MAX_FRAME_SIZE_MIN},
        {0, true,
            {NUt::THeaderListExample()
                .SetEncoded("0080822897")
                .SetReencoded("0000822897")
                .SetHeaders({{": \x65\x32\x2e", 1}})}
        , RFC_MAX_FRAME_SIZE_MIN},
        {255, true,
            {NUt::THeaderListExample()
                .SetEncoded(  "0092929292929271ff00d66dc41fe5e5e52a7f7f00")
                .SetReencoded("0091929292929271ff00acc820fc91794a9f7f00")
                .SetHeaders({{"dmecmecha!0piscaxc.fetz", "", 1},})
            }
        , RFC_MAX_FRAME_SIZE_MIN},
        {33, true,
            {NUt::THeaderListExample()
                .SetEncoded("a6a6a6a6a6a6aba6")
                .SetReencoded("a6a6a6a6a6a6aba6")
                .SetHeaders({
                    {"host: "},
                    {"host: "},
                    {"host: "},
                    {"host: "},
                    {"host: "},
                    {"host: "},
                    {"if-unmodified-since: "},
                    {"host: "}
                })
            }
        , 3}
    };

    const NUt::THPackSessionExample Requests{256, true, {
        NUt::THeaderListExample()
            .SetEncoded("8286 8441 8cf1 e3c2 e5f2 3a6b a0ab 90f4 ff")
            .SetHeaders({{":method: GET"}, {":scheme: http"}, {":path: /"}, {":authority: www.example.com"}})
            .SetTable({{":authority: www.example.com"}}, 57),
        NUt::THeaderListExample()
            .SetEncoded("8286 84be 5886 a8eb 1064 9cbf")
            .SetHeaders({{":method: GET"}, {":scheme: http"}, {":path: /"}, {":authority: www.example.com"},
                         {"cache-control: no-cache"}})
            .SetTable({{"cache-control: no-cache"}, {":authority: www.example.com"}}, 110),
        NUt::THeaderListExample()
            .SetEncoded("8287 85bf 4088 25a8 49e9 5ba9 7d7f 8925 a849 e95b b8e8 b4bf")
            .SetHeaders({{":method: GET"}, {":scheme: https"}, {":path: /index.html"}, {":authority: www.example.com"},
                         {"custom-key: custom-value"}})
            .SetTable({{"custom-key: custom-value"}, {"cache-control: no-cache"}, {":authority: www.example.com"}}, 164),
    }, RFC_MAX_FRAME_SIZE_MIN};

    const NUt::THPackSessionExample RequestsNoHuffman{256, false, {
        NUt::THeaderListExample(Requests.HeaderLists[0]).SetEncoded("8286 8441 0f77 7777 2e65 7861 6d70 6c65 2e63 6f6d"),
        NUt::THeaderListExample(Requests.HeaderLists[1]).SetEncoded("8286 84be 5808 6e6f 2d63 6163 6865"),
        NUt::THeaderListExample(Requests.HeaderLists[2]).SetEncoded("8287 85bf 400a 6375 7374 6f6d 2d6b 6579 0c63 7573 746f 6d2d 7661 6c75 65"),
    }, RFC_MAX_FRAME_SIZE_MIN};

    const NUt::THPackSessionExample Responses{256, true, {
        NUt::THeaderListExample()
            .SetEncoded(
                "4882 6402 5885 aec3 771a 4b61 96d0 7abe 9410 54d4 44a8 2005 9504 0b81 66e0 82a6 2d1b ff6e 919d 29ad "
                "1718 63c7 8f0b 97c8 e9ae 82ae 43d3")
            .SetHeaders({{":status: 302"}, {"cache-control: private"}, {"date: Mon, 21 Oct 2013 20:13:21 GMT"},
                         {"location: https://www.example.com"}})
            .SetTable({{"location: https://www.example.com"}, {"date: Mon, 21 Oct 2013 20:13:21 GMT"},
                       {"cache-control: private"}, {":status: 302"}}, 222),
        NUt::THeaderListExample()
            .SetEncoded("4883 640e ffc1 c0bf")
            .SetReencoded("4803 3330 37c1 c0bf") // we skip huffman if it gives no length reduction
            .SetHeaders({{":status: 307"}, {"cache-control: private"}, {"date: Mon, 21 Oct 2013 20:13:21 GMT"},
                         {"location: https://www.example.com"}})
            .SetTable({{":status: 307"}, {"location: https://www.example.com"}, {"date: Mon, 21 Oct 2013 20:13:21 GMT"},
                       {"cache-control: private"}}, 222),
        NUt::THeaderListExample()
            .SetEncoded(
                "88c1 6196 d07a be94 1054 d444 a820 0595 040b 8166 e084 a62d 1bff c05a 839b d9ab 77ad 94e7 821d d7f2 "
                "e6c7 b335 dfdf cd5b 3960 d5af 2708 7f36 72c1 ab27 0fb5 291f 9587 3160 65c0 03ed 4ee5 b106 3d50 07")
            .SetHeaders({{":status: 200"}, {"cache-control: private"}, {"date: Mon, 21 Oct 2013 20:13:22 GMT"},
                         {"location: https://www.example.com"}, {"content-encoding: gzip"},
                         {"set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1"}})
            .SetTable({{"set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1"}, {"content-encoding: gzip"},
                       {"date: Mon, 21 Oct 2013 20:13:22 GMT"}}, 215),
    }, RFC_MAX_FRAME_SIZE_MIN};

    const NUt::THPackSessionExample ResponsesNoHuffman{256, false, {
        NUt::THeaderListExample(Responses.HeaderLists[0]).SetEncoded(
                "4803 3330 3258 0770 7269 7661 7465 611d 4d6f 6e2c 2032 3120 4f63 7420 3230 3133 2032 303a 3133 3a32 "
                "3120 474d 546e 1768 7474 7073 3a2f 2f77 7777 2e65 7861 6d70 6c65 2e63 6f6d"),
        NUt::THeaderListExample(Responses.HeaderLists[1]).SetEncoded("4803 3330 37c1 c0bf"),
        NUt::THeaderListExample(Responses.HeaderLists[2]).SetEncoded(
                "88c1 611d 4d6f 6e2c 2032 3120 4f63 7420 3230 3133 2032 303a 3133 3a32 3220 474d 54c0 5a04 677a 6970 "
                "7738 666f 6f3d 4153 444a 4b48 514b 425a 584f 5157 454f 5049 5541 5851 5745 4f49 553b 206d 6178 2d61 "
                "6765 3d33 3630 303b 2076 6572 7369 6f6e 3d31"),
    },  RFC_MAX_FRAME_SIZE_MIN};
};

UNIT_TEST_SUITE_REGISTRATION(THPackTest);
