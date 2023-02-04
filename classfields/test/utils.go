package test

import (
	"database/sql"
	"fmt"
	"math/rand"
	"net"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/common/config"
	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/nhatthm/grpcmock"
	"github.com/nhatthm/grpcmock/planner"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	gormlog "gorm.io/gorm/logger"
)

func NewGorm(t *testing.T) *gorm.DB {
	var (
		address  = config.Str("DB_ADDRESS")
		username = config.Str("DB_USERNAME")
		password = config.Str("DB_PASSWORD")
		database = config.Str("DB_NAME")
		sslMode  = config.Str("DB_SSL_MODE")
	)
	dsn := fmt.Sprintf(
		"postgres://%s:%s@%s/%s?sslmode=%s&TimeZone=%s",
		username, password, address, database, sslMode, config.Str("TZ"),
	)
	pg := postgres.New(postgres.Config{
		DSN:                  dsn,
		PreferSimpleProtocol: true,
	})
	gormLog := gormlog.New(
		logrus.New(),
		gormlog.Config{
			SlowThreshold:             100 * time.Millisecond,
			LogLevel:                  gormlog.Error,
			IgnoreRecordNotFoundError: true,
			Colorful:                  false,
		},
	)
	db, err := gorm.Open(pg, &gorm.Config{
		Logger:                                   gormLog,
		DisableForeignKeyConstraintWhenMigrating: true,
	})
	require.NoError(t, err)
	return db
}

func RunUp(t *testing.T) {
	InitTestEnv()
	Clean(t)
}

func Clean(t *testing.T) {
	db := NewGorm(t)
	defer func() {
		sqlDB, err := db.DB()
		require.NoError(t, err)
		require.NoError(t, sqlDB.Close())
	}()

	CleanTables(t, db, "checks", "clusters", "hosts", "hosts_status")
}

func CleanTables(t *testing.T, db *gorm.DB, names ...string) {
	migrator := db.Migrator()
	tx := db.Begin()
	var result []string
	for _, n := range names {
		if migrator.HasTable(n) {
			result = append(result, n)
		}
	}
	if len(result) > 0 {
		tx.Exec(fmt.Sprintf("TRUNCATE %s", strings.Join(result, ", ")))
	}
	require.NoError(t, tx.Commit().Error)

	for _, n := range result {
		var count int64
		require.NoError(t, db.Table(n).Count(&count).Error)
		if count > 0 {
			logrus.New().Errorf("Table %s have count %d", n, count)
			assert.FailNow(t, "Table not clean")
		}
	}
}

/*
NewSeparatedGorm - make isolation db for parallel tests:
 - make new db,
 - enable indexes,
 - prepare cleanup with drop (it slow because use slow create new tables, but TRUNCATE is fast or copy db by template is fast)
More:
 - https://www.maragu.dk/blog/speeding-up-postgres-integration-tests-in-go/
 - https://www.postgresql.org/docs/13/manage-ag-templatedbs.html
*/
func NewSeparatedGorm(t *testing.T) *gorm.DB {
	rand.Seed(time.Now().UnixNano())

	var (
		address  = config.Str("DB_ADDRESS")
		username = config.Str("DB_USERNAME")
		password = config.Str("DB_PASSWORD")
		database = config.Str("DB_NAME")
		sslMode  = config.Str("DB_SSL_MODE")
		tz       = config.Str("TZ")
	)
	dsn := fmt.Sprintf("postgres://%s:%s@%s/%s?sslmode=%s&TimeZone=%s", username, password, address, database, sslMode, tz)
	sqlDB, err := sql.Open("pgx", dsn)
	require.NoError(t, err)

	dbName := fmt.Sprintf("test-db-%d", rand.Int63())
	_, err = sqlDB.Exec(fmt.Sprintf("create database %s with owner %s", pgQuote(dbName), pgQuote(username)))
	require.NoError(t, err, "db create failed")

	pg := postgres.New(postgres.Config{
		DSN:                  fmt.Sprintf("postgres://%s:%s@%s/%s?sslmode=%s&TimeZone=%s", username, password, address, dbName, sslMode, tz),
		PreferSimpleProtocol: true,
	})
	gormLog := gormlog.New(
		logrus.New(),
		gormlog.Config{
			SlowThreshold:             100 * time.Millisecond,
			LogLevel:                  gormlog.Error,
			IgnoreRecordNotFoundError: true,
			Colorful:                  false,
		},
	)
	db, err := gorm.Open(pg, &gorm.Config{
		Logger:                                   gormLog,
		DisableForeignKeyConstraintWhenMigrating: false,
	})
	require.NoError(t, err)
	t.Cleanup(func() {
		gormSQL, err := db.DB()
		require.NoError(t, err)
		require.NoError(t, gormSQL.Close())
		_, err = sqlDB.Exec(fmt.Sprintf("drop database %s", pgQuote(dbName)))
		require.NoError(t, err, "db drop failed")
		err = sqlDB.Close()
		require.NoError(t, err)
	})
	return db
}

func pgQuote(name string) string {
	return `"` + strings.ReplaceAll(name, `"`, `""`) + `"`
}

func GenerateHostname() string {
	rSource := rand.NewSource(time.Now().UnixNano())
	customRand := rand.New(rSource)
	return fmt.Sprintf("docker-%d-sas.prod.vertis.yandex.net", customRand.Intn(100))
}

func StartAgentServer(t *testing.T, name string, expectMethods func(s *grpcmock.Server)) (string, int) {
	srv := grpcmock.MockServer(
		grpcmock.WithPlanner(planner.FirstMatch()),
		grpcmock.RegisterServiceFromInstance(name, (*pbAgent.AgentServiceServer)(nil)),
		expectMethods,
	)(t)

	srv.Serve()
	t.Cleanup(func() {
		require.NoError(t, srv.Close())
	})

	host, portStr, err := net.SplitHostPort(srv.Address())
	require.NoError(t, err)
	port, err := strconv.Atoi(portStr)
	require.NoError(t, err)

	return host, port
}
