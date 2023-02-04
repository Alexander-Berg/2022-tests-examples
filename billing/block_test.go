package runtime

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"golang.org/x/exp/maps"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
)

func TestUpdateInputs(t *testing.T) {
	block := NewBlock()
	block.InputVars = []tplops.InputValue{
		{Name: entities.VarName{Name: "inp1", Block: "1"}},
		{Name: entities.VarName{Name: "inp2", Block: "1"}},
		{Name: entities.VarName{Name: "inp3", Block: "1"}},
		{Name: entities.VarName{Name: "inp4", Block: "2"}},
		{Name: entities.VarName{Name: "inp1", Block: entities.GlobalInputBlockName}},
	}
	block.Deps = map[entities.BlockName]struct{}{"1": {}, "2": {}}

	// Missing key inp1
	err := block.UpdateInputs(entities.GlobalInputBlockName, map[string]any{"miss": 1})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "missing")

	// Missing key inp3
	err = block.UpdateInputs("1", map[string]any{"inp1": 1, "inp2": map[string]int{"1": 1}, "inp10": 10})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "missing")
	maps.Clear(block.Inputs)

	// OK
	v1 := map[string]int{"1": 1}
	err = block.UpdateInputs(entities.GlobalInputBlockName, map[string]any{"inp1": 1, "inp2": v1})
	require.NoError(t, err)
	assert.Equal(t, map[entities.VarName]any{
		entities.VarName{Name: "inp1", Block: entities.GlobalInputBlockName}: 1,
	}, block.Inputs)
	assert.Empty(t, block.MetDeps)

	// OK
	err = block.UpdateInputs("1", map[string]any{"inp1": 1, "inp2": map[string]int{"1": 1}, "inp3": 3, "inp10": 10})
	require.NoError(t, err)
	assert.Equal(t, map[entities.VarName]any{
		entities.VarName{Name: "inp1", Block: entities.GlobalInputBlockName}: 1,
		entities.VarName{Name: "inp1", Block: "1"}:                           1,
		entities.VarName{Name: "inp2", Block: "1"}:                           map[string]int{"1": 1},
		entities.VarName{Name: "inp3", Block: "1"}:                           3,
	}, block.Inputs)
	assert.Equal(t, map[entities.BlockName]struct{}{"1": {}}, block.MetDeps)

	// Same map value for existing key
	err = block.UpdateInputs("1", map[string]any{"inp1": 1, "inp2": map[string]int{"1": 1}, "inp3": 3})
	require.NoError(t, err)

	// Another value for existing key
	err = block.UpdateInputs("1", map[string]any{"inp1": 2, "inp2": map[string]int{"1": 1}, "inp3": 3})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "values don't match")

	// Shouldn't call for not dep.
	err = block.UpdateInputs("10", map[string]any{"inp1": 2})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "doesn't depend")
}

func TestBlockValidation(t *testing.T) {
	block := NewBlock()
	block.InputVars = []tplops.InputValue{
		{Name: entities.VarName{Name: "inp1", Block: "1"}},
		{Name: entities.VarName{Name: "inp2", Block: "1"}},
	}
	block.Deps = map[entities.BlockName]struct{}{"1": {}, "2": {}}

	require.Error(t, block.UpdateInputs("1", map[string]any{"inp1": 1}), "need all inputs at once")

	require.NoError(t, block.UpdateInputs("1", map[string]any{"inp1": 1, "inp2": 2}))
	require.Error(t, block.Validate(), "dep2 isn't yet covered")

	require.NoError(t, block.UpdateInputs("2", nil))
	require.NoError(t, block.Validate(), "nothing is missing now")
}
