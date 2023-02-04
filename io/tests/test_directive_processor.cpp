#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_preprocessor.h>
#include <yandex_io/services/aliced/directive_processor/mocks/mock_i_directive_processor_listener.h>

#include <yandex_io/services/aliced/directive_processor/directive_processor.h>
#include <yandex_io/services/aliced/directive_processor/interface/i_directive_preprocessor.h>

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/sdk/interfaces/directive.h>
#include <yandex_io/sdk/interfaces/mocks/mock_directive.h>
#include <yandex_io/sdk/interfaces/mocks/mock_i_directive_handler.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    class DirectiveStub: public YandexIO::Directive {
    public:
        DirectiveStub(std::optional<proto::AudioChannel> channel,
                      const std::string& name,
                      const std::string& type = "custom-type")
            : Directive(Data(name, type, channel))
        {
        }

        bool isRequest() const override {
            return isRequest_;
        }

        void setIsRequest(bool value) {
            isRequest_ = value;
        }

    private:
        bool isRequest_ = false;
    };

    std::shared_ptr<DirectiveStub> makeDialog(const std::string& name, bool isRequest = false) {
        auto directive = std::make_shared<DirectiveStub>(proto::DIALOG_CHANNEL, name, "dialog");
        directive->setIsRequest(isRequest);
        return directive;
    }

    std::shared_ptr<DirectiveStub> makeContent(const std::string& name) {
        return std::make_shared<DirectiveStub>(proto::CONTENT_CHANNEL, name, "content");
    }

    std::shared_ptr<DirectiveStub> makeCommand(const std::string& name) {
        return std::make_shared<DirectiveStub>(std::nullopt, name, "external_command");
    }

    std::shared_ptr<DirectiveStub> makeRequest(const std::string& name) {
        return std::make_shared<DirectiveStub>(std::nullopt, name, "server_action");
    }

    std::shared_ptr<Directive> makeAudioPlayDirective(const std::string& streamId) {
        Json::Value json;
        json["name"] = "audio_play";
        json["type"] = "client_action";
        json["payload"]["stream"]["id"] = streamId;
        YandexIO::Directive::Data data = YandexIO::Directive::Data::fromJson(json);
        data.channel = proto::CONTENT_CHANNEL;
        return std::make_shared<YandexIO::Directive>(std::move(data));
    }

    std::shared_ptr<Directive> makeServerActionDirective() {
        YandexIO::Directive::Data data("alice_request", "server_action");
        return std::make_shared<YandexIO::Directive>(std::move(data));
    }

    YandexIO::DirectiveProcessor::State createSequencerState(
        YandexIO::DirectiveProcessor::Channel dialog = YandexIO::DirectiveProcessor::Channel(),
        YandexIO::DirectiveProcessor::Channel content = YandexIO::DirectiveProcessor::Channel())
    {
        return {std::move(content), std::move(dialog)};
    }

} // unnamed namespace

