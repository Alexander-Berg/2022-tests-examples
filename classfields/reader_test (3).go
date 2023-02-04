package reader

import (
	"context"
	"fmt"
	"sync"
	"syscall"
	"testing"
	"time"

	"github.com/YandexClassifieds/logs/api/collector"
	"github.com/YandexClassifieds/logs/pkg/agent/writer"
	"github.com/YandexClassifieds/logs/pkg/agent/writer/metrics"
	"github.com/containerd/fifo"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
)

func TestReader_Buffer(t *testing.T) {
	metrics.Init("test")
	tmpDir := t.TempDir()
	stdout := tmpDir + "/stdout"
	stderr := tmpDir + "/stderr"
	require.NoError(t, syscall.Mkfifo(stdout, 0777))
	require.NoError(t, syscall.Mkfifo(stderr, 0777))

	ro, err := fifo.OpenFifo(context.Background(), stdout, syscall.O_RDONLY|syscall.O_NONBLOCK, 0)
	require.NoError(t, err)
	re, err := fifo.OpenFifo(context.Background(), stderr, syscall.O_RDONLY|syscall.O_NONBLOCK, 0)
	require.NoError(t, err)

	rec := newLogRecorder()
	w := writer.NewWriter(writer.Config{
		BufferSize:      1000,
		BatchSize:       10,
		BatchWorkers:    1,
		FlushInterval:   time.Second,
		RateLimitWindow: time.Second * 5,
		CountRateLimit:  15000,
		SizeRateLimit:   64 << 20,
	}, rec, logrus.StandardLogger())
	w.Init()
	rdr := NewReader(ro, re, w)
	rdr.Init()

	<-time.After(time.Second)
	wo, err := fifo.OpenFifo(context.Background(), stdout, syscall.O_WRONLY|syscall.O_APPEND, 0)
	require.NoError(t, err)
	for i := 0; i < 10000; i++ {
		_, err := fmt.Fprintf(wo, `{"_message":"make buffer a little bigger","ctr":%d}`+"\n", i)
		require.NoError(t, err)
	}
	wo.Close()
	<-time.After(time.Second * 10)
	w.Stop()
	require.Equal(t, 10000, len(rec.Data), "data length check failed")
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
