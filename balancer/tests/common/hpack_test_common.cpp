#include "hpack_test_common.h"

#include <balancer/kernel/http2/server/common/http2_headers_fsm.h>
#include <balancer/kernel/http2/server/hpack/hpack.h>
#include <balancer/kernel/http2/server/hpack/hpack_definitions.h>
#include <balancer/kernel/http2/server/hpack/hpack_static_table.h>

#include <util/string/hex.h>
#include <util/string/builder.h>

namespace NSrvKernel::NHTTP2 {
    namespace NUt {
        NSc::TValue HeadersToJson(const THeadersList& headers, bool withIndexing, EHPackHeaderRule defaultRule) {
            NSc::TValue res;
            headers.ForEach([&res, withIndexing, defaultRule](const THeader* header) {
                auto& h = res.Push();
                h["name"] = ChunkListToString(header->Key);
                h["value"] = ChunkListToString(header->Value);
                if (withIndexing) {
                    h["indexing"] = IndexRuleToInt(header->HPackHeaderRule.GetOrElse(defaultRule));
                }
            });
            return res;
        }

        TString ChunkListToString(const TChunkList& lst) {
            const auto buf = Union(lst);
            return ConvertToHex(buf->AsStringBuf());
        }

        void StringToChunkList(TChunkList& lst, TStringBuf str) {
            lst = TChunkList{HexDecode(str)};
        }

        void JsonToHeaders(TVector<size_t>& sizeUpdates, THeadersList& headers, const NSc::TValue& json) {
            for (const auto& headerJson : json.GetArray()) {
                const TStringBuf type = headerJson.Get("type").GetString("literal");

                if (type == "size_update") {
                    sizeUpdates.push_back(headerJson.Get("value").GetIntNumber());
                    continue;
                }

                THolder<THeader> header = MakeHolder<THeader>();
                if (type == "indexed_field") {
                    auto headerField = TStaticTable::Instance().GetHeaderField(headerJson.Get("field_id").GetIntNumber());
                    header->Key = TChunkList{NewChunkNonOwning(headerField.Name)};
                    header->Value = TChunkList{NewChunkNonOwning(headerField.Value)};
                } else if (type == "indexed_name") {
                    auto headerName = TStaticTable::Instance().GetHeaderField(headerJson.Get("name_id").GetIntNumber());
                    header->Key = TChunkList{NewChunkNonOwning(headerName.Name)};
                    StringToChunkList(header->Value, headerJson.Get("value"));
                } else if (type == "literal") {
                    StringToChunkList(header->Key, headerJson.Get("name"));
                    StringToChunkList(header->Value, headerJson.Get("value"));
                } else {
                    ythrow yexception() << "unknown header item type " << type;
                }

                header->HPackHeaderRule = IntToIndexRule(headerJson.Get("indexing"));
                headers.PushBack(header.Release());
            }
        }

        ui32 IndexRuleToInt(EHPackHeaderRule rule) {
            switch (rule) {
            case EHPackHeaderRule::Index:
                return 0;
            case EHPackHeaderRule::DoNotIndex:
                return 1;
            case EHPackHeaderRule::NeverIndex:
                return 2;
            }
        }

        EHPackHeaderRule IntToIndexRule(ui32 val) {
            static const TVector<EHPackHeaderRule> rules{
                EHPackHeaderRule::Index,
                EHPackHeaderRule::DoNotIndex,
                EHPackHeaderRule::NeverIndex
            };
            return rules.at(val);
        }

