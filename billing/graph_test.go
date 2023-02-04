package template

import (
	"context"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/template"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	yaml2 "a.yandex-team.ru/billing/configshop/pkg/core/yaml"
	storagetest "a.yandex-team.ru/billing/configshop/pkg/storage/xtest"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/configops"
	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
)

type testBlock struct {
	id           entities.BlockName
	deps         []entities.BlockName
	explicitDeps []entities.BlockName
}

func (b testBlock) Name() entities.BlockName {
	return b.id
}

func (b testBlock) Type() string {
	return "testBlock"
}

func (b testBlock) Inputs() []entities.VarName {
	var inputs []entities.VarName
	for _, dep := range b.deps {
		inputs = append(inputs, entities.VarName{Name: "any", Block: dep})
	}
	return inputs
}

func (b testBlock) Outputs() []string {
	return []string{"any"}
}

func (b testBlock) IsEmbedded() bool {
	return false
}

func (b testBlock) OutputsWithTypes(tplops.Types) ([]tplops.OutputValue, error) {
	return []tplops.OutputValue{{Name: "any", Type: configopstype.ValueTypeAny}}, nil
}

func (b testBlock) GlobalOutputs() []string {
	return []string{"any"}
}

func (b testBlock) ExplicitDeps() []entities.BlockName {
	return b.explicitDeps
}

func (b testBlock) ToTemplateBlock() template.Block {
	return template.Block{}
}

func TestValidBlockGraph(t *testing.T) {
	testCases := []struct {
		name   string
		blocks []template.GraphBlock
	}{
		{
			name:   "empty",
			blocks: nil,
		},
		{
			name:   "one block",
			blocks: []template.GraphBlock{testBlock{id: "1"}},
		},
		{
			name: "two depending blocks",
			blocks: []template.GraphBlock{
				testBlock{id: "1", deps: []entities.BlockName{"2"}},
				testBlock{id: "2"},
			},
		},
		{
			name: "two depending blocks changed order",
			blocks: []template.GraphBlock{
				testBlock{id: "2"},
				testBlock{id: "1", deps: []entities.BlockName{"2"}},
			},
		},
		{
			name: "two not depending blocks",
			blocks: []template.GraphBlock{
				testBlock{id: "1"},
				testBlock{id: "2"},
			},
		},
		{
			name: "bambook",
			blocks: []template.GraphBlock{
				testBlock{id: "1", deps: []entities.BlockName{"2"}},
				testBlock{id: "2", deps: []entities.BlockName{"3"}},
				testBlock{id: "3", deps: []entities.BlockName{"4"}},
				testBlock{id: "4", deps: []entities.BlockName{"5"}},
				testBlock{id: "5"},
			},
		},
		{
			name: "oh no too many deps",
			blocks: []template.GraphBlock{
				testBlock{id: "1", deps: []entities.BlockName{"2", "3", "4", "5"}},
				testBlock{id: "2"},
				testBlock{id: "3"},
				testBlock{id: "4"},
				testBlock{id: "5"},
			},
		},
		{
			name: "ezhik",
			blocks: []template.GraphBlock{
				testBlock{id: "1"},
				testBlock{id: "2", deps: []entities.BlockName{"1"}},
				testBlock{id: "3", deps: []entities.BlockName{"1"}},
				testBlock{id: "4", deps: []entities.BlockName{"1"}},
				testBlock{id: "5", deps: []entities.BlockName{"1"}},
			},
		},
		{
			name: "cycle? WRONG! (rhombus)",
			blocks: []template.GraphBlock{
				testBlock{id: "1"},
				testBlock{id: "2", deps: []entities.BlockName{"1"}},
				testBlock{id: "3", deps: []entities.BlockName{"1"}},
				testBlock{id: "4", deps: []entities.BlockName{"2", "3"}},
			},
		},
		{
			name: "three dependent groups",
			blocks: []template.GraphBlock{
				testBlock{id: "1"},
				testBlock{id: "2"},
				testBlock{id: "3"},

				testBlock{id: "4", deps: []entities.BlockName{"2", "3"}},
				testBlock{id: "5", deps: []entities.BlockName{"1", "2"}},
				testBlock{id: "6", deps: []entities.BlockName{"2"}},

				testBlock{id: "7", deps: []entities.BlockName{"5", "4"}},
				testBlock{id: "8", deps: []entities.BlockName{"1", "6"}},
				testBlock{id: "9", deps: []entities.BlockName{"4", "5", "2"}},
			},
		},
		{
			name: "ezhik combined deps",
			blocks: []template.GraphBlock{
				testBlock{id: "1"},
				testBlock{id: "2", deps: []entities.BlockName{"1"}},
				testBlock{id: "3", explicitDeps: []entities.BlockName{"1"}},
				testBlock{id: "4", deps: []entities.BlockName{"1"}},
				testBlock{id: "5", explicitDeps: []entities.BlockName{"1"}},
			},
		},
		{
			name: "oh no too many (explicit?) deps",
			blocks: []template.GraphBlock{
				testBlock{id: "1", deps: []entities.BlockName{"2", "4"}, explicitDeps: []entities.BlockName{"3", "5"}},
				testBlock{id: "2"},
				testBlock{id: "3"},
				testBlock{id: "4"},
				testBlock{id: "5"},
			},
		},
	}

	for _, tt := range testCases {
		tt := tt
		var allNames []string
		var allVars []entities.VarName
		for _, blk := range tt.blocks {
			allNames = append(allNames, string(blk.Name()))
			allVars = append(allVars, entities.VarName{Block: blk.Name(), Name: "any"})
		}
		tt.blocks = append(tt.blocks, template.GlobalOutputBlock{
			BaseBlock:   template.BaseBlock{BlockName: "output"},
			InputVars:   allVars,
			OutputNames: allNames,
		})

		t.Run(tt.name, func(t *testing.T) {
			graph := NewBlockGraph(nil)

			require.NoError(t, graph.Build(tt.blocks))
			assert.Nil(t, graph.inverseDeps[""])
		})

		for i, blk := range tt.blocks {
			tblk, ok := blk.(testBlock)
			if !ok {
				continue
			}
			tblk.explicitDeps, tblk.deps = tblk.deps, tblk.explicitDeps
			tt.blocks[i] = tblk
		}
		t.Run(tt.name+" explicit deps", func(t *testing.T) {
			graph := NewBlockGraph(nil)
			require.NoError(t, graph.Build(tt.blocks))
			assert.Nil(t, graph.inverseDeps[""])
		})
	}
}

