package consumer

import (
	"context"
	"encoding/json"
	"errors"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/mediator/pkg/commands"
	"a.yandex-team.ru/billing/hot/mediator/pkg/core"
	"a.yandex-team.ru/billing/hot/mediator/pkg/core/mocks"
	"a.yandex-team.ru/billing/hot/mediator/pkg/storage/logbroker"
	bmocks "a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
)

type testSuite struct {
	btesting.BaseSuite
	consumer *consumer
}

func (s *testSuite) SetupTest() {
	ctx := extracontext.NewWithParent(context.Background())
	cmd := commands.StorageCommand{}
	cmd.Config = &core.Config{}

	require.NoError(s.T(), cmd.Init(ctx))

	ctx, _ = cmd.CreateContext(ctx)

	reader := &logbroker.LogbrokerConsumer{
		Namespace: btesting.RandS(16),
		Endpoint:  btesting.RandS(16),
	}

	s.consumer = New(commands.CreateContextFrom(ctx), reader)
}

type messageHandlerSuite struct {
	testSuite
	data          []byte
	processor     *mocks.MockProcessor
	request       *core.ProcessorRequest
	response      *mocks.MockProcessorResponseProtocol
	errorProducer *bmocks.MockProducerProtocol
}

func (s *messageHandlerSuite) SetupTest() {
	s.testSuite.SetupTest()

	s.processor = mocks.NewMockProcessor(s.Ctrl())

	s.errorProducer = bmocks.NewMockProducerProtocol(s.Ctrl())
	s.consumer.reader.Handler.ErrorProducer = s.errorProducer

	s.data = []byte(`{"event": {"contract_id":1}}`)

	request, err := core.NewProcessorRequest(s.consumer.reader.Namespace, s.consumer.reader.Endpoint, s.data)
	require.NoError(s.T(), err)
	s.request = request

	s.response = mocks.NewMockProcessorResponseProtocol(s.Ctrl())
}

func (s *messageHandlerSuite) callHandler() error {
	messages := []persqueue.ReadMessage{{Data: s.data}}
	handler := s.consumer.createMessageHandler(s.processor)
	return handler(context.Background(), messages[0], persqueue.MessageBatch{Messages: messages})
}

func (s *messageHandlerSuite) TestSuccess() {
	gomock.InOrder(
		s.processor.
			EXPECT().
			Process(gomock.Any(), s.request).
			Return(s.response).
			Times(1),
		s.response.
			EXPECT().
			FatalError().
			Return(nil).
			Times(1),
		s.response.
			EXPECT().
			Error().
			Return(nil).
			Times(1),
	)

	err := s.callHandler()
	require.NoError(s.T(), err)
}

func (s *messageHandlerSuite) TestUnexpectedError() {
	fatalError := errors.New("fatal error")

	gomock.InOrder(
		s.processor.
			EXPECT().
			Process(gomock.Any(), s.request).
			Return(s.response).
			Times(1),
		s.response.
			EXPECT().
			FatalError().
			Return(fatalError).
			Times(1),
	)

	err := s.callHandler()
	assert.Equal(s.T(), err, fatalError)
}

func (s *messageHandlerSuite) TestExpectedError() {
	retryableError := core.ProcessorMessage{
		Type: core.ProcessorMessageTypeError,
		Payload: map[string]any{
			"request": s.request,
			"error":   errors.New("retryable error"),
		},
	}

	retryableErrorMsg, err := json.Marshal(retryableError)
	require.NoError(s.T(), err)

	gomock.InOrder(
		s.processor.
			EXPECT().
			Process(gomock.Any(), s.request).
			Return(s.response).
			Times(1),
		s.response.
			EXPECT().
			FatalError().
			Return(nil).
			Times(1),
		s.response.
			EXPECT().
			Error().
			Return(&retryableError).
			Times(1),
		s.errorProducer.
			EXPECT().
			Write(gomock.Any(), retryableErrorMsg).
			Return(nil).
			Times(1),
	)

	err = s.callHandler()
	require.NoError(s.T(), err)
}

func TestMessageHandler(t *testing.T) {
	suite.Run(t, &messageHandlerSuite{})
}
