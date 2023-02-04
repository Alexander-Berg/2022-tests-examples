package test

import (
	"database/sql"
	"fmt"
	"math/rand"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	gormlog "gorm.io/gorm/logger"
)

var (
	r       = rand.New(rand.NewSource(time.Now().UnixNano()))
	curUint *uint64
)

func init() {
	zero := uint64(0)
	curUint = &zero
}

// DEPRECATED: use require.NoError from testify/require package
func Check(t *testing.T, err error) {

	if err != nil {
		stop := assert.FailNow(t, "test fatal", err)
		if !stop {
			panic(err)
		}
	}
}

// Deprecated: use NewSeparatedGorm
func NewGorm(t *testing.T) *gorm.DB {
	var (
		address  = config.Str("DB_ADDRESS")
		username = config.Str("DB_USERNAME")
		password = config.SafeStr("DB_PASSWORD")
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
		NewLogger(t),
		gormlog.Config{
			SlowThreshold: 100 * time.Millisecond,
			// TODO may be need Silent mode, but need view real problems
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
		password = config.SafeStr("DB_PASSWORD")
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
		NewLogger(t),
		gormlog.Config{
			SlowThreshold: 100 * time.Millisecond,
			// TODO may be need Silent mode, but need view real problems
			LogLevel:                  gormlog.Error,
			IgnoreRecordNotFoundError: true,
			Colorful:                  false,
		},
	)
	db, err := gorm.Open(pg, &gorm.Config{
		// TOOD off not found logs for test
		Logger:                                   gormLog,
		DisableForeignKeyConstraintWhenMigrating: false,
	})
	require.NoError(t, err)
	t.Cleanup(func() {
		gormSql, err := db.DB()
		require.NoError(t, err)
		require.NoError(t, gormSql.Close())
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

func RunUp(t *testing.T) {
	InitTestEnv()
	Clean(t)
}

func RunUpGhApp(t *testing.T) {
	InitTestEnvGithubApp()
	Clean(t)
}

func Down(t *testing.T) {
	Clean(t)
}

func AssertUserErrors(t *testing.T, expected *user_error.UserErrors, actual *user_error.UserErrors) {

	PrintUserErrors(t, "expected", expected)
	PrintUserErrors(t, "actual", actual)
	if actual.Len() != expected.Len() {
		assert.Equal(t, expected.Len(), actual.Len())
		return
	}

	notFound := user_error.NewUserErrors()
	for _, actualErr := range actual.Get() {
		contains := false
		for _, expectedErr := range expected.Get() {
			if actualErr == expectedErr || actualErr.Error() == expectedErr.Error() ||
				actualErr.RusMessage == expectedErr.RusMessage {
				contains = true
				break
			}
		}
		if !contains {
			notFound.AddError(actualErr)
		}
	}

	if notFound.Len() != 0 {
		PrintUserErrors(t, "not found", notFound)
		assert.Fail(t, "errors not found")
	}
}

func PrintUserErrors(t *testing.T, prefix string, errs *user_error.UserErrors) {
	for _, e := range errs.Get() {
		NewLogger(t).Infof("user error (%s): %s - %s", prefix, e.Error(), e.RusMessage)
	}
}

func Clean(t *testing.T) {
	db := NewGorm(t)
	defer func() {
		sqlDB, err := db.DB()
		require.NoError(t, err)
		require.NoError(t, sqlDB.Close())
	}()
	// TODO auto scan all table
	CleanTables(t, db, "approve", "deployment", "branch_cleanup", "service_maps", "favorite",
		"batch_task", "batch",
		"deploy_manifest", "staff_user", "issue", "nomad_scheduler", "drug_scheduler", "telegram_user", "telegram_group",
		"telegram_user_group_link", "grafana", "current_state", "include_data", "include_link", "subscribe", "deployment_includes",
		"setting", "hook", "feature_flags", "bulk_restart_status", "env_override", "issue_link", "external_env", "bulk_restart",
		"deployment", "service_maps", "deploy_manifest", "issue", "staff_user",
		"generator_task", "production_mirroring", "drills", "drills_recovery", "schema_migrations", "calculated_resource")
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
			NewLogger(t).Errorf("Table %s have count %d", n, count)
			assert.FailNow(t, "Table not clean")
		}
	}
}

func Wait(t *testing.T, f func() error) {
	timer := time.NewTimer(5 * time.Second)
	ticker := time.NewTicker(25 * time.Millisecond)
	defer timer.Stop()
	defer ticker.Stop()
	var err error
	for {
		select {
		case <-ticker.C:
			err = f()
			if err == nil {
				return
			}
		case <-timer.C:
			assert.FailNow(t, "test wait timeout", "err: %s", err)
			return
		}
	}
}

var letterRunes = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

func RandString(n int) string {
	b := make([]rune, n)
	for i := range b {
		b[i] = letterRunes[r.Intn(len(letterRunes))]
	}
	return string(b)
}

func AtomicNextUint() uint64 {
	return atomic.AddUint64(curUint, 1)
}
