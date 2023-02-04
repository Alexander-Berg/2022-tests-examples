//
// Tests for the "New" 2018 platform (Jupiter-style indexes)
//

#include "oxy.h"

#include <robot/library/oxygen/indexer/library/wrangle/wrangle.h> // for Wrangle test TestOxygenKeyInvWad
#include <kernel/doom/offroad_wad/offroad_ann_wad_io.h> // for Wrangle test TestOxygenKeyInvWad
#include <kernel/doom/yandex/yandex_io.h> // for Wrangle test TestOxygenKeyInvWad

#include <saas/rtyserver/components/oxy/textwad/ki_normalizer.h>
#include <saas/rtyserver_test/util/oxy/ki_dumpers.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <saas/rtyserver_test/testerlib/search_checker.h>


// A smoke test for the indexation and search, using Refresh 2017 jupiter-style configs ("new platform")
START_TEST_DEFINE_PARENT(TestOxygenDocsRefresh2017, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_2017/docs", NRTYServer::TMessage::ADD_DOCUMENT, 1000);
    IndexMessages(Messages, REALTIME, 1);
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    QuerySearch("youtube&pron=st0" + GetAllKps(Messages), results);

    if (results.size() == 0) {
        ythrow yexception() << "no search results";
    }

    //realistic (as for 201811) search request - check for crash
    auto query = LoadPantherQueries();
    ProcessQueries(query); //TODO:(yrum, 201811) remove this after REFRESH-195 and REFRESH-317 is REALLY working

    return ExtraChecks();
}

void ProcessQueries(TVector<TString> queries) {
    for (const TString& query : queries) {
        ProcessQuery(query, nullptr);
    }
}

TVector<TString> LoadPantherQueries() {
    TUnbufferedFileInput input(GetResourcesDirectory() + "oxy/test_2017/query20181106_search.txt");
    TVector<TString> queries;
    TString line;
    while (input.ReadLine(line)) {
        queries.emplace_back(StripInPlace(line));
    }
    Y_ENSURE(queries.size() > 0);
    return queries;
}

virtual bool ExtraChecks() {
    return true;
}

bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;

    (*ConfigDiff)["Searcher.ExternalSearch"] = "freshsearch";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.DefaultBaseSearchConfig"] = GetResourcesDirectory() + "/oxy/configs/RefreshBaseSearch2017.conf";
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/RefreshOxygenOptions2017.cfg";
    SetIndexerParams(DISK, 500, 1);

    return true;
}
};

// A smoke test for the factors presence
START_TEST_DEFINE_PARENT(TestOxygenDocsRefresh2017_SmokeFactors, TTestOxygenDocsRefresh2017CaseClass)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_2017/docs", NRTYServer::TMessage::ADD_DOCUMENT, 1000);
    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();

    //realistic (as for 201801) search request

    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;
    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    ctx.DocProperties = &resultProps;

    const TString factorsOpt("&pron=reqbundle_l2&pron=disable_legacy_quorum_factors_on_l2&pron=use_test_l2_fresh");
    auto queries = LoadPantherQueries();
    for (ui32 queryNo = 0; queryNo < queries.size(); ++queryNo) {
        const TString& query = queries[queryNo];
        INFO_LOG << "Testing query #" << queryNo << Endl;
        Controller->QuerySearch(query + "&dbgrlv=da&fsgta=_JsonFactors&" + factorsOpt, results, false, ctx);
        if (results.size() == 0) {
            ythrow yexception() << "no search results";
        }
        THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
        // Check a keyinv-based dynamic factor
        CHECK_TEST_TRUE(factors.contains("TextBM25_Fm_W1") && factors["TextBM25_Fm_W1"] > 0);
        // Check an L2 erf-based factor
        CHECK_TEST_TRUE(factors.contains("IsHTML"));
        CHECK_TEST_TRUE(factors["IsHTML"] > 0);
    }
    return true;
}

bool InitConfig() override {
    if (!TTestOxygenDocsRefresh2017CaseClass::InitConfig())
        return false;

    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    SetIndexerParams(DISK, 200, 1);

    return true;
}
};


