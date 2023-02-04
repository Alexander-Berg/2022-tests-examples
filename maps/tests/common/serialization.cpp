#include "serialization.h"

#include <maps/infopoint/lib/auth/user_controller.h>
#include <maps/infopoint/lib/export/pb_point_feed.h>
#include <maps/infopoint/lib/export/xml_point_feed.h>

namespace infopoint::tests {

const std::string& STUB_REMOTE_IP = "127.0.0.1";
const maps::http::URL STUB_BLACKBOX_URL = "http://localhost";

class MockBackend : public db::Backend {};

template<class Feed>
std::string toString(
    const infopoint::Infopoint &point,
    const std::string& suppliersConfFile)
{
    using namespace infopoint;

    auto pointXml = std::stringstream();

    auto backend = MockBackend();
    auto userController = infopoint::UserController(
        std::make_shared<UserConfig>(suppliersConfFile),
        &backend,
        std::make_unique<const Blackbox>(
            STUB_BLACKBOX_URL, BlackboxAuthorizationMethod::NoAuthorization),
        RoadEventCreationAuthorizationPolicy::AllowUnauthorized);

    auto feed = Feed(
        maps::i18n::bestLocale((maps::i18n::defaultLocale())));
    feed.setPoint(point);
    feed.write(userController, STUB_REMOTE_IP, pointXml);

    return pointXml.str();
}

std::string toXml(
    const infopoint::Infopoint &point,
    const std::string& suppliersConfFile)
{
    return toString<infopoint::XmlPointFeed>(point, suppliersConfFile);
}

std::string toProtobuf(
    const infopoint::Infopoint &point,
    const std::string& suppliersConfFile)
{
    return toString<infopoint::PbPointFeed>(point, suppliersConfFile);
}

} //namespaces infopoint::tests
