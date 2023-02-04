package clickhouse

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/vtail/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestQueryRunner_BuildParams(t *testing.T) {
	q := NewQueryRunner("tst", "someyql", test.NewTestLogger())
	now := time.Now()
	t.Run("short", func(t *testing.T) {
		res := q.BuildParams(QueryParams{Start: now, End: now.Add(-time.Minute * 30)})

		require.Len(t, res, 1)
		assert.Equal(t, now, res[0].Start)
	})
	t.Run("double", func(t *testing.T) {
		start := now.Add(-time.Hour * 2)
		res := q.BuildParams(QueryParams{Start: start, End: now})

		require.Len(t, res, 2)
		assert.Equal(t, now, res[0].End)
		assert.Equal(t, start, res[1].Start)
	})
	t.Run("check_asc", func(t *testing.T) {
		res := q.BuildParams(QueryParams{Start: now, End: now.Add(time.Hour * 2), SortDir: SortAsc})
		require.Len(t, res, 2)
		assert.True(t, res[0].Start.Before(res[1].Start), "span 0 start time should be before span 1 start time")
	})
	t.Run("long", func(t *testing.T) {
		start := now.Add(-time.Hour * 48)
		res := q.BuildParams(QueryParams{Start: start, End: now})

		require.Len(t, res, len(splitSpans)+1)
		assert.Equal(t, now.Add(-time.Hour).Truncate(time.Hour), res[0].Start)
		assert.Equal(t, now.Add(-time.Hour*3).Truncate(time.Hour), res[1].Start)
		assert.Equal(t, now.Add(-time.Hour*7).Truncate(time.Hour), res[2].Start)
		assert.Equal(t, now.Add(-time.Hour*13).Truncate(time.Hour), res[3].Start)
		assert.Equal(t, now.Add(-time.Hour*21).Truncate(time.Hour), res[4].Start)
		assert.Equal(t, start, res[5].Start)
	})
}
