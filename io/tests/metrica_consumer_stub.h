#pragma once

#include <yandex_io/libs/device/i_device.h>
#include <yandex_io/libs/metrica/base/metrica_consumer.h>
#include <yandex_io/protos/model_objects.pb.h>

#include <chrono>
#include <future>

namespace quasar {

    class MetricaConsumerStub: public IMetricaConsumer {
    public:
        static const std::string CONSUMER_NAME;

        MetricaConsumerStub(const Json::Value& config, std::shared_ptr<YandexIO::IDevice> device);
        ~MetricaConsumerStub();

        // IMetricaConsumer implementation
        void processEvent(const std::string& event, const std::string& eventValue, bool skipDatabase = false) override;
        void processError(const std::string& error, const std::string& errorValue, bool skipDatabase = false) override;

        void putEnvironmentVariable(const std::string& variableName, const std::string& variableValue) override;
        void deleteEnvironmentVariable(const std::string& variableName) override;

        void setConnectionType(proto::ConnectionType value) override;

        const std::string& getName() const override;
        void processConfigUpdate(const Json::Value& configUpdate, const Json::Value& fullConfig) override;
        void processStats(Json::Value payload) override;

        void waitForEvent();
        void waitForError();
        void waitForEnvVar();
        void waitForEnvVarDelete();
        void waitForConnectionStatus();
        void waitForConfig();
        void waitForStats();

        std::string lastEvent;
        std::string lastEventValue;

        std::string lastError;
        std::string lastErrorValue;

        std::string lastEnvironmentVariable;
        std::string lastEnvironmentVariableValue;
        std::string lastDeletedEnvironmentVariable;

        proto::ConnectionType connectionType{proto::CONNECTION_TYPE_UNKNOWN};

        std::string configVar;

    private:
        static void waitForPromise(std::promise<void>& promise);

        std::promise<void> eventReceived;
        std::promise<void> errorReceived;
        std::promise<void> envVarPut;
        std::promise<void> envVarDeleted;
        std::promise<void> connectionStatus;
        std::promise<void> configReceived;
        std::promise<void> statsReceived;
    };

} // namespace quasar
