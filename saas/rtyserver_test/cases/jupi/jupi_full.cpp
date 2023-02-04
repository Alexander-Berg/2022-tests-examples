#include <saas/rtyserver_test/cases/jupi/jupi_erf.h>
#include <saas/rtyserver_test/cases/oxy/oxy.h>

#include <robot/library/plutonium/protos/doc_wad_lumps.pb.h>
#include <kernel/doom/wad/wad_lump_id.h>
#include <kernel/doom/wad/mega_wad_reader.h>
#include <kernel/doom/offroad_omni_wad/omni_io.h>
#include <kernel/doom/offroad_erf_wad/erf_io.h>

#include <kernel/struct_codegen/print/struct_print.h>
#include <ysite/yandex/erf_format/erf_format.h>
#include <yweb/protos/indexeddoc.pb.h>
#include <util/system/fs.h>
#include <util/system/event.h>
#include <util/generic/array_ref.h>

using namespace NRTYServer;

// A smoke test for the indexation and search, using Oxy+Jupi combination
START_TEST_DEFINE_PARENT(TestOxygenJupiFull, TestOxygenDocs)

using TErfByUrl = TMap<TString, TBlob>;
static TErfByUrl DumpExpectedErfs(const TVector<NRTYServer::TMessage>& messages) {
    TErfByUrl expected;
    for (const NRTYServer::TMessage& message : messages) {
        Y_VERIFY(message.HasDocument() && message.GetDocument().HasIndexedDoc());
        const NRealTime::TIndexedDoc& doc = message.GetDocument().GetIndexedDoc();
        TBlob erfwad;
        bool found = false;
        for(size_t i = 0; i < doc.WadLumpsSize() && !found; ++i) {
            const NRealTime::TIndexedDoc::TMapType& entry = doc.GetWadLumps(i);
            if (entry.GetName() == "erf_doc_lumps"){
                NPlutonium::TDocWadLumps wadLumps;
                TStringBuf serialized = entry.GetData();
                const bool parsedOk = wadLumps.ParseFromArray(serialized.data(), serialized.size());
                Y_ENSURE(parsedOk);
                for(size_t j = 0; j < wadLumps.MutableLumpsList()->LumpsSize() && !found; ++i) {
                     const NPlutonium::TWadLump& lump = wadLumps.GetLumpsList().GetLumps(j);
                     NDoom::TWadLumpId id = FromString<NDoom::TWadLumpId>(lump.GetId());
                     if (id.Index == NDoom::EWadIndexType::ErfIndexType) {
                         erfwad = TBlob::FromString(lump.GetData());
                         found = true;
                     }
                }
            }
        }
        constexpr TStringBuf httpPrefix = "http://";
        TString url = doc.GetUrl();
        if (url.StartsWith(httpPrefix))
            url = url.substr(httpPrefix.size(), TString::npos);
        expected.insert(std::make_pair<TString, TBlob>(std::move(url), std::move(erfwad)));
    }
    return expected;
}

static TVector<TString> DumpUrlsFromOmni(const TFsPath& finalIndex, size_t expectedSize) {
    using namespace NDoom;
    THolder<IWad> omniWad = IWad::Open(finalIndex / "index.omni.wad"); // source for urls
    Y_ENSURE(omniWad->Size() == expectedSize);

    TVector<TString> urls;
    urls.reserve(omniWad->Size() + 1);
    {
        TOmniUrlIo::TReader omniReader(omniWad.Get());
        for (ui32 docId = 0; docId < omniWad->Size(); ++docId) {
            if (!omniReader.ReadDoc(&docId))
                continue;
            TStringBuf value;
            if (omniReader.ReadHit(&value)) {
                if (docId >= urls.size())
                    urls.resize(docId + 1);
                urls[docId] = value;
                DEBUG_LOG << "docId=" << docId << "\turl=" << value << Endl;
            }
        }
    }
    return urls;
}

