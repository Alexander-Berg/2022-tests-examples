#include <ads/quality/nsl/lib/util.h>
#include <ads/quality/nsl/lib/ml.h>
#include <ads/quality/nsl/lib/cfg.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace {

string DataForTests() {
    string arcadiaRoot = ArcadiaSourceRoot().c_str();
    string dataForTests = arcadiaRoot + "/ads/quality/nsl/data_for_tests/";
    return dataForTests;
}

Y_UNIT_TEST_SUITE(TUtilHTestSuite) {
    Y_UNIT_TEST(TestNumberSplitter) {
        string st = "10 20";
        char sep = ' ';
        TNumberSplitter numSplitter(st, sep);
        UNIT_ASSERT(numSplitter.End == 0);
        size_t num0 = numSplitter.Next();
        UNIT_ASSERT(num0 == 10);
        size_t num1 = numSplitter.Next();
        UNIT_ASSERT(num1 == 20);
        UNIT_ASSERT(numSplitter.End == 1);
    }
    Y_UNIT_TEST(TestNumberSplitterEmpty) {
        string st = "";
        char sep = ' ';
        TNumberSplitter numSplitter(st, sep);
        UNIT_ASSERT(numSplitter.End == 1);
    }
    Y_UNIT_TEST(TestNumberSplitterAtol) {
        string st = "100000000000 200000000000";
        char sep = ' ';
        TNumberSplitter numSplitter(st, sep);
        UNIT_ASSERT(numSplitter.End == 0);
        size_t num0 = numSplitter.Next();
        UNIT_ASSERT(num0 == 100000000000);
        size_t num1 = numSplitter.Next();
        UNIT_ASSERT(num1 == 200000000000);
        UNIT_ASSERT(numSplitter.End == 1);
    }
    Y_UNIT_TEST(TestNumberSplitterBigInt) {
        string st = "16746746856409515756";
        char sep = ' ';
        TNumberSplitter numSplitter(st, sep);
        UNIT_ASSERT(numSplitter.End == 0);
        size_t num0 = numSplitter.Next();
        UNIT_ASSERT(num0 / 2 == 8373373428204757878);
        UNIT_ASSERT(numSplitter.End == 1);
    }
    Y_UNIT_TEST(TestSplit) {
        string st = "10,20";
        char sep = ',';
        auto v = Split(st, sep);
        UNIT_ASSERT(v.size() == 2);
        UNIT_ASSERT(v[0] == "10");
        UNIT_ASSERT(v[1] == "20");
    }
    Y_UNIT_TEST(TestSplitEmpty) {
        string st = "";
        char sep = ',';
        auto v = Split(st, sep);
        UNIT_ASSERT(v.empty());
    }
    Y_UNIT_TEST(TestSplitNoSep) {
        string st = "test string";
        char sep = ',';
        auto v = Split(st, sep);
        UNIT_ASSERT(v.size() == 1);
        UNIT_ASSERT(v[0] == st);
    }
    Y_UNIT_TEST(TestProximal) {
        UNIT_ASSERT(Proximal(3., 1.) == 2.);
        UNIT_ASSERT(Proximal(-3., 1.) == -2.);
        UNIT_ASSERT(Proximal(.5, 1.) == 0.);
        UNIT_ASSERT(Proximal(-.5, 1.) == 0.);
        UNIT_ASSERT(Proximal(0., 1.) == 0.);
    }
    Y_UNIT_TEST(TestSigmoid) {
        UNIT_ASSERT(Sigmoid(0.) == .5);
        double sigmoidOne = 0.7310585;
        UNIT_ASSERT(fabs(Sigmoid(1.) - sigmoidOne) < 1e-6);
        UNIT_ASSERT(fabs(Sigmoid(-1.) + sigmoidOne - 1.) < 1e-6);
    }
    Y_UNIT_TEST(TestPermutation) {
        size_t permSize = 5;
        TPermutation perm(permSize);
        vector<size_t> res;
        while (perm.Valid())
            res.push_back(perm.Next());
        UNIT_ASSERT(res.size() == permSize);
    }
    Y_UNIT_TEST(TestFeatureMap) {
        string featureMapFileName = DataForTests() + "/feature.map";
        THashMap<string, SAMPLE_UINT_TYPE> featureMap = ReadFeatureMap(featureMapFileName);
        UNIT_ASSERT(featureMap.size() == 10);
        vector<string> lines = { "0,23128920", "1,3025809424", "2,190749857967285553", "3,200006580", "4,12089872638101825568",
            "5,27", "6,48420", "7,48420,100500", "8,48420,27", "9,48420,100500,27" };
        set<size_t> indices;
        for (const auto& p : featureMap)
            indices.insert(p.second);
        UNIT_ASSERT(indices.size() == 10);
        for (size_t i = 0; i < 10; i++)
            UNIT_ASSERT(indices.find(i) != indices.end());
        for (const auto& p : featureMap)
            UNIT_ASSERT(lines[p.second] == p.first);
    }
}

