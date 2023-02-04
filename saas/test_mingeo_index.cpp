#include "geo_kdtree.h"
#include "mingeo_index.h"

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <library/cpp/testing/benchmark/bench.h>
#include <util/datetime/cputimer.h>
#include <util/stream/buffer.h>
#include <util/generic/bitops.h>
#include <util/generic/buffer.h>
#include <util/string/hex.h>
#include <util/string/split.h>
#include <util/stream/file.h>
#include <util/folder/path.h>
#include <library/cpp/geo/geo.h>

#include <random>
#include <cmath>

namespace NRTYGeo {
    class TMingeoFatTest: public NUnitTest::TTestBase {
        UNIT_TEST_SUITE(TMingeoFatTest)
        UNIT_TEST(TestKdTreePerf);
        UNIT_TEST(TestPruningPerf);
        UNIT_TEST(TestCompareOutput);
        UNIT_TEST_SUITE_END();
    private:
        struct TGeneratorOpts {
            std::random_device::result_type RngSeed;
            size_t DocCount;
            double DistFromSample;
            double MinServiceRange;
            ui32 MaxRangeExp;
            double MaxRectAspectRatio;
        };

    private:
        static constexpr ui32 DocCount = 1000000;
        static constexpr std::random_device::result_type Seed = 0x9528BDA7;

        static constexpr ui32 PruningLimit = Max<ui32>();

        static constexpr ui8 StreamId = 0; //TODO(SAAS-5949): change to a non-zero value when SAAS-5949 is completed
        static constexpr ui64 Kps = (ui64)0x7F << 56;

    private:
        using TDocuments = TVector<NGeo::TGeoWindow>;
        TVector<NGeo::TGeoPoint> SamplePoints;
        TDocuments Documents;
        TDocuments Requests;

        static TVector<NGeo::TGeoPoint> ReadSamplePoints(const TFsPath& sample) {
            TVector<NGeo::TGeoPoint> samplePoints;
            TFileInput fin(sample);
            const TSetDelimiter<const char> delim("\t");
            for (TString s; fin.ReadLine(s); ) {
                TVector<TStringBuf> cols;
                size_t nCols = Split(TStringBuf(s), delim, cols);
                if (nCols != 2)
                    continue;
                samplePoints.emplace_back(/*lon=*/FromString<double>(cols[0]), /*lat=*/FromString<double>(cols[1]));
            }
            return samplePoints;
        }

        template <typename TRng>
        Y_FORCE_INLINE static std::pair<double, double> RandomStep(TRng& rng, double maxLen) {
            std::uniform_real_distribution<double> disAngle(-M_PI, M_PI);
            std::uniform_real_distribution<double> disLen(0, maxLen);
            double angle = disAngle(rng);
            double len = disLen(rng);
            return {len * cos(angle), len * sin(angle)};
        }

        template <typename TRng>
        Y_FORCE_INLINE static NGeo::TGeoWindow RandomRect(TRng& rng, NGeo::TGeoPoint& center, const TGeneratorOpts& opts) {
            std::uniform_real_distribution<double> disSize(1, 2);
            std::uniform_int_distribution<ui32> disSizeExp(0, opts.MaxRangeExp);
            std::uniform_real_distribution<double> disAspectRatio(-opts.MaxRectAspectRatio, opts.MaxRectAspectRatio);

            double dist = disSize(rng) * opts.MinServiceRange * (1 << disSizeExp(rng));
            double aspect = opts.MaxRectAspectRatio > 0 ? disAspectRatio(rng) : 0;

            if (aspect > 0) {
                double dist2 = dist * (1 - aspect);
                NGeo::TGeoPoint lower(center.Lon() - dist, center.Lat() - dist2);
                NGeo::TGeoPoint upper(center.Lon() + dist, center.Lat() + dist2);
                return NGeo::TGeoWindow(lower, upper);
            } else {
                double dist2 = dist * (1 + aspect);
                NGeo::TGeoPoint lower(center.Lon() - dist2, center.Lat() - dist);
                NGeo::TGeoPoint upper(center.Lon() + dist2, center.Lat() + dist);
                return NGeo::TGeoWindow(lower, upper);
            }
        }

