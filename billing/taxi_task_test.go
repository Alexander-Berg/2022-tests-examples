package actionworker

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/payout"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/processor"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/queue"
)

func (s *TaxiTaskTestSuite) TestActionOK() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, s.clients, s.actions, nil)
	s.Require().NoError(err)
	msg := baseMessage()
	msg.Action.Name = "payout"
	msg.Action.Params = map[string]any{"namespace": "taxi"}

	record := entities.QueueRecord{ObjectID: "123", TaskID: "12", TaskName: "task"}
	newStatusCall := s.storage.EXPECT().SetObjectHandleStatusNew(gomock.Any(), record).
		Return(entities.HandleStatusNew, 0, "", nil)
	payoutCall := s.processorAPI.EXPECT().Payout(gomock.Any(), processor.PayoutRequest{
		ClientID:   123,
		ExternalID: "taxi/task/12/123",
		EventTime:  time.Unix(msg.TaskStartTS, 0),
		Namespace:  "taxi",
	}).After(newStatusCall).Return(nil)

	byClientCall := s.payoutAPI.EXPECT().PayoutByClient(gomock.Any(), payout.ByClientRequest{
		ClientID:   123,
		ExternalID: "taxi/task/12/123",
		Namespace:  "taxi",
	}).After(payoutCall).Return(nil)
	s.storage.EXPECT().SetObjectHandleStatusDone(gomock.Any(), record).After(byClientCall).Return(nil)

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: s.marshalMessage(msg),
		},
	})
	s.Require().NoError(err)
}

func (s *TaxiTaskTestSuite) TestActionFail() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, s.clients, s.actions, nil)
	s.Require().NoError(err)

	msg := baseMessage()
	msg.Action.Name = "payout"
	msg.Action.Params = map[string]any{"namespace": "taxi"}

	record := entities.QueueRecord{ObjectID: "123", TaskID: "12", TaskName: "task"}
	newStatusCall := s.storage.EXPECT().SetObjectHandleStatusNew(gomock.Any(), record).
		Return(entities.HandleStatusNew, 0, "", nil)

	expectedErr := errors.New("some error")
	payoutCall := s.processorAPI.EXPECT().Payout(gomock.Any(), processor.PayoutRequest{
		ClientID:   123,
		ExternalID: "taxi/task/12/123",
		EventTime:  time.Unix(msg.TaskStartTS, 0),
		Namespace:  "taxi",
	}).After(newStatusCall).Return(expectedErr)
	s.storage.EXPECT().IncreaseRetries(gomock.Any(), record).After(payoutCall).Return(int64(1), nil)

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: s.marshalMessage(msg),
		},
	})
	s.Require().ErrorIs(err, expectedErr)
}

type TaxiTaskTestSuite struct {
	WorkerTestSuite
	payoutAPI    *mock.MockPayoutAPI
	processorAPI *mock.MockProcessorAPI
	clients      *interactions.Clients
}

func (s *TaxiTaskTestSuite) SetupTest() {
	s.WorkerTestSuite.SetupTest()
	s.payoutAPI = mock.NewMockPayoutAPI(s.controller)
	s.processorAPI = mock.NewMockProcessorAPI(s.controller)
	s.clients = &interactions.Clients{
		Payout:    s.payoutAPI,
		Processor: s.processorAPI,
	}
}

func TestTaxiTaskTestSuite(t *testing.T) {
	suite.Run(t, new(TaxiTaskTestSuite))
}
