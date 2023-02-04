#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/common/env.h>

#include <maps/analyzer/libs/gossip/lib/include/config.h>
#include <maps/analyzer/libs/gossip/lib/include/exception.h>
#include <maps/libs/json/include/exception.h>

#include <optional>
#include <string>

const std::string goodConf = static_cast<std::string>(ArcadiaSourceRoot() + "/maps/analyzer/libs/gossip/tests/configs/good_consumer.json");
const std::string goodConf2 = static_cast<std::string>(ArcadiaSourceRoot() + "/maps/analyzer/libs/gossip/tests/configs/good_conf2.json");
const std::string badConf = static_cast<std::string>(ArcadiaSourceRoot() + "/maps/analyzer/libs/gossip/tests/configs/not_hostname.json");
const std::string badConf2 = static_cast<std::string>(ArcadiaSourceRoot() + "/maps/analyzer/libs/gossip/tests/configs/bad_hostname.json");
const std::string nonExistentFile = "/non/existent/file/";

TEST(ReadConfigfromFile, FileNotFoundError) {
    EXPECT_THROW(maps::gossip::fromFile(nonExistentFile), maps::gossip::FileNotFoundError);
}

TEST(ReadConfigfromFile, NohostnameInConfException) {
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(maps::gossip::fromFile(badConf), maps::gossip::ConfigError, "set up host to consumer");
}

TEST(ReadConfigfromFile, ParserErrorException) {
    EXPECT_THROW(maps::gossip::fromFile(badConf2), maps::json::CastError);
}

TEST(ReadConfigfromFile, SuccessConfWithoutGroupNames) {
    auto t = maps::gossip::fromFile(goodConf);
    EXPECT_EQ(t.hostname_.value(), "localhost:5007");
    std::vector<std::string> consumers = {
            "localhost:5001", "localhost:5002", "localhost:5003", "localhost:5004", "localhost:5005",
            "localhost:5006", "localhost:5007", "localhost:5008", "localhost:5009", "localhost:5010",
            "localhost:5011", "localhost:5012", "localhost:5013", "localhost:5014", "localhost:5015"
    };
    EXPECT_EQ(t.consumers_, consumers);
    EXPECT_EQ(t.distributors_, std::vector<std::string> { "localhost:1001" });
    EXPECT_TRUE(t.groupNames_.empty());
    EXPECT_EQ(t.hostGroup_["localhost:5003"], "VLA");
    EXPECT_EQ(t.hostGroup_["localhost:5006"], "SAS");
    EXPECT_EQ(t.hostGroup_["localhost:5012"], "MAN");
}

TEST(ReadConfigfromFile, SuccessConfWithGroupNames) {
    auto t = maps::gossip::fromFile(goodConf2);
    EXPECT_EQ(t.hostname_.value(), "brvqnvgeccxv72bw.man.yp-c.yandex.net");
    std::vector<std::string> groups = { "vla", "man", "sas" };
    EXPECT_EQ(t.groupNames_, groups);
    EXPECT_EQ(t.hostGroup_["omna5plrj3c5jsj6.vla.yp-c.yandex.net"], "vla");
    EXPECT_EQ(t.hostGroup_["bdagveyvh4kzifbv.sas.yp-c.yandex.net"], "sas");
    EXPECT_EQ(t.hostGroup_["brvqnvgeccxv72bw.man.yp-c.yandex.net"], "man");
}
