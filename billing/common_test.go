package executors

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	mock2 "a.yandex-team.ru/billing/configshop/pkg/core/runtime/pipeline/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/configops"
	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
)

func TestFillBlockArgs(t *testing.T) {
	testCases := []struct {
		name        string
		blk         *runtime.Block
		args        xyaml.MapStr
		expected    xyaml.MapStr
		errContains string
	}{
		{
			name: "empty args",
		},
		{
			name: "one argument one value",
			blk: &runtime.Block{Inputs: map[entities.VarName]any{
				entities.VarName{Name: "name", Block: "1"}: 10,
			}},
			args: map[string]any{
				"123": "$1.name",
				"758": "anything",
			},
			expected: map[string]any{
				"123": 10,
				"758": "anything",
			},
		},
		{
			name: "nested: slice and dict",
			blk: &runtime.Block{Inputs: map[entities.VarName]any{
				entities.VarName{Name: "name", Block: "1"}:  10,
				entities.VarName{Name: "name2", Block: "1"}: 15,
			}},
			args: map[string]any{
				"123": xyaml.MapStr{
					"89": "$1.name",
				},
				"758": []any{"$1.name2"},
			},
			expected: map[string]any{
				"123": xyaml.MapStr{
					"89": 10,
				},
				"758": []any{15},
			},
		},
		{
			name: "global inputs",
			blk: &runtime.Block{Inputs: map[entities.VarName]any{
				entities.VarName{Name: "name", Block: entities.GlobalInputBlockName}: 10,
			}},
			args: map[string]any{
				"123": xyaml.MapStr{
					"89": "$name",
				},
				"758": []any{"name2"},
			},
			expected: map[string]any{
				"123": xyaml.MapStr{
					"89": 10,
				},
				"758": []any{"name2"},
			},
		},
		{
			name: "map key is variable",
			blk: &runtime.Block{Inputs: map[entities.VarName]any{
				entities.VarName{Name: "name", Block: entities.GlobalInputBlockName}: "10",
			}},
			args: map[string]any{
				"123": xyaml.MapStr{
					"$name": xyaml.MapStr{
						"value": "$name",
					},
				},
			},
			expected: map[string]any{
				"123": xyaml.MapStr{
					"10": xyaml.MapStr{
						"value": "10",
					},
				},
			},
		},
		{
			name: "map key is not string variable",
			blk: &runtime.Block{Inputs: map[entities.VarName]any{
				entities.VarName{Name: "name", Block: entities.GlobalInputBlockName}: 10,
			}},
			args: map[string]any{
				"123": xyaml.MapStr{
					"$name": "$name",
				},
			},
			errContains: "must be of string type",
		},
		{
			name: "map key already exists",
			blk: &runtime.Block{Inputs: map[entities.VarName]any{
				entities.VarName{Name: "name", Block: entities.GlobalInputBlockName}: "10",
			}},
			args: map[string]any{
				"123": xyaml.MapStr{
					"$name": "$name",
					"10":    "20",
				},
			},
			errContains: "key already exists",
		},
	}

	for _, testCase := range testCases {
		tt := testCase
		t.Run(tt.name, func(t *testing.T) {
			err := FillBlockArgs(tt.blk, tt.args)
			if tt.errContains != "" {
				require.Error(t, err)
				assert.Contains(t, err.Error(), tt.errContains)
			} else {
				require.NoError(t, err)
				assert.Equal(t, tt.expected, tt.args)
			}
		})
	}
}

func TestOutputBlock(t *testing.T) {
	ctrl := gomock.NewController(t)
	p := mock2.NewMockPipeline(ctrl)
	p.EXPECT().FinishBlock(gomock.Any(), runtime.ExecuteOutput{
		BlockID: 123,
		State: map[string]any{
			"out1": "123",
			"out2": "456",
		},
		Error: nil,
	}).Return(nil)

	outputFunc := OutputBlockExecFunc(p)

	err := outputFunc(context.Background(), &runtime.Block{
		ID:      123,
		GraphID: 10,
		Name:    "outputty",
		Type:    "output",
		InputVars: []tplops.InputValue{
			{Name: entities.VarName{Name: "name1", Block: "1"}, Type: configopstype.ValueTypeString},
			{Name: entities.VarName{Name: "name2", Block: "1"}, Type: configopstype.ValueTypeString},
		},
		Inputs: map[entities.VarName]any{
			entities.VarName{Name: "name1", Block: "1"}: "123",
			entities.VarName{Name: "name2", Block: "1"}: "456",
		},
		OutputNames: []tplops.OutputValue{
			{Name: "out1", Type: configopstype.ValueTypeString},
			{Name: "out2", Type: configopstype.ValueTypeString},
		},
		IsOutput: true,
	}, nil)
	require.NoError(t, err)
}

