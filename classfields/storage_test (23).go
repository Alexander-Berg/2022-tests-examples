package kv_storage

import (
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

func TestKV(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	s := NewKV(db, test.NewLogger(t))

	assertSaveValue(t, s, "key", "value")
	assertSaveValue(t, s, "key", "value1")
	assertSaveValue(t, s, "key", "")

}

func assertSaveValue(t *testing.T, s IKeyValue, key, value string) {
	require.NoError(t, s.Save(key, value))

	val, err := s.Get(key)
	require.NoError(t, err)
	require.Equal(t, value, val)
}
