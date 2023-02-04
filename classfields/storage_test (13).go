package env_override

import (
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSaveAndGet(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	s := NewStorage(test_db.NewDb(t), test.NewLogger(t))

	envOverride := []*Model{{
		DeploymentId: 1,
		Key:          "env1",
		Value:        "e1",
	}, {
		DeploymentId: 1,
		Key:          "env2",
		Value:        "e2",
	}, {
		DeploymentId: 2,
		Key:          "env1",
		Value:        "e3",
	}}

	require.NoError(t, s.Save(envOverride))
	co, err := s.GetByDeploymentId(1)
	require.NoError(t, err)
	assert.Len(t, co, 2)

	envMap := map[string]string{co[0].Key: co[0].Value, co[1].Key: co[1].Value}
	assert.Equal(t, envMap["env1"], "e1")
	assert.Equal(t, envMap["env2"], "e2")

	co, err = s.GetByDeploymentId(2)
	require.NoError(t, err)
	assert.Len(t, co, 1)

	assert.Equal(t, co[0].Key, "env1")
	assert.Equal(t, co[0].Value, "e3")
}
