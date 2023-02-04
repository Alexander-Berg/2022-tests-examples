package main

import (
	"database/sql"
	"testing"
	"time"

	"github.com/YandexClassifieds/logs/cmd/golf/domain"
	"github.com/YandexClassifieds/logs/cmd/golf/storage/clickHouse"
	"github.com/YandexClassifieds/logs/cmd/golf/test"
	"github.com/YandexClassifieds/logs/pkg/config"
	"github.com/google/uuid"
	"github.com/stretchr/testify/require"
)

func TestCleanClickHouse(t *testing.T) {
	db := runUp(t)
	ch := clickHouse.NewDistributedWriter(test.NewTestLogger())
	log.Print("running init")
	err := ch.Init([]clickHouse.Shard{
		{Replicas: config.StrSlice("ch_hosts", ",")},
	})
	log.Print("init done")
	require.NoError(t, err)

	go func() {
		for range ch.CommitC() {
			// необходимо вычитать коммиты
		}
	}()
	test.RequireCount(t, db, 0)
	err = ch.Add(MakeData(time.Now().Add(-time.Hour), 42), domain.BatchKey{Cookie: 41})
	require.NoError(t, err)
	err = ch.Add(MakeData(time.Now(), 55), domain.BatchKey{Cookie: 42})
	require.NoError(t, err)
	ch.Shutdown()

	require.Eventually(t, func() bool {
		return test.AssertCount(t, db, 42+55)
	}, time.Second, time.Second/5)

	CleanClickHouse()

	require.Eventually(t, func() bool {
		return test.AssertCount(t, db, 55)
	}, time.Second, time.Second/5)
}

func TestCleanClickHouse_SingleRun(t *testing.T) {
	db := runUp(t)
	ch := clickHouse.NewDistributedWriter(test.NewTestLogger())
	log.Print("running init")
	err := ch.Init([]clickHouse.Shard{
		{Replicas: config.StrSlice("ch_hosts", ",")},
	})
	log.Print("init done")
	require.NoError(t, err)

	go func() {
		for range ch.CommitC() {
			// необходимо вычитать коммиты
		}
	}()
	test.RequireCount(t, db, 0)
	err = ch.Add(MakeData(time.Now(), 100), domain.BatchKey{Cookie: 42})
	require.NoError(t, err)
	ch.Shutdown()

	require.Eventually(t, func() bool {
		return test.AssertCount(t, db, 100)
	}, time.Second, time.Second/5)
	CleanClickHouse()
	time.Sleep(time.Second / 5)
	test.RequireCount(t, db, 100)
}

func runUp(t *testing.T) *sql.DB {
	var err error
	initEnv()

	wrapExec := func(db *sql.DB, query string) {
		_, err := db.Exec(query)
		require.NoError(t, err)
	}
	conn, err := sql.Open("clickhouse", clickHouse.BuildDSN(clickHouse.ConnectParams{
		Hosts:    config.StrSlice("ch_hosts", ","),
		Database: config.Str("ch_database"),
		Username: config.Str("ch_username"),
		Password: config.Str("ch_password"),
		Secure:   config.SafeBool("ch_secure"),
	}))

	require.NoError(t, err)
	wrapExec(conn, clickHouse.CreateShardTable)
	wrapExec(conn, clickHouse.CreateDistributedTable)
	wrapExec(conn, "TRUNCATE TABLE logs_shard_v2")
	return conn
}

func initEnv() {
	test.InitEnv()
	test.SetEnv("CH_MAX_SIZE", "5000")
}

func MakeData(ts time.Time, sz int) []domain.Row {
	var rows []domain.Row

	for i := 0; i < sz; i++ {
		rows = append(rows, newRow(ts))
	}
	return rows
}

func newRow(ts time.Time) domain.Row {
	return domain.Row{
		newField(domain.TimeK, ts),
		newField(domain.TimeNano, 123),
		newField(domain.LayerNameK, "test"),
		newField(domain.ServiceK, "VertisService"),
		newField(domain.LevelK, "INFO"),
		newField(domain.MessageK, "Message for log"),
		newField(domain.BranchK, ""),
		newField(domain.VersionK, "tc35"),
		newField(domain.CanaryK, false),
		newField(domain.ContextK, "MyClass"),
		newField(domain.ThreadK, "[001]"),
		newField(domain.RequestIdK, "444535f9378a3dfa1b8604bc9e05a303"),
		newField(domain.ContainerIdK, "some-cont-id"),
		newField(domain.HostK, "docker-02-sas.test.vertis.yandex.net"),
		newField(domain.UuidK, uuid.New().String()),
		newField(domain.CustomFieldsK, `custom_field: "my data"`),
	}
}

func newField(k domain.Key, v interface{}) domain.Field {
	return domain.Field{
		Key:       k,
		JsonValue: v,
	}
}
