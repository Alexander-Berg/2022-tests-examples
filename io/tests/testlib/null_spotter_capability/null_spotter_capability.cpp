#include "null_spotter_capability.h"

using namespace YandexIO;

void NullSpotterCapability::setModelPaths(const std::map<std::string, std::string>& /*spotterTypeToModelPath*/) {
}

void NullSpotterCapability::setSpotterWord(const std::string& /*spotterWord*/) {
}

void NullSpotterCapability::addListener(std::weak_ptr<IListener> /*listener*/) {
}

void NullSpotterCapability::removeListener(std::weak_ptr<IListener> /*listener*/) {
}
