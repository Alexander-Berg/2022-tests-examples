#include <random>
#include <iostream>
#include <iomanip>
#include <map>
#include <random>
#include <stdlib.h>
#include <limits>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/digest/md5/md5.h>
#include <mapreduce/yt/client/client.h>
#include <library/cpp/yson/node/node_io.h>


using namespace NYT;
using namespace std;

const double Mu = 0.05;

TVector<TNode> ReadYsonDataFromFile(TString& filename) {
    TFileInput input(filename);
    TVector<TNode> data;
    TString s;
    while (input.ReadLine(s)) {
        if (s[s.size() - 1] == ';') {
            s = s.remove(s.size() - 1);  // remove stray semicolon
        }
        TNode row = NodeFromYsonString(s.data());
        data.push_back(row);
    }
    return data;
}

void WriteYsonDataToFile(TString& filename, TVector<TNode>& data) {
    TFileOutput output(filename);
    output.Write("[\n");
    for (auto row: data) {
        output.Write(NodeToCanonicalYsonString(row) + ";\n");
    }
    output.Write("]");
}

class TCrossCheckTests : public TTestBase {
private:
    void TestUniqIDOrderIDCase();
    void TestUniqIDBannerIDPhraseIDCase();
    void TestUniqIDCase();
    void TestUniqIDGroupBannerIDPhraseIDCase();
    void ChooseSaltUniqIDGroupBannerIDPhraseIDCase();
    void TestUniqIDTargetDomainIDCase();
    void TestTargetDomainIDHitLogIDCase();
    void CalcHashesAndRandom();
    void TestSyntheticCase();

UNIT_TEST_SUITE(TCrossCheckTests);

//    UNIT_TEST(TestUniqIDOrderIDCase);
//    UNIT_TEST(TestUniqIDBannerIDPhraseIDCase);
//    UNIT_TEST(TestUniqIDCase);
//    UNIT_TEST(TestUniqIDGroupBannerIDPhraseIDCase);
//    UNIT_TEST(ChooseSaltUniqIDGroupBannerIDPhraseIDCase);
//    UNIT_TEST(TestUniqIDTargetDomainIDCase);
//    UNIT_TEST(TestTargetDomainIDHitLogIDCase);
//    UNIT_TEST(CalcHashesAndRandom);
    UNIT_TEST(TestSyntheticCase);


UNIT_TEST_SUITE_END();

};

TString const TestDataDir =  ArcadiaSourceRoot() +
    "/ads/quality/trafarets/projects/implicit_payments/BSDEV-80822_bid_rotation/tests/data";

