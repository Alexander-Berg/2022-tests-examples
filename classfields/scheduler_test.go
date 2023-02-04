package batch

import (
	"database/sql"
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/batch/store/batch"
	"github.com/YandexClassifieds/shiva/cmd/shiva/batch/store/task"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/env_override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/include_links"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	env_resolver "github.com/YandexClassifieds/shiva/cmd/shiva/env/manifest"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/batch_task"
	batchPb "github.com/YandexClassifieds/shiva/pb/shiva/types/batch"
	taskPb "github.com/YandexClassifieds/shiva/pb/shiva/types/batch_task"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	generatePb "github.com/YandexClassifieds/shiva/pb/ss/secret"
	consul "github.com/YandexClassifieds/shiva/pkg/consul/locker"
	"github.com/YandexClassifieds/shiva/pkg/i/locker"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/include/domain"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"github.com/YandexClassifieds/shiva/pkg/secrets/secret"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/template"
	"github.com/YandexClassifieds/shiva/test"
	smock "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/election"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	mqMock "github.com/YandexClassifieds/shiva/test/mock/mq"
	sMapMock "github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

const (
	serviceName   = "testService"
	version       = "1.6.3"
	serviceMapYml = "{name: %s}"
	manifestYml   = `
name: %s
test:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
prod:
  datacenters:
    sas:
      count: 1
    vla:
      count: 1
`
	serviceMapPath = "maps/%s.yml"
	manifestPath   = "deploy/%s.yml"
)

var (
	_         scheduler.Scheduler = (*Scheduler)(nil)
	namespace                     = test.RandString(10)
)

func TestNoPeriodic(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	producerMock := mqMock.NewProducerMock()
	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, smock.NewMockBatchScheduler(), producerMock, &mocks.SecretClient{}, l)

	schCtx := makeContext(t, s, db, serviceName, "")
	_, states, err := s.Run(schCtx)
	require.NoError(t, err)
	assertSuccessResponse(t, states)

	states, err = s.State(schCtx)
	require.NoError(t, err)
	assertSuccessResponse(t, states)

	b, err := s.batchStore.Get(layer.Layer_TEST, serviceName, "")
	require.NoError(t, err)
	assert.Equal(t, b.DeploymentID, schCtx.DeploymentId)
	assert.Empty(t, b.Periodic)
	assert.Equal(t, sql.NullTime{Valid: false}, b.Next)
	assert.Equal(t, "danevge", b.Deployment.Author.Login)
	_, err = s.taskStore.GetProcess(b)
	assert.True(t, errors.Is(err, common.ErrNotFound))
	assert.Empty(t, producerMock.Msg)

	s.Close()
}

func TestPeriodic(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	m := smock.NewMockBatchScheduler()
	pMock := mqMock.NewProducerMock()
	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, m, pMock, &mocks.SecretClient{}, l)
	_, b := makeBatch(t, db, s, "")
	_, err := s.taskStore.GetProcess(b)
	assert.True(t, errors.Is(err, common.ErrNotFound))
	resultC := make(chan *scheduler.State, 2)
	resultC <- &scheduler.State{StateType: scheduler.Process}
	m.AddResultC(b.Name, b.Branch, resultC)
	runPeriodicTask(t, s, b)
	resultC <- &scheduler.State{StateType: scheduler.Success}
	close(resultC)
	successT := getTask(t, s, b, taskPb.State_Success)
	assert.Nil(t, successT.Deployment)
	assert.Equal(t, int64(0), successT.DeploymentID)
	now := time.Now()
	test.Wait(t, func() error {
		updatedB, err := s.batchStore.GetByID(b.ID)
		switch {
		case err != nil:
			return err
		case updatedB.Next.Time.Before(now):
			return fmt.Errorf("next time not updated")
		}
		return nil
	})
	assert.Equal(t, 2, len(pMock.Msg))
	assertEventState(t, taskPb.State_Process, pMock.Get(t))
	successE := assertEventState(t, taskPb.State_Success, pMock.Get(t))
	eventTime := successE.GetBatch().GetNextRun().AsTime()
	assert.True(t, eventTime.After(now), fmt.Sprintf("event time: %v, now: :%v", eventTime, now))
	s.Close()
}

