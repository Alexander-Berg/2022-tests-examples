package lock

import (
	"runtime"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestFirstTimeLocked(t *testing.T) {
	var lock Try
	assert.True(t, lock.TryLock(), "first time we should be able to lock")
}

func TestSecondTimeNotLocked(t *testing.T) {
	var lock Try
	lock.TryLock()
	assert.False(t, lock.TryLock(), "second time we should not be able to lock")
}

func TestLockAfterUnlock(t *testing.T) {
	var lock Try
	lock.TryLock()
	lock.Unlock()
	assert.True(t, lock.TryLock(), "after unlock we should be able to lock")
}

func TestFirstUnlockNoEffect(t *testing.T) {
	var lock Try
	lock.Unlock()
	assert.True(t, lock.TryLock(), "unlock shouldn't prevent first lock")
}

func TestProgress(t *testing.T) {
	var lock Try

	var wg sync.WaitGroup
	for i := 0; i < 1000; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for !lock.TryLock() {
				runtime.Gosched()
			}
			lock.Unlock()
		}()
	}

	const deadline = time.Second

	doneCh := make(chan struct{})
	go func() {
		wg.Wait()
		close(doneCh)
	}()

	select {
	case <-doneCh:
	case <-time.After(deadline):
		require.Fail(t, "deadlock in test detected")
	}
}
