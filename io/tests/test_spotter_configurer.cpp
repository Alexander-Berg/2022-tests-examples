#include <yandex_io/capabilities/spotter/interfaces/mocks/mock_spotter_capability.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/spotter_types/spotter_types.h>
#include <yandex_io/modules/spotter_configurer/spotter_configurer.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_spotter.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace testing;
using namespace YandexIO;

class SDKMock: public YandexIO::NullSDKInterface {
public:
    MOCK_METHOD(std::shared_ptr<ISpotterCapability>, getActivationSpotterCapability, (), (const, override));
    MOCK_METHOD(std::shared_ptr<ISpotterCapability>, getCommandSpotterCapability, (), (const, override));
    MOCK_METHOD(std::shared_ptr<ISpotterCapability>, getNaviOldSpotterCapability, (), (const, override));
};

class SpotterConfigurerTestFixture: public QuasarUnitTestFixtureWithoutIpc {
protected:
    void SetUp(NUnitTest::TTestContext& context) override {
        QuasarUnitTestFixtureWithoutIpc::SetUp(context);

        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard_);

        pathTemp_ = JoinFsPaths(tryGetRamDrivePath(), "temp-" + makeUUID());
        pathTemp_.MkDirs();

        customSpotterDir_ = std::string(JoinFsPaths(pathTemp_, "data/quasar/data/spotter_model"));

        // Add test fixture specific settings.
        config["common"]["tempDir"] = pathTemp_.GetPath();
        config["aliced"]["spotterModelsPath"] = tryGetRamDrivePath();
        config["aliced"]["customSpotterConfigPath"] = std::string(JoinFsPaths(pathTemp_, "data/quasar/data/spotter.json"));
        config["aliced"]["customSpotterDir"] = customSpotterDir_;

        config["aliced"]["downloaderInitialRetryTimeoutMs"] = 50;
        config["aliced"]["downloaderMaxRetryTimeoutMs"] = 200;
        config["aliced"]["longListeningEnabled"] = true;

        initMocks();

        initDefaultConfig();

        updateExpectedSpotterPaths();

        auto worker = std::make_unique<NamedCallbackQueue>("TestSpotterConfigurer");
        rawWorker_ = worker.get();
        configurer_ = SpotterConfigurer::install(std::move(worker), sdkMock_, getDeviceForTests());
        rawWorker_->wait();
    }

    void TearDown(NUnitTest::TTestContext& context) override {
        pathTemp_.ForceDelete();

        for (const auto& pathToClean : pathsToClean_) {
            TFsPath(pathToClean).ForceDelete();
        }

        QuasarUnitTestFixtureWithoutIpc::TearDown(context);
    }

    void updateExpectedSpotterPaths() {
        activationSpotterPaths_.clear();
        commandSpotterPaths_.clear();
        naviOldSpotterPaths_.clear();

        auto fillSpotterPaths = [this](auto& spotterPaths, const std::set<std::string>& spotterTypes) {
            for (const auto& spotterType : spotterTypes) {
                auto spotterPath = JoinFsPaths(tryGetRamDrivePath(), spotterType);
                if (spotterType == SpotterTypes::ACTIVATION || spotterType == SpotterTypes::INTERRUPTION) {
                    spotterPath = JoinFsPaths(spotterPath, spotterWord_.empty() ? "alisa" : spotterWord_);
                }
                TFsPath(spotterPath).MkDirs();
                pathsToClean_.insert(spotterPath);
                spotterPaths.emplace(spotterType, spotterPath);
            }
        };
        fillSpotterPaths(activationSpotterPaths_, {SpotterTypes::ACTIVATION, SpotterTypes::ADDITIONAL, SpotterTypes::INTERRUPTION});
        fillSpotterPaths(commandSpotterPaths_, SpotterTypes::COMMAND_ALL);
        fillSpotterPaths(naviOldSpotterPaths_, {SpotterTypes::NAVIGATION_OLD});
    }

    void waitDownloadingTasksFinished() {
        waitUntil([this] {
            return configurer_->getSpotterDownloader().downloadingTasksCount() == 0;
        });
    }

    std::string prepareSystemConfig(const std::set<std::string>& spotterConfigNames) {
        Json::Value config;

        for (const auto& spotterConfigName : spotterConfigNames) {
            UNIT_ASSERT(customSpotterConfigs_.isMember(spotterConfigName));
            config[spotterConfigName] = customSpotterConfigs_[spotterConfigName];
        }

        return jsonToString(config);
    }

    void configureSpottersAndWait(const std::set<std::string>& spotterConfigNames) {
        configurer_->onSystemConfig("spotter", prepareSystemConfig(spotterConfigNames));
        // wait for task to fall into spotter downloader
        rawWorker_->wait();
        // wait for downloading
        waitDownloadingTasksFinished();
        // wait for callbacks about new spotter
        rawWorker_->wait();
    }