void TCrossCheckTests::TestUniqIDOrderIDCase() {
    ui64 RatioRandomSalt = 54321ull;
    ui64 MultiplierRandomSalt = 98765ull;
    TString dataFile = TestDataDir + "/clicks_318930_rnd_sample.yson";
    auto data = ReadYsonDataFromFile(dataFile);
    for (auto& row: data) {
        double RndBidMult = row.ChildAsDouble("RndBidMult");
        ui64 UniqID = row.ChildConvertTo<ui64>("UniqID");
        ui64 OrderID = row.ChildConvertTo<ui64>("OrderID");

        auto hfcn1 = MD5 {};
        hfcn1.Update(&RatioRandomSalt, sizeof(RatioRandomSalt));
        hfcn1.Update(&UniqID, sizeof(UniqID));
        hfcn1.Update(&OrderID, sizeof(OrderID));
        ui64 h1 = hfcn1.EndHalfMix();
        double d1 = static_cast<double>(h1 % 1'000'000'001) / 1'000'000'000;

        auto hfcn2 = MD5 {};
        hfcn2.Update(&MultiplierRandomSalt, sizeof(MultiplierRandomSalt));
        hfcn2.Update(&UniqID, sizeof(UniqID));
        hfcn2.Update(&OrderID, sizeof(OrderID));
        ui64 h2 = hfcn2.EndHalfMix();
        double d2 = static_cast<double>(h2 % 1'000'000'001) / 1'000'000'000;

        Cerr << UniqID << "\t" << OrderID << "\t" << RndBidMult << "\t" << d1 << "\t" << d2 << Endl;

    }
}

void TCrossCheckTests::TestUniqIDBannerIDPhraseIDCase() {
    double Mu = 0.05;
    ui64 RatioRandomSalt = 54321ull;
    ui64 MultiplierRandomSalt = 98765ull;
    TString dataFile = TestDataDir + "/clicks_11965_11966_rnd_sample.yson";
    auto data = ReadYsonDataFromFile(dataFile);
    for (auto& row: data) {
        double RndBidMult = row.ChildAsDouble("RndBidMult");
        ui64 UniqID = row.ChildConvertTo<ui64>("UniqID");
        ui64 BannerID = row.ChildConvertTo<ui64>("BannerID");
        ui64 PhraseID = row.ChildConvertTo<ui64>("PhraseID");
        ui64 TargetPhraseID = row.ChildConvertTo<ui64>("TargetPhraseID");
        ui64 OrigPhraseID = row.ChildConvertTo<ui64>("OrigPhraseID");
        ui64 ContextType = row.ChildConvertTo<ui64>("ContextType");

        ui64 BidID = PhraseID;
        if (ContextType == 7 || ContextType == 8) {
            BidID = TargetPhraseID;
        } else if (ContextType == 3 || ContextType == 11) {
            BidID = OrigPhraseID;
        }

        auto hfcn1 = MD5{};
        hfcn1.Update(&RatioRandomSalt, sizeof(RatioRandomSalt));
        hfcn1.Update(&UniqID, sizeof(UniqID));
        hfcn1.Update(&BannerID, sizeof(BannerID));
        hfcn1.Update(&BidID, sizeof(BidID));
        ui64 h1 = hfcn1.EndHalfMix();
        double rnd1 = static_cast<double>(h1 % 1'000'000'001) / 1'000'000'000;

        auto hfcn2 = MD5 {};
        hfcn2.Update(&MultiplierRandomSalt, sizeof(MultiplierRandomSalt));
        hfcn2.Update(&UniqID, sizeof(UniqID));
        hfcn2.Update(&BannerID, sizeof(BannerID));
        hfcn2.Update(&BidID, sizeof(BidID));
        ui64 h2 = hfcn2.EndHalfMix();
        double rnd2 = static_cast<double>(h2 % 1'000'000'001) / 1'000'000'000;
        double rndBidMult = pow(rnd2, 1. / (1.0 - Mu));


        Cerr << "|| "
             << UniqID << " | "
             << BannerID << " | "
             << BidID << " | "
             << ContextType << " | "
             << RndBidMult << " | "
             << rnd1 << " | "
             << rndBidMult << " ||" << Endl;
    }
}

void TCrossCheckTests::TestUniqIDCase() {
    ui64 RatioRandomSalt = 24ull;
    ui64 MultiplierRandomSalt = 42ull;
    TString dataFile = TestDataDir + "/one_banner_ft_tests.yson";
    auto data = ReadYsonDataFromFile(dataFile);
    for (auto& row: data) {
        ui64 UniqID = row.ChildConvertTo<ui64>("UniqID");

        auto hfcn1 = MD5 {};
        hfcn1.Update(&RatioRandomSalt, sizeof(RatioRandomSalt));
        hfcn1.Update(&UniqID, sizeof(UniqID));
        ui64 h1 = hfcn1.EndHalfMix();
        double d1 = static_cast<double>(h1 % 1'000'000'001) / 1'000'000'000;

        auto hfcn2 = MD5 {};
        hfcn2.Update(&MultiplierRandomSalt, sizeof(MultiplierRandomSalt));
        hfcn2.Update(&UniqID, sizeof(UniqID));
        ui64 h2 = hfcn2.EndHalfMix();
        double d2 = static_cast<double>(h2 % 1'000'000'001) / 1'000'000'000;

        Cerr << UniqID << "\t" << d1 << "\t" << d2 << Endl;
    }
}

void TCrossCheckTests::ChooseSaltUniqIDGroupBannerIDPhraseIDCase() {

    for (auto saltVal=1ull; saltVal < 50; saltVal++) {
        Cerr << "\nsaltVal = " << saltVal << "\n" << Endl;

        ui64 RatioRandomSalt = saltVal;

        TString dataFile = TestDataDir + "/two_banners_ft_tests.yson";
        auto data = ReadYsonDataFromFile(dataFile);
        for (auto& row: data) {
            ui64 UniqID = row.ChildConvertTo<ui64>("UniqID");
            ui64 GroupBannerID = row.ChildConvertTo<ui64>("GroupBannerID");
            ui64 PhraseID = row.ChildConvertTo<ui64>("PhraseID");
            ui64 TargetPhraseID = row.ChildConvertTo<ui64>("TargetPhraseID");
            ui64 OrigPhraseID = row.ChildConvertTo<ui64>("OrigPhraseID");
            ui64 ContextType = row.ChildConvertTo<ui64>("ContextType");

            ui64 BidID = PhraseID;
            if (ContextType == 7 || ContextType == 8) {
                BidID = TargetPhraseID;
            } else if (ContextType == 3 || ContextType == 11) {
                BidID = OrigPhraseID;
            }

            auto hfcn1 = MD5{};
            hfcn1.Update(&RatioRandomSalt, sizeof(RatioRandomSalt));
            hfcn1.Update(&UniqID, sizeof(UniqID));
            hfcn1.Update(&GroupBannerID, sizeof(GroupBannerID));
            hfcn1.Update(&BidID, sizeof(BidID));
            ui64 h1 = hfcn1.EndHalfMix();

            double rnd1 = static_cast<double>(h1 % 1'000'000'001) / 1'000'000'000;

            Cerr << "||  "
                 << UniqID << "  |  "
                 << "GroupBannerID:  " << GroupBannerID << "  |  "
                 << "BidID: " << BidID << "  |  "
                 << "ContextType: " << ContextType << "  |  "
                 << "Ratio rand val: " << rnd1
                 << "  ||" << "\n" << Endl;

        }
    }
}

void TCrossCheckTests::TestUniqIDGroupBannerIDPhraseIDCase() {
    double Mu = 0.05;
    ui64 RatioRandomSalt = 54321ull;
    ui64 MultiplierRandomSalt = 98765ull;

    TString dataFile = TestDataDir + "/2h_exp_group_order_id.yson";

    auto data = ReadYsonDataFromFile(dataFile);
    for (auto& row: data) {
        ui64 UniqID = row.ChildConvertTo<ui64>("UniqID");
        ui64 GroupBannerID = row.ChildConvertTo<ui64>("GroupBannerID");
        ui64 PhraseID = row.ChildConvertTo<ui64>("PhraseID");
        ui64 TargetPhraseID = row.ChildConvertTo<ui64>("TargetPhraseID");
        ui64 OrigPhraseID = row.ChildConvertTo<ui64>("OrigPhraseID");
        ui64 ContextType = row.ChildConvertTo<ui64>("ContextType");

        ui64 BidID = PhraseID;
        if (ContextType == 7 || ContextType == 8) {
            BidID = TargetPhraseID;
        } else if (ContextType == 3 || ContextType == 11) {
            BidID = OrigPhraseID;
        }

        auto hfcn1 = MD5{};
        hfcn1.Update(&RatioRandomSalt, sizeof(RatioRandomSalt));
        hfcn1.Update(&UniqID, sizeof(UniqID));
        hfcn1.Update(&GroupBannerID, sizeof(GroupBannerID));
        hfcn1.Update(&BidID, sizeof(BidID));
        ui64 h1 = hfcn1.EndHalfMix();

        double rnd1 = static_cast<double>(h1 % 1'000'000'001) / 1'000'000'000;

        auto hfcn2 = MD5{};
        hfcn2.Update(&MultiplierRandomSalt, sizeof(MultiplierRandomSalt));
        hfcn2.Update(&UniqID, sizeof(UniqID));
        hfcn2.Update(&GroupBannerID, sizeof(GroupBannerID));
        hfcn2.Update(&BidID, sizeof(BidID));
        ui64 h2 = hfcn2.EndHalfMix();

        double rnd2 = static_cast<double>(h2 % 1'000'000'001) / 1'000'000'000;
        double rndBidMult = pow(rnd2, 1. / (1.0 - Mu));


        Cerr << "||  "
             << UniqID << "  |  "
             << "GroupBannerID:  " << GroupBannerID << "  |  "
             << "BidID: " << BidID << "  |  "
             << "ContextType: " << ContextType << "  |  "
             << "Ratio rand val: " << rnd1
             << "Bid rand multl: " << rndBidMult
             << "  ||" << "\n" << Endl;

    }
}

void TCrossCheckTests::TestUniqIDTargetDomainIDCase() {
    ui64 RatioRandomSalt = 512377ull;
    ui64 MultiplierRandomSalt = 815068ull;
    double Mu = 0.05;

//    TString dataFile = "/Users/katya-mineeva/data/RSYA/yson/exp-100154__UniqID-TargetDomainID__2022-06-03.yson";
//    TString outDataFile = "/Users/katya-mineeva/data/RSYA/yson/out/exp-100154__UniqID-TargetDomainID__2022-06-03.yson";

    TString dataFile = TestDataDir + "/exp-100154__UniqID-TargetDomainID__2022-06-03___100-rows.yson";
    TString outDataFile = TestDataDir + "/out/exp-100154__UniqID-TargetDomainID__2022-06-03___100-rows.yson";

    auto data = ReadYsonDataFromFile(dataFile);

    double accum_l1_error = 0;
    double accum_l2_error = 0;

    for (auto& row: data) {
        double RandomBidMultiplierLogs = row.ChildAsDouble("RandomBidMultiplier");
        ui64 UniqID = row.ChildConvertTo<ui64>("UniqID");
        ui64 TargetDomainID = row.ChildConvertTo<ui64>("TargetDomainID");

        auto hashFunctionRatio = MD5 {};
        hashFunctionRatio.Update(&RatioRandomSalt, sizeof(RatioRandomSalt));
        hashFunctionRatio.Update(&UniqID, sizeof(UniqID));
        hashFunctionRatio.Update(&TargetDomainID, sizeof(TargetDomainID));

        double randRatio = static_cast<double>(hashFunctionRatio.EndHalfMix() % 1'000'000'001) / 1'000'000'000;

        auto hashFunctionMultiplier = MD5 {};
        hashFunctionMultiplier.Update(&MultiplierRandomSalt, sizeof(MultiplierRandomSalt));
        hashFunctionMultiplier.Update(&UniqID, sizeof(UniqID));
        hashFunctionMultiplier.Update(&TargetDomainID, sizeof(TargetDomainID));

        double randMultiplier = static_cast<double>(hashFunctionMultiplier.EndHalfMix() % 1'000'000'001) / 1'000'000'000;

        row["randRatio"] = randRatio;
        row["randMultiplier"] = randMultiplier;

        double RandomBidMultiplierHere = 1;
        if (randRatio > 1 - Mu) {
            RandomBidMultiplierHere = pow(randMultiplier, 1.0 / (1.0 - Mu));
        }

//        Cerr << randRatio << "\t" << randMultiplier << "\t" << RandomBidMultiplierHere << "\t" << RandomBidMultiplierLogs << Endl;

        accum_l1_error += abs(RandomBidMultiplierLogs - RandomBidMultiplierHere);
        accum_l2_error += pow(RandomBidMultiplierLogs - RandomBidMultiplierHere, 2);
    }

    WriteYsonDataToFile(outDataFile, data);

    Cerr << "cnt rows:  " << data.size() << "\n" << Endl;

    Cerr << "acc l1 err:  " << accum_l1_error  << Endl;
    Cerr << "acc l2 err" << "\t" << accum_l2_error << "\n" << Endl;

    Cerr << "avg l1 err:  " << accum_l1_error / data.size()  << Endl;
    Cerr << "avg l2 err" << "\t" << accum_l2_error / data.size() << Endl;
}


void TCrossCheckTests::TestTargetDomainIDHitLogIDCase() {
    ui64 RatioRandomSalt = 773215ull;
    ui64 MultiplierRandomSalt = 860518ull;
    double Mu = 0.05;

    TString dataFile = "/Users/katya-mineeva/data/RSYA/yson/exp-100154__TargetDomainID-HitLogID__2022-06-03.yson";
    TString outDataFile = "/Users/katya-mineeva/data/RSYA/yson/out/exp-100154__TargetDomainID-HitLogID__2022-06-03.yson";

//    TString dataFile = TestDataDir + "/exp-100154__TargetDomainID-HitLogID__2022-06-03___100-rows.yson";
//    TString outDataFile = TestDataDir + "/out/exp-100154__TargetDomainID-HitLogID__2022-06-03___100-rows.yson";

    auto data = ReadYsonDataFromFile(dataFile);

    double accum_l1_error = 0;
    double accum_l2_error = 0;

    auto out_row = TNode::CreateList({});
    for (auto& row: data) {
        double RandomBidMultiplierLogs = row.ChildAsDouble("RandomBidMultiplier");
        ui64 TargetDomainID = row.ChildConvertTo<ui64>("TargetDomainID");
        ui64 HitLogID = row.ChildConvertTo<ui64>("HitLogID");

        auto hashFunctionRatio = MD5 {};
        hashFunctionRatio.Update(&RatioRandomSalt, sizeof(RatioRandomSalt));
        hashFunctionRatio.Update(&TargetDomainID, sizeof(TargetDomainID));
        hashFunctionRatio.Update(&HitLogID, sizeof(HitLogID));

        double randRatio = static_cast<double>(hashFunctionRatio.EndHalfMix() % 1'000'000'001) / 1'000'000'000;

        auto hashFunctionMultiplier = MD5 {};
        hashFunctionMultiplier.Update(&MultiplierRandomSalt, sizeof(MultiplierRandomSalt));
        hashFunctionMultiplier.Update(&TargetDomainID, sizeof(TargetDomainID));
        hashFunctionMultiplier.Update(&HitLogID, sizeof(HitLogID));

        double randMultiplier = static_cast<double>(hashFunctionMultiplier.EndHalfMix() % 1'000'000'001) / 1'000'000'000;

        row["randRatio"] = randRatio;
        row["randMultiplier"] = randMultiplier;

        double RandomBidMultiplierHere = 1;
        if (randRatio > 1 - Mu) {
            RandomBidMultiplierHere = pow(randMultiplier, 1.0 / (1.0 - Mu));
        }

//        Cerr << randRatio << "\t" << randMultiplier << "\t" << RandomBidMultiplierHere << "\t" << RandomBidMultiplierLogs << Endl;

        accum_l1_error += abs(RandomBidMultiplierLogs - RandomBidMultiplierHere);
        accum_l2_error += pow(RandomBidMultiplierLogs - RandomBidMultiplierHere, 2);
    }

    WriteYsonDataToFile(outDataFile, data);

    Cerr << "cnt rows:  " << data.size()  << Endl;

    Cerr << "acc l1 err:  " << accum_l1_error  << Endl;
    Cerr << "acc l2 err" << "\t" << accum_l2_error << "\n\n" << Endl;

    Cerr << "avg l1 err:  " << accum_l1_error / data.size()  << Endl;
    Cerr << "avg l2 err" << "\t" << accum_l2_error / data.size() << Endl;
}


void TCrossCheckTests::CalcHashesAndRandom() {
    ui64 salt = 111111ull;

    TString dataFile = "/Users/katya-mineeva/data/RSYA/yson/no_rot__2020-12-02.yson";
    TString outDataFile = "/Users/katya-mineeva/data/RSYA/yson/out/no_rot__2020-12-02.yson";

    auto data = ReadYsonDataFromFile(dataFile);

    auto out_row = TNode::CreateList({});
    for (auto &row: data) {
        ui64 UniqID = row.ChildConvertTo<ui64>("UniqID");
        ui64 OrderID = row.ChildConvertTo<ui64>("OrderID");
        ui64 TargetDomainID = row.ChildConvertTo<ui64>("TargetDomainID");
        ui64 HitLogID = row.ChildConvertTo<ui64>("HitLogID");

        auto hashFunctionUniqIDOrderID = MD5{};
        hashFunctionUniqIDOrderID.Update(&salt, sizeof(salt));
        hashFunctionUniqIDOrderID.Update(&UniqID, sizeof(UniqID));
        hashFunctionUniqIDOrderID.Update(&OrderID, sizeof(OrderID));
        row["randUniqIDOrderID"] =
                static_cast<double>(hashFunctionUniqIDOrderID.EndHalfMix() % 1'000'000'001) / 1'000'000'000;

        auto hashFunctionUniqIDTargetDomainID = MD5{};
        hashFunctionUniqIDTargetDomainID.Update(&salt, sizeof(salt));
        hashFunctionUniqIDTargetDomainID.Update(&UniqID, sizeof(UniqID));
        hashFunctionUniqIDTargetDomainID.Update(&TargetDomainID, sizeof(TargetDomainID));
        row["randUniqIDTargetDomainID"] =
                static_cast<double>(hashFunctionUniqIDTargetDomainID.EndHalfMix() % 1'000'000'001) / 1'000'000'000;


        auto hashFunctionTargetDomainIDHitLogID = MD5{};
        hashFunctionTargetDomainIDHitLogID.Update(&salt, sizeof(salt));
        hashFunctionTargetDomainIDHitLogID.Update(&TargetDomainID, sizeof(TargetDomainID));
        hashFunctionTargetDomainIDHitLogID.Update(&HitLogID, sizeof(HitLogID));
        row["randTargetDomainIDHitLogID"] =
                static_cast<double>(hashFunctionTargetDomainIDHitLogID.EndHalfMix() % 1'000'000'001) / 1'000'000'000;
    }

    WriteYsonDataToFile(outDataFile, data);
    Cerr << "written  " << data.size() << " rows" << Endl;
}


double getRealCostIP(double bid) {
    const ui64 range_from  = 0;
    const ui64 range_to    = std::numeric_limits<ui64>::max();

    std::random_device rand_dev;
    std::mt19937 generator(rand_dev());
    std::uniform_int_distribution<ui64> distr(range_from, range_to);

    double randRatio = static_cast<double>(distr(generator) % 1'000'000'001) / 1'000'000'000;
    double randMultiplier = static_cast<double>(distr(generator) % 1'000'000'001) / 1'000'000'000;

//    Cerr << randRatio << "\t" << randMultiplier << Endl;

    if (randRatio > 1 - Mu) {
        return bid * pow(randMultiplier, 1.0 / (1.0 - Mu));
    } else {
        return bid;
    }
}


void TCrossCheckTests::TestSyntheticCase() {
    double b1 = 10;
    double realCost_1 = 10;

    double b2 = 15;
    double realCost_2 = 15;

    double accum_GSP = 0;
    double accum_IP = 0;

    int n_iters = 1.e7;
    for (auto i = 0; i < n_iters; ++i) {
        realCost_1 = getRealCostIP(b1);
        realCost_2 = getRealCostIP(b2);

        if (realCost_1 < realCost_2) {
            accum_GSP += realCost_1;

            if (realCost_2 < b2) {
                accum_IP += (1 - 1 / Mu) * b2;
            } else {
                accum_IP += b2;
            }
        } else {
            accum_GSP += realCost_2;

            if (realCost_1 < b1) {
                accum_IP += (1 - 1 / Mu) * b1;
            } else {
                accum_IP += b1;
            }
        }
    }
    Cerr << "Finished " << n_iters << "iterations" << Endl;
    Cerr << "GSP payments: " << accum_GSP << "\tIP payments: " << accum_IP << "\tdiff: " << (accum_IP - accum_GSP) / std::min(accum_IP, accum_GSP) * 100 << " %" << Endl;
};

UNIT_TEST_SUITE_REGISTRATION(TCrossCheckTests);