func TestCycleBlockGraph(t *testing.T) {
	testCases := []struct {
		name   string
		blocks []template.GraphBlock
	}{
		{
			name:   "self choke",
			blocks: []template.GraphBlock{testBlock{id: "1", deps: []entities.BlockName{"1"}}},
		},
		{
			name: "two blocks",
			blocks: []template.GraphBlock{
				testBlock{id: "1", deps: []entities.BlockName{"2"}},
				testBlock{id: "2", deps: []entities.BlockName{"1"}},
			},
		},
		{
			name: "children dancing around pine tree",
			blocks: []template.GraphBlock{
				testBlock{id: "1", deps: []entities.BlockName{"2"}},
				testBlock{id: "2", deps: []entities.BlockName{"3"}},
				testBlock{id: "3", deps: []entities.BlockName{"4"}},
				testBlock{id: "4", deps: []entities.BlockName{"5"}},
				testBlock{id: "5", deps: []entities.BlockName{"1"}},
			},
		},
		{
			name: "children dancing around pine tree in reversed order",
			blocks: []template.GraphBlock{
				testBlock{id: "5", deps: []entities.BlockName{"1"}},
				testBlock{id: "4", deps: []entities.BlockName{"5"}},
				testBlock{id: "3", deps: []entities.BlockName{"4"}},
				testBlock{id: "2", deps: []entities.BlockName{"3"}},
				testBlock{id: "1", deps: []entities.BlockName{"2"}},
			},
		},
		{
			name: "three groups with one leak",
			blocks: []template.GraphBlock{
				testBlock{id: "1"},
				testBlock{id: "2"},
				testBlock{id: "3", deps: []entities.BlockName{"9"}}, // come on there is a cycle, don't you see by yourself?

				testBlock{id: "4", deps: []entities.BlockName{"1", "2"}},
				testBlock{id: "5", deps: []entities.BlockName{"3"}},
				testBlock{id: "6", deps: []entities.BlockName{"2", "3"}},

				testBlock{id: "7", deps: []entities.BlockName{"4", "5"}},
				testBlock{id: "8", deps: []entities.BlockName{"1", "4"}},
				testBlock{id: "9", deps: []entities.BlockName{"6"}},
			},
		},
	}

	for _, tt := range testCases {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			graph := NewBlockGraph(nil)
			err := graph.Build(tt.blocks)
			require.Error(t, err)
			assert.Contains(t, err.Error(), "cycle detected")
			assert.Nil(t, graph.inverseDeps[""])
		})

		for i, blk := range tt.blocks {
			tblk, ok := blk.(testBlock)
			if !ok {
				continue
			}
			tblk.explicitDeps, tblk.deps = tblk.deps, tblk.explicitDeps
			tt.blocks[i] = tblk
		}
		t.Run(tt.name+" explicit deps", func(t *testing.T) {
			graph := NewBlockGraph(nil)
			err := graph.Build(tt.blocks)
			require.Error(t, err)
			assert.Contains(t, err.Error(), "cycle detected")
			assert.Nil(t, graph.inverseDeps[""])
		})
	}
}

