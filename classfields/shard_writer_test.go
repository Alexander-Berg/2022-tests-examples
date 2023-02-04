package clickHouse

import (
	"fmt"
	"github.com/YandexClassifieds/logs/cmd/golf/test"
	"github.com/YandexClassifieds/logs/pkg/config"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"testing"
	"time"
)

func TestNewShardWriter(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	w := shardWriter(t, &mockedWriter{})
	require.Equal(t, w.State(), Active)

	stmtArgs := stmtArgsFixture(t)
	err := w.Write(stmtArgs)
	require.NoError(t, err)

	err = w.Close()
	require.NoError(t, err)
}

func TestWriteFirstInactive(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	badWriter := &mockedWriter{state: Inactive}
	w := shardWriter(t, badWriter, &mockedWriter{})
	require.Equal(t, w.State(), Active)

	stmtArgs := stmtArgsFixture(t)
	err := w.Write(stmtArgs)
	require.NoError(t, err)

	err = w.Close()
	require.NoError(t, err)
}

func TestWriteBothInactive(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	badWriter := &mockedWriter{state: Inactive}
	badWriter2 := &mockedWriter{state: Inactive}
	w := shardWriter(t, badWriter, badWriter2)
	require.Equal(t, w.State(), Inactive)

	stmtArgs := stmtArgsFixture(t)
	err := w.Write(stmtArgs)
	require.Error(t, err)
	require.Equal(t, ErrNoAvailableWriters, err)

	err = w.Close()
	require.NoError(t, err)
}

func TestStateChange(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	writer1 := mockedWriter{}
	writer2 := mockedWriter{}
	w := shardWriter(t, &writer1, &writer2)
	require.Equal(t, w.State(), Active)

	writer1.SetState(Inactive)
	require.Equal(t, Active, w.State())
	select {
	case <-w.StateChange():
		require.FailNow(t, "unexpected status change in channel")
	case <-time.After(time.Second / 10):
	}

	writer2.SetState(Inactive)
	require.Equal(t, Inactive, w.State())
	select {
	case state := <-w.StateChange():
		require.Equal(t, Inactive, state)
	case <-time.After(time.Second):
		require.FailNow(t, "failed to get state change")
	}

	writer2.SetState(Active)
	require.Equal(t, Active, w.State())
	select {
	case state := <-w.StateChange():
		require.Equal(t, Active, state)
	case <-time.After(time.Second):
		require.FailNow(t, "failed to get state change")
	}

	writer1.SetState(Active)
	require.Equal(t, Active, w.State())
	select {
	case <-w.StateChange():
		require.FailNow(t, "unexpected status change in channel")
	case <-time.After(time.Second / 10):
	}

	err := w.Close()
	require.NoError(t, err)
}

func shardWriter(t *testing.T, writers ...IWriter) *ShardWriter {
	w := NewShardWriter(test.NewTestLogger())
	w.writerFactory = factory(t, writers...)

	shards := make([]string, 0)
	for i := 0; i < len(writers); i++ {
		shards = append(shards, config.Str("ch_hosts"))
	}
	err := w.Init(shards)
	require.NoError(t, err)

	return w
}

func factory(t *testing.T, writers ...IWriter) WriterFactory {
	i := 0
	return func() IWriter {
		if i >= len(writers) {
			require.FailNowf(t, "unexpected factory call", "unexpected %d factory call", i)
		}
		w := writers[i]
		i++
		return w
	}
}

type mockedWriter struct {
	state      State
	stateCh    chan State
	writeCalls int
}

func (f *mockedWriter) Init(_ []string) error {
	f.stateCh = make(chan State, 0)
	return nil
}

func (f *mockedWriter) Write(_ []*StmtArgs) error {
	f.writeCalls++
	if f.state == Inactive {
		return fmt.Errorf("error on write")
	}

	return nil
}

func (f *mockedWriter) State() State {
	return f.state
}

func (f *mockedWriter) StateChange() <-chan State {
	return f.stateCh
}

func (f *mockedWriter) Close() error {
	close(f.stateCh)
	return nil
}

func (f *mockedWriter) SetState(state State) {
	f.state = state
	select {
	case f.stateCh <- state:
	case <-time.After(time.Second):
		panic("failed to push state to channel")
	}
}
