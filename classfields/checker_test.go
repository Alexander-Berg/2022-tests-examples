package health

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/log"
	"github.com/stretchr/testify/assert"
)

func TestEmptyChecker(t *testing.T) {
	svc := NewChecker(log.SilenceLogger, time.Second, []Pingable{})
	assert.True(t, svc.Healthy())
	ctx, cancel := context.WithCancel(context.Background())
	wc := svc.Watch(ctx)
	// init no data on same status
	svc.check()
	select {
	case <-wc:
		assert.FailNow(t, "expected to have no status change")
	default:
	}

	cancel()
	assert.Eventually(t, func() bool {
		if len(svc.watcher.updates) != 0 {
			return false
		}
		return true
	}, 5*time.Second, 250*time.Millisecond)
}

func TestChecker_Watch(t *testing.T) {
	p := &Ping{status: false}
	svc := NewChecker(log.SilenceLogger, time.Hour, []Pingable{p})
	ctx, cancel := context.WithCancel(context.Background())
	wc := svc.Watch(ctx)
	// init no data on same status
	svc.check()
	select {
	case <-wc:
		assert.FailNow(t, "expected to have no status change")
	default:
	}

	p.status = true
	svc.check()
	select {
	case v := <-wc:
		assert.True(t, v)
	default:
		assert.FailNow(t, "expected status, got nothing")
	}

	// check no data on same status
	svc.check()
	select {
	case <-wc:
		assert.FailNow(t, "expected to have no status change")
	default:
	}

	p.status = false
	svc.check()
	select {
	case v := <-wc:
		assert.False(t, v)
	default:
		assert.FailNow(t, "expected status, got nothing")
	}

	cancel()
	assert.Eventually(t, func() bool {
		if len(svc.watcher.updates) != 0 {
			return false
		}
		return true
	}, 5*time.Second, 250*time.Millisecond)
}

type Ping struct {
	status bool
}

func (p *Ping) Name() string {
	return "mock"
}

func (p *Ping) Ping(ctx context.Context) error {
	if p.status {
		return nil
	}
	return fmt.Errorf("fail")
}
