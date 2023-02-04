package ytreferences

import (
	"testing"

	sq "github.com/Masterminds/squirrel"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestYtSelectBuilder_ToYTSql(t *testing.T) {
	builder := sq.Select("*").
		From("[//test]").
		Where(sq.Eq{"g": 5}).
		Where(map[string]any{"h": "6"}).
		Where(sq.Eq{"i": []uint64{7, 8, 9}}).
		Where(sq.Or{sq.Expr("j = ?", 10), sq.And{sq.Eq{"k": 11}, sq.Expr("true")}}).
		GroupBy("timestamp / 86400 AS day")
	query, err := (&YtSelectBuilder{builder}).ToYTSql()
	require.NoError(t, err)
	assert.Equal(
		t,
		" * FROM [//test] WHERE g = 5 AND h = '6' AND i IN (7,8,9) AND (j = 10 OR (k = 11 AND true)) GROUP BY timestamp / 86400 AS day",
		query)
}

func TestYtSelectBuilder_ToYTSqlErrorIfBadCondition(t *testing.T) {
	builder := sq.Select("*").
		From("[//test]").
		Where(sq.Gt{"g": nil})
	_, err := (&YtSelectBuilder{builder}).ToYTSql()
	require.Error(t, err)
}

func TestYtSelectBuilder_ToYTSqlErrorIfBadParameter(t *testing.T) {
	builder := sq.Select("*").
		From("[//test]").
		Where(sq.Eq{"g": map[string]string{"can't handle": "map"}})
	_, err := (&YtSelectBuilder{builder}).ToYTSql()
	require.Error(t, err)
}