func TestPeriodic_Branch(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	m := smock.NewMockBatchScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, m, pMock, &mocks.SecretClient{}, smock.NewMockSilentLocker())
	_, b := makeBatch(t, db, s, "br1")
	_, err := s.taskStore.GetProcess(b)
	assert.True(t, errors.Is(err, common.ErrNotFound))
	resultC := make(chan *scheduler.State, 2)
	resultC <- &scheduler.State{StateType: scheduler.Process}
	m.AddResultC(b.Name, b.Branch, resultC)
	runPeriodicTask(t, s, b)
	resultC <- &scheduler.State{StateType: scheduler.Success}
	close(resultC)
	successT := getTask(t, s, b, taskPb.State_Success)
	assert.Nil(t, successT.Deployment)
	assert.Equal(t, int64(0), successT.DeploymentID)
	now := time.Now()
	test.Wait(t, func() error {
		updatedB, err := s.batchStore.GetByID(b.ID)
		switch {
		case err != nil:
			return err
		case updatedB.Next.Time.Before(now):
			return fmt.Errorf("next time not updated")
		}
		return nil
	})
	assert.Equal(t, 2, len(pMock.Msg))
	evt := getEvent(t, pMock.Get(t))
	assert.Equal(t, taskPb.State_Process, evt.GetTask().GetState())
	assert.Equal(t, "br1", evt.GetDeployment().GetBranch())
	successE := assertEventState(t, taskPb.State_Success, pMock.Get(t))
	eventTime := successE.GetBatch().GetNextRun().AsTime()
	assert.True(t, eventTime.After(now), fmt.Sprintf("event time: %v, now: :%v", eventTime, now))
	s.Close()
}

func getEvent(t *testing.T, msg *mq.Message) *batch_task.BatchTaskEvent {
	b := msg.Payload
	e := &batch_task.BatchTaskEvent{}
	err := proto.Unmarshal(b, e)
	require.NoError(t, err)
	return e
}

func assertEventState(t *testing.T, want taskPb.State, msg *mq.Message) *batch_task.BatchTaskEvent {
	b := msg.Payload
	e := &batch_task.BatchTaskEvent{}
	err := proto.Unmarshal(b, e)
	require.NoError(t, err)
	assert.Equal(t, want, e.Task.State)
	return e
}

func TestSkippedTask(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	m := smock.NewMockBatchScheduler()
	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	producerMock := mqMock.NewProducerMock()
	s := newService(t, db, m, producerMock, &mocks.SecretClient{}, l)
	_, b := makeBatch(t, db, s, "")

	result := make(chan *scheduler.State, 2)
	result <- &scheduler.State{
		StateType: scheduler.Process,
	}
	m.AddResultC(b.Name, b.Branch, result)

	runPeriodicTask(t, s, b)
	getTask(t, s, b, taskPb.State_Process)
	runPeriodicTask(t, s, b)
	getTask(t, s, b, taskPb.State_Skipped)
	assert.NotEmpty(t, producerMock.Msg)
	s.Close()
}

func TestSkippedWorkaround(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	m1 := smock.NewMockBatchScheduler()
	m2 := smock.NewMockBatchScheduler()
	l1 := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	l2 := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	producerMock := mqMock.NewProducerMock()
	s1 := newService(t, db, m1, producerMock, &mocks.SecretClient{}, l1)
	s2 := newService(t, db, m2, producerMock, &mocks.SecretClient{}, l2)

	// prepare freeze
	_, b := makeBatch(t, db, s1, "")
	_, err := s1.taskStore.GetLast(b)
	require.ErrorIs(t, err, common.ErrNotFound)
	resultC := make(chan *scheduler.State, 1)
	resultC <- &scheduler.State{StateType: scheduler.Process}
	m1.AddResultC(b.Name, b.Branch, resultC)
	require.NoError(t, s1.runTask(b, 0))
	task1 := getTask(t, s1, b, taskPb.State_Process)

	// prepare
	oldResult := make(chan *scheduler.State, 0)
	m2.AddResultC(b.Name, b.Branch, oldResult)
	resultC = make(chan *scheduler.State, 1)
	resultC <- &scheduler.State{StateType: scheduler.Process}
	m2.AddResultC(b.Name, b.Branch, resultC)
	s1.Close()

	// run new process
	result := make(chan error)
	go func() {
		result <- s2.runTask(b, 0)
	}()
	task1.State = taskPb.State_Success
	require.NoError(t, s2.taskStore.Save(task1))
	oldResult <- &scheduler.State{StateType: scheduler.Success}
	close(oldResult)

	require.NoError(t, <-result)
	lastTask, err := s2.taskStore.GetLast(b)
	require.NoError(t, err)
	assert.NotEqual(t, task1.ID, lastTask.ID)
	s2.Close()
}

