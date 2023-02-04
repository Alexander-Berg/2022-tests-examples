package logrus_test

import (
	"fmt"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/getsentry/sentry-go"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSentryHook_Fire(t *testing.T) {
	tmock := NewTransportMock()
	cli, err := sentry.NewClient(sentry.ClientOptions{
		AttachStacktrace: true,
		Transport:        tmock,
	})
	require.NoError(t, err)

	sentry.CurrentHub().BindClient(cli)
	logger := logrus.NewLogrusLogger(logrus.WithSentry())

	t.Run("WithOutError", func(t *testing.T) {
		logger.WithField("key", "value").Error("IT'S A TEST")
		require.NotEmpty(t, tmock.lastEvent)
		assert.Equal(t, "IT'S A TEST", tmock.lastEvent.Message)
		assert.Equal(t, "TestSentryHook_Fire.func1", tmock.lastEvent.Threads[0].Stacktrace.Frames[0].Function)
	})
	t.Run("WithError", func(t *testing.T) {
		err := fmt.Errorf("error msg")
		logger.WithError(err).Error("IT'S A TEST")
		require.NotEmpty(t, tmock.lastEvent)
		assert.Equal(t, "IT'S A TEST", tmock.lastEvent.Message)
		assert.Equal(t, "TestSentryHook_Fire.func2", tmock.lastEvent.Exception[0].Stacktrace.Frames[0].Function)
		assert.Equal(t, "*errors.errorString", tmock.lastEvent.Exception[0].Type)
		assert.Equal(t, "error msg", tmock.lastEvent.Exception[0].Value)
	})
}

type TransportMock struct {
	mu        sync.Mutex
	events    []*sentry.Event
	lastEvent *sentry.Event
}

func NewTransportMock() *TransportMock {
	return &TransportMock{
		events: make([]*sentry.Event, 0),
	}
}

func (t *TransportMock) Configure(options sentry.ClientOptions) {}
func (t *TransportMock) SendEvent(event *sentry.Event) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.events = append(t.events, event)
	t.lastEvent = event
}

func (t *TransportMock) Flush(timeout time.Duration) bool {
	return true
}

func (t *TransportMock) Events() []*sentry.Event {
	t.mu.Lock()
	defer t.mu.Unlock()
	return t.events
}
