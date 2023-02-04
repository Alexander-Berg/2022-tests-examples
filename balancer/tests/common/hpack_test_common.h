#pragma once

#include <balancer/kernel/http2/server/hpack/hpack_definitions.h>
#include <balancer/kernel/http/parser/http.h>

#include <library/cpp/scheme/scheme.h>

#include <util/generic/bt_exception.h>
#include <util/generic/buffer.h>
#include <util/generic/strbuf.h>
#include <util/generic/string.h>
#include <util/generic/yexception.h>
#include <util/string/ascii.h>

namespace NSrvKernel::NHTTP2 {
    namespace NUt {
        NSc::TValue HeadersToJson(const THeadersList& headers, bool withIndexing, EHPackHeaderRule defaultRule);

        TString ChunkListToString(const TChunkList& lst);

        void StringToChunkList(TChunkList& lst, TStringBuf str);

        void JsonToHeaders(THeadersList& headers, const NSc::TValue& json);

        ui32 IndexRuleToInt(EHPackHeaderRule rule);

        EHPackHeaderRule IntToIndexRule(ui32 val);

        enum class ETestMode {
            CPP, PYTHON
        };

        struct THPackSettings {
            THPackEncoderSettings Encoder;
            THPackDecoderSettings Decoder;

            void SetMaxHeaderTableSize(ui32 newSize) {
                Encoder.MaxHeaderTableSize = newSize;
                Decoder.MaxHeaderTableSize = newSize;
            }
        };

        NSc::TValue TestHPackDecoder(const NSc::TValue& input, const THPackSettings& settings, ETestMode testMode);

        NSc::TValue TestHPackEncoder(const NSc::TValue& input, const THPackSettings& settings);

        TStringBuf AsStrBuf(const void*, size_t);
        TStringBuf AsStrBuf(const void*, const void*);
        TStringBuf AsStrBuf(std::pair<const void*, size_t>);
        TStringBuf AsStrBuf(std::pair<const void*, const void*>);
        TStringBuf AsStrBuf(TStringBuf);
        TStringBuf AsStrBuf(const TBuffer&);

        TString ConvertToHex(const void*, size_t);
        TString ConvertToHex(const void*, const void*);
        TString ConvertToHex(TStringBuf);
        TString ConvertToHex(const TBuffer&);


        template <class TRegion>
        struct TDataBase {
            TString Data;
            TRegion Region;

            TDataBase(TString data)
                : Data(data)
                , Region(Data)
            {
                Y_ENSURE(Region.SizeAvailable() == Data.size());
                Y_ENSURE((const char*)Region.data() == (const char*)Data.data());
            }

            TString ToString() const {
                return ConvertToHex(TStringBuf(Data).SubStr(0, Region.SizeConsumed())) + "."
                    + ConvertToHex(TStringBuf(Data).SubStr(Region.SizeConsumed()));
            }
        };

        struct TInputData : public TDataBase<TInputRegion> {
        public:
            TInputData(TStringBuf d);
        };


        struct TOutputData : public TDataBase<TOutputRegion> {
        public:
            TOutputData(size_t len);

            static TString MakeString(size_t len);
        };


        struct TFuzzerData {
            THPackSettings Settings;
            TVector<TString> HeaderBlocks;
            TString OriginalInput;

            TString ToString() const;
        };


        bool ParseFuzzerInput(TFuzzerData& data, const TStringBuf wireData);

        void TestHPackOnFuzzerInput(const TFuzzerData& data);

#define Y_HPACK_ASSERT_C(cond, mess) \
        Y_ENSURE_EX(cond, TWithBackTrace<yexception>() << Y_STRINGIZE(cond) << " assertion failed " << mess)

#define Y_HPACK_FAIL_C(mess) \
        Y_HPACK_ASSERT_C(false, mess)

#define Y_HPACK_ASSERT_EQ_C(a, b, mess) Y_HPACK_ASSERT_C((a) == (b), \
        " (" << (a) << " == " << (b) << ") " << mess)

#define Y_HPACK_ASSERT_STR_EQ_C(a, b, mess) Y_HPACK_ASSERT_C(AsStrBuf(a) == AsStrBuf(b), \
        " (" << ConvertToHex(a) << " == " << ConvertToHex(b) << ") " << mess)

#define Y_HPACK_ASSERT_STR_EQ_NOCASE_C(a, b, mess) Y_HPACK_ASSERT_C(AsciiEqualsIgnoreCase(AsStrBuf(a), AsStrBuf(b)), \
        " (" << ConvertToHex(a) << " == " << ConvertToHex(b) << ") " << mess)

#define Y_HPACK_ASSERT_LE_C(a, b, mess) Y_HPACK_ASSERT_C(((a) < (b)) || ((a) == (b)), \
        " (" << (a) << " <= "  << (b) << ") " << mess)

    }
}
