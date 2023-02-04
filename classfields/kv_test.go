package kv

import (
	"encoding/json"
	"errors"
	"github.com/YandexClassifieds/go-common/conf"
	"github.com/YandexClassifieds/go-common/conf/viper"
	"math/rand"
	"os"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/kv"
	"github.com/hashicorp/consul/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

var (
	r           = rand.New(rand.NewSource(time.Now().UnixNano()))
	serviceName = RandString(10)
)

func TestEphemeralKV_Create(t *testing.T) {
	key := RandString(5)
	KV := newKV(t, serviceName)
	err := KV.Create(key, &TestValue{StrField: "v1"})
	require.NoError(t, err)
	err = KV.Create(key, &TestValue{StrField: "v2"})
	require.Error(t, err)
	require.Equal(t, kv.ErrKeyExists, err)
}

func TestSaveGet(t *testing.T) {
	key := RandString(5)
	value := RandString(10)
	KV := newKV(t, serviceName)
	require.NoError(t, KV.Save(key, &TestValue{StrField: value}))
	get, err := KV.Get(key)
	require.NoError(t, err)
	result, ok := get.(*TestValue)
	require.True(t, ok)
	assert.Equal(t, value, result.StrField)
}

func TestSaveGetAll(t *testing.T) {
	testData := map[string]string{
		RandString(5): RandString(10),
		RandString(5): RandString(10),
		RandString(5): RandString(10),
	}

	KV := newKV(t, serviceName)
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
	key := RandString(5)
	value := RandString(10)
	KV := newKV(t, serviceName)
	KV2 := newKV(t, serviceName)
	require.NoError(t, KV.Save(key, &TestValue{StrField: value}))
	value2 := RandString(10)
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
	key := RandString(5)
	KV := newKV(t, serviceName)
	_, err := KV.Get(key)
	require.True(t, errors.Is(err, kv.ErrNotFound))
}

func TestAllEmpty(t *testing.T) {
	KV := newKV(t, serviceName)
	all, err := KV.All()
	require.NoError(t, err)
	require.Len(t, all, 0)
}

func TestTTL(t *testing.T) {
	createKey := RandString(5)
	updateKey := RandString(5)
	value := RandString(10)
	KV := newKV(t, serviceName)
	require.NoError(t, KV.Save(updateKey, &TestValue{StrField: value}))
	time.Sleep(7 * time.Second) // 0.7 TTL
	_, err := KV.Get(updateKey)
	require.NoError(t, err)
	require.NoError(t, KV.Save(updateKey, &TestValue{StrField: value}))

	time.Sleep(7 * time.Second) // 0.7 TTL
	_, err = KV.Get(updateKey)
	require.NoError(t, err)
	_, err = KV.Get(createKey)
	require.True(t, errors.Is(err, kv.ErrNotFound))

	time.Sleep(14 * time.Second) // 1,4 TTL
	_, err = KV.Get(updateKey)
	require.True(t, errors.Is(err, kv.ErrNotFound))
}

func TestEphemeralKV_Save_Race(t *testing.T) {
	k := newKV(t, serviceName)

	testKey := RandString(5)
	require.NoError(t, k.Save(testKey, &TestValue{StrField: "initial"}))
	v1 := &TestValue{StrField: RandString(10)}
	v2 := &TestValue{StrField: RandString(10)}

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

var letterRunes = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

func RandString(n int) string {
	b := make([]rune, n)
	for i := range b {
		b[i] = letterRunes[r.Intn(len(letterRunes))]
	}
	return string(b)
}

func newKV(t *testing.T, name string) *EphemeralKV {
	require.NoError(t, os.Setenv("_DEPLOY_SERVICE_NAME", "common_ci/"+serviceName))
	require.NoError(t, os.Setenv("_DEPLOY_ALLOC_ID", RandString(5)))
	var params []Option
	confS := viper.NewTestConf()
	params = append(params, WithNamespace("go_common/"+name+"/"+t.Name()))
	if os.Getenv("CI") != "" {
		params = append(params, WithToken(conf.Str("CONSUL_API_TOKEN")))
		params = append(params, WithAddress(conf.Str("CONSUL_ADDRESS")))
		params = append(params, WithConf(confS))
	} else {
		params = append(params, WithAddress("localhost:8500"))
	}
	result, err := NewEphemeralKV(&TestValue{}, params...)
	require.NoError(t, err)
	return result
}