func TestWorkaround(t *testing.T) {

	type Case struct {
		name         string
		lock         bool
		oldTaskState taskPb.State

		state *scheduler.State
		err   error
	}

	cases := []Case{
		{
			name:         "main",
			lock:         true,
			state:        &scheduler.State{StateType: scheduler.Success},
			oldTaskState: taskPb.State_Success,
		},
		{
			name:         "without_lock",
			lock:         false,
			state:        &scheduler.State{StateType: scheduler.Success},
			oldTaskState: taskPb.State_Success,
		},
		{
			name:         "fail_state",
			lock:         true,
			state:        &scheduler.State{StateType: scheduler.Fail},
			oldTaskState: taskPb.State_Failed,
		},
		{
			name:         "nomad_gc",
			lock:         true,
			state:        nil,
			err:          common.ErrNotFound,
			oldTaskState: taskPb.State_Failed,
		},
		{
			name:         "empty",
			lock:         false,
			state:        nil,
			err:          common.ErrNotFound,
			oldTaskState: taskPb.State_Failed,
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			test.InitTestEnv()
			db := test_db.NewSeparatedDb(t)
			require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

			m1 := smock.NewMockBatchScheduler()
			m2 := smock.NewMockBatchScheduler()
			l1 := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
			l2 := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
			producerMock := mqMock.NewProducerMock()
			s1 := newService(t, db, m1, producerMock, &mocks.SecretClient{}, l1)
			s2 := newService(t, db, m2, producerMock, &mocks.SecretClient{}, l2)

			// make freeze
			_, b := makeBatch(t, db, s1, "")
			_, err := s1.taskStore.GetLast(b)
			require.ErrorIs(t, err, common.ErrNotFound)
			resultC := make(chan *scheduler.State, 1)
			resultC <- &scheduler.State{StateType: scheduler.Process}
			m1.AddResultC(b.Name, b.Branch, resultC)
			require.NoError(t, s1.runTask(b, 0))
			task1 := getTask(t, s1, b, taskPb.State_Process)

			// prepare test case
			oldResult := make(chan *scheduler.State, 1)
			if c.state != nil {
				oldResult <- c.state
				close(oldResult)
				m2.AddResultC(b.Name, b.Branch, oldResult)
			}
			resultC = make(chan *scheduler.State, 1)
			resultC <- &scheduler.State{StateType: scheduler.Process}
			m2.AddResultC(b.Name, b.Branch, resultC)
			if c.err != nil {
				m2.AddResultError(b.Name, b.Branch, c.err)
			}
			if c.lock {
				defer s1.Close()
			} else {
				s1.Close()
			}

			// run new process
			require.NoError(t, s2.runTask(b, 0))

			time.Sleep(time.Second)
			task2 := getTask(t, s2, b, taskPb.State_Process)
			assert.NotEmpty(t, producerMock.Msg)
			assert.NotEqual(t, task1.ID, task2.ID)
			reloadTask1, err := s1.taskStore.GetById(task1.ID)
			require.NoError(t, err)
			assert.Equal(t, reloadTask1.State.String(), c.oldTaskState.String())

			s2.Close()
		})
	}
}

func getTask(t *testing.T, s *Scheduler, b *batch.Batch, want taskPb.State) *task.Task {
	var result *task.Task
	test.Wait(t, func() error {
		last, err := s.taskStore.GetLast(b)
		switch {
		case err != nil:
			return err
		case last.State != want:
			return fmt.Errorf("state %s not %s ", last.State.String(), want.String())
		}
		result = last
		return nil
	})
	return result
}

func TestResizeNotSupport(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, smock.NewMockBatchScheduler(), mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	schCtx, _ := makeBatch(t, db, s, "")
	_, _, err := s.Update(schCtx)
	assert.True(t, errors.Is(err, common.ErrNotSupport))
	s.Close()
}

func TestCancel(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	m := smock.NewMockBatchScheduler()
	s := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)

	cases := []string{"", "br"}

	for _, c := range cases {
		t.Run(fmt.Sprintf("branch '%s'", c), func(t *testing.T) {
			schCtx, b := makeBatch(t, db, s, c)

			resultC := make(chan *scheduler.State, 1)
			resultC <- &scheduler.State{StateType: scheduler.Process}

			m.AddResultC(b.Name, b.Branch, resultC)

			runPeriodicTask(t, s, b)
			runAndAssertCommand(t, s, schCtx, s.Cancel)

			getTask(t, s, b, taskPb.State_Canceled)
		})
	}

	s.Close()
}

func TestRestart(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	m := smock.NewMockBatchScheduler()
	s := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	schCtx, b := makeBatch(t, db, s, "")

	resultC := make(chan *scheduler.State, 1)
	resultC <- &scheduler.State{StateType: scheduler.Process}
	m.AddResultC(b.Name, b.Branch, resultC)
	firstTask := runPeriodicTask(t, s, b)

	resultC = make(chan *scheduler.State, 1)
	resultC <- &scheduler.State{StateType: scheduler.Process}
	m.AddResultC(b.Name, b.Branch, resultC)
	runAndAssertCommand(t, s, schCtx, s.Restart)
	secondTask := getTask(t, s, b, taskPb.State_Process)
	assert.NotEqual(t, firstTask.ID, secondTask.ID)
	firstTask, err := s.taskStore.GetById(firstTask.ID)
	require.NoError(t, err)
	assert.Equal(t, taskPb.State_Canceled.String(), firstTask.State.String())
	s.Close()
}

