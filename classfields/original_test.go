package model

import (
	"github.com/YandexClassifieds/logs/api/collector"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestNewOriginalMessage(t *testing.T) {
	t.Run("base", func(t *testing.T) {
		msg, err := newOriginalMessage(&collector.LogRow{
			UniqueId: "test",
			RawJson:  []byte(`{"_time":"2018-11-28T13:12:39.776+03:00", "_context":"ctx1","_message":"test"}`),
		})
		require.NoError(t, err)
		_ = msg
	})
	t.Run("not_json", func(t *testing.T) {
		_, err := newOriginalMessage(&collector.LogRow{
			UniqueId: "test",
			RawJson:  []byte(`not json`),
		})
		require.Error(t, err)
	})
}
