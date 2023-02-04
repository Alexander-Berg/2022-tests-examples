package deployment

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/context"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/i/locker"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	runCtx    = &context.LockContext{Type: common.Run, Layer: "prod", ServiceName: "shiva-lock_test"}
	cancelCtx = &context.LockContext{Type: common.Cancel, Layer: "prod", ServiceName: "shiva-lock_test"}
)

func TestStealLock(t *testing.T) {
	l := NewDMockLocker()

	lock, err := l.Lock(runCtx)
	require.NoError(t, err)
	newLock, err := l.Steal(cancelCtx)
	require.NoError(t, err)
	assertExpired(t, lock)
	assertNoExpired(t, newLock)

	l.Stop()
}

func assertExpired(t *testing.T, lock locker.Lock) {
	select {
	case <-time.NewTimer(2 * time.Second).C:
		assert.FailNow(t, "lock expired ", lock.GetContext().Name())
	case <-lock.Expired():
	}
}

func assertNoExpired(t *testing.T, lock locker.Lock) {
	select {
	case <-time.NewTimer(2 * time.Second).C:
	case <-lock.Expired():
		assert.FailNow(t, "lock expired ", lock.GetContext().Name())
	}
}
