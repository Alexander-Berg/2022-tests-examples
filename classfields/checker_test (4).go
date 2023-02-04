package health

import (
	"context"
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

func TestChecker_Watch(t *testing.T) {
	svc := NewChecker(time.Second)
	ctx, cancel := context.WithCancel(context.Background())
	svc.setHealthyLocked(StatusHealthy)
	wc := svc.Watch(ctx)
	select {
	case v := <-wc:
		assert.Equal(t, StatusHealthy, v)
	default:
		assert.FailNow(t, "expected status, got nothing")
	}

	// check no data on same status
	svc.setHealthyLocked(StatusHealthy)
	select {
	case <-wc:
		assert.FailNow(t, "expected to have no status change")
	default:
	}

	svc.setHealthyLocked(StatusNotHealthy)
	select {
	case v := <-wc:
		assert.Equal(t, StatusNotHealthy, v)
	default:
		assert.FailNow(t, "expected status, got nothing")
	}

	cancel()
	<-time.After(time.Second / 2)
	assert.Len(t, svc.updates, 0, "expected update chan to be scraped")
}
