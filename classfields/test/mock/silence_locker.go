package mock

import (
	"fmt"
	"sync"

	"github.com/YandexClassifieds/shiva/pkg/i/locker"
)

type LockContext struct {
}

func (l LockContext) New() locker.Context {
	panic("implement me")
}

func (l LockContext) Name() string {
	panic("implement me")
}

func (l LockContext) Compare(context locker.Context) bool {
	panic("implement me")
}

func (l LockContext) AllowSteal(context locker.Context) bool {
	panic("implement me")
}

func (l LockContext) Marshal() ([]byte, error) {
	panic("implement me")
}

func (l LockContext) Unmarshal(bytes []byte) error {
	panic("implement me")
}

type Lock struct {
	lc locker.Context
}

func (l *Lock) IsExpired() bool {
	return false
}

func (l *Lock) Expired() <-chan struct{} {
	return nil
}

func (l *Lock) GetContext() locker.Context {
	return LockContext{}
}

type SilentLocker struct {
	c        chan struct{}
	contexts map[string]locker.Context
	mx       sync.Mutex
}

func NewMockSilentLocker() *SilentLocker {
	return &SilentLocker{
		c:        make(chan struct{}),
		contexts: make(map[string]locker.Context),
	}
}

func (s *SilentLocker) Lock(lc locker.Context) (locker.Lock, error) {
	s.mx.Lock()
	defer s.mx.Unlock()

	l := &Lock{
		lc: lc,
	}
	s.contexts[lc.Name()] = lc
	return l, nil
}

func (s *SilentLocker) Unlock(li locker.Lock) error {
	s.mx.Lock()
	defer s.mx.Unlock()

	l, ok := li.(*Lock)
	if !ok {
		return fmt.Errorf("wrong lock type %T", li)
	}
	delete(s.contexts, l.lc.Name())
	return nil
}

func (s *SilentLocker) Steal(other locker.Context) (locker.Lock, error) {
	s.mx.Lock()
	defer s.mx.Unlock()

	cur, exists := s.contexts[other.Name()]
	if exists && cur.Compare(other) {
		return nil, locker.ErrLockAlreadyExist
	}
	s.contexts[other.Name()] = other

	return &Lock{
		lc: other,
	}, nil
}

func (s *SilentLocker) Stop() {
	close(s.c)
}
