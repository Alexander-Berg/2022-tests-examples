package clickHouse

import (
	"fmt"
	"github.com/YandexClassifieds/logs/cmd/golf/domain"
	"github.com/YandexClassifieds/logs/cmd/golf/test"
	"github.com/YandexClassifieds/logs/pkg/config"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"testing"
	"time"
)

func TestBufferOverflowTrigger(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_size", 1)

	w := distributedWriter(t, &mockedWriter{})
	defer w.Shutdown()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	requireKeyCommitted(t, w, key)
}

func TestBufferFullTrigger(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_size", 2)

	w := distributedWriter(t, &mockedWriter{})
	defer w.Shutdown()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	requireKeyCommitted(t, w, key)
}

func TestTimerTrigger(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_time", "1s")

	w := distributedWriter(t, &mockedWriter{})
	defer w.Shutdown()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	requireKeyCommitted(t, w, key)
}

func TestFlushOnClose(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	w := distributedWriter(t, &mockedWriter{})
	require.False(t, w.Closed())

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	requireNoCommit(t, w)

	go requireKeyCommitted(t, w, key)
	w.Shutdown()
	require.True(t, w.Closed())
}

func TestClose(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	w := distributedWriter(t, &mockedWriter{})
	require.False(t, w.Closed())

	w.Shutdown()
	require.True(t, w.Closed())
}

func TestSkipInvalidRow(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_size", 1)

	invalidData := []domain.Row{
		{
			{Key: domain.TimeK, JsonValue: nil},
			{Key: domain.MessageK, JsonValue: "empty time"},
		},
		{
			{Key: domain.TimeK, JsonValue: "2020-12-01T14:55:00.123+03:00"},
			{Key: domain.MessageK, JsonValue: "ok row"},
		},
	}

	w := distributedWriter(t, &mockedWriter{})
	defer w.Shutdown()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add(invalidData, key)
	require.NoError(t, err)
	requireKeyCommitted(t, w, key)
}

func TestBrokenWriter(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_size", 1)

	w := distributedWriter(t, &mockedWriter{state: Inactive}, &mockedWriter{})
	defer w.Shutdown()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	assertKeyCommitted(t, w, key)
}

func Test1Message2Writers(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_size", 1)

	w := distributedWriter(t, &mockedWriter{}, &mockedWriter{})
	defer w.Shutdown()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add([]domain.Row{rowsFixture(t)[0]}, key)
	require.NoError(t, err)
	assertKeyCommitted(t, w, key)
}

func Test3Messages2Writers(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_size", 1)

	w := distributedWriter(t, &mockedWriter{}, &mockedWriter{})
	defer w.Shutdown()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add([]domain.Row{rowsFixture(t)[0], rowsFixture(t)[1], rowsFixture(t)[0]}, key)
	require.NoError(t, err)
	assertKeyCommitted(t, w, key)
}

func TestWriterRecovered(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_size", 1)

	flapWriter := mockedWriter{}
	w := distributedWriter(t, &flapWriter)

	t.Log("success commit")
	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	expectedWriteCalls := 1
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	assertKeyCommitted(t, w, key)

	t.Log("next write will fail")
	flapWriter.state = Inactive
	expectedWriteCalls++
	err = w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	// wait for flush
	require.Eventually(t, func() bool {
		return w.buffer.rows == 0
	}, time.Second, time.Second/10, "flush not occurred")
	flapWriter.SetState(Inactive) // push state change
	requireNoCommit(t, w)

	t.Log("writer become active")
	expectedWriteCalls++
	flapWriter.SetState(Active)
	assertKeyCommitted(t, w, key)
	w.Shutdown()

	require.Equal(t, expectedWriteCalls, flapWriter.writeCalls, "expected 2 write calls")
}

func TestPipelineWithoutSuccessWrite(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_size", "1")

	mockWriter := &mockedWriter{state: Inactive}
	w := distributedWriter(t, mockWriter)
	defer func() {
		// cleanup after test
		mockWriter.SetState(Active)
		<-w.CommitC()
		w.Shutdown()
	}()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	requireNoCommit(t, w)
}

