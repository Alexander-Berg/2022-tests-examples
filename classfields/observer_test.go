package task_test

import (
	"github.com/YandexClassifieds/vtail/api/backend"
	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/internal/task"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"testing"
	"time"
)

const waitTimeout = 200 * time.Millisecond

func prepare(t *testing.T) (*task.Storage, *task.Observer) {
	test.InitConfig(t)
	path := test.ZkPrepare(t)
	logger := test.NewTestLogger()

	addresses := viper.GetStringSlice("zk_addresses")
	storage, err := task.NewStorage(addresses, path, logger)
	require.NoError(t, err)

	observer := task.NewObserver(addresses, path, logger)

	return storage, observer
}

func TestObserver_NewNode(t *testing.T) {
	defer goleak.VerifyNone(t)

	storage, observer := prepare(t)
	defer storage.Close()
	defer observer.Close()

	updates, err := observer.ObserveChanges()
	require.NoError(t, err)

	flow := flowFixture()
	addFlow(t, storage, &flow)

	requireFlow(t, []string{flow.Id}, updates)
	requireNoUpdate(t, updates)

	err = storage.RemoveFlow(flow.Id)
	require.NoError(t, err)

	requireObsolete(t, flow.Id, updates)
	requireNoUpdate(t, updates)
}

func TestObserver_ExistingNode(t *testing.T) {
	defer goleak.VerifyNone(t)

	storage, observer := prepare(t)
	defer storage.Close()
	defer observer.Close()

	flow := flowFixture()
	addFlow(t, storage, &flow)

	updates, err := observer.ObserveChanges()
	require.NoError(t, err)

	requireFlow(t, []string{flow.Id}, updates)
	requireNoUpdate(t, updates)

	err = storage.RemoveFlow(flow.Id)
	require.NoError(t, err)

	requireObsolete(t, flow.Id, updates)
	requireNoUpdate(t, updates)
}

func TestObserver_MultipleNodes(t *testing.T) {
	defer goleak.VerifyNone(t)

	storage, observer := prepare(t)
	defer storage.Close()
	defer observer.Close()

	flow1 := flowFixture()
	addFlow(t, storage, &flow1)

	flow2 := flowFixture()
	flow2.Destination = "dest2"
	addFlow(t, storage, &flow2)

	updates, err := observer.ObserveChanges()
	require.NoError(t, err)

	requireFlow(t, []string{flow1.Id, flow2.Id}, updates)
	requireFlow(t, []string{flow1.Id, flow2.Id}, updates)
	requireNoUpdate(t, updates)
}

func TestObserver_AddThenRemove(t *testing.T) {
	defer goleak.VerifyNone(t)

	storage, observer := prepare(t)
	defer storage.Close()
	defer observer.Close()

	flow1 := flowFixture()
	addFlow(t, storage, &flow1)

	err := storage.RemoveFlow(flow1.Id)
	require.NoError(t, err)

	updates, err := observer.ObserveChanges()
	require.NoError(t, err)
	requireNoUpdate(t, updates)
}

func addFlow(t *testing.T, storage *task.Storage, flow *task.Flow) {
	flowId, err := storage.AddFlow(flow)
	require.NoError(t, err)
	flow.Id = flowId
}

func requireObsolete(t *testing.T, flowId string, updates <-chan task.FlowUpdate) {
	t.Helper()
	select {
	case update := <-updates:
		require.Equal(t, task.ActionDelete, update.Action)
		require.Equal(t, flowId, update.FlowId)
	case <-time.After(waitTimeout):
		t.Error("no expected obsolete FlowId value")
	}
}

func requireFlow(t *testing.T, flowIds []string, updates <-chan task.FlowUpdate) {
	t.Helper()
	select {
	case update := <-updates:
		require.Equal(t, task.ActionAdd, update.Action)
		require.Contains(t, flowIds, update.FlowId)
	case <-time.After(waitTimeout):
		t.Error("no expected flow value")
	}
}

func requireNoUpdate(t *testing.T, updates <-chan task.FlowUpdate) {
	t.Helper()
	select {
	case update := <-updates:
		t.Errorf("an unexpected update: %+v", update)
	case <-time.After(waitTimeout):
	}
}

func flowFixture() task.Flow {
	return task.Flow{
		Filters:     &core.Parenthesis{},
		Include:     &backend.IncludeFields{Message: true},
		Destination: "dest",
	}
}
