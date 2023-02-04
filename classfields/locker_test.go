package locker

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/pkg/i/locker"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/hashicorp/consul/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	ns = "testNS"
)

func TestLockUnlock(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	l := NewLocker(log, NewConf(ns))

	lock, err := l.Lock(t.Name())
	require.NoError(t, err)
	require.NoError(t, l.Unlock(lock))

	lock, err = l.Lock(t.Name())
	require.NoError(t, err)
	require.NoError(t, l.Unlock(lock))
}

func TestDoubleLock(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	l := NewLocker(log, NewConf(ns))

	lock, err := l.Lock(t.Name())
	require.NoError(t, err)
	_, err = l.Lock(t.Name())
	assert.Equal(t, locker.ErrLockAlreadyExist, err)

	// clean
	require.NoError(t, l.Unlock(lock))
}

func TestDoubleUnlock(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	l := NewLocker(log, NewConf(ns))

	lock, err := l.Lock(t.Name())
	require.NoError(t, err)
	require.NoError(t, l.Unlock(lock))
	assert.Equal(t, locker.ErrNoLock, l.Unlock(lock))
}

func TestDistributedDoubleLock(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	l := NewLocker(log, NewConf(ns))
	l2 := NewLocker(log, NewConf(ns))

	lock, err := l.Lock(t.Name())
	require.NoError(t, err)
	_, err = l2.Lock(t.Name())
	assert.Equal(t, locker.ErrLockAlreadyExist, err)

	// clean
	require.NoError(t, l.Unlock(lock))
}

func TestDistributeLockUnlock(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	l := NewLocker(log, NewConf(ns))
	l2 := NewLocker(log, NewConf(ns))

	lock, err := l.Lock(t.Name())
	require.NoError(t, err)
	require.NoError(t, l.Unlock(lock))
	lock2, err := l2.Lock(t.Name())
	require.NoError(t, err)
	require.NoError(t, l2.Unlock(lock2))
}

func TestCheckLockByMissLock(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	l := NewLocker(log, NewConf(ns))

	lock, err := l.Lock(t.Name())
	require.NoError(t, err)
	// manual delete lock
	_, err = l.consul.KV().Delete(l.key(t.Name()), &api.WriteOptions{Token: l.conf.token})
	require.NoError(t, err)
	assertExpired(t, lock, l)
}

func TestLockNoExpired(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	l := NewLocker(log, NewConf(ns))

	lock, err := l.Lock(t.Name())
	require.NoError(t, err)
	assertNoExpired(t, lock, l)
	require.NoError(t, l.Unlock(lock))
}

func assertExpired(t *testing.T, lock locker.Lock, l *Locker) {
	select {
	case <-time.NewTimer(getMoreThanTTL(l)).C:
		assert.FailNow(t, "lock expired ", lock.GetName())
	case <-lock.Expired():
	}
}

func assertNoExpired(t *testing.T, lock locker.Lock, l *Locker) {
	select {
	case <-time.NewTimer(getMoreThanTTL(l)).C:
	case <-lock.Expired():
		assert.FailNow(t, "lock expired ", lock.GetName())
	}
}

func getMoreThanTTL(l *Locker) time.Duration {
	ttl, err := time.ParseDuration(l.conf.TTL)
	if err != nil {
		panic("can't parse duration from string")
	}

	return 2 * ttl
}
