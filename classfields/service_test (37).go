package drills

import (
	"database/sql"
	"errors"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/drills/store"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills/store/drills"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestService_DC_Nomad(t *testing.T) {

	test.RunUp(t)
	s := newService(t)
	prepare(t, s)
	dc, err := s.DC(common.Test, map[string]int{"sas": 5, "myt": 5, "vla": 5})
	require.NoError(t, err)
	assert.Equal(t, 0, dc["sas"])
	assert.Equal(t, 5, dc["myt"])
	assert.Equal(t, 5, dc["vla"])
}

func TestService_DC_YD(t *testing.T) {

	test.RunUp(t)
	s := newService(t)
	prepare(t, s)
	_, err := s.DC(common.Test, map[string]int{"sas": 5, "yd_sas": 5})
	assert.True(t, errors.Is(err, ErrDrillsYDNotAllow))
}

func newService(t *testing.T) *Service {
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	return NewService(log, db)
}

func prepare(t *testing.T, s *Service) {
	now := time.Now()
	require.NoError(t, s.storage.Save(&drills.Drills{
		Scheduler: store.Nomad,
		Layer:     layer.Layer_TEST,
		DC:        "sas",
		StartTime: now,
	}))
	require.NoError(t, s.storage.Save(&drills.Drills{
		Scheduler: store.YD,
		Layer:     layer.Layer_TEST,
		DC:        "yd_sas",
		StartTime: now,
	}))
	require.NoError(t, s.storage.Save(&drills.Drills{
		Scheduler: store.Nomad,
		Layer:     layer.Layer_TEST,
		DC:        "myt",
		StartTime: now,
		EndTime:   sql.NullTime{Time: now, Valid: true},
	}))
	require.NoError(t, s.storage.Save(&drills.Drills{
		Scheduler: store.Nomad,
		Layer:     layer.Layer_TEST,
		DC:        "yd_myt",
		StartTime: now,
		EndTime:   sql.NullTime{Time: now, Valid: true},
	}))
}
