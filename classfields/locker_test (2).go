package locker

import (
	"errors"
	"github.com/YandexClassifieds/go-common/conf"
	"github.com/YandexClassifieds/go-common/conf/viper"
	"math/rand"
	"os"
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/locker"
	"github.com/hashicorp/consul/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	r           = rand.New(rand.NewSource(time.Now().UnixNano()))
	serviceName = RandString(10)
	name1       = "name1"
	name2       = "name2"
)

func TestLockUnlock(t *testing.T) {
	l := newLocker(t)
	// test
	lock, err := l.Lock(name1)
	require.NoError(t, err)
	require.NoError(t, l.Unlock(lock))
	lock, err = l.Lock(name1)
	require.NoError(t, err)
	require.NoError(t, l.Unlock(lock))
	// clean
	l.Stop()
}

// TODO prepare for CI
func newLocker(t *testing.T) *Locker {
	require.NoError(t, os.Setenv("_DEPLOY_SERVICE_NAME", "common_ci/"+serviceName))
	require.NoError(t, os.Setenv("_DEPLOY_ALLOC_ID", RandString(5)))
	var params []Option
	confS := viper.NewTestConf()
	params = append(params, WithNamespace("go_common/"+serviceName+"/"+t.Name()))
	if os.Getenv("CI") != "" {
		params = append(params, WithToken(conf.Str("CONSUL_API_TOKEN")))
		params = append(params, WithAddress(conf.Str("CONSUL_ADDRESS")))
		params = append(params, WithConf(confS))
	} else {
		params = append(params, WithAddress("localhost:8500"))
	}
	init, err := NewLocker(params...)
	require.NoError(t, err)
	return init
}

func TestDoubleLock(t *testing.T) {
	l := newLocker(t)
	// test
	lock, err := l.Lock(name1)
	require.NoError(t, err)
	_, err = l.Lock(name1)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	// clean
	require.NoError(t, l.Unlock(lock))
}

func TestDistributedDoubleLock(t *testing.T) {
	l := newLocker(t)
	l2 := newLocker(t)
	// test
	lock, err := l.Lock(name1)
	require.NoError(t, err)
	_, err = l2.Lock(name1)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	// clean
	require.NoError(t, l.Unlock(lock))
}

func TestDoubleLockByOtherContextValue(t *testing.T) {
	l := newLocker(t)
	// test
	lock, err := l.Lock(name1)
	require.NoError(t, err)
	_, err = l.Lock(name1)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	// clean
	require.NoError(t, l.Unlock(lock))
}

func TestDoubleUnlock(t *testing.T) {
	l := newLocker(t)
	lock, err := l.Lock(name1)
	require.NoError(t, err) // test
	require.NoError(t, l.Unlock(lock))
	assert.True(t, errors.Is(l.Unlock(lock), locker.ErrNoLock))
	assert.True(t, errors.Is(l.Unlock(lock), locker.ErrNoLock))
}

func TestHashLock(t *testing.T) {
	l := newLocker(t)
	// test
	lock, err := l.Lock(name1)
	require.NoError(t, err)
	assertNoExpired(t, lock)
	require.NoError(t, l.Unlock(lock))
}

func TestDoubleDistributeLock(t *testing.T) {
	locker1 := newLocker(t)
	locker2 := newLocker(t)
	// test
	lock, err := locker1.Lock(name1)
	require.NoError(t, err)
	_, err = locker2.Lock(name1)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	// clean
	require.NoError(t, locker1.Unlock(lock))
}

func TestDistributeLockUnlock(t *testing.T) {
	locker1 := newLocker(t)
	locker2 := newLocker(t)
	// test
	lock, err := locker1.Lock(name1)
	require.NoError(t, err)
	require.NoError(t, locker1.Unlock(lock))
	lock2, err := locker2.Lock(name1)
	require.NoError(t, err)
	require.NoError(t, locker2.Unlock(lock2))
}

func TestGracefulShutdown(t *testing.T) {
	l1 := newLocker(t)
	lock1, err := l1.Lock(name1)
	require.NoError(t, err)
	l2 := newLocker(t)
	lock2, err := l2.Lock(name2)
	require.NoError(t, err)

	l1.Stop()
	// assert GracefulShutdown for lock 1
	select {
	case <-time.NewTimer(5 * time.Second).C:
		assert.FailNow(t, "lock: no expired")
	case <-lock1.Expired():
	}
	// wait 1,5 TTL
	time.Sleep(15 * time.Second)
	_, err = l2.Lock(name1)
	assert.NoError(t, err)

	// assert GracefulShutdown for lock 2
	select {
	case <-time.NewTimer(5 * time.Second).C:
	case <-lock2.Expired():
		assert.FailNow(t, "lock 2: expired")
	}

	l2.Stop()
}
func TestCheckLockByMissLock(t *testing.T) {
	l := newLocker(t)

	lock, err := l.Lock(name1)
	require.NoError(t, err)
	// manual delete lock
	_, err = l.consul.KV().Delete(l.key(name1), &api.WriteOptions{Token: l.options.Token})
	require.NoError(t, err)
	assertExpired(t, lock)
}

func assertExpired(t *testing.T, lock locker.Lock) {
	select {
	case <-time.NewTimer(15 * time.Second).C: // 1,5 TTL
		assert.FailNow(t, "lock expired ", lock.Name())
	case <-lock.Expired():
	}
}

func assertNoExpired(t *testing.T, lock locker.Lock) {
	select {
	case <-time.NewTimer(15 * time.Second).C: // 1,5 TTL
	case <-lock.Expired():
		assert.FailNow(t, "lock expired ", lock.Name())
	}
}

var letterRunes = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

func RandString(n int) string {
	b := make([]rune, n)
	for i := range b {
		b[i] = letterRunes[r.Intn(len(letterRunes))]
	}
	return string(b)
}
