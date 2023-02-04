package events

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/github-app/pull_request"
	validation "github.com/YandexClassifieds/shiva/pb/validator/status"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

func TestStatus(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	s := NewService(db, log)

	status, err := s.Status(100, 0)
	require.NoError(t, err)
	require.Equal(t, validation.Status_UNKNOWN, status)

	require.NoError(t, db.GormDb.Create(&pull_request.ArcadiaPr{
		PrId:   100,
		Time:   time.Now(),
		Status: validation.Status_SUCCESS,
	}).Error)
	status, err = s.Status(100, 0)
	require.NoError(t, err)
	require.Equal(t, validation.Status_SUCCESS, status)
}