        static void MakeDocuments(TDocuments& output, const TVector<NGeo::TGeoPoint>& samplePoints, const TGeneratorOpts& opts) {
            std::minstd_rand rng(opts.RngSeed);

            ui32 multiplier = Max<ui32>(1, std::floor((double)opts.DocCount / samplePoints.size()));
            size_t targetSize = output.size() + opts.DocCount;
            while (!(output.size() >= targetSize)) {
                for (const NGeo::TGeoPoint& samplePoint: samplePoints) {
                    // generate one or more documents from the samplePoint
                    for (ui32 k = 0; k < multiplier; ++k) {
                        auto step = RandomStep(rng, opts.DistFromSample);
                        NGeo::TGeoPoint center(samplePoint.Lon() + step.first, samplePoint.Lat() + step.second);
                        NGeo::TGeoWindow&& area = RandomRect(rng, center, opts);
                        output.emplace_back(area);
                    }

                    if (output.size() >= opts.DocCount)
                        break;
                }
            }

        }

    public:
        static TFsPath GetTestResource(const TString& resource) {
            return TFsPath(GetWorkPath()) / resource;
        }

        void SetUp() override {
            TFormattedPrecisionTimer stopWatch("    SetUp took ");
            if (SamplePoints.empty()) {
                const TFsPath sampleFile = GetTestResource("10000_russians/10000_russians.tsv");
                SamplePoints = ReadSamplePoints(sampleFile);
                Y_ENSURE(!SamplePoints.empty());
            }

            if (Documents.empty()) {
                constexpr ui32 percent = DocCount / 100;
                ui32 rngSeed = Seed ? Seed : std::random_device()();
                Cout << "Tests will use RngSeed=" << Hex(rngSeed) << Endl;
                TVector<NGeo::TGeoPoint> samplePoints = SamplePoints;
                std::shuffle(samplePoints.begin(), samplePoints.end(), std::minstd_rand(rngSeed));

                TGeneratorOpts opts;
                opts.RngSeed = rngSeed;
                opts.DocCount = 95 * percent;
                opts.DistFromSample = 10.0 / 113; // approx 10 km
                opts.MaxRectAspectRatio = 0.7;
                opts.MinServiceRange = 0.3 / 113; // from approx. 300 meters
                opts.MaxRangeExp = 6;             // till 40 km
                MakeDocuments(Documents, samplePoints, opts);

                opts.RngSeed = ++rngSeed;
                opts.DocCount = 5 * percent;
                opts.DistFromSample = 10.0 / 113; // approx 10 km
                opts.MaxRectAspectRatio = 0.2;
                opts.MinServiceRange = 10.0 / 113; // from approx. 10 km
                opts.MaxRangeExp = 2;  // till approx. 80km
                MakeDocuments(Documents, samplePoints, opts);

                Cout << "Documents in index: " << Documents.size() << Endl;
            }

            // Now make the plan
            SetUpRequestsForPerfTest();
         }

         void SetUpRequestsForPerfTest() {
            Requests.clear();

            ui32 rngSeed = 100 + (Seed ? Seed : std::random_device()());
            TGeneratorOpts opts;
            // 9500 city-sized windows
            opts.RngSeed = ++rngSeed;
            TVector<NGeo::TGeoPoint> samplePoints = SamplePoints;
            std::shuffle(samplePoints.begin(), samplePoints.end(), std::minstd_rand(rngSeed));
            opts.DocCount = 9500;
            opts.DistFromSample = 1 / 113; // approx 1 km
            opts.MaxRectAspectRatio = 0.1; // always squares
            opts.MinServiceRange = 5.0 / 113; // from approx. 5 km
            opts.MaxRangeExp = 1;  // till approx. 20km
            MakeDocuments(Requests, samplePoints, opts);

            // 10 point-sized requests
            opts.RngSeed = ++rngSeed;
            opts.DocCount = 500;
            opts.DistFromSample = 15 / 113;
            opts.MaxRectAspectRatio = 0;
            opts.MinServiceRange = 0;
            opts.MaxRangeExp = 0;
            MakeDocuments(Requests, samplePoints, opts);
        }

