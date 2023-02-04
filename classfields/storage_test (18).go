package nomad

import (
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSaveAndGet(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	storage := NewStorage(test_db.NewDb(t), test.NewLogger(t))

	for i := 1; i < 10; i++ {
		nomad := &Model{
			EvaluationID: "random",
			Type:         common.Stop,
		}

		err := storage.Save(nomad)
		require.NoError(t, err)

		assert.NotNil(t, nomad)
		assert.NotNil(t, nomad.ID)
		assert.NotNil(t, nomad.EvaluationID)
		assert.NotNil(t, nomad.CreatedAt)
		assert.Equal(t, common.Stop, nomad.Type)

		logrus.Infof("id %d", nomad.ID)
		nomad2, err := storage.Get(nomad.ID)
		require.NoError(t, err)

		assert.NotNil(t, nomad2)
		assert.Equal(t, nomad2.ID, nomad.ID)
		assert.Equal(t, nomad2.EvaluationID, nomad.EvaluationID)
		assert.Equal(t, common.Stop, nomad2.Type)
	}
}

func TestStorage_GetByDeployment(t *testing.T) {
	test.RunUp(t)
	storage := NewStorage(test_db.NewDb(t), test.NewLogger(t))

	sm := &Model{DeploymentID: 42}
	err := storage.Save(sm)
	require.NoError(t, err)
	m, err := storage.GetByDeployment(42)
	require.NoError(t, err)
	assert.Equal(t, int64(42), m.DeploymentID)
	assert.Equal(t, sm.ID, m.ID)
}
