package task_test

import (
	"github.com/YandexClassifieds/vtail/internal/task"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/go-zookeeper/zk"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"testing"
	"time"
)

func TestCreateRecursivePath(t *testing.T) {
	defer goleak.VerifyNone(t)

	test.InitConfig(t)
	path := test.ZkPrepare(t)

	addresses := viper.GetStringSlice("zk_addresses")

	conn, _, err := zk.Connect(addresses, time.Second)
	require.NoError(t, err)
	defer conn.Close()

	exists, _, err := conn.Exists(path)
	require.NoError(t, err)
	require.False(t, exists)

	err = task.CreateRecursivePath(conn, path+"/b")
	require.NoError(t, err)
	err = task.CreateRecursivePath(conn, path)
	require.NoError(t, err)
	err = task.CreateRecursivePath(conn, path+"/b/c")
	require.NoError(t, err)

	exists, _, err = conn.Exists(path + "/b/c")
	require.NoError(t, err)
	require.True(t, exists)
}

func TestConvertToMap(t *testing.T) {
	defer goleak.VerifyNone(t)

	testCases := []struct {
		input  []string
		output map[string]bool
	}{
		{[]string{}, map[string]bool{}},
		{[]string{"a"}, map[string]bool{"a": true}},
		{[]string{"a", "b"}, map[string]bool{"a": true, "b": true}},
		{[]string{"a", "a"}, map[string]bool{"a": true}},
		{[]string{""}, map[string]bool{"": true}},
	}

	for _, testCase := range testCases {
		t.Run("internal test", func(t *testing.T) {
			output := task.ConvertToMap(testCase.input)
			assert.Equal(t, testCase.output, output)
		})
	}
}

func TestDetectChanges(t *testing.T) {
	defer goleak.VerifyNone(t)

	testCases := []struct {
		name    string
		a       []string
		b       []string
		new     []string
		removed []string
	}{
		{"both empty", []string{}, []string{}, []string{}, []string{}},
		{"empty a", []string{}, []string{"v"}, []string{"v"}, []string{}},
		{"equal", []string{"v"}, []string{"v"}, []string{}, []string{}},
		{"removed", []string{"v"}, []string{}, []string{}, []string{"v"}},
		{"new and removed", []string{"v"}, []string{"v2"}, []string{"v2"}, []string{"v"}},
		{"complicated", []string{"v1", "v2"}, []string{"v2", "v3"}, []string{"v3"}, []string{"v1"}},
	}

	for _, testCase := range testCases {
		t.Run(testCase.name, func(t *testing.T) {
			aMap := task.ConvertToMap(testCase.a)
			bMap := task.ConvertToMap(testCase.b)
			newValues, removed := task.DetectChanges(aMap, bMap)
			assert.Equal(t, testCase.new, newValues)
			assert.Equal(t, testCase.removed, removed)
		})
	}
}