func TestRestore(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	svcId := makeServiceMap(t, db, "test-svc")
	manId := makeManifest(t, db, "test-svc")
	user := getUser(t, db, "danevge")

	require.NoError(t, db.GormDb.AutoMigrate(model.Deployment{}, batch.Batch{}, task.Task{}))
	d := &model.Deployment{
		Layer:            common.Test,
		State:            model.Success,
		ServiceMapsID:    svcId,
		DeployManifestID: manId,
		AuthorID:         user.ID,
	}
	require.NoError(t, db.GormDb.Create(d).Error)
	b := &batch.Batch{
		Layer:        layer.Layer_TEST,
		State:        batchPb.State_Active,
		DeploymentID: d.ID,
	}
	require.NoError(t, db.GormDb.Create(b).Error)
	tsk := &task.Task{
		BatchID:      b.ID,
		State:        taskPb.State_Process,
		DeploymentID: d.ID,
	}
	require.NoError(t, db.GormDb.Create(tsk).Error)

	stateC := make(chan *scheduler.State, 1)
	stateC <- &scheduler.State{StateType: scheduler.Success}
	close(stateC)

	m := new(mocks.BatchScheduler)
	m.On("State", mock.Anything).Return(stateC, nil).Once()

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	svc := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	defer svc.Close()

	assert.Eventually(t, func() bool {
		result, err := svc.taskStore.GetById(tsk.ID)

		return err == nil && result.State == taskPb.State_Success
	}, time.Second*15, time.Second)

	m.AssertExpectations(t)
}

func TestRestore_Fail_NotFound(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	svcId := makeServiceMap(t, db, "test-svc")
	manId := makeManifest(t, db, "test-svc")
	user := getUser(t, db, "danevge")
	require.NoError(t, db.GormDb.AutoMigrate(model.Deployment{}, batch.Batch{}, task.Task{}))

	d := &model.Deployment{
		Layer:            common.Test,
		State:            model.Success,
		ServiceMapsID:    svcId,
		DeployManifestID: manId,
		AuthorID:         user.ID,
	}
	db.GormDb.Create(d)
	b := &batch.Batch{
		Layer:        layer.Layer_TEST,
		State:        batchPb.State_Active,
		DeploymentID: d.ID,
	}
	db.GormDb.Create(b)
	tsk := &task.Task{
		BatchID:      b.ID,
		State:        taskPb.State_Process,
		DeploymentID: d.ID,
	}
	db.GormDb.Create(tsk)

	m := new(mocks.BatchScheduler)
	m.On("State", mock.Anything).Return(nil, common.ErrNotFound)

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	svc := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	defer svc.Close()

	assert.Eventually(t, func() bool {
		result, err := svc.taskStore.GetById(tsk.ID)

		return err == nil && result.State == taskPb.State_Failed
	}, time.Second*15, time.Second)

	m.AssertExpectations(t)
}

func TestRestore_Retry(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	svcId := makeServiceMap(t, db, "test-svc")
	manId := makeManifest(t, db, "test-svc")
	user := getUser(t, db, "danevge")
	require.NoError(t, db.GormDb.AutoMigrate(model.Deployment{}, batch.Batch{}, task.Task{}))

	d := &model.Deployment{
		Layer:            common.Test,
		State:            model.Success,
		ServiceMapsID:    svcId,
		DeployManifestID: manId,
		AuthorID:         user.ID,
	}
	db.GormDb.Create(d)
	b := &batch.Batch{
		Layer:        layer.Layer_TEST,
		State:        batchPb.State_Active,
		DeploymentID: d.ID,
	}
	db.GormDb.Create(b)
	tsk := &task.Task{
		BatchID:      b.ID,
		State:        taskPb.State_Process,
		DeploymentID: d.ID,
	}
	db.GormDb.Create(tsk)

	stateC := make(chan *scheduler.State, 1)
	stateC <- &scheduler.State{StateType: scheduler.Success}
	close(stateC)

	m := new(mocks.BatchScheduler)
	m.On("State", mock.Anything).Return(nil, fmt.Errorf("some transient err")).Once()
	m.On("State", mock.Anything).Return(stateC, nil).Once()

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	svc := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	defer svc.Close()

	assert.Eventually(t, func() bool {
		result, err := svc.taskStore.GetById(tsk.ID)

		return err == nil && result.State == taskPb.State_Success
	}, time.Second*15, time.Second)

	m.AssertExpectations(t)
}