         void SetUpRequestsForPruningTest() {
            Requests.clear();

            ui32 rngSeed = 100 + (Seed ? Seed : std::random_device()());
            TGeneratorOpts opts;
            // 5000 city-sized windows (scaled down at random)
            opts.RngSeed = ++rngSeed;
            TVector<NGeo::TGeoPoint> samplePoints = SamplePoints;
            std::shuffle(samplePoints.begin(), samplePoints.end(), std::minstd_rand(rngSeed));
            opts.DocCount = 500;
            opts.DistFromSample = 1 / 113; // approx 1 km
            opts.MaxRectAspectRatio = 0.5; // always squares
            opts.MinServiceRange = 5.0 / 113; // from approx. 5 km
            opts.MaxRangeExp = 1;  // till approx. 20km
            MakeDocuments(Requests, samplePoints, opts);

            // 5000 300m square zones
            opts.RngSeed = ++rngSeed;
            opts.DocCount = 500;
            opts.DistFromSample = 15 / 113;
            opts.MaxRectAspectRatio = 0;
            opts.MinServiceRange = 0.3 / 113;
            opts.MaxRangeExp = 0;
            MakeDocuments(Requests, samplePoints, opts);
        }

        void TestKdTreePerf() {
            TGeoIndex kdIndex;
            TGeoIndexBuilder bld;
            Y_ENSURE(!Documents.empty());
            ui32 docId = 0;
            for (const NGeo::TGeoWindow& rect: Documents) {
                bld.AddDoc(StreamId, Kps, docId++, rect);
            }
            {
                TFormattedPrecisionTimer stopWatch("    kdTree.Finalize() took ");
                bld.Finalize(kdIndex);
            }


            {
                size_t totalFound = 0;
                TStringStream timerMsg;
                timerMsg << "executing " << Requests.size() << " lookups in KdTree took ";
                TFormattedPrecisionTimer stopWatch(timerMsg.Str().c_str());
                for (const NGeo::TGeoWindow& req: Requests) {
                    //Cout << "Query: " << req << Endl;

                    TGeoIndexTree& kdTree = *kdIndex.Trees[TPartKey{StreamId, Kps}];
                    auto result = kdTree.FindIntersections(req, PruningLimit);
                    totalFound += result.size();

                    //for (size_t k = 0; k < Min<size_t>(10u, result.size()); ++k) {
                    //    const NGeo::TGeoWindow& document = Documents[result[k]];
                    //    TMaybe<NGeo::TGeoWindow> intersection = NGeo::Intersection(req, document);
                    //    Cout << "DocId=" << result[k] << "\t" << Documents[result[k]] << " , intersection: " << (intersection ? *intersection: NGeo::TGeoWindow()) << Endl;
                    //}
                    //Cout << Endl;
                }
                Cout << "totalFound: " << totalFound << Endl;
            }
            {
                size_t totalHits = 0;
                TStringStream timerMsg;
                timerMsg << "executing " << Requests.size() << " CountHits in KdTree took ";
                TFormattedPrecisionTimer stopWatch(timerMsg.Str().c_str());
                for (const NGeo::TGeoWindow& req: Requests) {
                    //Cout << "Query: " << req << Endl;

                    TVector<NGeo::TGeoWindow> normalized = TGeoIndexTree::Normalize(req);
                    TGeoIndexTree& kdTree = *kdIndex.Trees[TPartKey{StreamId, Kps}];
                    auto result = kdTree.CountHits(normalized);
                    totalHits += result;

                    //for (size_t k = 0; k < Min<size_t>(10u, result.size()); ++k) {
                    //    const NGeo::TGeoWindow& document = Documents[result[k]];
                    //    TMaybe<NGeo::TGeoWindow> intersection = NGeo::Intersection(req, document);
                    //    Cout << "DocId=" << result[k] << "\t" << Documents[result[k]] << " , intersection: " << (intersection ? *intersection: NGeo::TGeoWindow()) << Endl;
                    //}
                    //Cout << Endl;
                }
                Cout << "Total count of hits processed by CountHits(): " << totalHits << Endl;
            }
        }

