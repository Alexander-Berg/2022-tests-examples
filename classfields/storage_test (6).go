package task_test

import (
	"github.com/YandexClassifieds/vtail/internal/task"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/go-zookeeper/zk"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"testing"
	"time"
)

func TestStorage_AddRemoveFlow(t *testing.T) {
	defer goleak.VerifyNone(t)

	test.InitConfig(t)
	path := test.ZkPrepare(t)
	logger := test.NewTestLogger()

	addresses := viper.GetStringSlice("zk_addresses")

	storage, err := task.NewStorage(addresses, path, logger)
	require.NoError(t, err)
	defer storage.Close()

	conn, _, err := zk.Connect(addresses, time.Second)
	require.NoError(t, err)
	defer conn.Close()

	flow := flowFixture()

	flowId, err := storage.AddFlow(&flow)
	require.NoError(t, err)
	require.NotEmpty(t, flowId)
	exists, _, err := conn.Exists(path + "/" + flowId)
	require.NoError(t, err)
	require.True(t, exists)

	err = storage.RemoveFlow(flowId)
	require.NoError(t, err)
	exists, _, err = conn.Exists(path + "/" + flowId)
	require.NoError(t, err)
	require.False(t, exists)
}

func TestStorage_GetFlow(t *testing.T) {
	defer goleak.VerifyNone(t)

	test.InitConfig(t)
	path := test.ZkPrepare(t)
	logger := test.NewTestLogger()

	addresses := viper.GetStringSlice("zk_addresses")

	storage, err := task.NewStorage(addresses, path, logger)
	require.NoError(t, err)
	defer storage.Close()

	flow := flowFixture()

	flowId, err := storage.AddFlow(&flow)
	require.NoError(t, err)
	flow.Id = flowId

	resFlow := storage.GetFlow(flowId)
	require.Equal(t, flow.Id, resFlow.Id)
	require.Equal(t, flow.Destination, resFlow.Destination)
	require.Equal(t, flow.Include.Message, resFlow.Include.Message)
}
