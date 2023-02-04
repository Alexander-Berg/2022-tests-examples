package clickHouse

import (
	"github.com/YandexClassifieds/logs/cmd/golf/domain"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestFromRow(t *testing.T) {
	t.Run("base", func(t *testing.T) {
		args, err := FromRow([]domain.Field{
			{Key: domain.TimeK, JsonValue: "2020-01-01T13:00:00+03:00"},
		})
		require.NoError(t, err)
		_, ok := args.values[domain.TimeK]
		assert.True(t, ok)
	})
	t.Run("err_empty_time", func(t *testing.T) {
		_, err := FromRow([]domain.Field{
			{Key: domain.TimeK, JsonValue: ""},
			{Key: domain.MessageK, JsonValue: "msg with empty timestamp"},
		})
		require.Error(t, err)
	})
}
