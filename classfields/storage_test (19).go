package yadeploy

import (
	"testing"

	"github.com/YandexClassifieds/shiva/arcadia/yp/go/proto/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestStorage_Save(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	s := newStorage(db, test.NewLogger(t))
	stage := &api.TStage{Meta: &api.TStageMeta{Id: "test-id"}}
	m := &Model{
		UUID:          uuid.New(),
		DeploymentId:  42,
		StageId:       "test-stage",
		TargetState:   StateRun,
		Revision:      3,
		SpecTimestamp: 12345,
	}
	require.NoError(t, m.SetStage(stage))
	require.NoError(t, s.Save(m))

	r, err := s.GetByDeployment(42)
	require.NoError(t, err)
	rs, err := r.GetStage()
	require.NoError(t, err)
	assert.Equal(t, "test-id", rs.GetMeta().GetId())
}

func TestStorage_GetByUUID(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	s := newStorage(db, test.NewLogger(t))
	id := uuid.New()
	err := s.Save(&Model{
		UUID:         id,
		StageId:      "test-stage",
		DeploymentId: 44,
		TargetState:  StateRun,
	})
	require.NoError(t, err)

	m, err := s.GetByUUID(id)
	require.NoError(t, err)
	assert.Equal(t, int64(44), m.DeploymentId)
	assert.Equal(t, "test-stage", m.StageId)
	assert.Equal(t, StateRun, m.TargetState)
}
