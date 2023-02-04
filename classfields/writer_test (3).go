package clickHouse

import (
	"database/sql"
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/ClickHouse/clickhouse-go"
	"github.com/YandexClassifieds/logs/cmd/golf/domain"
	"github.com/YandexClassifieds/logs/cmd/golf/test"
	"github.com/YandexClassifieds/logs/pkg/config"
	"github.com/sony/gobreaker"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
)

func TestPipeline(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	db := runUp(t)
	//goland:noinspection GoUnhandledErrorResult
	defer db.Close()

	w := NewWriter(test.NewTestLogger())
	err := w.Init(config.StrSlice("ch_hosts", ","))
	require.NoError(t, err)
	require.NotEqual(t, w.State(), Inactive)

	stmtArgs := stmtArgsFixture(t)
	err = w.Write(stmtArgs)
	require.NoError(t, err)

	test.RequireCount(t, db, 2)

	err = w.Close()
	require.NoError(t, err)
}

func TestDoubleClose(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	w := NewWriter(test.NewTestLogger())
	err := w.Init(config.StrSlice("ch_hosts", ","))
	require.NoError(t, err)
	require.NotEqual(t, w.State(), Inactive)

	err = w.Close()
	require.NoError(t, err)
	err = w.Close()
	require.NoError(t, err)
}

func TestBreaker(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	db := runUp(t)
	//goland:noinspection GoUnhandledErrorResult
	defer db.Close()
	stmtArgs := stmtArgsFixture(t)

	w := NewWriter(test.NewTestLogger())
	go func() {
		// read all state changes
		for range w.StateChange() {
		}
	}()
	err := w.Init(config.StrSlice("ch_hosts", ","))
	require.NoError(t, err)
	require.NotEqual(t, w.State(), Inactive)

	t.Log("open circuit")
	for i := 0; i < 6; i++ {
		_, _ = w.breaker.Execute(func() (interface{}, error) {
			return nil, errors.New("")
		})
	}
	require.Equal(t, w.State(), Inactive)

	err = w.Write(stmtArgs)
	require.Error(t, err)
	require.Equal(t, gobreaker.ErrOpenState, err)

	t.Log("wait for circuit becomes half-open")
	require.Eventually(t, func() bool {
		return w.State() == Active
	}, config.Duration("ch_open_circuit_timeout")+time.Second, time.Second/5, "Circuit breaker is still open")

	err = w.Write(stmtArgs)
	require.NoError(t, err)

	test.RequireCount(t, db, 2)

	err = w.Close()
	require.NoError(t, err)
}

func TestCloseInactive(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	w := NewWriter(test.NewTestLogger())
	go func() {
		// read all state changes
		for range w.StateChange() {
		}
	}()
	err := w.Init(config.StrSlice("ch_hosts", ","))
	require.NoError(t, err)
	require.NotEqual(t, w.State(), Inactive)

	t.Log("open circuit")
	for i := 0; i < 6; i++ {
		_, _ = w.breaker.Execute(func() (interface{}, error) {
			return nil, errors.New("")
		})
	}
	require.Equal(t, w.State(), Inactive)

	err = w.Close()
	require.NoError(t, err)
}

func TestWriteAfterClose(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	db := runUp(t)
	//goland:noinspection GoUnhandledErrorResult
	defer db.Close()

	w := NewWriter(test.NewTestLogger())
	go func() {
		// read all state changes
		for range w.StateChange() {
		}
	}()
	err := w.Init(config.StrSlice("ch_hosts", ","))
	require.NoError(t, err)
	require.NotEqual(t, w.State(), Inactive)

	err = w.Close()
	require.NoError(t, err)

	stmtArgs := stmtArgsFixture(t)
	for i := 0; i < 6; i++ {
		err = w.Write(stmtArgs)
		require.Error(t, err)
	}
	err = w.Write(stmtArgs)
	require.Error(t, err)
}

func TestWriteAfterCloseInactive(t *testing.T) {
	defer goleak.VerifyNone(t, goleak.IgnoreTopFunction("github.com/ClickHouse/clickhouse-go.init.0.func1"))

	test.InitEnv()

	w := NewWriter(test.NewTestLogger())
	go func() {
		// read all state changes
		for range w.StateChange() {
		}
	}()
	err := w.Init(config.StrSlice("ch_hosts", ","))
	require.NoError(t, err)
	require.NotEqual(t, w.State(), Inactive)

	t.Log("open circuit")
	for i := 0; i < 6; i++ {
		_, _ = w.breaker.Execute(func() (interface{}, error) {
			return nil, errors.New("")
		})
	}
	require.Equal(t, w.State(), Inactive)

	err = w.Close()
	require.NoError(t, err)

	stmtArgs := stmtArgsFixture(t)
	err = w.Write(stmtArgs)
	require.Error(t, err)
}

func stmtArgsFixture(t *testing.T) []*StmtArgs {
	rows := rowsFixture(t)

	args, err := FromRow(rows[0])
	require.NoError(t, err)
	args2, err := FromRow(rows[1])
	require.NoError(t, err)
	stmtArgs := []*StmtArgs{args, args2}
	return stmtArgs
}

func rowsFixture(t *testing.T) []domain.Row {
	fields, err := domain.JsonToFields([]byte(`
	{
	"_message": "message value",
	"_time": "2018-10-25T16:35:00.000+05:00",
	"_service": "service_value",
	"_uuid": "eed1aaef-b885-4ab4-a071-fd573a0b9b2b",
	"_timestamp": "2018-10-25T16:34:00",
	"_request_id": null,
	"_layer" : "ci"
	}
	`))
	require.NoError(t, err)
	row := domain.Row(fields)

	fields2, err := domain.JsonToFields([]byte(`
	{
	"_time": "2018-10-25T16:35:00.000+05:00",
	"_message": null,
	"_service": null,
	"_uuid": null,
	"_request_id": null,
	"_canary": null,
	"_level": null,
	"_timestamp": null,
	"_layer" : "ci"
	}
	`))
	require.NoError(t, err)
	row2 := domain.Row(fields2)

	return []domain.Row{row, row2}
}

func runUp(t testing.TB) *sql.DB {
	t.Helper()
	var err error
	test.InitEnv()

	conn, err := sql.Open("clickhouse", BuildDSN(ConnectParams{
		Hosts:    config.StrSlice("ch_hosts", ","),
		Database: config.Str("ch_database"),
		Username: config.Str("ch_username"),
		Password: config.Str("ch_password"),
		Secure:   config.SafeBool("ch_secure"),
	}))
	require.NoError(t, err)
	_, err = conn.Exec(CreateShardTable)
	require.NoError(t, err)
	_, err = conn.Exec("TRUNCATE TABLE logs_shard_v2")
	require.NoError(t, err)

	return conn
}

func TestShouldLogClickhouseErr(t *testing.T) {
	assert.True(t, shouldLogClickhouseErr(&clickhouse.Exception{Code: 42}))
	assert.False(t, shouldLogClickhouseErr(&clickhouse.Exception{Code: 252}))
	assert.True(t, shouldLogClickhouseErr(fmt.Errorf("some err")))
}