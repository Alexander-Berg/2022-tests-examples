package locker

import (
	"errors"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/logger"
	"github.com/YandexClassifieds/shiva/pkg/i/locker"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/hashicorp/consul/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	namespace = test.RandString(10)
)

var (
	runCtx     = mock.NewContext(common.Run, "prod", "shiva-lock_test", "")
	runCtx2    = mock.NewContext(common.Run, "prod", "shiva-lock_test2", "")
	restartCtx = mock.NewContext(common.Restart, "prod", "shiva-lock_test", "")
	cancelCtx  = mock.NewContext(common.Cancel, "prod", "shiva-lock_test", "")
	d          = time.Second
)

func TestLockUnlock(t *testing.T) {
	test.InitTestEnv()
	locker := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	// test
	lock, err := locker.Lock(runCtx)
	require.NoError(t, err)
	require.NoError(t, locker.Unlock(lock))
	lock, err = locker.Lock(runCtx)
	require.NoError(t, err)
	require.NoError(t, locker.Unlock(lock))
	// clean
	locker.Stop()
}

func newTestConf(t *testing.T) Conf {
	conf := NewConf(namespace)
	conf.ServiceName = "shiva-ci/" + t.Name()
	conf.AllocationID = test.RandString(5)
	return conf
}

func newLocker(log logger.Logger, conf Conf, ctx *mock.MockContext) *Locker {
	return NewLocker(log, conf, ctx)
}

func TestDoubleLock(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	// test
	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	_, err = l.Lock(runCtx)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	// clean
	require.NoError(t, l.Unlock(lock))
}

func TestDistributedDoubleLock(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	l2 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	// test
	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	_, err = l2.Lock(runCtx)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	// clean
	require.NoError(t, l.Unlock(lock))
}

func TestDoubleLockByOtherContextValue(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	// test
	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	_, err = l.Lock(restartCtx)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	// clean
	require.NoError(t, l.Unlock(lock))
}

func TestDoubleUnlock(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	lock, err := l.Lock(runCtx)
	require.NoError(t, err) // test
	require.NoError(t, l.Unlock(lock))
	assert.True(t, errors.Is(l.Unlock(lock), common.ErrNotFound))
	assert.True(t, errors.Is(l.Unlock(lock), common.ErrNotFound))
}

func TestHashLock(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	// test
	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	assertNoExpired(t, lock)
	require.NoError(t, l.Unlock(lock))
}

func TestDoubleDistributeLock(t *testing.T) {
	test.InitTestEnv()

	locker1 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	locker2 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	// test
	lock, err := locker1.Lock(runCtx)
	require.NoError(t, err)
	_, err = locker2.Lock(runCtx)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	// clean
	require.NoError(t, locker1.Unlock(lock))
}

func TestDistributeLockUnlock(t *testing.T) {
	test.InitTestEnv()

	locker1 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	locker2 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	// test
	lock, err := locker1.Lock(runCtx)
	require.NoError(t, err)
	require.NoError(t, locker1.Unlock(lock))
	lock2, err := locker2.Lock(runCtx)
	require.NoError(t, err)
	require.NoError(t, locker2.Unlock(lock2))
}

func TestGracefulShutdown(t *testing.T) {
	test.InitTestEnv()

	l1 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	lock1, err := l1.Lock(runCtx)
	require.NoError(t, err)
	l2 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	lock2, err := l2.Lock(runCtx2)
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
	_, err = l2.Lock(runCtx)
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
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})

	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	// manual delete lock
	_, err = l.consul.KV().Delete(l.key(runCtx), &api.WriteOptions{Token: l.conf.Token})
	require.NoError(t, err)
	assertExpired(t, lock)
}

func assertExpired(t *testing.T, lock locker.Lock) {
	select {
	case <-time.NewTimer(15 * time.Second).C: // 1,5 TTL
		assert.FailNow(t, "lock expired ", lock.GetContext().Name())
	case <-lock.Expired():
	}
}

func assertNoExpired(t *testing.T, lock locker.Lock) {
	select {
	case <-time.NewTimer(15 * time.Second).C: // 1,5 TTL
	case <-lock.Expired():
		assert.FailNow(t, "lock expired ", lock.GetContext().Name())
	}
}

func TestStealLock(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})

	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	newLock, err := l.Steal(cancelCtx)
	require.NoError(t, err)
	assertExpired(t, lock)
	assertNoExpired(t, newLock)

	l.Stop()
}

func TestStealExpiredLock(t *testing.T) {
	test.InitTestEnv()
	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	defer l.Stop()

	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	require.NoError(t, l.Unlock(lock))
	_, err = l.Steal(cancelCtx)
	assert.True(t, errors.Is(err, common.ErrNotFound))
}

func TestStealNoLock(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	_, err := l.Steal(cancelCtx)
	assert.True(t, errors.Is(err, common.ErrNotFound))
	l.Stop()
}

func TestDistributeStealLock(t *testing.T) {
	test.InitTestEnv()

	l1 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	l2 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})

	lock, err := l1.Lock(runCtx)
	require.NoError(t, err)
	newLock, err := l2.Steal(cancelCtx)
	require.NoError(t, err)
	assertExpired(t, lock)
	assertNoExpired(t, newLock)

	l1.Stop()
	l2.Stop()
}

func TestLockAfterDistributeStealLock(t *testing.T) {
	test.InitTestEnv()

	l1 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	l2 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})

	lock, err := l1.Lock(runCtx)
	require.NoError(t, err)
	newLock, err := l2.Steal(cancelCtx)
	require.NoError(t, err)
	_, err = l1.Lock(runCtx)
	assert.Equal(t, locker.ErrLockAlreadyExist, err)
	assertExpired(t, lock)
	assertNoExpired(t, newLock)

	l1.Stop()
	l2.Stop()
}

func TestUnlockAfterDistributeStealLock(t *testing.T) {
	test.InitTestEnv()

	l1 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	l2 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})

	lock, err := l1.Lock(runCtx)
	require.NoError(t, err)
	newLock, err := l2.Steal(cancelCtx)
	require.NoError(t, err)
	err = l1.Unlock(lock)
	assert.True(t, errors.Is(err, common.ErrNotFound), err)
	assertExpired(t, lock)
	assertNoExpired(t, newLock)

	l1.Stop()
	l2.Stop()
}

func TestStealLockByNotAllowType(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})

	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	_, err = l.Steal(restartCtx)
	require.Equal(t, locker.ErrLockNotAllow, err)
	assertNoExpired(t, lock)
	l.Stop()
}

func TestDoubleStealLock(t *testing.T) {
	test.InitTestEnv()

	l := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})

	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	newLock, err := l.Steal(cancelCtx)
	require.NoError(t, err)
	assertExpired(t, lock)
	assertNoExpired(t, newLock)

	_, err = l.Steal(cancelCtx)
	require.Equal(t, locker.ErrLockAlreadyExist, err)
	l.Stop()
}

func TestDoubleDistributeStealLock(t *testing.T) {
	test.InitTestEnv()

	l1 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	l2 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})
	l3 := newLocker(test.NewLogger(t), newTestConf(t), &mock.MockContext{})

	lock, err := l1.Lock(runCtx)
	require.NoError(t, err)
	newLock, err := l2.Steal(cancelCtx)
	require.NoError(t, err)
	assertExpired(t, lock)
	assertNoExpired(t, newLock)

	_, err = l3.Steal(cancelCtx)
	require.Equal(t, locker.ErrLockAlreadyExist, err)
	l1.Stop()
	l2.Stop()
	l3.Stop()
}