func TestRestore_RetryFail(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	svcId := makeServiceMap(t, db, "test-svc")
	manId := makeManifest(t, db, "test-svc")
	user := getUser(t, db, "danevge")
	require.NoError(t, db.GormDb.AutoMigrate(model.Deployment{}, batch.Batch{}, task.Task{}))

	d := &model.Deployment{
		Layer:            common.Test,
		State:            model.Success,
		ServiceMapsID:    svcId,
		DeployManifestID: manId,
		AuthorID:         user.ID,
	}
	db.GormDb.Create(d)
	b := &batch.Batch{
		Layer:        layer.Layer_TEST,
		State:        batchPb.State_Active,
		DeploymentID: d.ID,
	}
	db.GormDb.Create(b)
	tsk := &task.Task{
		BatchID:      b.ID,
		State:        taskPb.State_Process,
		DeploymentID: d.ID,
	}
	db.GormDb.Create(tsk)

	stateC := make(chan *scheduler.State, 1)
	stateC <- &scheduler.State{StateType: scheduler.Success}
	close(stateC)

	m := new(mocks.BatchScheduler)
	m.On("State", mock.Anything).Return(nil, fmt.Errorf("some err"))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	svc := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	defer svc.Close()

	assert.Eventually(t, func() bool {
		result, err := svc.taskStore.GetById(tsk.ID)

		return err == nil && result.State == taskPb.State_Failed
	}, time.Second*15, time.Second)

	m.AssertExpectations(t)
}

func TestForceRun_OneOff(t *testing.T) {
	// test force run is working without batch schedule
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	m := new(mocks.BatchScheduler)
	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	defer s.Close()

	sc := makeContext(t, s, db, "test-oneoff", "")
	m.On("Run", mock.Anything).Return(mockStateChan(scheduler.Success), nil).Once()

	_, _, err := s.ForceRun(sc)
	require.NoError(t, err)

	assert.Eventually(t, func() bool {
		tsk := &task.Task{}
		err = db.GormDb.First(tsk, task.Task{DeploymentID: sc.DeploymentId}).Error
		require.NoError(t, err)
		return assert.Equal(t, taskPb.State_Success, tsk.State)
	}, time.Second*2, time.Second/2)

	// check second run is okay
	sc2 := makeContext(t, s, db, "test-oneoff", "")
	sc2.Version = "1.6.4"
	m.On("Run", mock.Anything).Return(mockStateChan(scheduler.Success), nil).Once()
	_, _, err = s.ForceRun(sc2)
	require.NoError(t, err)

	assert.Eventually(t, func() bool {
		tsk2 := &task.Task{}
		err = db.GormDb.First(tsk2, task.Task{DeploymentID: sc2.DeploymentId}).Error
		require.NoError(t, err)
		return assert.Equal(t, taskPb.State_Success, tsk2.State)
	}, time.Second*2, time.Second/2)
}

func mockStateChan(lastState scheduler.StateType) chan *scheduler.State {
	stateC := make(chan *scheduler.State, 2)
	stateC <- &scheduler.State{StateType: scheduler.Process}
	stateC <- &scheduler.State{StateType: lastState}
	close(stateC)
	return stateC
}

func TestForceRun(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	m := smock.NewMockBatchScheduler()
	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	sc1, b := makeBatch(t, db, s, "")

	resultTask1 := make(chan *scheduler.State, 2)
	resultTask1 <- &scheduler.State{StateType: scheduler.Process}
	m.AddResultC(b.Name, b.Branch, resultTask1)
	task1 := runPeriodicTask(t, s, b)
	resultTask1 <- &scheduler.State{StateType: scheduler.Success}
	close(resultTask1)
	getTask(t, s, b, taskPb.State_Success)

	// mock force deployment record
	d := &model.Deployment{}
	db.GormDb.Take(&d, "id = ?", sc1.DeploymentId)
	d.ID = 0
	d.Version = "v2"
	require.NoError(t, db.GormDb.Save(d).Error)
	sc2 := &scheduler.Context{
		DeploymentId: d.ID,
		Version:      "v2",
		ServiceMap:   sc1.ServiceMap,
		Manifest:     sc1.Manifest,
	}

	resultForceTask := make(chan *scheduler.State, 2)
	resultForceTask <- &scheduler.State{StateType: scheduler.Process}
	m.AddResultC(b.Name, b.Branch, resultForceTask)

	// execute force run with new version
	runAndAssertCommand(t, s, sc2, s.ForceRun)
	forceTask := getTask(t, s, b, taskPb.State_Process)
	resultForceTask <- &scheduler.State{StateType: scheduler.Success}
	close(resultForceTask)
	successT := getTask(t, s, b, taskPb.State_Success)
	assert.Equal(t, forceTask.ID, successT.ID)
	assert.NotEqual(t, forceTask.ID, task1.ID)
	assert.NotNil(t, successT.Deployment)
	assert.Equal(t, sc2.DeploymentId, successT.DeploymentID)
	s.Close()

	resultBatch, err := s.batchStore.Get(layer.Layer_TEST, sc2.GetName(), "")
	require.NoError(t, err)
	assert.Equal(t, "v2", resultBatch.Version)
	assert.Equal(t, sc2.DeploymentId, resultBatch.DeploymentID)
}

