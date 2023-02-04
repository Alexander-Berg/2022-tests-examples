package impl

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	sqs2 "github.com/aws/aws-sdk-go/service/sqs"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/queue"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/task"
)

func defaultTaskTrigger() *entities.TaskTrigger {
	return &entities.TaskTrigger{
		TaskName:    "name",
		TaskID:      "123",
		TaskStartTS: time.Now().Unix(),
		QueueName:   "queue",
		Filters:     nil,
		Action:      nil,
	}
}

func (s *TaskActionTestSuite) TestTaskPipeline() {
	ctx := context.Background()
	trigger := defaultTaskTrigger()

	descr, err := s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err)

	s.Require().NoError(s.actions.ProcessTask(ctx, nil, descr, 10, nil))

	_, err = s.actions.InsertTask(ctx, trigger, "1")
	s.Require().ErrorIs(err, ErrTaskAlreadyDone)
}

func (s *TaskActionTestSuite) TestInsertTaskManyTimes() {
	ctx := context.Background()
	trigger := defaultTaskTrigger()
	trigger.TaskName = "name1"
	trigger.TaskID = "124"

	descr1, err := s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err)

	descr2, err := s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err, "second insert must also proceed (sqs guarantees exclusive work with message)")

	s.Assert().Equal(descr2.PreviousTaskTime, descr1.PreviousTaskTime,
		"with no previous tasks they must be equal")
}

func (s *TaskActionTestSuite) TestInsertTaskFromDifferentMessages() {
	ctx := context.Background()
	trigger := defaultTaskTrigger()
	trigger.TaskName = "name333"
	trigger.TaskID = "333"

	_, err := s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err)

	_, err = s.actions.InsertTask(ctx, trigger, "2")
	s.Require().ErrorIs(err, ErrDuplicateTask, "second insert is a duplicate message for task")
}

func (s *TaskActionTestSuite) TestInsertAfterDone() {
	ctx := context.Background()
	trigger := defaultTaskTrigger()
	trigger.TaskName = "name322"
	trigger.TaskID = "322"

	_, err := s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err)

	_, err = s.store.GetDatabase(ctx, pg.Master).
		ExecContext(ctx, "UPDATE sched.t_task SET status = 'done' WHERE task_id = '322'")
	s.Require().NoError(err)

	trigger.TaskID = "323"
	_, err = s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err, "second insert must proceed after done task")
}

func (s *TaskActionTestSuite) TestTaskIsAlreadyRunning() {
	ctx := context.Background()
	trigger := defaultTaskTrigger()
	trigger.TaskName = "name2"
	trigger.TaskID = "125"

	_, err := s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err)

	trigger.TaskID = "126"

	_, err = s.actions.InsertTask(ctx, trigger, "1")
	s.Require().ErrorIs(err, ErrAnotherTaskInProcess)
}

func (s *TaskActionTestSuite) TestNoObjectsTaskIsSetDone() {
	ctx := context.Background()
	trigger := defaultTaskTrigger()
	trigger.TaskName = "name13"
	trigger.TaskID = "131"

	descr, err := s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err)

	// Must complete task since there are no objects
	s.Require().NoError(s.actions.ProcessTask(ctx, nil, descr, 10, nil))

	trigger.TaskID = "132"
	_, err = s.actions.InsertTask(ctx, trigger, "2")
	s.Require().NoError(err, "new task should be inserted since previous is done")
}