        void TestPruningPerf() {
            Y_ENSURE(!Documents.empty());
            ui32 docId = 0;
            TGeoIndex kdIndex;
            TGeoIndexBuilder bld;
            for (const NGeo::TGeoWindow& rect: Documents) {
                bld.AddDoc(StreamId, Kps, docId++, rect);
            }
            {
                TFormattedPrecisionTimer stopWatch("    kdTree.Finalize() took ");
                bld.Finalize(kdIndex);
            }

            SetUpRequestsForPruningTest();

            {
                TStringStream timerMsg;
                timerMsg << "executing " << Requests.size() << " 'prne' pruning calculations took ";
                TFormattedPrecisionTimer stopWatch(timerMsg.Str().c_str());

                const ui32 prne = 2000; // expand until 2000 docs are in range
                const double distanceLimitDeg = NGeo::GetLongitudeFromMetersAtEquator(100000); // 100 km
                const NGeo::TSize spanLimit{distanceLimitDeg, distanceLimitDeg};
                for (const NGeo::TGeoWindow& req: Requests) {
                    const NGeo::TSize limit = NGeo::ConstructWindowFromEquatorSize(req.GetCenter(), spanLimit).GetSize();
                    TGeoIndexTree& kdTree = *kdIndex.Trees[TPartKey{StreamId, Kps}];
                    kdTree.ExpandToDocCount(req, 0.f, limit, prne);
                }
            }
            {
                TStringStream timerMsg;
                timerMsg << "executing " << Requests.size() << " 'prn' lookups took ";
                TFormattedPrecisionTimer stopWatch(timerMsg.Str().c_str());

                const ui32 prn = 1000; // expand or collapse to obtain exactly 1000 docs
                const double distanceLimitDeg = NGeo::GetLongitudeFromMetersAtEquator(200000); // 200 km
                const NGeo::TSize spanLimit{distanceLimitDeg, distanceLimitDeg};
                for (const NGeo::TGeoWindow& req: Requests) {
                    const NGeo::TSize limit = NGeo::ConstructWindowFromEquatorSize(req.GetCenter(), spanLimit).GetSize();
                    TGeoIndexTree& kdTree = *kdIndex.Trees[TPartKey{StreamId, Kps}];
                    kdTree.FindIntersectionsWithDynamicPruning(req, prn, true, true, &limit);
                }
            }
        }

        TVector<ui32> FindIntersectionsBruteforce(const NGeo::TGeoWindow& req, ui32 pruningLimit) {
            TVector<ui32> result;
            for (size_t docId = 0; docId < Documents.size() && result.size() < pruningLimit; ++docId) {
                if (NGeo::Intersects(req, Documents[docId]))
                    result.push_back(docId);
            }
            return result;
        }

        static double GetGeodeiticEps(double metricEps) {
            return NGeo::GetLongitudeFromMetersAtEquator(metricEps);
        }

        const double GeodeiticEps = GetGeodeiticEps(1); // ε=0.001 km

        //
        // Check the difference between expected and actual. Ignore any differences less than 1 meter
        //
        bool CheckDifference(IOutputStream& o, const TVector<ui32>& expected, const TVector<ui32>& actual, const NGeo::TGeoWindow& req, ui64& coarserStat, TString resultDispName) {
            bool differs = false;

            // Check for missing docIds
            TVector<ui32> diff;
            std::set_difference(expected.begin(), expected.end(), actual.begin(), actual.end(), std::back_inserter(diff));
            if (!diff.empty()) {
                // recheck the missing docIds with ε=0.001 km
                auto rechecker = [&](ui32 docId) {
                    TMaybe<NGeo::TGeoWindow> intersect = NGeo::Intersection(req, Documents[docId]);
                    return intersect.Defined() && (intersect->GetSize().GetWidth() < GeodeiticEps || intersect->GetSize().GetHeight() < GeodeiticEps);
                };
                auto e = std::remove_if(diff.begin(), diff.end(), rechecker);
                coarserStat += diff.end() - e;
                diff.erase(e, diff.end());
            }
            if (!diff.empty()) {
                differs = true;
                o << resultDispName << " misses docIds: " << JoinSeq(" ", diff) << Endl;
            }


            diff.clear();
            std::set_difference(actual.begin(), actual.end(), expected.begin(), expected.end(), std::back_inserter(diff));
            if (!diff.empty()) {
                // recheck the unexpected docIds with ε=0.001 km
                auto rechecker = [&](ui32 docId) {
                    auto inflatedDoc = Documents[docId];
                    inflatedDoc.Inflate(GeodeiticEps);
                    return NGeo::Intersects(req, inflatedDoc);
                };
                auto e = std::remove_if(diff.begin(), diff.end(), rechecker);
                coarserStat += diff.end() - e;
                diff.erase(e, diff.end());
            }
            if (!diff.empty()) {
                differs = true;
                o << resultDispName << " has unexpected docIds: " << JoinSeq(" ", diff) << Endl;
            }

            return differs;
        }

