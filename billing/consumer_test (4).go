package logbroker

import (
	"context"
	"sync/atomic"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"
	"golang.org/x/sync/errgroup"

	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
)

func (s *ConsumerTestSuite) TestHandleAllMessages() {
	testCases := []struct {
		name       string
		numWorkers int
	}{
		{
			name:       "zero workers - no limit",
			numWorkers: 0,
		},
		{
			name:       "one worker",
			numWorkers: 1,
		},
		{
			name:       "5 workers",
			numWorkers: 5,
		},
		{
			name:       "too many workers",
			numWorkers: 100500,
		},
	}

	for _, testCase := range testCases {
		t := testCase
		s.Run(t.name, func() {
			consumer := Consumer{
				config: ConsumerConfig{
					Workers: t.numWorkers,
				},
			}

			var processed int32
			msgBatcher := messageBatcher{b: []persqueue.MessageBatch{
				{
					Messages: make([]persqueue.ReadMessage, 4),
				},
				{
					Messages: make([]persqueue.ReadMessage, 7),
				},
				{
					Messages: nil, // Skip such batches.
				},
				{
					Messages: make([]persqueue.ReadMessage, 1),
				},
			}}

			g, ctx := errgroup.WithContext(context.Background())
			consumer.runBatchHandler(g, ctx, msgBatcher,
				func(ctx context.Context, message persqueue.ReadMessage, batch persqueue.MessageBatch) error {
					atomic.AddInt32(&processed, 1)
					return nil
				},
			)

			s.Require().NoError(g.Wait())
			s.Assert().EqualValues(3, processed)

			processed = 0

			g, ctx = errgroup.WithContext(context.Background())
			consumer.runMessageHandler(g, ctx, msgBatcher,
				func(ctx context.Context, message persqueue.ReadMessage, batch persqueue.MessageBatch) error {
					atomic.AddInt32(&processed, 1)
					return nil
				},
			)

			s.Require().NoError(g.Wait())
			s.Assert().EqualValues(12, processed)
		})
	}
}

var _ batcher = messageBatcher{}

type messageBatcher struct {
	b []persqueue.MessageBatch
}

func (m messageBatcher) Batches() []persqueue.MessageBatch {
	return m.b
}

func (s *ConsumerTestSuite) setupContext() (*gomock.Controller, func(), error) {
	ctrl := gomock.NewController(s.T())

	return ctrl, func() {
		ctrl.Finish()
	}, nil
}

type ConsumerTestSuite struct {
	suite.Suite
	controller *gomock.Controller
	cleanup    func()
}

func (s *ConsumerTestSuite) SetupTest() {
	var err error

	s.controller, s.cleanup, err = s.setupContext()
	s.Require().NoError(err)
}

func (s *ConsumerTestSuite) TearDownTest() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func TestConsumerTestSuite(t *testing.T) {
	suite.Run(t, new(ConsumerTestSuite))
}