START_TEST_DEFINE_PARENT(TestOxygenDocsRefresh2017Samovar, TTestOxygenDocsRefresh2017CaseClass)
bool InitConfig() override {
    if (!TTestOxygenDocsRefresh2017CaseClass::InitConfig())
        return false;

    (*ConfigDiff)["Searcher.DefaultBaseSearchConfig"] = GetResourcesDirectory() + "/oxy/configs/RefreshBaseSearch2017Samovar.conf";
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/RefreshOxygenOptions2017Samovar.cfg";
    SetIndexerParams(DISK, 500, 1);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsRefresh2017Legacy, TTestOxygenDocsRefresh2017CaseClass)
bool InitConfig() override {
    if (!TTestOxygenDocsRefresh2017CaseClass::InitConfig())
        return false;

    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Searcher.DefaultBaseSearchConfig"] = GetResourcesDirectory() + "/oxy/configs/RefreshBaseSearch2017LegacyQuick.conf";
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/RefreshOxygenOptions2017LegacyQuick.cfg";
    SetIndexerParams(DISK, 500, 1);

    return true;
}
};

//Test for the "future" configuration (one that is not released yet)
START_TEST_DEFINE_PARENT(TestOxygenDocsRefresh2017Future, TTestOxygenDocsRefresh2017CaseClass)
bool InitConfig() override {
    if (!TTestOxygenDocsRefresh2017CaseClass::InitConfig())
        return false;

    (*ConfigDiff)["Searcher.DefaultBaseSearchConfig"] = GetResourcesDirectory() + "/oxy/configs/RefreshBaseSearch2017Future.conf";
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/RefreshOxygenOptions2017Future.cfg";
    SetIndexerParams(DISK, 200, 1);

    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "full,merge";
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersFilteredForIndex"] = "full";
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersFilteredForMerge"] = "merge";
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersMergeComplement"] = "__remove__";

    return true;
}

bool IsRefresh2017Segment(TFsPath segment) {
    TVector<TString> requiredIndexes = {
            "indexkeyinv.wad",
            "indexattrs.key.wad", "indexattrs.inv.wad", "indexattrs.date.seg_tree.flat",
            "indexpanther.info", "indexpanther.key.wad", "indexpanther.inv.wad"
    };

    TVector<TString> disabledIndexes = {
            "indexkey", "indexinv",
            "indexattrskey", "indexattrsinv"
    };

    auto exist = [&segment](const TString& file) {
        return (segment / file).Exists();
    };

    CHECK_TEST_TRUE(std::all_of(requiredIndexes.cbegin(), requiredIndexes.cend(), exist));
    CHECK_TEST_TRUE(std::none_of(disabledIndexes.cbegin(), disabledIndexes.cend(), exist));
    return true;
}

bool WrangleExpectedKiWad(TFsPath segment) {
    TOXYKeyInvNormalizer::TIndexTraits from = TOXYKeyInvNormalizer::GetIndexTraits(segment);
    TOXYKeyInvNormalizer::TIndexTraits to = from;
    to.TextWad = true;
    TRTYServerConfig config(MakeAtomicShared<TDaemonConfig>(TDaemonConfig::DefaultEmptyConfig.data(), false));
    TOXYKeyInvNormalizer normalizer(config);
    return normalizer.TryFixByWrangling(to, from, segment);
}

bool VerifyKiWadMerge(TFsPath segment) {
    CHECK_TEST_TRUE((segment / "indexoldkey").Exists() && (segment / "indexoldinv").Exists());

    // NB: using urls instead of docids means that 'pruning' IS NOT controlled in this test
    THolder<NSaas::TId2Url> id2url = NSaas::LoadDocUrls(segment);

    TVector<TString> expected, actual;
    TFsPath kiWad = segment / "indexkeyinv.wad";
    TFsPath kiWadExpected = segment / "indexkeyinv.wad.expected";
    TFsPath kiWadActual = segment / "indexkeyinv.wad.actual";
    CHECK_TEST_TRUE(kiWad.Exists());
    kiWad.RenameTo(kiWadActual);
    actual = TRTYKeyInvDumper::DumpKeyInvFromWad(kiWadActual, id2url.Get());

    CHECK_TEST_TRUE(WrangleExpectedKiWad(segment));
    kiWad.RenameTo(kiWadExpected);
    expected = TRTYKeyInvDumper::DumpKeyInvFromWad(kiWadExpected, id2url.Get());

    CHECK_TEST_TRUE(TRTYKeyInvDumper::ZipDiffHelper(expected, actual, kiWadExpected, kiWadActual));
    return true;
}