type varBlock struct {
	id      entities.BlockName
	inputs  []entities.VarName
	outputs []string
}

func (b varBlock) Name() entities.BlockName {
	return b.id
}

func (b varBlock) Type() string {
	return "varBlock"
}

func (b varBlock) Inputs() []entities.VarName {
	return b.inputs
}

func (b varBlock) Outputs() []string {
	return b.outputs
}

func (b varBlock) IsEmbedded() bool {
	return false
}

func (b varBlock) OutputsWithTypes(tplops.Types) ([]tplops.OutputValue, error) {
	var outputs []tplops.OutputValue
	for _, output := range b.outputs {
		outputs = append(outputs, tplops.OutputValue{
			Name: output,
			Type: configopstype.ValueTypeAny,
		})
	}
	return outputs, nil
}

func (b varBlock) GlobalOutputs() []string {
	return b.outputs
}

func (b varBlock) ExplicitDeps() []entities.BlockName {
	return nil
}

func (b varBlock) ToTemplateBlock() template.Block {
	return template.Block{}
}

func TestBlockGraphVarsValidation(t *testing.T) {
	testCases := []struct {
		name   string
		blocks []template.GraphBlock
	}{
		{
			name: "one dep global input",
			blocks: []template.GraphBlock{
				varBlock{id: "1", inputs: []entities.VarName{{Name: "hey", Block: entities.GlobalInputBlockName}}, outputs: []string{"hey"}},
				varBlock{id: "2", inputs: []entities.VarName{{Block: "1", Name: "hey"}}, outputs: nil},
			},
		},
		{
			name: "several blocks with global input",
			blocks: []template.GraphBlock{
				varBlock{id: "1", inputs: []entities.VarName{{Name: "hey", Block: entities.GlobalInputBlockName}}, outputs: nil},
				varBlock{id: "2", inputs: []entities.VarName{{Name: "hello", Block: entities.GlobalInputBlockName}}, outputs: nil},
			},
		},
		{
			name: "inputs from block and globals",
			blocks: []template.GraphBlock{
				varBlock{id: "1", inputs: nil, outputs: []string{"valid"}},
				varBlock{id: "2", inputs: []entities.VarName{
					{Block: "1", Name: "valid"},
					{Block: entities.GlobalInputBlockName, Name: "hey"},
				}, outputs: nil},
			},
		},
	}

	for _, tt := range testCases {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			graph := NewBlockGraph(nil)
			graph.inputs = []string{"hey", "hello"}
			require.NoError(t, graph.Build(tt.blocks))
			assert.Nil(t, graph.inverseDeps[""])
		})
	}
}

func TestBlockGraphBuildFail(t *testing.T) {
	testCases := []struct {
		name   string
		blocks []template.GraphBlock
	}{
		{
			name: "invalid block name",
			blocks: []template.GraphBlock{
				varBlock{id: "1", inputs: []entities.VarName{{Block: "invalid", Name: "hey"}}, outputs: nil},
			},
		},
		{
			name: "several blocks with same name",
			blocks: []template.GraphBlock{
				varBlock{id: "1", inputs: nil, outputs: nil},
				varBlock{id: "1", inputs: nil, outputs: nil},
			},
		},
		{
			name: "block doesn't have given output",
			blocks: []template.GraphBlock{
				varBlock{id: "1", inputs: []entities.VarName{{Name: "hey"}}, outputs: []string{"not hello"}},
				varBlock{id: "2", inputs: []entities.VarName{{Block: "1", Name: "hello"}}, outputs: nil},
			},
		},
		{
			name: "graph doesn't have given input",
			blocks: []template.GraphBlock{
				varBlock{id: "1", inputs: []entities.VarName{{Name: "hey"}}, outputs: nil},
				varBlock{id: "2", inputs: []entities.VarName{{Name: "hello"}}, outputs: nil},
			},
		},
	}

	for _, tt := range testCases {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			graph := NewBlockGraph(nil)
			graph.inputs = []string{"hey"}
			require.Error(t, graph.Build(tt.blocks))
			assert.Nil(t, graph.inverseDeps[entities.GlobalInputBlockName])
		})
	}
}