static TErfByUrl DumpActualErfs(const TFsPath& finalIndex) {
    using namespace NDoom;

    TErfByUrl result;
    TVector<size_t> typeMapping;

    auto wad = IWad::Open(finalIndex / "indexerf2.wad");
    TVector<TString> urls = DumpUrlsFromOmni(finalIndex, wad->Size());

    const TVector<TWadLumpId> type{TWadLumpId(EWadIndexType::ErfIndexType, EWadLumpRole::Struct)};
    typeMapping.resize(type.size());
    wad->MapDocLumps(type, typeMapping);

    for(size_t docId = 0; docId < wad->Size(); docId++) {
        const TString& url = urls[docId];
        Y_VERIFY(!url.empty());

        TVector<TArrayRef<const char>> regs;
        regs.resize(1);
        wad->LoadDocLumps(docId, typeMapping, regs);

        result[url] = TBlob::Copy(regs[0].data(), regs[0].size());
    }
    return result;
}


using TErfName2Value = TMap<TString, TString>;
using TErfHit = NDoom::TErf2Io::TSearcher::THit;

template <typename TErfStruct>
class TErfFieldsVisitor : public TErfStruct::IFieldsVisitor {
private:
    TErfName2Value& Map;

public:
    TErfFieldsVisitor(TErfName2Value& m)
        : Map(m)
    {
    }

    void Visit(const char* name, const ui32& value) override {
        Map[name] = ToString(value);
    }

    void Visit(const char* name, const float& value) override {
        Map[name] = ToString(value);
    }

    static TErfName2Value Dump(const TErfStruct& erf) {
        TErfName2Value result;
        TErfFieldsVisitor<TErfStruct> coll(result);
        NErf::VisitFields(erf, coll);
        return result;
    }
};


static bool EqualErfs(const TString& displayKey, const TErfHit& erf2Actual, const TErfHit& erf2Expected) {
    TErfName2Value mapActual = TErfFieldsVisitor<SDocErf2Info>::Dump(*erf2Actual);
    TErfName2Value mapExpected = TErfFieldsVisitor<SDocErf2Info>::Dump(*erf2Expected);

    bool differ = false;

    for (TErfName2Value::const_iterator it = mapExpected.begin() ; it != mapExpected.end() ; ++it) {
        TString actual = mapActual[it->first];
        TString expected = it->second;

        if (actual != expected) {
            if (!differ) {
                differ = true;
                if (displayKey) {
                    WARNING_LOG << "Erf differs for url=" << displayKey << Endl;
                }
                WARNING_LOG << "    field name | actual | expected" << Endl;
            }
            WARNING_LOG << "    " << it->first << " | " << actual << " | " << expected << Endl;
        }
    }
    return !differ;
}

static ui32 CompareAllErfs(const TErfByUrl& expectedErfs, TErfByUrl& actualErfs) {
    ui32 nErrors = 0;
    for (auto kv : expectedErfs) {
        const TString& url = kv.first;
        auto iActualErf = actualErfs.find(url);
        if (iActualErf == actualErfs.end()) {
            ERROR_LOG << "A document is not found: url=" << url << Endl;
            nErrors++;
            continue;
        }

        const auto& actualErf = iActualErf->second;
        const auto& expectedErf = kv.second;
        if (actualErf.Size() != expectedErf.Size()) {
            if (actualErf.Size() == 0 && expectedErf.Size() != 0) {
                ERROR_LOG << "Erf missing for url=" << url << Endl;
                nErrors++;
                continue;
            } else {
                WARNING_LOG << "Erf size differs (" << expectedErf.Size() << "!=" << actualErf.Size() << ") for url=" << url << Endl;
            }
        }

        if (0 != memcmp(actualErf.AsCharPtr(), expectedErf.AsCharPtr(), Min(expectedErf.Size(), actualErf.Size())) &&
            !EqualErfs(url, reinterpret_cast<const TErfHit>(actualErf.AsCharPtr()), reinterpret_cast<const TErfHit>(expectedErf.AsCharPtr()))) {
            nErrors++;
        }

        actualErfs.erase(iActualErf);
    }

    for (const auto& kv : actualErfs) {
        const TString& url = kv.first;
        ERROR_LOG << "A document should not be there: url=" << url << Endl;
        nErrors++;
    }
    return nErrors;
}

bool ReopenAndMerge()
{
    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CHECK_TEST_TRUE(Controller->GetFinalIndexes(false).size() == 1);
    return true;
}

static TVector<std::pair<ui32, ui32>> ExpectedRanges(const ui32 begin, const ui32 end, const TVector<ui32>& deletions) {
    TVector<std::pair<ui32, ui32>> result;
    for(ui32 p0 = begin; p0 <= end; ) {
        ui32 p = end + 1; // past the end
        for (ui32 deletion : deletions) {
            if (deletion >= p0) {
                p = deletion;
                break;
            }
        }
        if (p > p0) {
            result.emplace_back(p0, p - p0);
        }
        p0 = p + 1; // p points to deleted doc
    }

    return result;

}