        NSc::TValue TestHPackDecoder(const NSc::TValue& input, const THPackSettings& settings, ETestMode testMode) {
            THPackDecoder hpack{settings.Decoder};
            NSc::TValue output;
            for (size_t i = 0, sz = input.ArraySize(); i < sz; ++i) {
                auto& current = output.Push();

                THeadersList headers;

                try {
                    TryRethrowError(hpack.Decode(HexDecode(input.Get(i))).AssignTo(headers));

                    if (ETestMode::PYTHON == testMode) {
                        // The sanitizing had been done inside the hpack,
                        // but later I decided to move it to the headers processing stage in http/2.
                        // I like the sanitizing tests avdemin@ made for hpack so I reused them here.
                        for (const auto& header : headers) {
                            Y_ENSURE_EX(IsRequestHeaderSpecial(header.Key)
                                        || TStringBuf(RESP_HEADER_STATUS) == header.Key
                                        || IsRequestHeaderNameValid(header.Key),
                                        TConnectionError(EErrorCode::PROTOCOL_ERROR, TUnspecifiedReason()));

                            Y_ENSURE_EX(IsRequestHeaderValueValid(header.Value),
                                        TConnectionError(EErrorCode::PROTOCOL_ERROR, TUnspecifiedReason()));
                        }
                    }
                } catch (const TConnectionError& e) {
                    current = ::ToString(e.ErrorCode);
                    break;
                }

                current["headers"] = HeadersToJson(headers, true, settings.Encoder.DefaultHeaderRule);

                THeadersList table;
                size_t tableSize = 0;
                hpack.DumpDynamicTable(tableSize, table);
                current["table"] = HeadersToJson(table, false, settings.Encoder.DefaultHeaderRule);

                if (ETestMode::CPP == testMode) {
                    current["table_size"] = tableSize;
                }
            }
            return output;
        }

        NSc::TValue TestHPackEncoder(const NSc::TValue& input, const THPackSettings& settings) {
            THPackEncoder hpack{settings.Encoder};
            NSc::TValue output;
            for (size_t i = 0, sz = input.ArraySize(); i < sz; ++i) {
                TVector<size_t> sizeUpdates;
                THeadersList headers;

                JsonToHeaders(sizeUpdates, headers, input.Get(i));

                for (auto update : sizeUpdates) {
                    hpack.UpdateHeaderTableSize(update);
                }

                output.Push() = ChunkListToString(hpack.Encode(headers));
            }
            return output;
        }


        TStringBuf AsStrBuf(const void* data, size_t size) {
            return TStringBuf((const char*)data, size);
        }

        TStringBuf AsStrBuf(const void* begin, const void* end) {
            return AsStrBuf(begin, (const char*)end - (const char*)begin);
        }

        TStringBuf AsStrBuf(std::pair<const void*, size_t> p) {
            return AsStrBuf(p.first, p.second);
        }

        TStringBuf AsStrBuf(std::pair<const void*, const void*> p) {
            return AsStrBuf(p.first, p.second);
        }

        TStringBuf AsStrBuf(TStringBuf str) {
            return str;
        }

        TStringBuf AsStrBuf(const TBuffer& str) {
            return AsStrBuf(str.Data(), str.Size());
        }

        TString ConvertToHex(const void* data, size_t size) {
            auto s = HexEncode(data, size);
            s.to_lower();
            return s;
        }

        TString ConvertToHex(const void* begin, const void* end) {
            return ConvertToHex(begin, (const char*)end - (const char*)begin);
        }

        TString ConvertToHex(TStringBuf str) {
            return ConvertToHex(str.data(), str.size());
        }

        TString ConvertToHex(const TBuffer& str) {
            return ConvertToHex(str.Data(), str.Size());
        }


        TInputData::TInputData(TStringBuf d)
            : TDataBase(TString{d})
        {}

        TString TOutputData::MakeString(size_t len) {
            TString s;
            s.resize(len, 0);
            return s;
        }

        TOutputData::TOutputData(size_t len)
            : TDataBase(MakeString(len))
        {}


        TString TFuzzerData::ToString() const {
            TStringBuilder res;

            res << "fullWire='" << ConvertToHex(OriginalInput) << "'";
            res << " encoderSettings=" << Settings.Encoder.ToString() << "";
            res << " decoderSettings=" << Settings.Decoder.ToString() << "";
            res << " headerBlocks=[";
            for (const auto& hb : HeaderBlocks) {
                res << '"' << ConvertToHex(hb) << '"' << ", ";
            }
            if (TStringBuf(res).EndsWith(", ")) {
                res.pop_back();
                res.pop_back();
            }
            res << "]";
            return res;
        }