func TestGlobalOutputsValidationFail(t *testing.T) {
	graph := NewBlockGraph([]tplops.OutputValue{
		{
			Name: "out1",
			Type: configopstype.ValueTypeAny,
		},
		{
			Name: "out2",
			Type: configopstype.ValueTypeAny,
		},
	})
	err := graph.Build([]template.GraphBlock{
		template.GlobalOutputBlock{
			BaseBlock:   template.BaseBlock{BlockName: "1"},
			InputVars:   []entities.VarName{{Name: "out1", Block: entities.GlobalInputBlockName}},
			OutputNames: []string{"out1"},
		},
		template.GlobalOutputBlock{
			BaseBlock: template.BaseBlock{BlockName: "2"},
			InputVars: []entities.VarName{
				{Name: "out1", Block: entities.GlobalInputBlockName},
				{Name: "out2", Block: entities.GlobalInputBlockName},
			},
			OutputNames: []string{"out2", "out1"},
		},
	})
	require.Error(t, err)
	assert.Contains(t, err.Error(), `duplicate "out1" found in global outputs`)
}

func TestGlobalOutputsAndTypesValidation(t *testing.T) {
	graph := NewBlockGraph([]tplops.OutputValue{
		{
			Name: "out2",
			Type: configopstype.ValueTypeString,
		},
	})
	toIntOp, err := configops.NewToInt([]any{"$out2"})
	require.NoError(t, err)

	err = graph.Build([]template.GraphBlock{
		template.CalcBlock{
			BaseBlock: template.BaseBlock{BlockName: "3"},
			InputVars: []entities.VarName{{Name: "out2", Block: entities.GlobalInputBlockName}},
			Exprs: []configops.BaseTransformWithMarshal{{
				BaseTransform: configops.BaseTransform{
					Transform: toIntOp,
				},
			}},
			OutputNames: []string{"calc1"},
		},
		template.GlobalOutputBlock{
			BaseBlock:   template.BaseBlock{BlockName: "1"},
			InputVars:   []entities.VarName{{Name: "calc1", Block: "3"}},
			OutputNames: []string{"out3"},
		},
		template.GlobalOutputBlock{
			BaseBlock:   template.BaseBlock{BlockName: "2"},
			InputVars:   []entities.VarName{{Name: "out2", Block: entities.GlobalInputBlockName}},
			OutputNames: []string{"out4"},
		},
	})
	require.NoError(t, err)

	require.NoError(t, graph.validateOutputs())

	blocks, err := graph.Blocks()
	require.NoError(t, err)
	require.Len(t, blocks, 3, "2 global output blocks and 1 calc block expected")

	expectedTypes := map[entities.BlockName][]tplops.OutputValue{
		"1": {{
			Name: "out3",
			Type: configopstype.ValueTypeInt,
		}},
		"2": {{
			Name: "out4",
			Type: configopstype.ValueTypeString,
		}},
		"3": {{
			Name: "calc1",
			Type: configopstype.ValueTypeInt,
		}},
	}

	var outputBlocksChecked int
	for _, block := range blocks {
		if types, ok := expectedTypes[block.Name]; ok {
			assert.Equal(t, types, block.Outputs)
			outputBlocksChecked++
		}
	}
	assert.Equal(t, 3, outputBlocksChecked, "both global output blocks and calc block must be present in Blocks()")
}