func TestForceRun_Skipped(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	m := smock.NewMockBatchScheduler()
	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)

	schCtx1, b1 := makeBatch(t, db, s, "")
	result := make(chan *scheduler.State, 2)
	result <- &scheduler.State{
		StateType: scheduler.Process,
	}
	m.AddResultC(b1.Name, b1.Branch, result)
	_, _, err := s.ForceRun(schCtx1)
	require.NoError(t, err)
	getTask(t, s, b1, taskPb.State_Process)

	//
	//result = make(chan *scheduler.State, 2)
	//result <- &scheduler.State{
	//	StateType: scheduler.Process,
	//}
	//m.AddResultC(schCtx1.GetName(), schCtx1.GetBranchName(), result)
	schCtx2, _ := makeBatch(t, db, s, "")
	_, _, err = s.ForceRun(schCtx2)
	require.ErrorIs(t, err, ErrTaskAlreadyRunning)

	s.Close()
}

func TestFailRun(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	m := &mocks.BatchScheduler{}
	m.On("Run", mock.Anything).Return(nil, fmt.Errorf("random error"))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	schCtx, b := makeBatch(t, db, s, "")
	_, _, err := s.ForceRun(schCtx)
	require.Error(t, err)
	task := getTask(t, s, b, taskPb.State_InitFailed)
	s.Close()

	assert.Equal(t, taskPb.State_InitFailed, task.State)
}

func TestStopWithProcessTask(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	m := smock.NewMockBatchScheduler()
	s := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)

	schCtx, b := makeBatch(t, db, s, "")
	resultC := make(chan *scheduler.State, 1)
	resultC <- &scheduler.State{StateType: scheduler.Process}
	m.AddResultC(b.Name, b.Branch, resultC)
	runPeriodicTask(t, s, b)
	runAndAssertCommand(t, s, schCtx, s.Stop)

	b, err := s.batchStore.Get(layer.Layer_TEST, serviceName, "")
	require.NoError(t, err)
	assert.Equal(t, sql.NullTime{}, b.Next)
	assert.Equal(t, batchPb.State_Inactive.String(), b.State.String())
	getTask(t, s, b, taskPb.State_Canceled)
	s.Close()
}

func TestStopWithProcessTask_Branch(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	m := smock.NewMockBatchScheduler()
	s := newService(t, db, m, mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	schCtx, b := makeBatch(t, db, s, "br1")
	resultC := make(chan *scheduler.State, 1)
	resultC <- &scheduler.State{StateType: scheduler.Process}
	m.AddResultC(b.Name, b.Branch, resultC)
	runPeriodicTask(t, s, b)
	runAndAssertCommand(t, s, schCtx, s.Stop)

	_, err := s.batchStore.Get(layer.Layer_TEST, serviceName, "br1")
	require.True(t, errors.Is(err, common.ErrNotFound))

	b, err = s.batchStore.GetByID(b.ID)
	require.NoError(t, err)
	assert.Equal(t, sql.NullTime{}, b.Next)
	assert.Equal(t, true, b.DeletedAt.Valid)
	assert.Equal(t, batchPb.State_Inactive.String(), b.State.String())
	task := getTask(t, s, b, taskPb.State_Canceled)
	s.Close()

	assert.Equal(t, taskPb.State_Canceled, task.State)
}

func TestStopWithoutProcessTask(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, smock.NewMockBatchScheduler(), mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	makeBatch(t, db, s, "")

	schCtx := makeContext(t, s, db, serviceName, "")
	_, states, err := s.Stop(schCtx)
	require.NoError(t, err)
	assertSuccessResponse(t, states)
	assertState(t, s, schCtx)
	b, err := s.batchStore.Get(layer.Layer_TEST, serviceName, "")
	require.NoError(t, err)
	assert.Equal(t, sql.NullTime{}, b.Next)
	assert.Equal(t, batchPb.State_Inactive.String(), b.State.String())

	s.Close()
}

func TestBatchContext_OverrideParams(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, smock.NewMockBatchScheduler(), mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	_, batchModel := makeBatch(t, db, s, "")
	taskModel := makeTask(t, batchModel)
	overridesEnvMap := map[string]string{"TEST_KEY": "TEST_VALUE"}
	makeEnvOverrides(t, db, batchModel.DeploymentID, overridesEnvMap)

	ctx, err := s.batchContext(batchModel, taskModel)
	require.NoError(t, err)
	assert.Equal(t, overridesEnvMap, ctx.Manifest.Config.OverrideParams)
}

func TestBatchContext_OverrideFiles(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, smock.NewMockBatchScheduler(), mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	_, batchModel := makeBatch(t, db, s, "")
	taskModel := makeTask(t, batchModel)
	makeIncludeOverrides(t, db, batchModel.DeploymentID, map[string]string{
		"shiva/override.yml": `TEST_PARAM: test param`,
	})

	ctx, err := s.batchContext(batchModel, taskModel)
	require.NoError(t, err)
	assert.Len(t, ctx.Manifest.Config.OverrideFiles, 1)
	assert.Equal(t, "shiva/override.yml", ctx.Manifest.Config.OverrideFiles[0].Path)
}

func TestBatchContext_SecretGetter(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	mockSsCli := &mocks.SecretClient{}
	secretMap := map[string]string{"k1": "${sec_id-1:ver-1:k1}", "k2": "${sec_id-1:ver-1:k2}"}
	mockSsCli.On("GetSecrets", mock.Anything, &generatePb.GetSecretsRequest{
		ServiceName: serviceName,
		Layer:       layer.Layer_TEST,
		VersionId:   "ver-1",
	}).Return(&generatePb.GetSecretResponse{SecretEnvs: secretMap}, nil)

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, smock.NewMockBatchScheduler(), mqMock.NewProducerMock(), mockSsCli, l)
	_, batchModel := makeBatch(t, db, s, "")
	batchModel.Deployment.SecretVersion = "ver-1"
	taskModel := makeTask(t, batchModel)

	ctx, err := s.batchContext(batchModel, taskModel)
	require.NoError(t, err)
	require.Equal(t, secretMap, ctx.Manifest.Config.SecretParams)
}

