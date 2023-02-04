package drills

import (
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/drills/store"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestStorage_Actual(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)

	m := Drills{
		Scheduler: store.Nomad,
		Layer:     layer.Layer_TEST,
		DC:        "sas",
		RecoverID: 0,
	}
	db.GormDb.Create(&m)

	s := NewStorage(db, test.NewLogger(t))
	lst, err := s.Actual(layer.Layer_TEST)
	require.NoError(t, err)
	require.Len(t, lst, 1)
	assert.Equal(t, layer.Layer_TEST, lst[0].Layer)
	assert.Equal(t, store.Nomad, lst[0].Scheduler)

	var cnt int64
	require.NoError(t, db.GormDb.Model(Drills{}).Where("scheduler = ?", store.Nomad).Count(&cnt).Error)
}