func TestGlobalOutputsAndTypesValidationFail(t *testing.T) {
	graph := NewBlockGraph([]tplops.OutputValue{
		{
			Name: "out2",
			Type: configopstype.ValueTypeString,
		},
	})
	eqOp, err := configops.NewEq([]any{"$out2", 1})
	require.NoError(t, err)

	err = graph.Build([]template.GraphBlock{
		template.ValidationBlock{
			BaseBlock: template.BaseBlock{BlockName: "3"},
			InputVars: []entities.VarName{{Name: "out2", Block: entities.GlobalInputBlockName}},
			Exprs: []configops.BaseLogicWithMarshal{{
				BaseLogic: configops.BaseLogic{
					Logic: eqOp,
				},
			}},
		},
	})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "first value type is string but second value type is int")

	graph = NewBlockGraph([]tplops.OutputValue{
		{
			Name: "out2",
			Type: configopstype.ValueTypeString,
		},
	})
	addOp, err := configops.NewAdd([]any{"$out2", 1})
	require.NoError(t, err)

	err = graph.Build([]template.GraphBlock{
		template.CalcBlock{
			BaseBlock:   template.BaseBlock{BlockName: "3"},
			InputVars:   []entities.VarName{{Name: "out2", Block: entities.GlobalInputBlockName}},
			OutputNames: []string{"abc"},
			Exprs: []configops.BaseTransformWithMarshal{{
				BaseTransform: configops.BaseTransform{
					Transform: addOp,
				},
			}},
		},
		template.GlobalOutputBlock{
			BaseBlock:   template.BaseBlock{BlockName: "1"},
			InputVars:   []entities.VarName{{Name: "abc", Block: "3"}},
			OutputNames: []string{"out3"},
		},
	})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "contains 2 different types: string and int")
}

func TestUnusedBlockVarsValidation(t *testing.T) {
	graph := NewBlockGraph(nil)
	err := graph.Build([]template.GraphBlock{
		testBlock{id: "1", deps: []entities.BlockName{"2"}},

		testBlock{id: "2", deps: []entities.BlockName{"3"}},
		testBlock{id: "3"},

		testBlock{id: "4"},
	})
	require.Error(t, err)
	assert.Contains(t, err.Error(), `1.any`)
	assert.Contains(t, err.Error(), `4.any`)
	assert.NotContains(t, err.Error(), `3.any`)
	assert.NotContains(t, err.Error(), `2.any`)
}

func TestUnusedGlobalVarsValidation(t *testing.T) {
	graph := NewBlockGraph([]tplops.OutputValue{{Name: "any"}})
	err := graph.Build([]template.GraphBlock{
		template.GlobalOutputBlock{
			BaseBlock:   template.BaseBlock{BlockName: "output"},
			InputVars:   []entities.VarName{{Block: "1", Name: "any"}},
			OutputNames: []string{"out1"},
		},

		testBlock{id: "1", deps: []entities.BlockName{entities.GlobalInputBlockName}},
	})
	require.NoError(t, err)

	graph = NewBlockGraph([]tplops.OutputValue{{Name: "any"}, {Name: "extraInput"}})
	err = graph.Build([]template.GraphBlock{
		template.GlobalOutputBlock{
			BaseBlock:   template.BaseBlock{BlockName: "output"},
			InputVars:   []entities.VarName{{Block: "1", Name: "any"}},
			OutputNames: []string{"out1"},
		},

		testBlock{id: "1", deps: []entities.BlockName{entities.GlobalInputBlockName}},
	})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "extraInput")
}

func TestInvalidEmbedType(t *testing.T) {
	filesPrefix := filepath.Join("templates", "embed")
	testStorage := storagetest.NewMemoryTestTemplateStorage()

	file, err := example.TemplatesFS.ReadFile(filepath.Join(filesPrefix, "simple.yaml"))
	require.NoError(t, err)

	var b yaml2.BlockTree
	require.NoError(t, yaml.Unmarshal(file, &b))

	_, err = testStorage.Insert(context.Background(), "simple", string(file), nil, nil)
	require.NoError(t, err)

	file, err = example.TemplatesFS.ReadFile(filepath.Join(filesPrefix, "invalid_type.yaml"))
	require.NoError(t, err)

	require.NoError(t, yaml.Unmarshal(file, &b))

	enriched, err := b.EmbedTemplates(context.Background(), "invalid_type", testStorage)
	require.NoError(t, err)

	_, err = NewBlockGraphFromYAML(enriched)
	require.Error(t, err)
	assert.Contains(t, err.Error(), `is expected to match type string`)
}
