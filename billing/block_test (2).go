package yaml

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/configops"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
)

func TestIntegrationBlockValidate(t *testing.T) {
	blk := IntegrationBlock{
		baseInputBlock: baseInputBlock{baseBlock: baseBlock{BlockName: ""}},
		Integration:    "queue.action",
	}
	err := blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "block name cannot be empty")

	blk = IntegrationBlock{
		baseInputBlock: baseInputBlock{
			Inputs:    []entities.VarName{{Name: "any", Block: entities.GlobalInputBlockName}},
			baseBlock: baseBlock{BlockName: "2"},
		},
		Integration: "queue.action",
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "must reference to root block inputs")

	blk = IntegrationBlock{
		baseInputBlock: baseInputBlock{
			Inputs:    []entities.VarName{{Name: "any", Block: "another_block"}},
			baseBlock: baseBlock{BlockName: "2"},
		},
		Integration: "queue",
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "must be in format")

	blk = IntegrationBlock{
		baseInputBlock: baseInputBlock{
			Inputs:    []entities.VarName{{Name: "any", Block: "another_block"}},
			baseBlock: baseBlock{BlockName: "2"},
		},
		Outputs:     []tplops.OutputValue{{Name: "one"}, {Name: "one"}},
		Integration: "queue.action",
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `output "one" is specified twice`)

	blk = IntegrationBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "any", Block: "another_block"},
				{Name: "any", Block: "another_block"},
			},
			baseBlock: baseBlock{BlockName: "2"},
		},
		Outputs:     []tplops.OutputValue{{Name: "one"}},
		Integration: "queue.action",
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `input "another_block.any" is specified twice`)

	blk = IntegrationBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "any", Block: "another_block"},
			},
			Repeat:    &entities.BlockRepeat{Var: entities.VarName{Name: "any", Block: "another_block"}},
			baseBlock: baseBlock{BlockName: "2"},
		},
		Outputs:     []tplops.OutputValue{{Name: "one"}},
		Integration: "queue.action",
	}
	blk.Prepare()
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `input "another_block.any" is specified twice`)

	blk = IntegrationBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "any", Block: "another_block"},
			},
			baseBlock: baseBlock{BlockName: "2"},
		},
		Outputs:     []tplops.OutputValue{{}},
		Integration: "queue.action",
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "output in integration block cannot have empty name")

	blk = IntegrationBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "any", Block: "another_block"},
			},
			baseBlock: baseBlock{BlockName: "2"},
		},
		Outputs:     []tplops.OutputValue{{Name: "one"}},
		Integration: "queue.action",
		Args: xyaml.MapStr{
			"val1": "val2",
			"val2": 135,
			"nested": xyaml.MapStr{
				"val3": "$fail.var",
			},
		},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "but inputs don't have it")

	blk = IntegrationBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "any", Block: "another_block"},
			},
			Repeat:    &entities.BlockRepeat{Var: entities.VarName{Name: "any2", Block: "another_block"}},
			baseBlock: baseBlock{BlockName: "2"},
		},
		Outputs:     []tplops.OutputValue{{Name: "one"}},
		Integration: "queue.action",
		Args: xyaml.MapStr{
			"val1": "val2",
			"val2": 135,
			"val3": "$another_block.any",
			"nested": xyaml.MapStr{
				"val4": "$another_block.any",
			},
			"nested_array": []any{
				"$another_block.any", 135, "228",
			},
		},
	}
	blk.Prepare()
	err = blk.Validate()
	require.NoError(t, err)
}

func TestCalcBlockValidate(t *testing.T) {
	blk := CalcBlock{
		baseInputBlock: baseInputBlock{baseBlock: baseBlock{BlockName: ""}},
		Exprs: []Expression{{Var: "123", Expr: configops.BaseTransformWithMarshal{
			BaseTransform: configops.BaseTransform{Transform: configops.Add{}},
		}}},
	}
	err := blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "block name cannot be empty")

	blk = CalcBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
				{Name: "two", Block: "2"},
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Exprs: []Expression{{Var: "123", Expr: configops.BaseTransformWithMarshal{
			BaseTransform: configops.BaseTransform{Transform: configops.Add{}},
		}}},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `input "2.one" is specified twice`)

	blk = CalcBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Exprs: []Expression{{Var: "123", Expr: configops.BaseTransformWithMarshal{
			BaseTransform: configops.BaseTransform{Transform: configops.Add{}},
		}}, {Var: "123", Expr: configops.BaseTransformWithMarshal{
			BaseTransform: configops.BaseTransform{Transform: configops.Add{}},
		}}},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `var "123" is specified twice`)

	blk = CalcBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Exprs: []Expression{},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "calc block must have at least 1 expression")

	blk = CalcBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Exprs: []Expression{{Expr: configops.BaseTransformWithMarshal{
			BaseTransform: configops.BaseTransform{Transform: configops.Add{}},
		}}},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "var in calc block cannot have empty name")

	blk = CalcBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Exprs: []Expression{{Var: "123", Expr: configops.BaseTransformWithMarshal{
			BaseTransform: configops.BaseTransform{Transform: configops.Add{}},
		}}},
	}
	err = blk.Validate()
	require.NoError(t, err)
}

