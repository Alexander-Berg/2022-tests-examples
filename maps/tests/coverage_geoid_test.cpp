#include "../test_tools/io_std.h"

#include "common.h"

#include <yandex/maps/coverage5/coverage.h>
#include <yandex/maps/coverage5/layer.h>

#include <boost/test/unit_test.hpp>

#include <fstream>
#include <sstream>

using namespace maps;
using namespace maps::coverage5::test;

typedef std::vector<coverage5::RegionId> RegionIDs;

const char* g_mmsGeoidCoverage = "mms/libcov_geoid.mms.1";
struct PointGeoIDTestData {
    std::string name;
    geolib3::Point2 point;
    RegionIDs ids;
};

PointGeoIDTestData g_geoidTestData[] {
    { "Moscow", {37.614924, 55.75298}, {213, 225} },
    { "Moscow obl", {37.306299, 56.185361}, {1, 225} },
    { "Spb", {30.348842, 59.922735}, {2, 10174, 225} },
    { "Ekb", {60.596572, 56.8220}, {54, 225} },
    { "N.Novgorod", {43.995539, 56.31316}, {47, 225} },
    { "Novosib", {82.94651, 55.020292}, {65, 225} },
    { "Rostov-on-Don", {39.717452, 47.22501}, {39, 225} },
    { "Krasnodar", {38.976719, 45.02637}, {35, 10995, 225} },
    { "Samara", {50.103002, 53.19561}, {51, 225} },
    { "Kazan", {49.138646, 55.7842}, {43, 225} },
    { "Kharkov", {36.273572, 49.9922}, {147, 187} },
    { "Odessa", {30.726804, 46.4698}, {145, 20541, 187} },
    { "Orel", {36.0544, 52.9682}, {10, 225} },
    { "Ufa", {55.9681, 54.7324}, {172, 225} },
    { "Chelabinsk", {61.4067, 55.1652}, {56, 225} },
    { "N.Chelny", {52.3989, 55.7395}, {236, 225} },
    { "Tula", {37.6114, 54.1932}, {15, 225} },
    { "Ryazan", {39.7158, 54.6202}, {11, 225} },
    { "Dnepropetrovsk", {35.0109, 48.45}, {141, 187} },
    { "Donetsk", {37.7665, 47.9877}, {142, 187} },
    { "Lvov", {24.0103, 49.8303}, {144, 187} },
    { "Zaporoj'e", {35.1859, 47.8395}, {960, 187} },
    { "Almaty", {76.9257, 43.2782}, {162} },
    { "Astana", {71.4527, 51.1624}, {163} },
    { "Krasnoyarsk", {92.8598, 56.0133}, {62, 225} },
    { "Omsk", {73.3808, 54.989}, {66, 225} },
    { "Poltava", {34.538, 49.5915}, {964, 187} },
    { "Volgograd", {44.5046, 48.7093}, {38, 225} },
    { "Yaroslavl", {39.8939, 57.6241}, {16, 225} },
    { "Khabarovsk", {135.084, 48.476}, {76, 11457, 225} },
    { "Orenburg", {55.1097, 51.7695}, {48, 225} },
    { "Perm", {56.2411, 58}, {50, 225} },
    { "Saratov", {46.0237, 51.5379}, {194, 225} },
    { "Voronej", {39.1903, 51.6492}, {193, 225} },
    { "Kiev", {30.5223, 50.4511}, {143, 187} },
    { "Minsk", {27.5617, 53.9035}, {157} },
    { "Irkutsk", {104.276, 52.313}, {63, 225} },
    { "Vladivostok", {131.928, 43.1341}, {75, 11409, 225} },
    { "Tjumen", {65.5584, 57.1826}, {55, 225} },
    { "Penza", {44.9974, 53.1832}, {49, 225} },
    { "Kursk", {36.1817, 51.7172}, {8, 225} },
    { "Kirov", {49.6672, 58.6034}, {46, 225} },
    { "V.Novgorod", {31.2703, 58.5227}, {24, 225} },
    { "Sochi", {39.7223, 43.5828}, {239, 10995, 225} },
    { "Tver", {35.8922, 56.8555}, {14, 225} },
    { "Gelendzhik", {38.0824, 44.5582}, {10990, 10995, 225} },
    { "Anapa", {37.3231, 44.8898}, {1107, 10995, 225} },
    { "Tuapse", {39.0801, 44.0995}, {1058, 10995, 225} },
    { "Krasnaja Poljana", {40.2056, 43.6794}, {10994, 10995, 225} },
    { "Novorossijsk", {37.7767, 44.7205}, {970, 10995, 225} },
    { "Uzhgorod", {22.2884, 48.6259}, {10358, 20530, 187} },
    { "Ivano-Frankovsk", {24.7171, 48.9117}, {10345, 187} },
    { "Lutsk", {25.3445, 50.7569}, {20222, 187} },
    { "Ternopol", {25.5945, 49.5536}, {10357, 187} },
    { "Chernovtsy", {25.9349, 48.2923}, {10365, 187} },
    { "Rovno", {26.2508, 50.6198}, {10355, 187} },
    { "Khmelnitskij", {26.9787, 49.42}, {961, 187} },
    { "Vinnitsa", {28.4737, 49.2336}, {963, 187} },
    { "Cherkassy", {32.0594, 49.4447}, {10363, 187} },
    { "Nikolaev", {31.9876, 46.965}, {148, 187} },
    { "Kirovograd", {32.2665, 48.5104}, {20221, 187} },
    { "Herson", {32.6139, 46.6403}, {962, 187} },
    { "Sumy", {34.8029, 50.912}, {965, 187} },
    { "Lugansk", {39.3076, 48.5721}, {222, 187} },
    { "Chernigov", {31.2984, 51.4911}, {966, 187} },
    { "Sevastopol", {33.5246, 44.6175}, {959, 977, 187} },
    { "Simferopol", {34.0971, 44.9523}, {146, 977, 187} },
    { "Zhitomir", {28.6574, 50.2546}, {10343, 187} },
    { "Stavropol", {41.9652, 45.0429}, {36, 11069, 225} },
    { "Murmansk", {33.0778, 68.9633}, {23, 10897, 225} },
    { "Kaliningrad", {20.5008, 54.7197}, {22, 10857, 225} },
    { "Petrozavodsk", {34.345, 61.7844}, {10933, 225} },
    { "Pskov", {28.3621, 57.8044}, {10926, 225} },
    { "Ivanovo", {40.9797, 57.0176}, {10687, 225} },
    { "Vladimir", {40.4203, 56.1303}, {10658, 225} },
    { "Tambov", {41.4282, 52.7174}, {10802, 225} },
    { "Lipetsk", {39.5681, 52.6235}, {10712, 225} },
    { "Belgorod", {36.6067, 50.5943}, {10645, 225} },
    { "Elista", {44.2788, 46.3288}, {11015, 225} },
    { "Maikop", {40.134, 44.6174}, {11004, 225} },
    { "Mahachkala", {47.4904, 42.9802}, {11010, 225} },
    { "Cherkessk", {42.0676, 44.2386}, {11020, 225} },
    { "Nalchick", {43.618, 43.4907}, {11013, 225} },
    { "Grozniy", {45.6858, 43.3084}, {11024, 225} },
    { "Vladikavkaz", {44.6882, 43.0378}, {11021, 225} },
    { "Nazran", {44.7695, 43.229}, {11012, 225} },
    { "Syiktyivkar", {50.7919, 61.6582}, {10939, 225} },
    { "Kostroma", {40.9254, 57.7666}, {10699, 225} },
    { "Barnaul", {83.7572, 53.3513}, {11235, 225} },
    { "Blagoveshensk", {127.541, 50.2875}, {11375, 225} },
    { "Birobidgan", {132.935, 48.7929}, {10243, 225} },
    { "Kemerovo", {86.0603, 55.3438}, {11282, 225} },
    { "Kurgan", {65.3197, 55.4416}, {11158, 225} },
    { "Gorno-Altaysk", {85.9596, 51.9578}, {10231, 225} },
    { "Ulan-Ude", {107.583, 51.8407}, {11330, 225} },
    { "Kyizyil", {94.4223, 51.7023}, {10233, 225} },
    { "Tomsk", {84.9914, 56.4603}, {11353, 225} },
    { "Hanty-Mansiysk", {69.0272, 61.0087}, {11193, 225} },
    { "Magadan", {150.799, 59.5605}, {11403, 225} },
    { "Smolensk", {32.0345, 54.798}, {10795, 225} },
    { "Kaluga", {36.2786, 54.5335}, {10693, 225} },
    { "Cheboksary", {47.2643, 56.1132}, {11156, 225} },
    { "Bryansk", {34.4059, 53.262}, {10650, 225} },
    { "Izhevsk", {53.1898, 56.8015}, {11148, 225} },
    { "Yoshkar-Ola", {47.8802, 56.6219}, {11077, 225} },
    { "Yoshkar-Ola", {47.8802, 56.6219}, {11077, 225} },
    { "Petropavlovsk-Kamchatskiy", {158.636, 53.0564}, {11398, 225} },
    { "Chita", {113.495, 52.0282}, {21949, 225} },
    { "Astrakhan", {48.059, 46.3604}, {10946, 225} },
    { "Saransk", {45.1904, 54.1956}, {11117, 225} },
    { "Ulyanovsk", {48.41216, 54.306953}, {11153, 225} },
    { "Arkhangelsk", {40.5742, 64.5508}, {10842, 225} },
    { "Salekhard", {66.6269, 66.5313}, {11232, 225} },
    { "Yakutsk", {129.756, 62.0407}, {11443, 225} },
    { "Anadyr", {177.521, 64.7363}, {10251, 225} },
    { "Naryan-Mar", {53.0064, 67.6389}, {10176, 225} },
    { "Vologda", {39.8831, 59.2068}, {10853, 225} },
    { "Uelen, Chukotka", {-169.802, 66.1558}, {10251, 225} },
    { "nowhere", {30.3488, 85.9227}, {} }
};