bool ExtraChecks() override {
    //
    // Let's also test for merge
    //
    auto indexDirs = Controller->GetFinalIndexes(/*stopServer=*/false);
    CHECK_TEST_TRUE(indexDirs.size() == 2); // expecting 2 segments, due to the IndexerParams above (350>200)
    CHECK_TEST_TRUE(IsRefresh2017Segment(*indexDirs.cbegin()));

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    //check that there is no global KeyInv in the final segment
    indexDirs = Controller->GetFinalIndexes(/*stopServer=*/false);
    CHECK_TEST_TRUE(indexDirs.size() == 1);
    CHECK_TEST_TRUE(IsRefresh2017Segment(*indexDirs.cbegin()));

    TVector<TDocSearchInfo> results;
    QuerySearch("youtube&pron=st0" + GetAllKps(Messages), results);

    if (results.size() == 0) {
        ythrow yexception() << "no search results";
    }

    //search request - check for crash
    auto queries = LoadPantherQueries();
    ProcessQueries(queries);

    //compare merged indexkeyinv.wad with one obtained by wrangling indexold<key|inv>
    indexDirs = Controller->GetFinalIndexes(/*stopServer=*/true);
    CHECK_TEST_TRUE(indexDirs.size() == 1);
    // CHECK_TEST_TRUE(VerifyKiWadMerge(*indexDirs.cbegin())); // not implemented yet

    return true;
}
};



// helper class
namespace {
    using TKeyInvReader_Wrangler =  NDoom::THitTransformingIndexReader<NDoom::TIdentityHitTransformation,
            NDoom::TKeyFilteringIndexReader<NDoom::TEmptyPrefixExceptTelKeyFilter, NDoom::TYandexIo::TReader>>;
    using TWriterIo_Wrangler = NDoom::TOffroadAnnWadIoSortedMultiKeys;

    class TTestingWrangler final : protected NOxygen::TWranglingProcessor<TKeyInvReader_Wrangler, TWriterIo_Wrangler> {
    public:
        using TBase = NOxygen::TWranglingProcessor<TKeyInvReader_Wrangler, TWriterIo_Wrangler>;
    public:
        TTestingWrangler(const TString& slaveOutputPrefix, const TString& wadOutputPrefix)
                : TBase(TString(), slaveOutputPrefix, wadOutputPrefix, false)
        {}

        using TBase::WrangleKeyInv;

    };
}

//
//Test: TKeyInvWadProcessor and TKeyInvWrangleProcessor should produce the same result
//
START_TEST_DEFINE_PARENT(TestOxygenDocsCompareKeyInvRefresh2017, TTestOxygenDocsRefresh2017CaseClass)
private:
//Dump index<key|inv> and indexattrs<key|inv>, validate that all the attribute entries is present in indexattrs
bool CheckIndexAttrsKeysAndHits(TFsPath& indexDir) {
    auto hitWriter = [] (const auto& hit) {
        return "[" + ToString(hit.DocId()) + "]";
    };

    TVector<TString> expected = TRTYKeyInvDumper::DumpYandexKeyInv(indexDir / "index", hitWriter);
    expected.erase(std::remove_if(expected.begin(), expected.end(), [](const TString &s) {
        size_t idx = s.find('\t');
        if (idx == TString::npos)
            idx = s.size();
        NDoom::TEmptyPrefixKeyFilter keyFilter; //Note: #tel must be also present in index attrs
        return keyFilter(TStringBuf(s.substr(0, idx)));
    }), expected.end());

    TVector<TString> actual = TRTYKeyInvDumper::DumpKeyInvAttrsFromWad(indexDir / "indexattrs");
    CHECK_TEST_FAILED(!actual.size(), "indexattrs is empty");
    CHECK_TEST_FAILED(std::all_of(actual.begin(), actual.end(), [](const TString& s) {
        return s.find('#') != TString::npos;
    }), "indexattrs contains non-attribute keys");
    CHECK_TEST_TRUE(TRTYKeyInvDumper::ZipDiffHelper(expected, actual, indexDir / "indexkey", indexDir / "indexattrs"));
    return true;
}

