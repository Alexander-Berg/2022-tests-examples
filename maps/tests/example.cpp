#include <maps/wikimap/mapspro/libs/stat_client/include/stat_client.h>

#include <maps/libs/chrono/include/days.h>
#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/introspection/include/comparison.h>
#include <maps/libs/log8/include/log8.h>
#include <yandex/maps/wiki/common/extended_xml_doc.h>
#include <yandex/maps/wiki/common/stat_utils.h>

#include <sstream>
#include <string>

namespace maps::wiki::stat_client::examples {

using introspection::operator==;

class Dimensions {
public:
    Dimensions(chrono::TimePoint fielddate, const std::string& region)
        : fielddate_(std::chrono::round<chrono::Days>(fielddate))
        , region_(region)
    {}

    auto introspect() const { return std::tie(fielddate_, region_); }

    static void printHeader(csv::OutputStream& os) { os << "fielddate" << "region"; }

    void print(csv::OutputStream& os) const {
        os << chrono::formatIntegralDateTime(fielddate_, "%Y-%m-%d") << region_;
    }

private:
    std::chrono::time_point<std::chrono::system_clock, chrono::Days> fielddate_;
    std::string region_;
};

struct Measures {
    size_t activeUsers;
    size_t moderatedTasks;

    static void printHeader(csv::OutputStream& os) { os << "active_users" << "moderated_tasks"; }
    void print(csv::OutputStream& os) const { os << activeUsers << moderatedTasks; }
};

struct ShinyDailyReport: public stat_client::Report<Dimensions, Measures, stat_client::Scale::Daily> {
    ShinyDailyReport(): Report("Maps.Wiki/ShinyReports/ShinyReport") {}
};

ShinyDailyReport prepareReport() {
    const auto now = chrono::TimePoint::clock::now();

    ShinyDailyReport report;
    report[Dimensions(now, "cis")] = {100500, 42};

    return report;
}

void upload(
    const std::string& statUploadApiUrl,
    const ShinyDailyReport& report)
{
    try {
        stat_client::Uploader(statUploadApiUrl).upload(report);
    } catch (const stat_client::ReportUploadError &e) {
        ERROR() << e.what();
    }
}

void uploadShinyDailyReport(
    const std::string& statUploadApiUrl,
    const std::string& csvTable)
{
    try {
        stat_client::Uploader(statUploadApiUrl).upload(
            "Maps.Wiki/ShinyReports/ShinyReport",
            csvTable,
            Scale::Daily
        );
    } catch (const stat_client::ReportUploadError &e) {
        ERROR() << e.what();
    }
}

std::string
getStatUploadApiUrl()
{
    const common::ExtendedXmlDoc config("/etc/yandex/maps/wiki/services/services.xml");
    const auto statUploadApiUrl = common::getStatUploadApiUrl(config);
    return statUploadApiUrl;
}

} // namespace maps::wiki::stat_client::examples
