#include <boost/test/unit_test.hpp>

#include "mocks/bitmaps_downloader_mock.h"
#include "../bitmaps_downloader.h"

namespace yandex {
namespace maps {
namespace navi {
namespace ads {

BOOST_AUTO_TEST_SUITE(DownloadBitmapTests)

BOOST_AUTO_TEST_CASE(Download)
{
    MockBitmapsDownloader downloader({ "3", "4" });
    std::set<std::string> ids { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" };
    std::set<std::string> downloadedIds { "1", "2", "5", "6", "7", "8", "9", "10" };
    runtime::async::global()->spawn([&] {
        downloadAllBitmaps(&downloader, { ids.begin(), ids.end() });
    }).wait();

    BOOST_REQUIRE(downloadedIds == downloader.downloaded());
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace ads
} // namespace navikit
} // namespace maps
} // namespace yandex