func TestBatchContext_Branch(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))

	l := consul.NewLocker(test.NewLogger(t), NewLockerTestConf(t), &smock.MockContext{})
	s := newService(t, db, smock.NewMockBatchScheduler(), mqMock.NewProducerMock(), &mocks.SecretClient{}, l)
	_, batchModel := makeBatch(t, db, s, "br1")
	taskModel := makeTask(t, batchModel)

	ctx, err := s.batchContext(batchModel, taskModel)
	require.NoError(t, err)
	assert.Equal(t, "br1", ctx.Branch)
}

func runAndAssertCommand(t *testing.T, s *Scheduler, cxt *scheduler.Context, f func(cxt *scheduler.Context) (int64, chan *scheduler.State, error)) {
	_, states, err := f(cxt)
	require.NoError(t, err)
	assertSuccessResponse(t, states)
	assertState(t, s, cxt)
}

func runPeriodicTask(t *testing.T, s *Scheduler, b *batch.Batch) *task.Task {
	b.Next = sql.NullTime{Valid: true, Time: time.Now().Add(-2 * time.Minute)}
	require.NoError(t, s.batchStore.Save(b))
	result := getTask(t, s, b, taskPb.State_Process)
	assert.True(t, result.StartDate.After(b.Next.Time.Add(time.Minute)))
	return result
}

func makeTask(t *testing.T, b *batch.Batch) *task.Task {
	return &task.Task{
		BatchID:      b.ID,
		DeploymentID: 0,
		State:        taskPb.State_Prepare,
		StartDate:    time.Now(),
	}
}

func makeBatch(t *testing.T, db *storage.Database, s *Scheduler, branch string) (*scheduler.Context, *batch.Batch) {
	sc := makeContext(t, s, db, serviceName, branch)
	sc.Manifest.Periodic = "1 1 1 1 1"
	_, states, err := s.Run(sc)
	require.NoError(t, err)
	assertSuccessResponse(t, states)
	assertState(t, s, sc)
	b, err := s.batchStore.Get(layer.Layer_TEST, serviceName, branch)
	require.NoError(t, err)
	assert.NotNil(t, b.Next)
	return sc, b
}

func assertState(t *testing.T, s *Scheduler, sctx *scheduler.Context) {
	states, err := s.State(sctx)
	require.NoError(t, err)
	assertSuccessResponse(t, states)

	states, err = s.Restore(sctx)
	require.NoError(t, err)
	assertSuccessResponse(t, states)
}

func assertSuccessResponse(t *testing.T, states chan *scheduler.State) {
	assert.Len(t, states, 1)
	result := <-states
	assert.Equal(t, scheduler.Success.String(), result.StateType.String())
	_, ok := <-states
	assert.False(t, ok)
}

func makeServiceMap(t *testing.T, db *storage.Database, name string) int64 {
	svc := service_map.NewService(db, test.NewLogger(t), sMapMock.NewNotificationMock())
	sMapPath := fmt.Sprintf(serviceMapPath, name)
	require.NoError(t, svc.ReadAndSave([]byte(fmt.Sprintf(serviceMapYml, name)), 10, sMapPath))
	_, sMapID, err := svc.GetByFullPath(sMapPath)
	require.NoError(t, err)
	return sMapID
}

func makeManifest(t *testing.T, db *storage.Database, name string) int64 {
	log := test.NewLogger(t)
	svc := manifest.NewService(db, log, parser.NewService(log, nil), include.NewService(db, log))
	require.NoError(t, svc.ReadAndSave([]byte(fmt.Sprintf(manifestYml, name)), 10, fmt.Sprintf(manifestPath, name)))
	_, id, err := svc.GetByNameWithId(common.Test, name)
	require.NoError(t, err)
	return id
}

