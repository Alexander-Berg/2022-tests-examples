package sync

import (
	"testing"
	"time"

	kv2 "github.com/YandexClassifieds/shiva/pkg/consul/kv"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	namespace = test.RandString(10)
)

func Test(t *testing.T) {
	test.InitTestEnv()
	KV := kv2.NewEphemeralKV(test.NewLogger(t), newTestConf(t), &Value{})
	service := NewService(test.NewLogger(t), KV, NewConf(t.Name()))
	now := time.Now()
	require.NoError(t, service.SetLastUpdate(now.Add(-time.Minute)))
	result := make(chan error)
	go func() {
		result <- service.WaitForUpdate()
	}()
	time.Sleep(service.conf.RetryDuration * 2)
	require.NoError(t, service.SetLastUpdate(now.Add(time.Minute)))
	err := <-result
	require.NoError(t, err)
}

func TestErrTimeout(t *testing.T) {
	test.InitTestEnv()
	conf := NewConf(t.Name())
	conf.WaitDuration = 500 * time.Millisecond
	KV := kv2.NewEphemeralKV(test.NewLogger(t), newTestConf(t), &Value{})
	service := NewService(test.NewLogger(t), KV, NewConf(t.Name()))
	require.NoError(t, service.SetLastUpdate(time.Now().Add(-time.Minute)))
	result := make(chan error)
	go func() {
		result <- service.WaitForUpdate()
	}()
	err := <-result
	assert.Equal(t, ErrTimeout, err)
}

func newTestConf(t *testing.T) kv2.Conf {
	conf := kv2.NewConf(namespace)
	conf.ServiceName = "shiva-ci/" + t.Name()
	conf.AllocationID = test.RandString(5)
	return conf
}
