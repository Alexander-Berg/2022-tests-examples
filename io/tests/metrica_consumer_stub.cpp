#include "metrica_consumer_stub.h"

#include <yandex_io/libs/logging/logging.h>

using namespace quasar;

const std::string MetricaConsumerStub::CONSUMER_NAME = "stub";

MetricaConsumerStub::MetricaConsumerStub(const Json::Value& config, std::shared_ptr<YandexIO::IDevice> /*device*/)
{
    configVar = config["configVar"].asString();
    YIO_LOG_INFO("MetricaConsumerStub created");
}

MetricaConsumerStub::~MetricaConsumerStub() {
    YIO_LOG_INFO("MetricaConsumerStub destroyed");
}

void MetricaConsumerStub::processEvent(const std::string& event, const std::string& eventValue, bool /*skipDatabase*/) {
    YIO_LOG_DEBUG("Got processEvent: event=" << event << ", eventValue=" << eventValue);

    lastEvent = event;
    lastEventValue = eventValue;
    eventReceived.set_value();
}

void MetricaConsumerStub::processError(const std::string& error, const std::string& errorValue, bool /*skipDatabase*/) {
    YIO_LOG_DEBUG("Got processError: error=" << error << ", errorValue=" << errorValue);

    lastError = error;
    lastErrorValue = errorValue;
    errorReceived.set_value();
}

void MetricaConsumerStub::putEnvironmentVariable(const std::string& variableName, const std::string& variableValue) {
    YIO_LOG_DEBUG("Got putEnvironmentVariable: key=" << variableName << ", value=" << variableValue);

    lastEnvironmentVariable = variableName;
    lastEnvironmentVariableValue = variableValue;
    envVarPut.set_value();
}

void MetricaConsumerStub::deleteEnvironmentVariable(const std::string& variableName) {
    YIO_LOG_DEBUG("Got deleteEnvironmentVariable: key=" << variableName);

    lastDeletedEnvironmentVariable = variableName;
    envVarDeleted.set_value();
}

void MetricaConsumerStub::setConnectionType(proto::ConnectionType value) {
    connectionType = value;
    connectionStatus.set_value();
}

const std::string& MetricaConsumerStub::getName() const {
    return CONSUMER_NAME;
}

void MetricaConsumerStub::processConfigUpdate(const Json::Value& configUpdate, const Json::Value& /*fullConfig*/) {
    configVar = configUpdate["configVar"].asString();
    configReceived.set_value();
}

void MetricaConsumerStub::processStats(Json::Value /*payload*/) {
    statsReceived.set_value();
}

void MetricaConsumerStub::waitForPromise(std::promise<void>& promise) {
    promise.get_future().wait();
    promise = std::promise<void>();
}

void MetricaConsumerStub::waitForEvent() {
    waitForPromise(eventReceived);
}

void MetricaConsumerStub::waitForError() {
    waitForPromise(errorReceived);
}

void MetricaConsumerStub::waitForEnvVar() {
    waitForPromise(envVarPut);
}

void MetricaConsumerStub::waitForEnvVarDelete() {
    waitForPromise(envVarDeleted);
}

void MetricaConsumerStub::waitForConnectionStatus() {
    waitForPromise(connectionStatus);
}

void MetricaConsumerStub::waitForConfig() {
    waitForPromise(configReceived);
}

void MetricaConsumerStub::waitForStats() {
    waitForPromise(statsReceived);
}