func (s *TaskActionTestSuite) TestEnqueueMessages() {
	ctx := context.Background()
	ctrl := gomock.NewController(s.T())
	sqsapi := mock.NewMockSQSAPI(ctrl)

	now := time.Now()
	nowTS := now.Unix()

	err := s.actions.AddObjects(ctx, []entities.Object{
		{
			ID:            "123",
			DataNamespace: "task",
			LastEventTime: now,
		},
		{
			ID:            "124",
			DataNamespace: "task",
			LastEventTime: now.Add(-time.Hour),
		},
		{
			ID:            "125",
			DataNamespace: "task",
			LastEventTime: now.Add(-2 * time.Hour),
		},
		{
			ID:            "126",
			DataNamespace: "another-task",
			LastEventTime: now,
		},
		{
			ID:            "127",
			DataNamespace: "task",
			LastEventTime: now,
		},
	})
	s.Require().NoError(err)

	q := &queue.Queue{
		SQS:  sqsapi,
		Name: "queue",
		URL:  "url",
	}

	jobAttrValue := &sqs2.MessageAttributeValue{
		DataType:    aws.String("String"),
		StringValue: aws.String("queue"),
	}
	messageAttributes := map[string]*sqs2.MessageAttributeValue{task.AttributeJobName: jobAttrValue}
	sqsapi.EXPECT().SendMessageBatchWithContext(gomock.Any(), &sqs2.SendMessageBatchInput{
		Entries: []*sqs2.SendMessageBatchRequestEntry{
			{
				Id:                aws.String("0"),
				MessageAttributes: messageAttributes,
				MessageBody: aws.String(
					fmt.Sprintf(
						`{"object_id":"123","task_id":"127","task_name":"name3","task_start_ts":%d,"action":{"do":"nothing"}}`,
						nowTS,
					),
				),
			},
			{
				Id:                aws.String("1"),
				MessageAttributes: messageAttributes,
				MessageBody: aws.String(
					fmt.Sprintf(
						`{"object_id":"124","task_id":"127","task_name":"name3","task_start_ts":%d,"action":{"do":"nothing"}}`,
						nowTS,
					),
				),
			},
		},
		QueueUrl: aws.String("url"),
	}).Return(&sqs2.SendMessageBatchOutput{
		Failed:     nil,
		Successful: []*sqs2.SendMessageBatchResultEntry{{Id: aws.String("0")}, {Id: aws.String("1")}},
	}, nil)
	sqsapi.EXPECT().SendMessageBatchWithContext(gomock.Any(), &sqs2.SendMessageBatchInput{
		Entries: []*sqs2.SendMessageBatchRequestEntry{
			{
				Id:                aws.String("0"),
				MessageAttributes: messageAttributes,
				MessageBody: aws.String(
					fmt.Sprintf(
						`{"object_id":"127","task_id":"127","task_name":"name3","task_start_ts":%d,"action":{"do":"nothing"}}`,
						nowTS,
					),
				),
			},
		},
		QueueUrl: aws.String("url"),
	}).Return(&sqs2.SendMessageBatchOutput{
		Failed:     nil,
		Successful: []*sqs2.SendMessageBatchResultEntry{{Id: aws.String("0")}},
	}, nil)

	trigger := defaultTaskTrigger()
	trigger.TaskName = "name3"
	trigger.TaskID = "127"
	trigger.TaskStartTS = nowTS
	trigger.Filters = []entities.ObjectFilter{
		{
			DataNamespace: "task",
			Since: entities.SinceObjectFilter{
				Time:     "1h5m",
				Relative: entities.RelativeMomentLastRun,
			},
		},
	}
	trigger.Action = map[string]any{"do": "nothing"}

	descr, err := s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err)

	// Emulate worker and add new records.
	records := []entities.QueueRecord{
		{
			ObjectID: "123",
			TaskID:   "127",
			TaskName: "name3",
		},
		{
			ObjectID: "124",
			TaskID:   "127",
			TaskName: "name3",
		},
		{
			ObjectID: "127",
			TaskID:   "127",
			TaskName: "name3",
		},
	}
	for _, record := range records {
		do, retries, err := s.actions.SetObjectHandleStatusNew(ctx, record)
		s.Require().NoError(err)
		s.Require().True(do)
		s.Require().Zero(retries)
	}

	// To check that nothing is actually done we need to try to insert the same task with new ID.
	s.Require().NoError(s.actions.UpdateDoneTasks(ctx))

	trigger.TaskID = "128"
	_, err = s.actions.InsertTask(ctx, trigger, "1")
	s.Require().ErrorIs(err, ErrAnotherTaskInProcess, "cannot insert next task since current isn't done")

	trigger.TaskID = "127"
	s.Require().NoError(s.actions.ProcessTask(ctx, q, descr, 2, nil))

	for i, record := range records {
		if i == 0 {
			s.Require().NoError(s.actions.SetObjectHandleStatusFailed(ctx, record))
			continue
		}
		s.Require().NoError(s.actions.SetObjectHandleStatusDone(ctx, record))
	}

	// Now we set the task actually done.
	s.Require().NoError(s.actions.UpdateDoneTasks(ctx))

	trigger.TaskID = "128"
	_, err = s.actions.InsertTask(ctx, trigger, "1")
	s.Require().NoError(err, "inserted new task successfully")
}

type TaskActionTestSuite struct {
	ActionTestSuite
}

func TestTaskActionTestSuite(t *testing.T) {
	suite.Run(t, new(TaskActionTestSuite))
}