func TestAddWhileNoAvailableWriters(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_time", "1s")

	mockWriter := &mockedWriter{state: Inactive}
	w := distributedWriter(t, mockWriter)
	defer func() {
		// cleanup after test
		mockWriter.SetState(Active)
		<-w.CommitC()
		<-w.CommitC()
		w.Shutdown()
	}()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
	require.Eventually(t, func() bool {
		return w.buffer.rows == 0
	}, 2*time.Second, time.Second/10, "flush not occurred")

	err = w.Add(rowsFixture(t), key)
	require.NoError(t, err)
}

func TestCalcPartSize(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	tests := []struct {
		rows             int
		writers          int
		availableWriters int
		partSize         int
	}{
		{1, 2, 1, 1},
		{0, 2, 1, 0},
		{0, 2, 0, 0},
		{1, 2, 2, 1},
		{2, 2, 2, 1},
		{3, 2, 2, 2},
		{3, 2, 0, 2},
		{3, 2, 1, 3},
		{20, 2, 2, 10},
	}
	for _, tt := range tests {
		t.Run(fmt.Sprintf("%d-%d-%d", tt.rows, tt.writers, tt.availableWriters), func(t *testing.T) {
			w := distributedWriter(t, &mockedWriter{})
			writer := w.writers[0]
			defer func() {
				w.writers = []IWriter{writer}
				w.Shutdown()
			}()

			w.writers = make([]IWriter, tt.writers)
			w.pickWriterChan = make(chan IWriter, tt.writers)
			for i := 0; i < tt.availableWriters; i++ {
				w.pickWriterChan <- writer
			}
			if got := w.calcPartSize(tt.rows); got != tt.partSize {
				t.Errorf("calcPartSize() = %v, want %v", got, tt.partSize)
			}
		})
	}
}

func TestFlushConcurrentCalls(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_time", "1ns")

	w := distributedWriter(t, &mockedWriter{})
	defer w.Shutdown()

	key := domain.BatchKey{Cookie: 42, SessionID: "test-session"}
	go assertKeyCommitted(t, w, key)
	err := w.Add(rowsFixture(t), key)
	require.NoError(t, err)
}

func TestCloseAndAddConcurrent(t *testing.T) {
	for i := 0; i < 500; i++ {
		t.Run("", testCloseAndAddConcurrent)
	}
}

func testCloseAndAddConcurrent(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()
	viper.SetDefault("ch_buffer_time", "1ns")

	w := distributedWriter(t, &mockedWriter{})
	defer w.Shutdown()

	go func() {
		for range w.CommitC() {
		}
	}()
	var i uint64
	closed := make(chan struct{}, 0)
	go func() {
		for {
			select {
			case <-closed:
				return
			default:
			}
			i++
			key := domain.BatchKey{Cookie: i, SessionID: "test-session"}
			_ = w.Add(rowsFixture(t), key)
		}
	}()
	time.Sleep(time.Millisecond)
	close(closed)
}

func distributedWriter(t *testing.T, writers ...IWriter) *DistributedWriter {
	w := NewDistributedWriter(test.NewTestLogger())
	w.writerFactory = factory(t, writers...)

	shards := make([]Shard, 0)
	for i := 0; i < len(writers); i++ {
		shards = append(shards, Shard{[]string{config.Str("ch_hosts")}})
	}
	err := w.Init(shards)
	require.NoError(t, err)
	require.Len(t, w.pickWriterChan, len(writers), "%d writers have to be initialized", len(writers))

	return w
}

func requireKeyCommitted(t *testing.T, w *DistributedWriter, expectedKey domain.BatchKey) {
	t.Helper()
	require.Eventually(t, func() bool {
		select {
		case key := <-w.CommitC():
			return key == expectedKey
		default:
			return false
		}
	}, 2*time.Second, time.Second/10, "key not committed")
}

func assertKeyCommitted(t *testing.T, w *DistributedWriter, expectedKey domain.BatchKey) {
	t.Helper()
	require.Eventually(t, func() bool {
		select {
		case key := <-w.CommitC():
			return key == expectedKey
		default:
			return false
		}
	}, 2*time.Second, time.Second/10, "key is not committed")
}

func requireNoCommit(t *testing.T, w *DistributedWriter) {
	t.Helper()

	time.Sleep(time.Second)
	select {
	case c := <-w.CommitC():
		require.FailNow(t, "commit was not expected", "%v", c)
	default:
	}
}