Y_UNIT_TEST_SUITE(DirectiveProcessorTest) {
    Y_UNIT_TEST(testIdleState) {
        DirectiveProcessor sequencer;

        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            DirectiveProcessor::Channel(),
                            DirectiveProcessor::Channel())));
    }

    Y_UNIT_TEST(testAddDialogDirective) {
        DirectiveProcessor sequencer;

        auto dialogDirective = makeDialog("test_dialog_name", true);
        sequencer.addDirectives({dialogDirective});

        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            {dialogDirective},
                            DirectiveProcessor::Channel())));
    }

    Y_UNIT_TEST(testAddContentDirective) {
        DirectiveProcessor sequencer;

        auto contentDirective = makeContent("test_content_name");
        sequencer.addDirectives({contentDirective});

        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            DirectiveProcessor::Channel(),
                            {contentDirective})));
    }

    Y_UNIT_TEST(testAddCommandDirective) {
        DirectiveProcessor sequencer;

        auto commandDirective = makeCommand("test_command_name");
        sequencer.addDirectives({commandDirective});

        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            DirectiveProcessor::Channel(),
                            DirectiveProcessor::Channel())));
    }

    Y_UNIT_TEST(testAddDirectivesDisplacesDirectivesByAudioChannel) {
        DirectiveProcessor sequencer;

        auto contentDirective1 = makeContent("test_content_name1");
        auto requestDirective1 = makeRequest("test_request_name1");
        sequencer.addDirectives({contentDirective1, requestDirective1});
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            DirectiveProcessor::Channel(),
                            {contentDirective1, requestDirective1})));

        auto contentDirective2 = makeContent("test_content_name2");
        auto requestDirective2 = makeRequest("test_request_name2");
        sequencer.addDirectives({contentDirective2, requestDirective2});
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            DirectiveProcessor::Channel(),
                            {contentDirective2, requestDirective2})));
    }

    Y_UNIT_TEST(testClearQueueDirective) {
        DirectiveProcessor sequencer;

        auto dialogDirective = makeDialog("test_dialog_name", true);
        auto contentDirective = makeContent("test_content_name");
        auto requestDirective = makeRequest("test_request_name");
        sequencer.addDirectives({contentDirective, requestDirective});
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            DirectiveProcessor::Channel(),
                            {contentDirective, requestDirective})));

        sequencer.addDirectives({dialogDirective});
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            {dialogDirective},
                            {contentDirective, requestDirective})));

        auto clearQueueDirective = std::make_shared<Directive>(Directive::Data(Directives::CLEAR_QUEUE, "client_action"));
        sequencer.addDirectives({clearQueueDirective});
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            DirectiveProcessor::Channel(),
                            DirectiveProcessor::Channel())));
    }

    Y_UNIT_TEST(testAudioStopDirective) {
        DirectiveProcessor sequencer;

        auto dialogDirective = makeDialog("test_dialog_name", true);
        auto contentDirective = makeContent("test_content_name");
        auto requestDirective = makeRequest("test_request_name");
        sequencer.addDirectives({contentDirective, requestDirective});
        sequencer.addDirectives({dialogDirective});
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            {dialogDirective},
                            {contentDirective, requestDirective})));

        auto audioStopDirective = std::make_shared<Directive>(Directive::Data(Directives::AUDIO_STOP, "client_action"));
        sequencer.addDirectives({audioStopDirective});
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            {dialogDirective},
                            DirectiveProcessor::Channel())));
    }

    Y_UNIT_TEST(testDialogDirectiveCompletion) {
        DirectiveProcessor sequencer;

        auto dialogDirective = makeDialog("test_dialog_name", true);
        auto commandDirective = makeCommand("test_command_name");
        sequencer.addDirectives({dialogDirective, commandDirective});
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            {dialogDirective, commandDirective},
                            DirectiveProcessor::Channel())));

        sequencer.onHandleDirectiveCompleted(dialogDirective, true);
        EXPECT_THAT(sequencer.getState(),
                    ContainerEq(
                        createSequencerState(
                            DirectiveProcessor::Channel(),
                            DirectiveProcessor::Channel())));
    }

    Y_UNIT_TEST(testLastServerActionNotPrefetched_ifPrecedingDirectiveBlocksPrefetch) {
        DirectiveProcessor sequencer;
        sequencer.setPrefetchEnabled(true);

        auto requestDirective = makeRequest("request_test_name");
        auto audioPlayDirective = makeAudioPlayDirective("streamId1");
        audioPlayDirective->setFirstInChain();
        audioPlayDirective->setBlocksSubsequentPrefetch(true);

        const std::string endpointId;
        const std::string handleName = "handlerName";
        std::set<std::string> directiveNames = {"alice_request"};
        auto serverActionHandler = std::make_shared<MockIDirectiveHandler>();
        EXPECT_CALL(*serverActionHandler, getEndpointId()).WillRepeatedly(ReturnRef(endpointId));
        EXPECT_CALL(*serverActionHandler, getHandlerName()).WillRepeatedly(ReturnRef(handleName));
        EXPECT_CALL(*serverActionHandler, getSupportedDirectiveNames()).WillRepeatedly(ReturnRef(directiveNames));

        EXPECT_CALL(*serverActionHandler, prefetchDirective(_)).Times(0);

        ASSERT_TRUE(sequencer.addDirectiveHandler(serverActionHandler));
        sequencer.onHandleDirectiveCompleted(requestDirective, {audioPlayDirective, makeServerActionDirective()});
    }

    Y_UNIT_TEST(testLastServerActionPrefetched_AfterOnBlockPrefetchChanged) {
        DirectiveProcessor sequencer;
        sequencer.setPrefetchEnabled(true);

        auto requestDirective = makeRequest("request_test_name");
        auto audioPlayDirective = makeAudioPlayDirective("streamId1");
        audioPlayDirective->setFirstInChain();
        audioPlayDirective->setBlocksSubsequentPrefetch(true);
        auto serverActionDirective = makeServerActionDirective();

        const std::string endpointId;
        const std::string handleName = "handlerName";
        std::set<std::string> directiveNames = {"alice_request"};
        auto serverActionHandler = std::make_shared<MockIDirectiveHandler>();
        EXPECT_CALL(*serverActionHandler, getEndpointId()).WillRepeatedly(ReturnRef(endpointId));
        EXPECT_CALL(*serverActionHandler, getHandlerName()).WillRepeatedly(ReturnRef(handleName));
        EXPECT_CALL(*serverActionHandler, getSupportedDirectiveNames()).WillRepeatedly(ReturnRef(directiveNames));
        EXPECT_CALL(*serverActionHandler, prefetchDirective(serverActionDirective)).Times(1);
        ASSERT_TRUE(sequencer.addDirectiveHandler(serverActionHandler));

        sequencer.onHandleDirectiveCompleted(requestDirective, {audioPlayDirective, serverActionDirective});
        audioPlayDirective->setBlocksSubsequentPrefetch(false);
        sequencer.onBlockPrefetchChanged(audioPlayDirective);
    }

    Y_UNIT_TEST(testAddGetRemovePreprocessors) {
        DirectiveProcessor sequencer;

        ASSERT_TRUE(sequencer.getDirectivePreprocessors().empty());
        ASSERT_FALSE(sequencer.addDirectivePreprocessor(nullptr));
        ASSERT_TRUE(sequencer.getDirectivePreprocessors().empty());

        auto preprocessor1 = std::make_shared<MockIDirectivePreprocessor>();
        ASSERT_TRUE(sequencer.addDirectivePreprocessor(preprocessor1));
        ASSERT_TRUE(sequencer.getDirectivePreprocessors().size() == 1);
        ASSERT_TRUE(sequencer.getDirectivePreprocessors()[0] == preprocessor1);
        ASSERT_FALSE(sequencer.addDirectivePreprocessor(preprocessor1));
        ASSERT_TRUE(sequencer.getDirectivePreprocessors().size() == 1);
        ASSERT_TRUE(sequencer.getDirectivePreprocessors()[0] == preprocessor1);

        auto preprocessor2 = std::make_shared<MockIDirectivePreprocessor>();
        ASSERT_TRUE(sequencer.addDirectivePreprocessor(preprocessor2));
        ASSERT_TRUE(sequencer.getDirectivePreprocessors().size() == 2);
        ASSERT_TRUE(sequencer.getDirectivePreprocessors()[0] == preprocessor1);
        ASSERT_TRUE(sequencer.getDirectivePreprocessors()[1] == preprocessor2);

        auto preprocessor3 = std::make_shared<MockIDirectivePreprocessor>();
        ASSERT_FALSE(sequencer.removeDirectivePreprocessor(preprocessor3));
        ASSERT_TRUE(sequencer.getDirectivePreprocessors().size() == 2);
        ASSERT_TRUE(sequencer.getDirectivePreprocessors()[0] == preprocessor1);
        ASSERT_TRUE(sequencer.getDirectivePreprocessors()[1] == preprocessor2);
        ASSERT_TRUE(sequencer.removeDirectivePreprocessor(preprocessor1));
        ASSERT_TRUE(sequencer.getDirectivePreprocessors().size() == 1);
        ASSERT_TRUE(sequencer.getDirectivePreprocessors()[0] == preprocessor2);
    }

    Y_UNIT_TEST(testAddDirectivesPreprocessDirectives) {
        std::list<std::shared_ptr<Directive>> directives;
        auto directive1 = std::make_shared<MockDirective>();
        auto directive2 = std::make_shared<MockDirective>();
        directives.push_back(directive1);
        directives.push_back(directive2);

        DirectiveProcessor directiveProcessor;
        auto preprocessor1 = std::make_shared<MockIDirectivePreprocessor>();
        auto preprocessor2 = std::make_shared<MockIDirectivePreprocessor>();

        directiveProcessor.addDirectivePreprocessor(preprocessor1);
        directiveProcessor.addDirectivePreprocessor(preprocessor2);

        {
            InSequence sequence;

            // check directives are passed through preprocessors one by one
            EXPECT_CALL(*preprocessor1, preprocessDirectives(ContainerEq(directives))).WillOnce(InvokeWithoutArgs([&directives] { directives.erase(directives.begin()); }));
            EXPECT_CALL(*preprocessor2, preprocessDirectives(ContainerEq(directives))).WillOnce(InvokeWithoutArgs([&directives, directive1] { directives.push_back(directive1); }));
        }

        directiveProcessor.addDirectives(directives);
    }

    Y_UNIT_TEST(testHandleDirectiveCompletedPreprocessDirectives) {
        std::list<std::shared_ptr<Directive>> directives;
        auto directive1 = std::make_shared<MockDirective>();
        auto directive2 = std::make_shared<MockDirective>();
        directives.push_back(directive1);
        directives.push_back(directive2);

        DirectiveProcessor directiveProcessor;
        auto preprocessor1 = std::make_shared<MockIDirectivePreprocessor>();
        auto preprocessor2 = std::make_shared<MockIDirectivePreprocessor>();

        directiveProcessor.addDirectivePreprocessor(preprocessor1);
        directiveProcessor.addDirectivePreprocessor(preprocessor2);

        {
            InSequence sequence;

            // check directives are passed through preprocessors one by one
            EXPECT_CALL(*preprocessor1, preprocessDirectives(ContainerEq(directives))).WillOnce(InvokeWithoutArgs([&directives] { directives.erase(directives.begin()); }));
            EXPECT_CALL(*preprocessor2, preprocessDirectives(ContainerEq(directives))).WillOnce(InvokeWithoutArgs([&directives, directive1] { directives.push_back(directive1); }));
        }

        directiveProcessor.onHandleDirectiveCompleted(std::make_shared<MockDirective>(), directives);
    }

    Y_UNIT_TEST(testPrefetchDirectiveCompletedPreprocessDirectives) {
        DirectiveProcessor directiveProcessor;

        auto preprocessor = std::make_shared<MockIDirectivePreprocessor>();
        const std::string name = "name";
        EXPECT_CALL(*preprocessor, getPreprocessorName()).WillRepeatedly(ReturnRef(name));
        directiveProcessor.addDirectivePreprocessor(preprocessor);

        auto directive = std::make_shared<MockDirective>();
        {
            InSequence sequence;

            EXPECT_CALL(*preprocessor, preprocessDirectives(_)).WillOnce(Invoke([](std::list<std::shared_ptr<Directive>>& directives) {
                directives.push_back(makeCommand("test_command_name"));
            }));
            EXPECT_CALL(*directive, setPrefetchResult(_)).WillOnce(Invoke([&](std::list<std::shared_ptr<Directive>> directives) {
                UNIT_ASSERT(directives.size() == 1);
                UNIT_ASSERT((*directives.begin())->is("test_command_name"));
            }));
        }

        directiveProcessor.onPrefetchDirectiveCompleted(directive, {});
    }

    Y_UNIT_TEST(testHandlerDirectivesUnique) {
        DirectiveProcessor directiveProcessor;
        auto handler1 = std::make_shared<MockIDirectiveHandler>();
        auto handler2 = std::make_shared<MockIDirectiveHandler>();

        const std::string endpointId = "endpointId";
        const std::string name1 = "handlerName1";
        std::set<std::string> handlerNames1 = {"directive1", "directive2"};
        EXPECT_CALL(*handler1, getEndpointId()).WillRepeatedly(ReturnRef(endpointId));
        EXPECT_CALL(*handler1, getHandlerName()).WillRepeatedly(ReturnRef(name1));
        EXPECT_CALL(*handler1, getSupportedDirectiveNames()).WillRepeatedly(ReturnRef(handlerNames1));

        const std::string name2 = "handlerName2";
        std::set<std::string> handlerNames2 = {"directive3", "directive1"};
        EXPECT_CALL(*handler2, getEndpointId()).WillRepeatedly(ReturnRef(endpointId));
        EXPECT_CALL(*handler2, getHandlerName()).WillRepeatedly(ReturnRef(name2));
        EXPECT_CALL(*handler2, getSupportedDirectiveNames()).WillRepeatedly(ReturnRef(handlerNames2));

        ASSERT_TRUE(directiveProcessor.addDirectiveHandler(handler1));
        ASSERT_FALSE(directiveProcessor.addDirectiveHandler(handler2));
    }

    Y_UNIT_TEST(testPrefetchOnContentDirectiveOnce) {
        DirectiveProcessor directiveProcessor;
        directiveProcessor.setPrefetchEnabled(true);

        Directive::Data ttsData("tts_play_placeholder", "client_action", std::make_optional(quasar::proto::DIALOG_CHANNEL));
        const auto ttsDirective = std::make_shared<Directive>(ttsData);

        Directive::Data audioPlayData("audio_play", "client_action", std::make_optional(quasar::proto::CONTENT_CHANNEL));
        const auto audioPlayDirective1 = std::make_shared<Directive>(audioPlayData);
        const auto audioPlayDirective2 = std::make_shared<Directive>(audioPlayData);

        const std::string endpointId;
        const std::string name = "handlerName";
        const auto handler = std::make_shared<MockIDirectiveHandler>();
        const std::set<std::string> handlerNames = {"tts_play_placeholder", "audio_play"};
        ON_CALL(*handler, getEndpointId()).WillByDefault(ReturnRef(endpointId));
        ON_CALL(*handler, getHandlerName()).WillByDefault(ReturnRef(name));
        ON_CALL(*handler, getSupportedDirectiveNames()).WillByDefault(ReturnRef(handlerNames));

        ASSERT_TRUE(directiveProcessor.addDirectiveHandler(handler));

        {
            InSequence sequence;
            EXPECT_CALL(*handler, handleDirective(ttsDirective));
            EXPECT_CALL(*handler, prefetchDirective(audioPlayDirective1));
            EXPECT_CALL(*handler, handleDirective(audioPlayDirective1));
            EXPECT_CALL(*handler, prefetchDirective(audioPlayDirective2));
            EXPECT_CALL(*handler, handleDirective(audioPlayDirective2));
        }
        directiveProcessor.addDirectives({ttsDirective, audioPlayDirective1, audioPlayDirective2});
        directiveProcessor.onHandleDirectiveCompleted(ttsDirective, true);
        directiveProcessor.onHandleDirectiveCompleted(audioPlayDirective1, true);
    }

}