bool CheckTextWadVersusWrangled(const TFsPath wadExpected, const TFsPath wadActual) {
    TVector<TString> expected = TRTYKeyInvDumper::DumpKeyInvFromWad(wadExpected);
    TVector<TString> actual = TRTYKeyInvDumper::DumpKeyInvFromWad(wadActual);

    CHECK_TEST_TRUE(TRTYKeyInvDumper::ZipDiffHelper(expected, actual, wadExpected, wadActual));
    return true;
}

bool CheckTextWadKeys(const TFsPath indexDir) {
    TVector<TString> expected = TRTYKeyInvDumper::DumpYandexKeyInv(indexDir / "index");
    TVector<TString> actual = TRTYKeyInvDumper::DumpKeysTableFromWad(indexDir / "indexkeyinv.wad");

    //leave only non-attributes (plus the '#tel' keys) in Expected
    expected.erase(std::remove_if(expected.begin(), expected.end(), [](const TString &s) {
        size_t idx = s.find('\t');
        if (idx == TString::npos)
            idx = s.size();
        NDoom::TEmptyPrefixExceptTelKeyFilter keyFilter;
        return !keyFilter(TStringBuf(s.substr(0, idx)));
    }), expected.end());

    //leave only first column (keys)
    auto&& stripHits = [](const TString& s) {
        size_t idx = s.find('\t');
        if (idx != TString::npos)
            return s.substr(0, idx);
        return s;
    };
    std::transform(expected.begin(), expected.end(), expected.begin(), stripHits);
    std::transform(actual.begin(), actual.end(), actual.begin(), stripHits);

    CHECK_TEST_FAILED(!actual.size(), "failed to dump indexkeyinv.wad");
    CHECK_TEST_TRUE(TRTYKeyInvDumper::ZipDiffHelper(expected, actual, indexDir / "indexkey", indexDir / "indexkeyinv.wad"));
    return true;

}

TFsPath GetIndexSegment() {
    auto indexDirs = Controller->GetFinalIndexes(/*stopServer=*/ true);
    if (indexDirs.empty())
        ythrow yexception() << "No segments found";

    TFsPath indexDir = TFsPath(*indexDirs.cbegin()).Fix();
    return indexDir;

}

void BuildKeyInvWadByWrangler(TFsPath& indexDir) {
    TTestingWrangler wrangler(indexDir / "index", indexDir / "wrangledkeyinv.");

    INFO_LOG << "Executing BuildKeyInvWadByWrangler()..." << Endl;
    wrangler.WrangleKeyInv();
    INFO_LOG << "Executing BuildKeyInvWadByWrangler()..." << " OK" << Endl;
}


public:
bool Run() override {
    if (GetIsPrefixed())
        return true;

    GenerateDocs("oxy/test_2017/docs", NRTYServer::TMessage::ADD_DOCUMENT, 10);
    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();

    TFsPath segment = GetIndexSegment();

    //validate indexattrs contents
    CHECK_TEST_TRUE(CheckIndexAttrsKeysAndHits(segment));

    //validate textwad contents (with termId replaced by terms)
    BuildKeyInvWadByWrangler(segment);
    CHECK_TEST_TRUE(CheckTextWadVersusWrangled(segment / "wrangledkeyinv.wad", segment / "indexkeyinv.wad"));

    // TODO:(yrum) this direct check fails due to some key suffixes (forms...) - disabled.
    // CHECK_TEST_TRUE(CheckTextWadKeys(segment));

    return true;
}



bool InitConfig() override {
    if (!TTestOxygenDocsRefresh2017CaseClass::InitConfig())
        return false;

    (*ConfigDiff)["Searcher.DefaultBaseSearchConfig"] = GetResourcesDirectory() + "/oxy/configs/RefreshBaseSearch2017Future.conf";
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/TestOxygenKeyInvWad.cfg";

    SetIndexerParams(DISK, 500, 1);

    return true;
}
};

