package deployment

import (
	"fmt"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/context"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/i/locker"
	"github.com/stretchr/testify/assert"
)

var errCast = fmt.Errorf("Test err: fail cast context to deployment lock context ")

type Lock struct {
	expiredChan chan struct{}
	ctx         *context.LockContext
}

func (l *Lock) IsExpired() bool {
	select {
	case <-l.Expired():
		return true
	default:
		return false
	}
}

func (l *Lock) Expired() <-chan struct{} {
	return l.expiredChan
}

func (l *Lock) GetContext() locker.Context {
	return l.ctx
}

type DMockLocker struct {
	mx            sync.Mutex
	LockInc       int
	LockIncChan   chan bool
	LockFailInc   int
	UnlockInc     int
	UnlockIncChan chan bool
	HasLockInc    int
	StealLockInc  int
	StealIncChan  chan bool
	Locked        map[string]*Lock
	NotControl    map[string]common.Type
	lockMutex     sync.Mutex
	Close         chan bool
}

func (m *DMockLocker) AddNotControl(lockCtx locker.Context) error {
	m.mx.Lock()
	defer m.mx.Unlock()

	ctx, ok := lockCtx.(*context.LockContext)
	if !ok {
		return errCast
	}

	m.NotControl[m.makeKey(ctx)] = ctx.Type
	return nil
}

func (m *DMockLocker) DeleteNotControl(lockCtx locker.Context) error {
	m.mx.Lock()
	defer m.mx.Unlock()

	ctx, ok := lockCtx.(*context.LockContext)
	if !ok {
		return errCast
	}

	delete(m.NotControl, m.makeKey(ctx))
	return nil
}

func (m *DMockLocker) Lock(lockCtx locker.Context) (locker.Lock, error) {
	m.mx.Lock()
	defer m.mx.Unlock()

	ctx, ok := lockCtx.(*context.LockContext)
	if !ok {
		return nil, errCast
	}

	key := m.makeKey(ctx)
	m.lockMutex.Lock()
	defer m.lockMutex.Unlock()
	if _, ok := m.NotControl[key]; ok {
		m.LockFailInc++
		m.LockIncChan <- true
		return nil, locker.ErrLockAlreadyExist
	}
	if _, ok := m.Locked[key]; ok {
		m.LockFailInc++
		m.LockIncChan <- true
		return nil, locker.ErrLockAlreadyExist
	}

	lock := &Lock{
		ctx:         ctx,
		expiredChan: make(chan struct{}, 1),
	}

	m.Locked[key] = lock
	m.LockInc++
	m.LockIncChan <- true
	return lock, nil
}

func (m *DMockLocker) Unlock(lock locker.Lock) error {
	m.mx.Lock()
	defer m.mx.Unlock()

	ctx, ok := lock.GetContext().(*context.LockContext)
	if !ok {
		return errCast
	}

	key := m.makeKey(ctx)
	m.lockMutex.Lock()
	defer m.lockMutex.Unlock()
	if lock, ok := m.Locked[key]; !ok || lock.ctx.Type != ctx.Type {
		return common.ErrNotFound
	}
	delete(m.Locked, key)
	m.UnlockInc++
	m.UnlockIncChan <- true
	return nil
}

func (m *DMockLocker) Steal(lockCtx locker.Context) (locker.Lock, error) {
	m.mx.Lock()
	defer m.mx.Unlock()

	ctx, ok := lockCtx.(*context.LockContext)
	if !ok {
		return nil, errCast
	}

	m.lockMutex.Lock()
	defer m.lockMutex.Unlock()
	key := m.makeKey(ctx)
	defer func() {
		m.StealIncChan <- true
	}()
	if dtype, ok := m.NotControl[key]; ok {

		if !ctx.AllowSteal(&context.LockContext{
			Type:        dtype,
			Layer:       ctx.Layer,
			ServiceName: ctx.ServiceName,
			Branch:      ctx.Branch,
		}) {
			m.LockFailInc++
			return nil, locker.ErrLockNotAllow
		}
		delete(m.NotControl, key)
	}
	lock, ok := m.Locked[key]
	if !ok {
		return nil, common.ErrNotFound
	}

	if !ctx.AllowSteal(&context.LockContext{
		Type:        lock.ctx.Type,
		Layer:       ctx.Layer,
		ServiceName: ctx.ServiceName,
		Branch:      ctx.Branch,
	}) {
		m.LockFailInc++
		return nil, locker.ErrLockNotAllow
	}

	lock.expiredChan <- struct{}{}
	delete(m.Locked, key)

	lock = &Lock{
		expiredChan: make(chan struct{}, 1),
		ctx:         ctx,
	}

	m.Locked[key] = lock
	m.StealLockInc++
	return lock, nil
}

func NewDMockLocker() *DMockLocker {

	locker := &DMockLocker{
		Locked:        map[string]*Lock{},
		lockMutex:     sync.Mutex{},
		NotControl:    map[string]common.Type{},
		UnlockIncChan: make(chan bool, 25),
		LockIncChan:   make(chan bool, 25),
		StealIncChan:  make(chan bool, 25),
		Close:         make(chan bool),
	}
	return locker
}

func (m *DMockLocker) makeKey(ctx *context.LockContext) string {
	return strings.Join([]string{ctx.Layer, ctx.ServiceName, strings.ToLower(ctx.Branch)}, "_")
}

func (m *DMockLocker) Closed() chan bool {
	return m.Close
}

func (m *DMockLocker) Wait(t *testing.T, c <-chan bool, expected int) {
	count := 0
	timer := time.NewTimer(5 * time.Second)
	defer timer.Stop()
	for {
		select {
		case <-c:
			count++
			if count == expected {
				return
			}
		case <-timer.C:
			assert.FailNow(t, "Waiting timeout for lock counter")
		}
	}
}

func (m *DMockLocker) Stop() {
}

func (m *DMockLocker) RemoveLocksAndReconnect() {
	for _, l := range m.Locked {
		close(l.expiredChan)
	}
	m.Locked = map[string]*Lock{}
}