func makeEnvOverrides(t *testing.T, db *storage.Database, deploymentID int64, envOverride map[string]string) {
	envOverrideStore := env_override.NewStorage(db, test.NewLogger(t))
	var deploymentEnvOverride []*env_override.Model
	for key, value := range envOverride {
		deploymentEnvOverride = append(deploymentEnvOverride, &env_override.Model{
			DeploymentId: deploymentID,
			Key:          key,
			Value:        value,
		})
	}
	err := envOverrideStore.Save(deploymentEnvOverride)
	require.NoError(t, err)
}

func makeIncludeOverrides(t *testing.T, db *storage.Database, deploymentID int64, includeOverride map[string]string) {
	var incs []*domain.Include
	for path, file := range includeOverride {
		incs = append(incs, makeInclude(t, db, file, path))
	}

	includeLinksStore := include_links.NewStorage(db, test.NewLogger(t))
	var deploymentIncludes []*include_links.DeploymentIncludes
	for _, inc := range incs {
		deploymentIncludes = append(deploymentIncludes, &include_links.DeploymentIncludes{
			DeploymentId: deploymentID,
			IncludeId:    inc.ID(),
			Override:     true,
		})
	}
	err := includeLinksStore.Save(deploymentIncludes)
	require.NoError(t, err)
}

func makeInclude(t *testing.T, db *storage.Database, includeFile, path string) *domain.Include {
	service := include.NewService(db, test.NewLogger(t))
	err := service.ReadAndSave([]byte(includeFile), 10, path)
	require.NoError(t, err)
	inc, err := service.GetByPath(path)
	require.NoError(t, err)
	return inc
}

func getUser(t *testing.T, db *storage.Database, name string) *staff.User {
	log := test.NewLogger(t)

	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin(name)
	require.NoError(t, err)
	return user
}

func makeContext(t *testing.T, s *Scheduler, db *storage.Database, name string, branch string) *scheduler.Context {
	log := test.NewLogger(t)
	includeSrv := include.NewService(db, log)
	manifestSrv := manifest.NewService(db, log, parser.NewService(log, nil), includeSrv)
	sMapPath := fmt.Sprintf(serviceMapPath, name)
	require.NoError(t, s.sMapSrv.ReadAndSave([]byte(fmt.Sprintf(serviceMapYml, name)), 10, sMapPath))
	require.NoError(t, manifestSrv.ReadAndSave([]byte(fmt.Sprintf(manifestYml, name)), 10, fmt.Sprintf(manifestPath, name)))
	sMap, sMapID, err := s.sMapSrv.GetByFullPath(sMapPath)
	require.NoError(t, err)
	m, mID, err := manifestSrv.GetByNameWithId(common.Test, name)
	require.NoError(t, err)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	d := &model.Deployment{
		Name:             name,
		Branch:           branch,
		DeployManifestID: mID,
		ServiceMapsID:    sMapID,
		AuthorID:         user.ID,
		Layer:            common.Test,
		Version:          version,
		State:            model.Process,
		Type:             common.Run,
		StartDate:        time.Now(),
	}
	require.NoError(t, db.GormDb.AutoMigrate(d))
	newDB := db.GormDb.Save(d)
	require.NoError(t, newDB.Error)
	return &scheduler.Context{
		Layer:        common.Test,
		Version:      version,
		BranchName:   branch,
		ServiceMap:   sMap,
		Manifest:     m,
		DeploymentId: d.ID,
	}
}

func newService(t *testing.T, db *storage.Database, mockBS scheduler.BatchScheduler, mockProducer mqMock.ProducerMock, mockSsCli generatePb.SecretClient, locker locker.Locker) *Scheduler {
	log := test.NewLogger(t).WithField("test_name", t.Name())
	sMapSrv := service_map.NewService(db, log, sMapMock.NewNotificationMock())
	includeSrv := include.NewService(db, log)
	manifestSrv := manifest.NewService(db, log, parser.NewService(log, nil), includeSrv)
	overrideSrv := override.NewService(db, log, includeSrv, manifestSrv)
	secretService := secret.NewService(log, mockSsCli)
	ssMock := smock.NewAccessClientMock(t)
	envResolver := env_resolver.NewService(template.NewService(sMapSrv, reader.NewService(db, log)), secrets.NewService(secrets.NewConf(0), ssMock, log))

	return RunScheduler(
		NewConf(layer.Layer_TEST),
		log,
		db,
		overrideSrv,
		sMapSrv,
		secretService,
		mockBS,
		election.NewElectionStub(),
		locker,
		mockProducer,
		envResolver,
	)
}

func NewLockerTestConf(t *testing.T) consul.Conf {
	conf := consul.NewConf(namespace)
	conf.ServiceName = t.Name()
	conf.AllocationID = test.RandString(5)
	conf.TTL = "10s"

	return conf
}
