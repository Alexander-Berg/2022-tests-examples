package writer

import (
	"context"
	"github.com/YandexClassifieds/logs/api/collector"
	"github.com/YandexClassifieds/logs/pkg/agent/writer/metrics"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc"
	"sync"
	"testing"
	"time"
)

func TestWriter_Stop(t *testing.T) {
	metrics.Init("test")
	logrus.SetFormatter(&logrus.TextFormatter{
		FullTimestamp:   true,
		DisableColors:   true,
		TimestampFormat: time.RFC3339Nano,
	})
	rec := newLogRecorder()
	cfg := Config{
		BufferSize:      10,
		BatchSize:       2,
		BatchWorkers:    2,
		FlushInterval:   time.Second,
		RateLimitWindow: time.Second,
		CountRateLimit:  100,
		SizeRateLimit:   100,
	}
	w := NewWriter(cfg, rec, logrus.StandardLogger())
	w.Init()
	for i := 0; i < 5; i++ {
		w.Add(&collector.LogRow{RawJson: []byte(`{}`)})
	}

	closeComplete := make(chan struct{})
	go func() {
		w.Stop()
		close(closeComplete)
	}()
	select {
	case <-closeComplete:
	case <-time.After(time.Second * 5):
		t.Fatal("close wait timed out")
	}
	assert.Len(t, rec.Data, 5)
}

type logRecorder struct {
	Data []*collector.LogRow
	mu   sync.Mutex
}

func (l *logRecorder) BatchWrite(ctx context.Context, in *collector.BatchRequest, opts ...grpc.CallOption) (*collector.BatchResponse, error) {
	l.mu.Lock()
	defer l.mu.Unlock()
	for _, v := range in.GetData() {
		l.Data = append(l.Data, v)
	}
	return &collector.BatchResponse{}, nil
}

func newLogRecorder() *logRecorder {
	recorder := &logRecorder{
		Data: make([]*collector.LogRow, 0),
	}
	return recorder
}