        void TestCompareOutput() {
            TGeoIndex kdIndex;
            TGeoIndexBuilder bld;
            {
                Y_ENSURE(!Documents.empty());
                TFormattedPrecisionTimer stopWatch("Building index took ");
                ui32 docId = 0;
                for (const NGeo::TGeoWindow& rect: Documents) {
                    bld.AddDoc(StreamId, Kps, docId++, rect);
                }
                bld.Finalize(kdIndex);
            }

            //
            // save kdTree to a file and read back
            //
            TGeoIndex loadedIndex;
            {
                const TPathName indexPrefix{TFsPath::Cwd() / "index"};
                const TFsPath indexPath = TGeoIndexFormatter::FormatFileName(indexPrefix);
                UNIT_ASSERT_VALUES_EQUAL(indexPath.Basename(), "indexgeo.rty");

                indexPath.DeleteIfExists();
                {
                    TFormattedPrecisionTimer stopWatch("Writing file took ");
                    TGeoIndexFormatter::Save(indexPath, kdIndex);
                }
                const ui32 origHitsCount = kdIndex.GetHitsCount();
                TFileStat fileStat;
                UNIT_ASSERT(indexPath.Exists());
                UNIT_ASSERT(indexPath.Stat(fileStat));
                Cout << "Successfully created an inverted index with " << origHitsCount << " hits, file name: '" << indexPath.Basename() << "', file size: " << fileStat.Size << Endl;

                {
                    TFormattedPrecisionTimer stopWatch("Reading file took ");
                    TGeoIndexFormatter::Load(indexPath, loadedIndex);
                }

                const ui32 loadedHitsCount = loadedIndex.GetHitsCount();
                UNIT_ASSERT_VALUES_EQUAL(origHitsCount, loadedHitsCount);
            }

            //
            // Execute some amount of queries on the loaded Tree, validating the result every time
            //
            {
                size_t reqCount = Min<size_t>(Requests.size(), 500);
                const ui32 printStep = Max<ui32>(1, reqCount / 10);
                ui32 printStepLast = Max<ui32>();
                Cout << "Executing " << reqCount << " queries..." << Endl;

                ui64 totalResultsCnt = 0;
                ui64 coarseResultsCnt = 0;
                for (ui32 reqNo = 1; reqNo <= reqCount; ++reqNo) {
                    const NGeo::TGeoWindow& req = Requests[reqNo - 1];

                    TGeoIndexTree& loadedTree = *loadedIndex.Trees[TPartKey{StreamId, Kps}];
                    auto resTree = loadedTree.FindIntersections(req, PruningLimit);
                    if (!std::is_sorted(resTree.begin(), resTree.end()))
                        std::sort(resTree.begin(), resTree.end());

                    ui32 printStepCur = reqNo / printStep;
                    if (printStepLast != printStepCur) {
                        printStepLast = printStepCur;
                        Cout << "Query " << reqNo << ": " << req << Endl;
                        Cout << "Results count: " << resTree.size() << Endl;
                    }

                    auto expected = FindIntersectionsBruteforce(req, PruningLimit);
                    bool differ = CheckDifference(Cout, expected, resTree, req, coarseResultsCnt, "resTree");
                    UNIT_ASSERT_C(!differ, "reqNo " << reqNo);

                    totalResultsCnt += resTree.size();
                }

                Cout << Endl;
                Cout << "Total DocIds: " << totalResultsCnt << Endl;
                Cout << "Coarse DocIds (ε=0.001 km): " << coarseResultsCnt << Endl;
                Cout << "Test passed." << Endl;
            }
        }
    };
}

UNIT_TEST_SUITE_REGISTRATION(NRTYGeo::TMingeoFatTest);
