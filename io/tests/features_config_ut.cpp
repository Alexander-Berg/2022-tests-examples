#include <yandex_io/libs/configuration/features_config.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/protobuf_utils/json.h>

#include <library/cpp/testing/unittest/registar.h>
#include <google/protobuf/util/message_differencer.h>

using namespace quasar;
using namespace google::protobuf::util;

namespace {
    struct Experiments {
        Json::Value config;
        NAlice::TExperimentsProto expected;
    };

    struct SupportedFeatures {
        Json::Value config;
        std::unordered_set<TString> expected;
    };

    struct UnsupportedFeatures {
        Json::Value config;
        std::unordered_set<TString> expected;
    };

    Experiments makeExperiments(const std::vector<TString>& names) {
        Experiments experiments;
        for (const auto& name : names) {
            experiments.config["experiments"].append(name);
            NAlice::TExperimentsProto::TValue val;
            val.SetString("1");
            experiments.expected.MutableStorage()->insert({name, val});
        }
        return experiments;
    }

    Experiments makeDictExperiments(const std::vector<TString>& arrayExperiments,
                                    const std::vector<std::pair<TString, TString>>& dictExperiments) {
        Experiments experiments;
        for (const auto& name : arrayExperiments) {
            experiments.config["experiments"].append(name);
            NAlice::TExperimentsProto::TValue val;
            val.SetString("1");
            experiments.expected.MutableStorage()->insert({name, val});
        }
        for (const auto& [key, val] : dictExperiments) {
            experiments.config["dictExperiments"][key] = val;
            NAlice::TExperimentsProto::TValue value;
            value.SetString(val);
            experiments.expected.MutableStorage()->insert({key, value});
        }
        return experiments;
    }

    SupportedFeatures makeSupportedFeatures(const std::vector<TString>& names) {
        SupportedFeatures result;
        for (const auto& name : names) {
            result.config.append(name);
            result.expected.insert(name);
        }
        return result;
    }

    UnsupportedFeatures makeUnsupportedFeatures(const std::vector<TString>& names) {
        UnsupportedFeatures result;
        for (const auto& name : names) {
            result.config.append(name);
            result.expected.insert(name);
        }
        return result;
    }

    bool protoMapEqual(const google::protobuf::Map<TString, NAlice::TExperimentsProto::TValue>& a, const google::protobuf::Map<TString, NAlice::TExperimentsProto::TValue>& b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (const auto& [key, val] : a) {
            if (!b.contains(key)) {
                return false;
            }
            if (const auto equal = MessageDifferencer::Equals(val, b.at(key)); !equal) {
                return false;
            }
        }
        return true;
    }
} // namespace