func TestCalcBlock(t *testing.T) {
	ctrl := gomock.NewController(t)
	p := mock2.NewMockPipeline(ctrl)
	p.EXPECT().FinishBlock(gomock.Any(), runtime.ExecuteOutput{
		BlockID: 123,
		State: map[string]any{
			"calcOut": "123456",
			"nilCalc": tplops.NilValue{},
		},
		Error: nil,
	}).Return(nil)

	calcFunc := CalcBlockExecFunc(p)

	addOp, err := configops.NewAdd([]any{"$1.name1", "$1.name2"})
	require.NoError(t, err)

	addNilOp, err := configops.NewAdd([]any{"$1.name2", "$1.name3"})
	require.NoError(t, err)

	err = calcFunc(context.Background(), &runtime.Block{
		ID:      123,
		GraphID: 10,
		Name:    "calcy",
		Type:    "calc",
		InputVars: []tplops.InputValue{
			{Name: entities.VarName{Name: "name1", Block: "1"}, Type: configopstype.ValueTypeString},
			{Name: entities.VarName{Name: "name2", Block: "1"}, Type: configopstype.ValueTypeString},
			{Name: entities.VarName{Name: "name3", Block: "1"}, Type: configopstype.ValueTypeString},
		},
		Inputs: map[entities.VarName]any{
			{Name: "name1", Block: "1"}: "123",
			{Name: "name2", Block: "1"}: "456",
			{Name: "name3", Block: "1"}: tplops.NilValue{},
		},
		OutputNames: []tplops.OutputValue{
			{Name: "calcOut", Type: configopstype.ValueTypeString},
			{Name: "nilCalc", Type: configopstype.ValueTypeString},
		},
		Exprs: []configops.BaseTransformWithMarshal{
			{
				BaseTransform: configops.BaseTransform{
					Transform: addOp,
				},
			},
			{
				BaseTransform: configops.BaseTransform{
					Transform: addNilOp,
				},
			},
		},
	}, nil)
	require.NoError(t, err)
}

func TestValidationBlock(t *testing.T) {
	ctrl := gomock.NewController(t)
	p := mock2.NewMockPipeline(ctrl)

	nullableError := false

	p.EXPECT().FinishBlock(gomock.Any(), gomock.Any()).Times(2).
		DoAndReturn(func(ctx context.Context, b runtime.ExecuteOutput) error {
			require.Equal(t, nullableError, b.Error == nil)

			if !nullableError {
				err := b.Error.Args["error"].(string)
				assert.NotEmpty(t, err)
				assert.Equal(t, err, "validation 0 failed")
			}

			return nil
		})

	calcFunc := ValidateBlockExecFunc(p)

	eqOp, err := configops.NewEq([]any{"$1.name1", "$1.name2"})
	require.NoError(t, err)

	nilEqOp, err := configops.NewEq([]any{"$1.name2", "$1.name3"})
	require.NoError(t, err)

	block := runtime.Block{
		ID:      123,
		GraphID: 10,
		Name:    "validaty",
		Type:    "validate",
		InputVars: []tplops.InputValue{
			{Name: entities.VarName{Name: "name1", Block: "1"}, Type: configopstype.ValueTypeString},
			{Name: entities.VarName{Name: "name2", Block: "1"}, Type: configopstype.ValueTypeString},
			{Name: entities.VarName{Name: "name3", Block: "1"}, Type: configopstype.ValueTypeString},
		},
		Inputs: map[entities.VarName]any{
			{Name: "name1", Block: "1"}: "123",
			{Name: "name2", Block: "1"}: "456",
			{Name: "name3", Block: "1"}: tplops.NilValue{},
		},
		ValidateExprs: []configops.BaseLogicWithMarshal{{
			BaseLogic: configops.BaseLogic{
				Logic: eqOp,
			},
		}, {
			BaseLogic: configops.BaseLogic{
				Logic: nilEqOp,
			},
		}},
	}

	err = calcFunc(context.Background(), &block, nil)
	require.NoError(t, err)

	block.ValidateExprs = []configops.BaseLogicWithMarshal{{
		BaseLogic: configops.BaseLogic{
			Logic: nilEqOp,
		},
	}}

	nullableError = true

	err = calcFunc(context.Background(), &block, nil)
	require.NoError(t, err)
}
