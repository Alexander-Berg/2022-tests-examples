package test

import (
	"database/sql"
	"testing"

	vlog "github.com/YandexClassifieds/go-common/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func NewTestLogger() vlog.Logger {
	logger := vlogrus.NewLogger(vlogrus.WithLevel("debug"))
	logger.Level = logrus.DebugLevel
	return vlogrus.Wrap(logger.WithField(vlog.ContextF, "test"))
}

func RequireCount(t *testing.T, db *sql.DB, expected int) {
	rows, err := db.Query("SELECT count(*) FROM logs_shard_v2")
	require.NoError(t, err)
	defer func() {
		err := rows.Close()
		require.NoError(t, err)
	}()
	require.True(t, rows.Next())
	var actual int
	err = rows.Scan(&actual)
	require.NoError(t, err)
	require.Equal(t, expected, actual)
}

func AssertCount(t *testing.T, db *sql.DB, expected int) bool {
	t.Helper()
	row := db.QueryRow("SELECT count(*) FROM logs_shard_v2")
	var actual int
	err := row.Scan(&actual)
	require.NoError(t, err)
	require.NoError(t, row.Err())
	return assert.Equal(t, expected, actual)
}
