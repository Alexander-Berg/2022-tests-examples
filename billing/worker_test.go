package actionworker

import (
	"context"
	"encoding/json"
	"strings"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/scheduler/mock"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/actions"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/actions/impl"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/actionworker/processors"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/queue"
	"a.yandex-team.ru/library/go/core/xerrors"
)

func baseMessage() entities.ActionMessage {
	return entities.ActionMessage{
		ObjectID:    "123",
		TaskID:      "12",
		TaskName:    "task",
		TaskStartTS: time.Now().Unix(),
		Action: entities.Action{
			Name:     "nop",
			Retries:  3,
			Timeout:  "1h",
			Deadline: time.Now().Add(time.Hour),
			Params:   nil,
		},
	}
}

func (s *WorkerTestSuite) marshalMessage(msg entities.ActionMessage) *string {
	res, err := json.Marshal(msg)
	s.Require().NoError(err)

	return aws.String(string(res))
}

func (s *WorkerTestSuite) TestProcessOK() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, nil, s.actions, nil)
	s.Require().NoError(err)

	msg := baseMessage()

	record := entities.QueueRecord{ObjectID: "123", TaskID: "12", TaskName: "task"}
	newStatusCall := s.storage.EXPECT().SetObjectHandleStatusNew(gomock.Any(), record).
		Return(entities.HandleStatusNew, 0, "", nil)
	s.storage.EXPECT().SetObjectHandleStatusDone(gomock.Any(), record).After(newStatusCall).Return(nil)

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: s.marshalMessage(msg),
		},
	})
	s.Require().NoError(err)
}

func (s *WorkerTestSuite) TestProcessFailedNewSet() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, nil, s.actions, nil)
	s.Require().NoError(err)

	msg := baseMessage()

	failErr := xerrors.New("fail new")
	record := entities.QueueRecord{ObjectID: "123", TaskID: "12", TaskName: "task"}
	s.storage.EXPECT().SetObjectHandleStatusNew(gomock.Any(), record).Return(entities.HandleStatus(""), 0, "", failErr)

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: s.marshalMessage(msg),
		},
	})
	s.Require().ErrorIs(err, failErr)
}

func (s *WorkerTestSuite) TestProcessAlreadyDone() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, nil, s.actions, nil)
	s.Require().NoError(err)

	msg := baseMessage()

	record := entities.QueueRecord{ObjectID: "123", TaskID: "12", TaskName: "task"}
	s.storage.EXPECT().SetObjectHandleStatusNew(gomock.Any(), record).Return(entities.HandleStatusDone, 0, "", nil)

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: s.marshalMessage(msg),
		},
	})
	s.Require().NoError(err)
}

func (s *WorkerTestSuite) TestProcessRetriesExceeded() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, nil, s.actions, nil)
	s.Require().NoError(err)

	msg := baseMessage()

	record := entities.QueueRecord{ObjectID: "123", TaskID: "12", TaskName: "task"}
	s.storage.EXPECT().SetObjectHandleStatusNew(gomock.Any(), record).Return(entities.HandleStatusNew, 10, "", nil)
	s.storage.EXPECT().SetObjectHandleStatusFailed(gomock.Any(), record).Return(nil)

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: s.marshalMessage(msg),
		},
	})
	s.Require().ErrorIs(err, ErrRetriesExceeded)
}

func (s *WorkerTestSuite) TestProcessFailedDone() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, nil, s.actions, nil)
	s.Require().NoError(err)

	msg := baseMessage()

	failErr := xerrors.New("fail done")
	record := entities.QueueRecord{ObjectID: "123", TaskID: "12", TaskName: "task"}
	newStatusCall := s.storage.EXPECT().SetObjectHandleStatusNew(gomock.Any(), record).
		Return(entities.HandleStatusNew, 0, "", nil)
	s.storage.EXPECT().SetObjectHandleStatusDone(gomock.Any(), record).After(newStatusCall).Return(failErr)

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: s.marshalMessage(msg),
		},
	})
	s.Require().ErrorIs(err, failErr)
}

func (s *WorkerTestSuite) TestProcessParseError() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, nil, s.actions, nil)
	s.Require().NoError(err)

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: aws.String("abc"),
		},
	})
	s.Require().Error(err)
	s.Assert().True(strings.HasPrefix(err.Error(), "error parsing task payload"))
}

func (s *WorkerTestSuite) TestProcessNoAction() {
	ctx := context.Background()
	task, err := newTaxiTask(ctx, nil, nil, s.actions, nil)
	s.Require().NoError(err)

	msg := baseMessage()
	msg.Action.Name = "invalid"

	err = task.Process(ctx, &queue.Message{
		SQSMessage: &sqs.Message{
			Body: s.marshalMessage(msg),
		},
	})
	s.Require().ErrorIs(err, processors.ErrMissingFactory)
}

func (s *WorkerTestSuite) setupTest() (*gomock.Controller, func()) {
	ctrl := gomock.NewController(s.T())

	return ctrl, func() {
		ctrl.Finish()
	}
}

type WorkerTestSuite struct {
	suite.Suite
	controller *gomock.Controller
	storage    *mock.MockStorage
	actions    actions.Actions
	cleanup    func()
}

func (s *WorkerTestSuite) SetupTest() {
	s.controller, s.cleanup = s.setupTest()

	s.storage = mock.NewMockStorage(s.controller)
	s.actions = impl.NewActions(s.storage)
}

func (s *WorkerTestSuite) TearDownTest() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func TestWorkerTestSuite(t *testing.T) {
	suite.Run(t, new(WorkerTestSuite))
}