Y_UNIT_TEST_SUITE(FeaturesConfigTest) {

    Y_UNIT_TEST(checkGetExperiments) {
        {
            FeaturesConfig config((Json::Value()));
            NAlice::TExperimentsProto exps{};
            UNIT_ASSERT(protoMapEqual(config.getExperiments().GetStorage(), exps.GetStorage()));
        }

        {
            const auto experiments = makeExperiments({"exp1", "exp2", "exp3"});
            FeaturesConfig config(experiments.config);
            YIO_LOG_INFO(convertMessageToJsonString(config.getExperiments()));
            YIO_LOG_INFO(convertMessageToJsonString(experiments.expected));
            UNIT_ASSERT(protoMapEqual(config.getExperiments().GetStorage(), experiments.expected.GetStorage()));
        }

        {
            const auto fileExperiments = makeExperiments({"exp1", "exp2", "exp3"});
            FeaturesConfig config(fileExperiments.config);

            const auto quasmoExperiments = makeExperiments({"exp4", "exp5"});
            Json::Value systemConfig;
            config.processNewConfig(quasmoExperiments.config);

            YIO_LOG_INFO(convertMessageToJsonString(config.getExperiments()));
            YIO_LOG_INFO(convertMessageToJsonString(quasmoExperiments.expected));
            UNIT_ASSERT(protoMapEqual(config.getExperiments().GetStorage(), quasmoExperiments.expected.GetStorage()));
        }
    }

    Y_UNIT_TEST(checkSetExperiments) {
        {
            const auto experiments = makeExperiments({"exp1", "exp2", "exp3"});
            FeaturesConfig config(experiments.config);
            UNIT_ASSERT(protoMapEqual(config.getExperiments().GetStorage(), experiments.expected.GetStorage()));

            const auto experiments_set = makeExperiments({"exp2", "exp3", "exp7"});
            config.setExperiments(experiments_set.config);
            UNIT_ASSERT(protoMapEqual(config.getExperiments().GetStorage(), experiments_set.expected.GetStorage()));
        }
    }

    Y_UNIT_TEST(checkGetSupportedFeatures) {
        {
            FeaturesConfig config((Json::Value()));
            UNIT_ASSERT(config.getSupportedFeatures() == std::unordered_set<TString>{});
        }

        {
            const auto sf = makeSupportedFeatures({"sf1", "sf2", "sf3"});
            Json::Value cfg = Json::objectValue;
            cfg["supportedFeatures"] = sf.config;
            FeaturesConfig config(cfg);
            for (const auto& kek : config.getSupportedFeatures()) {
                YIO_LOG_INFO(kek);
            }
            for (const auto& kek : sf.expected) {
                YIO_LOG_INFO(kek);
            }
            UNIT_ASSERT(config.getSupportedFeatures() == sf.expected);
        }
    }

    Y_UNIT_TEST(checkAddSupportedFeatures) {
        {
            const auto sf = makeSupportedFeatures({"sf1", "sf2", "sf3"});
            Json::Value cfg = Json::objectValue;
            cfg["supportedFeatures"] = sf.config;
            FeaturesConfig config(cfg);
            UNIT_ASSERT(config.getSupportedFeatures() == sf.expected);

            const auto sf_to_add = makeSupportedFeatures({"sf4", "sf5", "sf6"});
            for (const auto& sf : sf_to_add.config) {
                config.addSupportedFeature(sf.asString());
            }

            const auto sf_after_add = makeSupportedFeatures({"sf1", "sf2", "sf3", "sf4", "sf5", "sf6"});
            UNIT_ASSERT(config.getSupportedFeatures() == sf_after_add.expected);

            // added duplicates should be ignored
            for (const auto& sf : sf_to_add.config) {
                config.addSupportedFeature(sf.asString());
            }
            UNIT_ASSERT(config.getSupportedFeatures() == sf_after_add.expected);
        }
    }

    Y_UNIT_TEST(checkSetSupportedFeatures) {
        {
            const auto sf = makeSupportedFeatures({"sf1", "sf2", "sf3"});
            Json::Value cfg = Json::objectValue;
            cfg["supportedFeatures"] = sf.config;
            FeaturesConfig config(cfg);
            UNIT_ASSERT(config.getSupportedFeatures() == sf.expected);

            const auto sf_set = makeSupportedFeatures({"sf4", "sf5", "sf6"});
            config.setSupportedFeatures(sf_set.config);
            UNIT_ASSERT(config.getSupportedFeatures() == sf_set.expected);
        }
    }

    Y_UNIT_TEST(checkGetUnsupportedFeatures) {
        {
            FeaturesConfig config((Json::Value()));
            UNIT_ASSERT(config.getUnsupportedFeatures() == std::unordered_set<TString>{});
        }

        {
            const auto usf = makeUnsupportedFeatures({"usf1", "usf2", "usf3"});
            Json::Value cfg = Json::objectValue;
            cfg["unsupportedFeautres"] = usf.config;
            FeaturesConfig config(cfg);
            UNIT_ASSERT(config.getUnsupportedFeatures() == usf.expected);
        }
    }

    Y_UNIT_TEST(checkSetUnsupportedFeatures) {
        {
            const auto usf = makeUnsupportedFeatures({"usf1", "usf2", "usf3"});
            Json::Value cfg = Json::objectValue;
            cfg["unsupportedFeautres"] = usf.config;
            FeaturesConfig config(cfg);
            UNIT_ASSERT(config.getUnsupportedFeatures() == usf.expected);

            const auto usf_set = makeUnsupportedFeatures({"usf4", "usf5", "usf6"});
            config.setUnsupportedFeatures(usf_set.config);
            UNIT_ASSERT(config.getUnsupportedFeatures() == usf_set.expected);
        }
    }

    Y_UNIT_TEST(checkDictExperiments) {
        {
            FeaturesConfig config((Json::Value()));

            const auto quasmoExperiments = makeDictExperiments(
                {"exp1", "exp2", "exp3"},
                {
                    std::make_pair("exp4", "value4"),
                    std::make_pair("exp5", "value5"),
                    std::make_pair("exp3", "value3"),
                });
            config.processNewConfig(quasmoExperiments.config);

            const auto& result = config.getExperiments();
            UNIT_ASSERT(protoMapEqual(result.GetStorage(), quasmoExperiments.expected.GetStorage()));
        }
    }

    Y_UNIT_TEST(checkMergeConfig) {
        {
            FeaturesConfig srcConfig((Json::Value()));
            const auto srcExperiments = makeExperiments({"exp1", "exp2", "exp3"});
            srcConfig.setExperiments(srcExperiments.config);
            const auto srcSupported = makeSupportedFeatures({"sf1", "sf2", "sf3"});
            srcConfig.setSupportedFeatures(srcSupported.config);

            FeaturesConfig dstConfig((Json::Value()));
            const auto dstExperiments = makeExperiments({"exp3", "exp4", "exp5"}); // exp3 is duplicated
            dstConfig.setExperiments(dstExperiments.config);
            YIO_LOG_INFO(convertMessageToJsonString(dstConfig.getExperiments()))
            const auto dstSupported = makeSupportedFeatures({"sf5", "sf4", "sf3"}); // sf3 is duplicated
            dstConfig.setSupportedFeatures(dstSupported.config);

            dstConfig.merge(srcConfig);

            const auto expectedExperiments = makeExperiments({"exp1", "exp2", "exp3", "exp4", "exp5"});
            const auto expectedSupported = makeSupportedFeatures({"sf5", "sf4", "sf3", "sf1", "sf2"});

            YIO_LOG_INFO(convertMessageToJsonString(dstConfig.getExperiments()));
            YIO_LOG_INFO(convertMessageToJsonString(expectedExperiments.expected));
            UNIT_ASSERT(protoMapEqual(dstConfig.getExperiments().GetStorage(), expectedExperiments.expected.GetStorage()));
            UNIT_ASSERT(expectedSupported.expected == dstConfig.getSupportedFeatures());
        }
    }

}
