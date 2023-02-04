package impl

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
)

func (s *QueueActionTestSuite) TestPipelineOfActions() {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	// Successfully set new status.
	needProcess, retries, err := s.actions.SetObjectHandleStatusNew(ctx, entities.QueueRecord{
		ObjectID:  "123",
		TaskID:    "id",
		TaskName:  "name",
		MessageID: "id",
	})
	s.Require().NoError(err)
	s.Assert().True(needProcess, "need to process if inserted record")
	s.Assert().Zero(retries, "retries are 0 on insert")

	// Concurrent process should pass since we don't know if first one has failed without symptoms.
	needProcess, retries, err = s.actions.SetObjectHandleStatusNew(ctx, entities.QueueRecord{
		ObjectID:  "123",
		TaskID:    "id",
		TaskName:  "name",
		MessageID: "id",
	})
	s.Require().NoError(err)
	s.Assert().True(needProcess, "need to process on conflict if status was new")
	s.Assert().Zero(retries, "retries are 0 on conflict")

	// Another message id tells about duplicate, we shouldn't process further.
	needProcess, _, err = s.actions.SetObjectHandleStatusNew(ctx, entities.QueueRecord{
		ObjectID:  "123",
		TaskID:    "id",
		TaskName:  "name",
		MessageID: "another",
	})
	s.Require().NoError(err)
	s.Assert().False(needProcess, "don't process duplicate message")

	// Successfully increase retries.
	isDone, err := s.actions.IncreaseRetries(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id",
		TaskName: "name",
	})
	s.Require().NoError(err)
	s.Assert().False(isDone, "successfully increased, status is not done")

	// Set status done.
	err = s.actions.SetObjectHandleStatusDone(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id",
		TaskName: "name",
	})
	s.Require().NoError(err)

	// Don't increase retries on done status
	isDone, err = s.actions.IncreaseRetries(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id",
		TaskName: "name",
	})
	s.Require().NoError(err)
	s.Assert().True(isDone, "already done, no need to increase retries count")

	// No need to process if status is done
	needProcess, retries, err = s.actions.SetObjectHandleStatusNew(ctx, entities.QueueRecord{
		ObjectID:  "123",
		TaskID:    "id",
		TaskName:  "name",
		MessageID: "id",
	})
	s.Require().NoError(err)
	s.Assert().False(needProcess, "no need to process if status is done")
	s.Assert().Equal(1, retries, "retries are 1 at this moment")

	// New task ID
	needProcess, retries, err = s.actions.SetObjectHandleStatusNew(ctx, entities.QueueRecord{
		ObjectID:  "123",
		TaskID:    "id2",
		TaskName:  "name",
		MessageID: "newid",
	})
	s.Require().NoError(err)
	s.Assert().True(needProcess, "need to process if task id changed")
	s.Assert().Zero(retries, "retries are 0 on new task id")

	// No old task ID to set done.
	err = s.actions.SetObjectHandleStatusDone(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id",
		TaskName: "name",
	})
	s.Require().NoError(err)

	result, err := s.store.GetDatabase(ctx, pg.Master).
		ExecContext(ctx, "UPDATE sched.t_queue SET status = 'done' WHERE task_id = 'id'")
	s.Require().NoError(err)
	affected, err := result.RowsAffected()
	s.Require().NoError(err)
	s.Assert().Zero(affected)

	// Action above didn't change status for current task - must be able to increase retries.
	isDone, err = s.actions.IncreaseRetries(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id2",
		TaskName: "name",
	})
	s.Require().NoError(err)
	s.Assert().False(isDone, "increased retries")

	// Cannot increase retries for old task id.
	isDone, err = s.actions.IncreaseRetries(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id",
		TaskName: "name",
	})
	s.Require().NoError(err)
	s.Assert().True(isDone, "no need to increase")

	// Set current task as failed.
	err = s.actions.SetObjectHandleStatusFailed(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id2",
		TaskName: "name",
	})
	s.Require().NoError(err)

	// Cannot increase retries for failed task.
	isDone, err = s.actions.IncreaseRetries(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id2",
		TaskName: "name",
	})
	s.Require().NoError(err)
	s.Assert().True(isDone, "no need to increase")

	// Can insert new task after failed one.
	needProcess, retries, err = s.actions.SetObjectHandleStatusNew(ctx, entities.QueueRecord{
		ObjectID: "123",
		TaskID:   "id3",
		TaskName: "name",
	})
	s.Require().NoError(err)
	s.Assert().True(needProcess, "need to process if task id changed")
	s.Assert().Zero(retries, "retries are 0 on new task id")
}

type QueueActionTestSuite struct {
	ActionTestSuite
}

func TestQueueActionTestSuite(t *testing.T) {
	suite.Run(t, new(QueueActionTestSuite))
}