        bool ParseFuzzerInput(TFuzzerData& data, const TStringBuf wireData) {
            TInputRegion wire{wireData};

            if (wire.SizeAvailable() < 3) {
                return false;
            }

            data.Settings.SetMaxHeaderTableSize(wire[0]);
            wire.Consume(1);
            data.Settings.Encoder.MaxFrameSize = (ui8)(wire[1]) + 1;
            wire.Consume(1);

            const size_t numReqs = (wire[0] & 0x3) + 1;
            Y_HPACK_ASSERT_LE_C(numReqs, 4, ConvertToHex(wireData));

            const size_t lengthsLen = (numReqs * 6 + 7 + 2) / 8;

            if (wire.SizeAvailable() < lengthsLen) {
                return false;
            }

            ui32 lengths = 0;
            memcpy(&lengths, wire.begin(), lengthsLen);
            wire.Consume(lengthsLen);

            lengths >>= 2;
            for (size_t n = 0; n < numReqs && wire; ++n) {
                auto len = std::min<size_t>(wire.SizeAvailable(), lengths & 0x3F);
                data.HeaderBlocks.emplace_back(wire.AsStringBuf(len));
                lengths >>= 6;
                wire.Consume(len);
            }

            data.OriginalInput.assign(wireData);
            return true;
        }

        void TestHPackOnFuzzerInput(const TFuzzerData& data) {
            TVector<THeadersList> headerLists;
            {
                THPackDecoder hpack{data.Settings.Decoder};
                for (const auto& headerBlock : data.HeaderBlocks) {
                    try {
                        THeadersList headers;
                        TryRethrowError(hpack.Decode(headerBlock).AssignTo(headers));
                        headerLists.emplace_back(std::move(headers));
                    } catch (const TConnectionError&) {
                        return;
                    }
                }
            }

            TVector<TChunkList> headerBlocks;
            {
                THPackEncoder hpack{data.Settings.Encoder};
                for (const auto& headerList : headerLists) {
                    headerBlocks.emplace_back(hpack.Encode(headerList));
                }
            }

            TVector<THeadersList> headerLists2;
            {
                THPackDecoder hpack{data.Settings.Decoder};
                for (size_t n = 0, sz = headerBlocks.size(); n < sz; ++n) {
                    const auto& headerBlock = headerBlocks[n];
                    try {
                        const auto buf = Union(headerBlock);
                        THeadersList headers;
                        TryRethrowError(hpack.Decode(buf->AsStringBuf()).AssignTo(headers));
                        headerLists2.emplace_back(std::move(headers));
                    } catch (...) {
                        const auto buf = Union(headerBlock);
                        Y_HPACK_FAIL_C(
                            data.ToString()
                                << " @" << n << " " << CurrentExceptionMessage()
                                << " @" << ConvertToHex(buf->AsStringBuf())
                        );
                    }
                }
            }

            Y_HPACK_ASSERT_EQ_C(headerLists.size(), headerLists2.size(), data.ToString());
            Y_HPACK_ASSERT_EQ_C(data.HeaderBlocks.size(), headerBlocks.size(), data.ToString());

            for (size_t n = 0, sz = headerLists.size(); n < sz; ++n) {
                const auto& curHeaders2 = headerLists2[n];
                const auto& curHeaders = headerLists[n];
                Y_HPACK_ASSERT_EQ_C(curHeaders.Size(), curHeaders2.Size(), data.ToString() << " @" << n);

                size_t idx = 0;
                auto iter2 = curHeaders2.CBegin();
                auto iter = curHeaders.CBegin();
                const auto end2 = curHeaders2.CEnd();
                const auto end = curHeaders.CEnd();

                for (; iter != end && iter2 != end2; ++iter, ++iter2, ++idx) {
                    const auto iterKeyBuf = Union(iter->Key);
                    const auto iter2KeyBuf = Union(iter2->Key);
                    Y_HPACK_ASSERT_STR_EQ_NOCASE_C(
                        iter2KeyBuf->AsStringBuf(),
                        iterKeyBuf->AsStringBuf(),
                        data.ToString() << " @" << n << ":" << idx
                    );
                    const auto iterValueBuf = Union(iter->Value);
                    const auto iter2ValueBuf = Union(iter2->Value);
                    Y_HPACK_ASSERT_STR_EQ_C(
                        iter2ValueBuf->AsStringBuf(),
                        iterValueBuf->AsStringBuf(),
                        data.ToString() << " @" << n << ":" << idx
                    );
                    Y_HPACK_ASSERT_EQ_C(
                        (iter2->HPackHeaderRule == EHPackHeaderRule::NeverIndex),
                        (iter->HPackHeaderRule == EHPackHeaderRule::NeverIndex),
                        data.ToString() << " @" << n << ":" << idx
                    );
                }
            }
        }
    }
}
