package kv

import (
	"encoding/json"
	"errors"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/i/kv"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/hashicorp/consul/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

var (
	namespace = test.RandString(10)
)

func TestEphemeralKV_Create(t *testing.T) {
	test.InitTestEnv()
	key := test.RandString(5)
	ekv := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})
	err := ekv.Create(key, &TestValue{StrField: "v1"})
	require.NoError(t, err)
	err = ekv.Create(key, &TestValue{StrField: "v2"})
	require.Error(t, err)
	require.Equal(t, kv.ErrKeyExists, err)
}

func TestSaveGet(t *testing.T) {
	test.InitTestEnv()
	key := test.RandString(5)
	value := test.RandString(10)
	KV := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})
	require.NoError(t, KV.Save(key, &TestValue{StrField: value}))
	get, err := KV.Get(key)
	require.NoError(t, err)
	result, ok := get.(*TestValue)
	require.True(t, ok)
	assert.Equal(t, value, result.StrField)
}

func TestSaveGetAll(t *testing.T) {
	test.InitTestEnv()
	testData := map[string]string{
		test.RandString(5): test.RandString(10),
		test.RandString(5): test.RandString(10),
		test.RandString(5): test.RandString(10),
	}

	KV := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})
	for k, v := range testData {
		require.NoError(t, KV.Save(k, &TestValue{StrField: v}))
	}
	all, err := KV.All()
	require.NoError(t, err)
	require.Len(t, all, 3)
	for key, value := range all {
		result, ok := value.(*TestValue)
		require.True(t, ok)
		expected, ok := testData[key]
		assert.True(t, ok, key)
		assert.Equal(t, expected, result.StrField)
	}
}

func TestUpdate(t *testing.T) {
	test.InitTestEnv()
	key := test.RandString(5)
	value := test.RandString(10)
	KV := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})
	KV2 := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})
	require.NoError(t, KV.Save(key, &TestValue{StrField: value}))
	value2 := test.RandString(10)
	require.NoError(t, KV2.Save(key, &TestValue{StrField: value2}))

	get, err := KV.Get(key)
	require.NoError(t, err)
	result, ok := get.(*TestValue)
	require.True(t, ok)
	assert.Equal(t, value2, result.StrField)

	get, err = KV2.Get(key)
	require.NoError(t, err)
	result, ok = get.(*TestValue)
	require.True(t, ok)
	assert.Equal(t, value2, result.StrField)
}

func TestGetNotFound(t *testing.T) {
	test.InitTestEnv()
	key := test.RandString(5)
	KV := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})
	_, err := KV.Get(key)
	require.True(t, errors.Is(err, common.ErrNotFound))
}

func TestAllEmpty(t *testing.T) {
	test.InitTestEnv()
	KV := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})
	all, err := KV.All()
	require.NoError(t, err)
	require.Len(t, all, 0)
}

func TestTTL(t *testing.T) {
	test.InitTestEnv()
	createKey := test.RandString(5)
	updateKey := test.RandString(5)
	value := test.RandString(10)
	KV := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})
	require.NoError(t, KV.Save(updateKey, &TestValue{StrField: value}))
	time.Sleep(7 * time.Second) // 0.7 TTL
	_, err := KV.Get(updateKey)
	require.NoError(t, err)
	require.NoError(t, KV.Save(updateKey, &TestValue{StrField: value}))

	time.Sleep(7 * time.Second) // 0.7 TTL
	_, err = KV.Get(updateKey)
	require.NoError(t, err)
	_, err = KV.Get(createKey)
	require.True(t, errors.Is(err, common.ErrNotFound))

	time.Sleep(14 * time.Second) // 1,4 TTL
	_, err = KV.Get(updateKey)
	require.True(t, errors.Is(err, common.ErrNotFound))
}

func TestEphemeralKV_Save_Race(t *testing.T) {
	test.InitTestEnv()
	k := NewEphemeralKV(test.NewLogger(t), newTestConf(t), &TestValue{})

	testKey := test.RandString(5)
	require.NoError(t, k.Save(testKey, &TestValue{StrField: "initial"}))
	v1 := &TestValue{StrField: test.RandString(10)}
	v2 := &TestValue{StrField: test.RandString(10)}

	// use kv wrapper for delay on Get
	mkv := &wrapKV{consulKV: k.kv}
	k.kv = mkv

	wg := sync.WaitGroup{}
	wg.Add(2)
	m := sync.Mutex{}
	m.Lock()
	go func() {
		defer wg.Done()
		time.Sleep(time.Second / 2)
		mkv.On("Get", k.prefix()+testKey, mock.Anything).Once()
		err := k.Save(testKey, v1)
		require.NoError(t, err)
		m.Unlock()
	}()
	go func() {
		defer wg.Done()
		mkv.On("Get", k.prefix()+testKey, mock.Anything).Once().Run(func(args mock.Arguments) {
			m.Lock()
		})
		err := k.Save(testKey, v2)
		require.Error(t, err)
	}()
	wg.Wait()

	mkv.On("Get", mock.Anything, mock.Anything).Once()
	v, err := k.Get(testKey)
	require.NoError(t, err)
	vv := v.(*TestValue)
	assert.Equal(t, vv.StrField, v1.StrField, "key data should be equal to value1")
}

func newTestConf(t *testing.T) Conf {
	conf := NewConf(namespace)
	conf.ServiceName = "shiva-ci/" + t.Name()
	conf.AllocationID = test.RandString(5)
	return conf
}

type TestValue struct {
	StrField string `json:"str_field"`
	IntField int    `json:"int_field"`
}

func (t *TestValue) New() kv.Value {
	return &TestValue{}
}

func (t *TestValue) Marshal() ([]byte, error) {
	return json.Marshal(t)
}

func (t *TestValue) Unmarshal(data []byte) error {
	return json.Unmarshal(data, t)
}

type wrapKV struct {
	consulKV
	mock.Mock
}

func (m *wrapKV) Get(key string, q *api.QueryOptions) (*api.KVPair, *api.QueryMeta, error) {
	pair, meta, err := m.consulKV.Get(key, q)
	m.Called(key, q)
	return pair, meta, err
}
