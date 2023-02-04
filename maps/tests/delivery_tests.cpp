#include <maps/factory/libs/sproto_helpers/delivery.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {

namespace {

const auto DATE = "2021-01-01";
const auto NAME = "test_delivery";
const auto PATH = "/";
const auto DOWNLOAD_URL = "url";
const int64_t YEAR = 2021;
const std::vector<std::string> COPYRIGHTS = {"copyright"};
const bool ENABLED = true;

db::Delivery dbDelivery()
{
    auto delivery = db::Delivery(
        std::nullopt, DATE, NAME, PATH);
    delivery.setYear(YEAR)
            .setDownloadUrl(DOWNLOAD_URL)
            .setCopyrights(COPYRIGHTS)
            .enable();
    return delivery;
}

sdelivery::Delivery sprotoDelivery()
{
    sdelivery::Delivery delivery;
    delivery.name() = NAME;
    delivery.year() = YEAR;
    delivery.copyrights() = COPYRIGHTS;
    delivery.downloadUrl() = DOWNLOAD_URL;
    delivery.downloadEnabled() = ENABLED;
    return delivery;
}

} // namespace

TEST(test_convert_delivery, test_convert_to_sproto)
{
    auto sdelivery = convertToSproto(dbDelivery());
    EXPECT_EQ(sdelivery.name(), NAME);
    EXPECT_EQ(sdelivery.year(), YEAR);
    EXPECT_EQ(sdelivery.copyrights().at(0), COPYRIGHTS.at(0));
    EXPECT_EQ(*sdelivery.downloadUrl(), DOWNLOAD_URL);
    EXPECT_EQ(*sdelivery.downloadEnabled(), ENABLED);
}

TEST(test_convert_delivery, test_convert_from_sproto)
{
    auto delivery = convertFromSproto(sprotoDelivery());
    EXPECT_EQ(delivery.year(), YEAR);
    EXPECT_EQ(*delivery.downloadUrl(), DOWNLOAD_URL);
    EXPECT_EQ(delivery.name(), NAME);
    EXPECT_EQ(delivery.enabled(), ENABLED);
    EXPECT_EQ(delivery.copyrights().at(0), COPYRIGHTS.at(0));
}

TEST(test_etag_utils, test_delivery_etag)
{
    const auto original = sprotoDelivery();
    auto copy = original;

    EXPECT_EQ(calculateEtag(original), calculateEtag(copy));

    copy.name() = "updated_name";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.year() = 1998;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.copyrights().push_back("new_copyright");
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.downloadUrl() = "updated_url";
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
    copy = original;

    copy.downloadEnabled() = false;
    EXPECT_NE(calculateEtag(original), calculateEtag(copy));
}

} // namespace maps::factory::sproto_helpers::tests
