package writer

import (
	"testing"
	"time"
)

func TestRateLimiter_Firing(t *testing.T) {
	r := newRateLimiter(100, time.Second*5)
	defer r.Stop()
	r.Record(1)
	r.Record(502)
	tC := time.After(time.Millisecond * 5100)
	stop := false
	for !stop {
		select {
		case <-r.Firing():
			stop = true
		case <-tC:
			t.Fatal("not firing after timeout")
		default:
		}
	}
	select {
	case <-r.Firing():
	default:
		t.Fatal("should always be firing")
	}

	<-time.After(time.Millisecond * 5200)
	select {
	case <-r.Firing():
		t.Fatal("should not be firing")
	default:
	}
}

func TestRateLimiter_Record(t *testing.T) {
	r := &rateLimiter{
		closing: make(chan struct{}),
		input:   make(chan uint64),
	}
	go func() {
		<-time.After(time.Second / 10)
		r.Stop()
	}()

	recorded := make(chan struct{})
	go func() {
		<-time.After(time.Second / 2)
		r.Record(1)
		close(recorded)
	}()
	select {
	case <-recorded:
	case <-time.After(time.Second):
		t.Fatal("record not finished")
	}
}
