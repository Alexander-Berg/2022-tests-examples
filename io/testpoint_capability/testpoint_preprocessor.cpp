#include "testpoint_preprocessor.h"

#include <yandex_io/sdk/interfaces/directive.h>
#include <yandex_io/protos/functional_tests.pb.h>
#include <yandex_io/libs/logging/logging.h>

using namespace YandexIO;

TestpointDirectivePreprocessor::TestpointDirectivePreprocessor(std::shared_ptr<TestpointPeer> testpointPeer)
    : testpointPeer_(std::move(testpointPeer))
{
}

const std::string& TestpointDirectivePreprocessor::getPreprocessorName() const {
    static const std::string name = "Testpoint preprocessor";
    return name;
}
void TestpointDirectivePreprocessor::preprocessDirectives(std::list<std::shared_ptr<Directive>>& directives) {
    quasar::proto::TestpointMessage testpointMessage;
    for (const auto& directive : directives) {
        testpointMessage.add_incoming_directive()->CopyFrom(YandexIO::Directive::convertToDirectiveProtobuf(directive));
    }
    testpointPeer_->sendMessage(std::move(testpointMessage));
}
