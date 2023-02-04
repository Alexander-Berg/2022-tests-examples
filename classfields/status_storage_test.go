package bulk_deployment

import (
	"fmt"
	"math/rand"
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestStorage(t *testing.T) {

	id := rand.Int63()
	test.RunUp(t)
	defer test.Down(t)
	s := NewStatusStorage(test_db.NewDb(t), test.NewLogger(t))

	states := []*Status{}
	for i := 0; i < 5; i++ {
		states = append(states, &Status{
			BulkDeploymentId: id,
			Name:             fmt.Sprintf("test%d", i),
			State:            NotStarted,
		})
	}

	require.NoError(t, s.SaveAll(states))
	dbStates, err := s.GetUnprocessed(id)
	require.NoError(t, err)
	assert.Len(t, dbStates, len(states))

	states[3].State = Success
	require.NoError(t, s.Update(states[3]))
	dbState, err := s.Get(states[3].ID)
	require.NoError(t, err)
	assert.Equal(t, Success.String(), dbState.State.String())

	count, err := s.FinishNotStarted(id)
	require.NoError(t, err)
	assert.Equal(t, int64(4), count)

	dbStates, err = s.GetUnprocessed(id)
	require.NoError(t, err)
	assert.Len(t, dbStates, 0)
}