bool Run() override {
    if (GetIsPrefixed())
        return true;
    const auto* path = "oxy/test_jupi_100/docs";
    GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, 40, false, true, 1);
    GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, 3, false, true, 37); //same-segment upd
    GenerateDocs(path, NRTYServer::TMessage::DELETE_DOCUMENT, 1, false, true, 30); //delete from 1st
    GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, 3, false, true, 15); //update in 1st
    IndexMessages(Messages, REALTIME, 1);
    CHECK_TEST_TRUE(ReopenAndMerge());
    CHECK_TEST_TRUE(NFs::Exists(TString(*Controller->GetFinalIndexes(false).cbegin()) + "/indexerf2.wad"));

    Messages.clear();
    GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, 20, false, true, 41);
    GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, 1, false, true, 35); //upd
    GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, 1, false, true, 55); //upd
    IndexMessages(Messages, REALTIME, 1);
    CHECK_TEST_TRUE(ReopenAndMerge());

    Messages.clear();
    GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, 20, false, true, 61);
    GenerateDocs(path, NRTYServer::TMessage::DELETE_DOCUMENT, 1, false, true, 1); //delete from main
    GenerateDocs(path, NRTYServer::TMessage::DELETE_DOCUMENT, 1, false, true, 65); //delete from delta
    GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, 4, false, true, 35); //upd
    IndexMessages(Messages, REALTIME, 1);
    CHECK_TEST_TRUE(ReopenAndMerge());

    TVector<TDocSearchInfo> results;
    const TString refreshProns = "&pron=termsearchsafe&pron=disable_legacy_quorum_factors_on_l2"
        "&pron=disable_legacy_quorum_factors_on_l3&pron=reqbundle_l2&pron=reqbundle_l3"; //TODO(yrum): shorten this after REFRESH-278
    QuerySearch("effectively&pron=st0" + GetAllKps(Messages) + refreshProns, results);

    if (results.size() == 0) {
        ythrow yexception() << "no search results";
    }

    //realistic (as for 201801) search request - check for crash
    auto query = LoadPantherQuery();
    ProcessQuery(query, nullptr);

    auto finalIndexes = Controller->GetFinalIndexes(/*stopServer=*/true);
    CHECK_TEST_TRUE(finalIndexes.size() == 1);
    TFsPath finalIndex(*finalIndexes.cbegin());
    for (const auto fileName : { "indexerf2.wad", "index.omni.wad" }) {
        CHECK_TEST_TRUE((finalIndex / fileName).Exists());
    }

    // Check for ERF correctness
    Messages.clear();
    for(auto r : ExpectedRanges(1, 80, /*deletions=*/{1, 30, 65})) {
        GenerateDocs(path, NRTYServer::TMessage::MODIFY_DOCUMENT, r.second, false, true, r.first);
    }
    TErfByUrl expectedErfs = DumpExpectedErfs(Messages);
    Messages.clear();

    TErfByUrl actualErfs = DumpActualErfs(finalIndex);

    ui32 nErrors = CompareAllErfs(expectedErfs, actualErfs);
    CHECK_TEST_EQ(0u, nErrors);
    return true;
}

TString LoadPantherQuery() {
    return Strip(TUnbufferedFileInput(GetResourcesDirectory() + "oxy/test_2017/query20180111_search.txt").ReadAll());
}

bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;

    (*ConfigDiff).erase("Components");
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 1;
    (*ConfigDiff)["Searcher.ExternalSearch"] = "freshsearch";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";

    (*ConfigDiff)["Searcher.DefaultBaseSearchConfig"] = GetResourcesDirectory() + "/oxy/configs/RefreshBaseSearch2017.conf";
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/RefreshOxygenOptionsJupi.cfg";
    SetIndexerParams(DISK, 25, 1); //MaxDocuments=25 is used (implicitly) in Run

    (*ConfigDiff)["Indexer.Common.HttpOptions.Threads"] = "1";
    (*ConfigDiff)["Indexer.Disk.Threads"] = "1";
    (*ConfigDiff)["AdditionalModules"] = "EXTBUILDER";

    return true;
}
};