private:
    void initDefaultConfig() {
        Spotter spotter = createSpotter();

        customSpotterConfigs_["yandex"]["url"] = "http://localhost:" + std::to_string(mockSpotterStorage_.port()) + "/yandex.tar.gz";
        customSpotterConfigs_["yandex"]["crc"] = spotter.crc32;
        customSpotterConfigs_["alisa"]["url"] = "http://localhost:" + std::to_string(mockSpotterStorage_.port()) + "/alisa.tar.gz";
        customSpotterConfigs_["alisa"]["crc"] = spotter.crc32;
        customSpotterConfigs_["yandex_zip"]["url"] = "http://localhost:" + std::to_string(mockSpotterStorage_.port()) + "/yandex.zip";
        customSpotterConfigs_["yandex_zip"]["crc"] = spotter.crc32;
        customSpotterConfigs_["yandex_zip"]["type"] = SpotterTypes::ACTIVATION;
        customSpotterConfigs_["yandex_zip"]["word"] = "yandex";
        customSpotterConfigs_["yandex_zip"]["format"] = "zip";
        customSpotterConfigs_["yandex_bad"]["url"] = "http://localhost:" + std::to_string(mockSpotterStorage_.port()) + "/yandex_bad.tar.gz";
        customSpotterConfigs_["yandex_bad"]["crc"] = spotter.crc32;
        customSpotterConfigs_["yandex_bad"]["type"] = SpotterTypes::ACTIVATION;
        customSpotterConfigs_["yandex_bad"]["word"] = "yandex";
        customSpotterConfigs_["yandex_3ret"]["url"] = "http://localhost:" + std::to_string(mockSpotterStorage_.port()) + "/yandex_3ret.tar.gz";
        customSpotterConfigs_["yandex_3ret"]["crc"] = spotter.crc32;
        customSpotterConfigs_["yandex_3ret"]["type"] = SpotterTypes::ACTIVATION;
        customSpotterConfigs_["yandex_3ret"]["word"] = "yandex";
        customSpotterConfigs_["yandex_crc_fail"]["url"] = "http://localhost:" + std::to_string(mockSpotterStorage_.port()) + "/yandex.tar.gz";
        customSpotterConfigs_["yandex_crc_fail"]["crc"] = spotter.crc32 + 1;
        customSpotterConfigs_["yandex_crc_fail"]["type"] = SpotterTypes::ACTIVATION;
        customSpotterConfigs_["yandex_crc_fail"]["word"] = "yandex";
        customSpotterConfigs_["yandex_wrong_format"]["url"] = "http://localhost:" + std::to_string(mockSpotterStorage_.port()) + "/yandex.zip";
        customSpotterConfigs_["yandex_wrong_format"]["crc"] = spotter.crc32;
        customSpotterConfigs_["yandex_wrong_format"]["type"] = SpotterTypes::ACTIVATION;
        customSpotterConfigs_["yandex_wrong_format"]["word"] = "yandex";
        customSpotterConfigs_[SpotterTypes::NAVIGATION_OLD]["url"] = "http://localhost:" + std::to_string(mockSpotterStorage_.port()) + "/navi_old.tar.gz";
        customSpotterConfigs_[SpotterTypes::NAVIGATION_OLD]["crc"] = spotter.crc32;
        customSpotterConfigs_[SpotterTypes::NAVIGATION_OLD]["type"] = SpotterTypes::NAVIGATION_OLD;
        customSpotterConfigs_[SpotterTypes::NAVIGATION_OLD]["word"] = "";
    }

    void initMocks() {
        mockSpotterStorage_.onHandlePayload = [this](const TestHttpServer::Headers& header, const std::string /*payload*/,
                                                     TestHttpServer::HttpConnection& handler) {
            httpRequestsCount_++;
            Spotter spotter = createSpotter();
            if (header.resource == "/alisa.tar.gz" || header.resource == "/yandex.tar.gz" || header.resource == "/navi_old.tar.gz" || header.resource == "/general.tar.gz") {
                handler.doReplay(200, "application/x-tar", spotter.gzipData);
            } else if (header.resource == "/yandex.zip") {
                handler.doReplay(200, "application/zip", spotter.zipData);
            } else if (header.resource == "/yandex_bad.tar.gz") {
                handler.doError("fail");
            } else if (header.resource == "/yandex_3ret.tar.gz") {
                if (httpRequestsCount_ >= 3) {
                    handler.doReplay(200, "application/x-tar", spotter.gzipData);
                } else {
                    handler.doError("fail");
                }
            }
        };

        mockSpotterStorage_.start(getPort());

        activationSpotterCapability_ = std::make_shared<NiceMock<MockSpotterCapability>>();
        commandSpotterCapability_ = std::make_shared<NiceMock<MockSpotterCapability>>();
        naviOldSpotterCapability_ = std::make_shared<NiceMock<MockSpotterCapability>>();

        ON_CALL(sdkMock_, getActivationSpotterCapability).WillByDefault(Return(activationSpotterCapability_));
        ON_CALL(sdkMock_, getCommandSpotterCapability).WillByDefault(Return(commandSpotterCapability_));
        ON_CALL(sdkMock_, getNaviOldSpotterCapability).WillByDefault(Return(naviOldSpotterCapability_));
    }