RegionIDs getRegionIDs(const coverage5::Regions& regions)
{
    RegionIDs ids;
    for (const coverage5::Region& region: regions) {
        ids.push_back(*region.id());
    }
    return ids;
}

BOOST_AUTO_TEST_CASE(tree_test_point_geoid)
{
    using namespace maps::coverage5;
    setTestsDataCwd();

    Coverage cov(g_mmsGeoidCoverage, SpatialRefSystem::Geodetic);

    for (const PointGeoIDTestData& test: g_geoidTestData) {
        RegionIDs regionIDs = getRegionIDs(
            cov["geoid"].regions(test.point, boost::none));
        BOOST_CHECK_MESSAGE(
            (regionIDs.size() == test.ids.size() && regionIDs == test.ids),
            "Region IDs check failed:\n\tPoint: " << test.point <<
            "\n\tName: " << test.name <<
            "\n\texpected: " << test.ids <<
            "\n\treceived: " << regionIDs);
    }
}

BOOST_AUTO_TEST_CASE(tree_test_point_geoid_min_area)
{
    using namespace maps::coverage5;
    setTestsDataCwd();

    Coverage cov(g_mmsGeoidCoverage, SpatialRefSystem::Geodetic);

    for (const PointGeoIDTestData& test: g_geoidTestData) {
        boost::optional<Region> region =
            cov["geoid"].minAreaRegion(test.point, boost::none);
        std::ostringstream os;
        if (region) {
            os << *(*region).id();
        } else {
            os << "(none)";
        }
        BOOST_CHECK_MESSAGE((!region && test.ids.empty()) ||
            (region && *(*region).id() == test.ids.front()),
            "Min area region ID check failed:\n\tPoint: " << test.point <<
            "\n\tName: " << test.name <<
            "\n\texpected: " << (test.ids.empty() ? -1 : test.ids.front()) <<
            "\treceived: " << os.str());
    }
}