func TestValidationBlockValidate(t *testing.T) {
	blk := ValidationBlock{
		baseInputBlock: baseInputBlock{baseBlock: baseBlock{BlockName: ""}},
		Exprs: []ValidateExpression{{Expr: configops.BaseLogicWithMarshal{
			BaseLogic: configops.BaseLogic{Logic: configops.Eq{}},
		}}},
	}
	err := blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "block name cannot be empty")

	blk = ValidationBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
				{Name: "two", Block: "2"},
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Exprs: []ValidateExpression{{Expr: configops.BaseLogicWithMarshal{
			BaseLogic: configops.BaseLogic{Logic: configops.Eq{}},
		}}},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `input "2.one" is specified twice`)

	blk = ValidationBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Exprs: []ValidateExpression{},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "validate block must have at least 1 expression")

	blk = ValidationBlock{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Exprs: []ValidateExpression{{Expr: configops.BaseLogicWithMarshal{
			BaseLogic: configops.BaseLogic{Logic: configops.Eq{}},
		}}},
	}
	err = blk.Validate()
	require.NoError(t, err)
}

func TestRootBlockValidate(t *testing.T) {
	blk := RootBlock{
		baseBlock: baseBlock{BlockName: ""},
	}
	err := blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "block name cannot be empty")

	blk = RootBlock{
		baseBlock: baseBlock{BlockName: "2"},
		Inputs: []GlobalInput{
			{YAMLValue: tplops.OutputValue{Name: "one"}},
			{YAMLValue: tplops.OutputValue{Name: "two"}},
			{YAMLValue: tplops.OutputValue{Name: "one"}},
		},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `input "one" is specified twice`)

	blk = RootBlock{
		baseBlock: baseBlock{BlockName: "2"},
		Outputs: []GlobalOutput{
			{Name: "two", Var: entities.VarName{Name: "1", Block: "1"}},
			{Name: "two", Var: entities.VarName{Name: "2", Block: "6"}},
		},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `output "two" is specified twice`)

	blk = RootBlock{
		baseBlock: baseBlock{BlockName: "2"},
		Inputs: []GlobalInput{
			{YAMLValue: tplops.OutputValue{Name: "one"}},
			{YAMLValue: tplops.OutputValue{Name: "two"}},
		},
		Outputs: []GlobalOutput{{Name: "", Var: entities.VarName{Name: "1", Block: "1"}}},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "output cannot have empty name")

	blk = RootBlock{
		baseBlock: baseBlock{BlockName: "2"},
		Inputs: []GlobalInput{
			{YAMLValue: tplops.OutputValue{Name: "one"}},
			{YAMLValue: tplops.OutputValue{Name: "two"}},
		},
		Outputs: []GlobalOutput{{Name: "two", Var: entities.VarName{Name: "1", Block: "1"}}},
	}
	err = blk.Validate()
	require.NoError(t, err)
}

func TestEmbedBlockValidate(t *testing.T) {
	blk := Embed{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Template: EmbedTemplate{
			Code:    "abc",
			Version: 0,
		},
	}
	err := blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "template version must")

	blk = Embed{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Template: EmbedTemplate{
			Code:    "",
			Version: 10,
		},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "template code cannot")

	blk = Embed{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Template: EmbedTemplate{
			Code:    "lalala",
			Version: 10,
		},
	}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "is specified twice")

	blk = Embed{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			Repeat:    &entities.BlockRepeat{Var: entities.VarName{Name: "two", Block: "2"}},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Template: EmbedTemplate{
			Code:    "lalala",
			Version: 10,
		},
	}
	blk.Prepare()
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "cannot be repeated")

	blk = Embed{
		baseInputBlock: baseInputBlock{
			Inputs: []entities.VarName{
				{Name: "one", Block: "2"},
			},
			baseBlock: baseBlock{BlockName: "1"},
		},
		Template: EmbedTemplate{
			Code:    "lalala",
			Version: 10,
		},
	}
	err = blk.Validate()
	require.NoError(t, err)
}

func TestBaseBlockValidation(t *testing.T) {
	blk := baseBlock{BlockName: ""}
	err := blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), "block name cannot be empty")

	if entities.GlobalInputBlockName != "" {
		blk = baseBlock{BlockName: entities.GlobalInputBlockName}
		err = blk.Validate()
		require.Error(t, err)
		assert.Contains(t, err.Error(), "block name cannot be equal to internal technical name")
	}

	blk = baseBlock{BlockName: "ok", Depends: []entities.BlockName{"one", "two", "one"}}
	err = blk.Validate()
	require.Error(t, err)
	assert.Contains(t, err.Error(), `dependency "one" is specified twice`)

	blk = baseBlock{BlockName: "ok", Depends: []entities.BlockName{"one", "two", "three"}}
	err = blk.Validate()
	require.NoError(t, err)
}
