#pragma once

#include <yandex_io/services/aliced/directive_processor/interface/i_directive_preprocessor.h>
#include <yandex_io/modules/testpoint/testpoint_peer.h>
#include <yandex_io/libs/ipc/i_connector.h>

namespace YandexIO {

    class TestpointDirectivePreprocessor: public IDirectivePreprocessor {
    public:
        explicit TestpointDirectivePreprocessor(std::shared_ptr<TestpointPeer> testpointPeer);

        const std::string& getPreprocessorName() const override;
        void preprocessDirectives(std::list<std::shared_ptr<Directive>>& directives) override;

    private:
        const std::shared_ptr<TestpointPeer> testpointPeer_;
    };

} // namespace YandexIO