protected:
    YandexIO::Configuration::TestGuard testGuard_;
    TestHttpServer mockSpotterStorage_;
    int httpRequestsCount_ = 0;
    Json::Value customSpotterConfigs_;
    TFsPath pathTemp_;
    std::string spotterWord_ = "alisa";
    std::string customSpotterDir_;

    NiceMock<SDKMock> sdkMock_;
    std::shared_ptr<MockSpotterCapability> activationSpotterCapability_;
    std::shared_ptr<MockSpotterCapability> commandSpotterCapability_;
    std::shared_ptr<MockSpotterCapability> naviOldSpotterCapability_;

    std::map<std::string, std::string> activationSpotterPaths_;
    std::map<std::string, std::string> commandSpotterPaths_;
    std::map<std::string, std::string> naviOldSpotterPaths_;

    std::set<std::string> pathsToClean_;

    ICallbackQueue* rawWorker_;
    std::shared_ptr<SpotterConfigurer> configurer_;
};

Y_UNIT_TEST_SUITE_F(SpotterConfigurerTest, SpotterConfigurerTestFixture) {
    Y_UNIT_TEST(Defaults) {
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths(activationSpotterPaths_)).Times(1);
        EXPECT_CALL(*commandSpotterCapability_, setModelPaths(commandSpotterPaths_)).Times(1);
        EXPECT_CALL(*naviOldSpotterCapability_, setModelPaths(naviOldSpotterPaths_)).Times(1);

        auto worker = std::make_unique<NamedCallbackQueue>("TestSpotterConfigurer");
        rawWorker_ = worker.get();
        auto configurer = SpotterConfigurer::install(std::move(worker), sdkMock_, getDeviceForTests());
        rawWorker_->wait();
    }

    Y_UNIT_TEST(ChangeWakeWordAndReset) {
        spotterWord_ = "yandex";
        updateExpectedSpotterPaths();

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths(activationSpotterPaths_)).Times(1);
        EXPECT_CALL(*commandSpotterCapability_, setModelPaths(commandSpotterPaths_)).Times(0);
        EXPECT_CALL(*naviOldSpotterCapability_, setModelPaths(naviOldSpotterPaths_)).Times(0);

        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();

        spotterWord_.clear();
        updateExpectedSpotterPaths();

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths(activationSpotterPaths_)).Times(1);
        EXPECT_CALL(*commandSpotterCapability_, setModelPaths(commandSpotterPaths_)).Times(0);
        EXPECT_CALL(*naviOldSpotterCapability_, setModelPaths(naviOldSpotterPaths_)).Times(0);

        configurer_->onAccountConfig("spotter", "null");
        rawWorker_->wait();
    }

    Y_UNIT_TEST(customActivationSpotterAndReset) {
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::ACTIVATION) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });

        configureSpottersAndWait({"alisa"});

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths(activationSpotterPaths_)).Times(1);

        configurer_->onSystemConfig("spotter", "null");
        rawWorker_->wait();
    }

    Y_UNIT_TEST(customActivationSpotterZip) {
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::ACTIVATION) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });

        configureSpottersAndWait({"yandex_zip"});
    }

    Y_UNIT_TEST(severalCustomSpottersAndResetOne) {
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::ACTIVATION) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });
        EXPECT_CALL(*naviOldSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::NAVIGATION_OLD) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });

        configureSpottersAndWait({"alisa", SpotterTypes::NAVIGATION_OLD});

        EXPECT_CALL(*naviOldSpotterCapability_, setModelPaths(naviOldSpotterPaths_)).Times(1);
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths).Times(0);
        configureSpottersAndWait({"alisa"});
    }

    Y_UNIT_TEST(downloadSpotterWhenSpotterWordIsChangedToCustomSpottersWord) {
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths).Times(0);
        configureSpottersAndWait({"yandex"});

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::ACTIVATION) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();
        waitDownloadingTasksFinished();
        rawWorker_->wait();
    }

    Y_UNIT_TEST(fallbackToDefaultSpotterWhenHasCustomSpotterAndSpotterWordChanges) {
        configureSpottersAndWait({"alisa"});

        spotterWord_ = "yandex";
        updateExpectedSpotterPaths();
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths(activationSpotterPaths_)).Times(1);
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();
    }

    Y_UNIT_TEST(downloadSpotterOnSecondConfigUpdate) {
        spotterWord_ = "yandex";
        updateExpectedSpotterPaths();
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::ACTIVATION) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });
        configureSpottersAndWait({"yandex"});

        {
            InSequence seq;

            // fallback to defaults before download
            EXPECT_CALL(*activationSpotterCapability_, setModelPaths(activationSpotterPaths_)).Times(1);
            // custom downloaded spotter
            EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
                .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                    for (const auto& [type, path] : spotterTypeToModelPath) {
                        if (type == SpotterTypes::ACTIVATION) {
                            EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                        }
                    }
                });
            configureSpottersAndWait({"yandex_zip"});
        }
    }

    Y_UNIT_TEST(ifDownloadFailsItRetriesUntilSuccess) {
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::ACTIVATION) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });
        configureSpottersAndWait({"yandex_3ret"});
        UNIT_ASSERT(httpRequestsCount_ >= 3);
    }

    Y_UNIT_TEST(successfulDownloadAfterBad) {
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths).Times(0);

        configurer_->onSystemConfig("spotter", prepareSystemConfig({"yandex_bad"}));
        waitUntil([&]() {
            return httpRequestsCount_ >= 3;
        });

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::ACTIVATION) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });
        configureSpottersAndWait({"yandex_zip"});
    }

    Y_UNIT_TEST(anotherWakeWordCancelsBadDownloading) {
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();
        configurer_->onSystemConfig("spotter", prepareSystemConfig({"yandex_bad"}));
        waitUntil([&]() {
            return httpRequestsCount_ >= 3;
        });

        configurer_->onAccountConfig("spotter", "\"alisa\"");
        waitDownloadingTasksFinished();
    }

    Y_UNIT_TEST(fallbackToDefaultOnActivationSpotterModelError) {
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths)
            .WillOnce([this](const std::map<std::string, std::string>& spotterTypeToModelPath) {
                for (const auto& [type, path] : spotterTypeToModelPath) {
                    if (type == SpotterTypes::ACTIVATION) {
                        EXPECT_THAT(path, HasSubstr(customSpotterDir_));
                    }
                }
            });
        configureSpottersAndWait({"alisa"});

        EXPECT_CALL(*activationSpotterCapability_, setModelPaths(activationSpotterPaths_)).Times(1);
        configurer_->onModelError(activationSpotterCapability_, {SpotterTypes::ACTIVATION});
        rawWorker_->wait();
    }

    Y_UNIT_TEST(noCustomSpotterOnCrcCheckFailed) {
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths).Times(0);
        configureSpottersAndWait({"yandex_crc_fail"});
    }

    Y_UNIT_TEST(noCustomSpotterOnWrongSpotterFormat) {
        configurer_->onAccountConfig("spotter", "\"yandex\"");
        rawWorker_->wait();
        EXPECT_CALL(*activationSpotterCapability_, setModelPaths).Times(0);
        configureSpottersAndWait({"yandex_wrong_format"});
    }
}