Y_UNIT_TEST_SUITE(TCfgHTestSuite) {
    Y_UNIT_TEST(TestCfgRead) {
        auto cfgName = DataForTests() + "/config.yml";
        auto cfg = TCfgWrapper(cfgName);
        UNIT_ASSERT(cfg.Exists("nsl"));
        UNIT_ASSERT(cfg.Get("nsl").Exists("threads"));
        UNIT_ASSERT(cfg.Get("nsl").Get("threads").AsInt() == 16);
        UNIT_ASSERT(cfg.Get("nsl").Exists("initial_alpha"));
        UNIT_ASSERT(cfg.Get("nsl").Get("initial_alpha").AsDouble() == .01);
        UNIT_ASSERT(cfg.Exists("ml"));
        UNIT_ASSERT(cfg.Get("ml").Exists("warm_start"));
        UNIT_ASSERT(cfg.Get("ml").Get("warm_start").AsBool() == false);
        UNIT_ASSERT(cfg.Get("ml").Exists("target_name"));
        UNIT_ASSERT(cfg.Get("ml").Get("target_name").AsString() == "Target");
        UNIT_ASSERT(cfg.Get("ml").Exists("factors"));
        vector<vector<string>> f = cfg.Get("ml").Get("factors").AsVectorOfVectorsOfStrings();
        UNIT_ASSERT(f.size() == 3);
        UNIT_ASSERT(f[0].size() == 1);
        UNIT_ASSERT(f[0][0] == "OrderID");
        UNIT_ASSERT(f[1].size() == 1);
        UNIT_ASSERT(f[1][0] == "PageID");
        UNIT_ASSERT(f[2].size() == 2);
        UNIT_ASSERT(f[2][0] == "OrderID");
        UNIT_ASSERT(f[2][1] == "PageID");
    }
}

Y_UNIT_TEST_SUITE(TMlHTestSuite) {
    Y_UNIT_TEST(TestSum) {
        TSample s;
        s.x.push_back(1);
        s.x.push_back(2);
        vector<TCoeffIdxData> cdata(2);
        cdata[1].Coeff = 10.;
        double res = Sum(cdata, s);
        UNIT_ASSERT(res == 10.);
    }
    Y_UNIT_TEST(TestFreadSample) {
        string testFreadSampleFileName = DataForTests() + "/test.FreadSample";
        FILE *f = fopen(testFreadSampleFileName.c_str(), "rb");
        TSample s;
        vector<SAMPLE_UINT_TYPE> tmpReadV;
        bool freadSampleResult = FreadSample(f, s, tmpReadV);
        UNIT_ASSERT(freadSampleResult);
        UNIT_ASSERT(s.Label == 10.);
        UNIT_ASSERT(s.Weight == 0.5);
        UNIT_ASSERT(s.x.size() == 2);
        UNIT_ASSERT(s.x[0] == 100);
        UNIT_ASSERT(s.x[1] == 200);
        freadSampleResult = FreadSample(f, s, tmpReadV);
        UNIT_ASSERT(!freadSampleResult);
        fclose(f);
    }
}

}
