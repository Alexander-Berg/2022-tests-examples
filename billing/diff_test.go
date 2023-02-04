package runtime

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
)

func TestGetDiffIO(t *testing.T) {
	currIO := map[string]any{
		"added":          1,
		"changed":        "one",
		"perhaps":        "perhaps",
		"unchangedArray": []any{1, "one"},
		"unchangedMap":   map[string]any{"one": 1, "two": 2},
	}

	otherIO := map[string]any{
		"removed":        2,
		"changed":        "two",
		"perhaps":        tplops.NilValue{},
		"unchangedArray": []any{1, "one"},
		"unchangedMap":   map[string]any{"one": 1, "two": 2},
	}

	// unchanged
	diff, diffType := GetDiffIO(currIO, currIO)

	assert.Equal(t, UnchangedDiffType, diffType)
	for name, diffValue := range diff {
		assert.Equal(t, UnchangedDiffType, diffValue.DiffType)
		assert.Equal(t, currIO[name], diffValue.Current)
		assert.Equal(t, currIO[name], diffValue.Other)
	}

	//perhapsChanged
	diff, diffType = GetDiffIO(otherIO, otherIO)

	assert.Equal(t, PerhapsChangedDiffType, diffType)
	for name, diffValue := range diff {
		if name == "perhaps" {
			assert.Equal(t, PerhapsChangedDiffType, diffValue.DiffType)
		} else {
			assert.Equal(t, UnchangedDiffType, diffValue.DiffType)
		}

		assert.Equal(t, otherIO[name], diffValue.Current)
		assert.Equal(t, otherIO[name], diffValue.Other)
	}

	// changed
	diff, diffType = GetDiffIO(currIO, otherIO)

	assert.Equal(t, ChangedDiffType, diffType)

	assert.Equal(t, ChangedDiffType, diff["changed"].DiffType)
	assert.Equal(t, currIO["changed"], diff["changed"].Current)
	assert.Equal(t, otherIO["changed"], diff["changed"].Other)

	assert.Equal(t, AddedDiffType, diff["added"].DiffType)
	assert.Equal(t, RemovedDiffType, diff["removed"].DiffType)
	assert.Equal(t, PerhapsChangedDiffType, diff["perhaps"].DiffType)
	assert.Equal(t, UnchangedDiffType, diff["unchangedArray"].DiffType)
	assert.Equal(t, UnchangedDiffType, diff["unchangedMap"].DiffType)

}

func TestBlocksDiff(t *testing.T) {
	unchBlock := NewBlock()
	unchBlock.Name = "UnchangedBlock"
	unchBlock.Inputs = map[entities.VarName]any{
		{Name: "int", Block: unchBlock.Name}:    1,
		{Name: "float", Block: unchBlock.Name}:  1.1,
		{Name: "string", Block: unchBlock.Name}: "1",
		{Name: "slice", Block: unchBlock.Name}:  []any{1, "1"},
		{Name: "map", Block: unchBlock.Name}:    map[string]any{"one": 1, "two": "2"},
	}

	perhapsChangeBlock := NewBlock()
	perhapsChangeBlock.Name = "PerhapsChangeBlock"
	perhapsChangeBlock.OutputNames = []tplops.OutputValue{{Name: "nilvalue"}}
	perhapsChangeBlock.State = map[string]any{
		"nilvalue": tplops.NilValue{},
	}

	changedBlock := NewBlock()
	changedBlock.Name = "ChangedBlock"
	changedBlock.OutputNames = []tplops.OutputValue{{Name: "change_output"}}
	changedBlock.State = map[string]any{"change_output": 666}

	yetChangedBlock := NewBlock()
	yetChangedBlock.Name = changedBlock.Name
	yetChangedBlock.OutputNames = []tplops.OutputValue{{Name: "change_output"}}
	yetChangedBlock.State = map[string]any{"change_output": 999}

	removedBlock := NewBlock()
	removedBlock.Name = "RemovedBlock"

	addedBlock := NewBlock()
	addedBlock.Name = "AddedBlock"

	diff := Blocks{unchBlock, perhapsChangeBlock, yetChangedBlock, addedBlock}.Diff(Blocks{unchBlock, perhapsChangeBlock, changedBlock, removedBlock})

	assert.Len(t, diff, 5)

	assert.Equal(t, AddedDiffType, diff[addedBlock.Name].Verdict)
	assert.Equal(t, RemovedDiffType, diff[removedBlock.Name].Verdict)
	assert.Equal(t, UnchangedDiffType, diff[unchBlock.Name].Verdict)
	assert.Equal(t, PerhapsChangedDiffType, diff[perhapsChangeBlock.Name].Verdict)
	assert.Equal(t, ChangedDiffType, diff[changedBlock.Name].Verdict)
}

func TestBlockDiffEqualInputNames(t *testing.T) {
	// Use two separate blocks to have different map order
	b1 := &Block{Inputs: map[entities.VarName]any{
		{Name: "v1", Block: "1"}: 10,
		{Name: "v1", Block: "2"}: 11,
		{Name: "v1", Block: "3"}: 12,
	}}
	b2 := &Block{Inputs: map[entities.VarName]any{
		{Name: "v1", Block: "2"}: 11,
		{Name: "v1", Block: "1"}: 10,
		{Name: "v1", Block: "3"}: 12,
	}}

	diff := b1.Diff(b2)
	assert.Equal(t, UnchangedDiffType, diff.Verdict)
}
